package com.vyulabs.update.tests

import com.vyulabs.libs.git.GitRepository
import com.vyulabs.update.builder.BuilderMain
import com.vyulabs.update.builder.config.{RepositoryConfig, SourcesConfig}
import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.common.Common.TaskId
import com.vyulabs.update.common.config._
import com.vyulabs.update.common.distribution.client.graphql.AdministratorGraphqlCoder.{administratorMutations, administratorSubscriptions}
import com.vyulabs.update.common.distribution.client.{DistributionClient, HttpClientImpl, SyncDistributionClient, SyncSource}
import com.vyulabs.update.common.distribution.server.{DistributionDirectory, SettingsDirectory}
import com.vyulabs.update.common.utils.IoUtils
import com.vyulabs.update.common.version.{ClientDistributionVersion, ClientVersion, DeveloperDistributionVersion, DeveloperVersion}
import org.slf4j.LoggerFactory
import spray.json.DefaultJsonProtocol._

import java.io.File
import java.net.URL
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, ExecutionContext, Promise}

class TestLifecycle {
  implicit val log = LoggerFactory.getLogger(this.getClass)

  def run()(implicit executionContext: ExecutionContext): Unit = {
    println("====================================== Setup and start distribution server")
    println()
    val distributionName = "test-distribution"
    val distributionDir = new DistributionDirectory(Files.createTempDirectory("distribution").toFile)
    val distributionReady = Promise[Unit]()
    new Thread {
      override def run(): Unit = {
        try {
          BuilderMain.main(Array("buildDistribution", "cloudProvider=None",
            s"distributionDirectory=${distributionDir.directory.toString}", s"distributionName=${distributionName}", "distributionTitle=Test distribution server",
            "mongoDbName=test", "author=ak", "test=true"))
          distributionReady.success()
        } finally {
          log.error("Distribution server is terminated")
        }
      }
    }.start()
    Await.ready(distributionReady.future, FiniteDuration(5, TimeUnit.MINUTES))

    println("====================================== Configure test service")
    println()
    val testServiceName = "test"
    val serviceSourceDir = Files.createTempDirectory("service").toFile
    val git = GitRepository.createBareRepository(serviceSourceDir).getOrElse {
      sys.error(s"Can't create Git repository in the file ${serviceSourceDir}")
    }
    val buildConfig = BuildConfig(None, Seq(CopyFileConfig("sourceScript.sh", "runScript.sh", None, None)))
    val installConfig = InstallConfig(None, None, Some(RunServiceConfig("/bin/sh", Some(Seq("./runScript.sh")), None, None, None, None, None, None)))
    val updateConfig = UpdateConfig(Map.empty + (testServiceName -> ServiceUpdateConfig(buildConfig, Some(installConfig))))
    if (!IoUtils.writeJsonToFile(new File(serviceSourceDir, Common.UpdateConfigFileName), updateConfig)) {
      sys.error(s"Can't write update config file")
    }
    val scriptContent = "echo \"output line1\n\""
    if (!IoUtils.writeBytesToFile(new File(serviceSourceDir, "runScript.sh"), scriptContent.getBytes("utf8"))) {
      sys.error(s"Can't write script")
    }
    val settingsDirectory = new SettingsDirectory(distributionDir.getBuilderDir(), distributionName)
    val sourcesConfig = IoUtils.readFileToJson[SourcesConfig](settingsDirectory.getSourcesFile()).getOrElse {
      sys.error(s"Can't read sources config file")
    }
    sourcesConfig.sources + (testServiceName -> Seq(RepositoryConfig(git.getUrl(), None, None)))
    if (!IoUtils.writeJsonToFile(settingsDirectory.getSourcesFile(), sourcesConfig)) {
      sys.error(s"Can't write sources config file")
    }

    val distributionUrl = new URL("http://admin:admin@localhost")
    val distributionClient = new SyncDistributionClient(
      new DistributionClient(new HttpClientImpl(distributionUrl)), FiniteDuration(60, TimeUnit.SECONDS))

    println("====================================== Build developer version of test service")
    println()
    val taskId = distributionClient.graphqlRequest(administratorMutations.buildDeveloperVersion(testServiceName, DeveloperVersion.initialVersion)).getOrElse {
      sys.error("Can't execute build developer version task")
    }
    if (!subscribeTask(distributionClient, taskId)) {
      sys.error("Execution of build developer version task error")
    }

    println("====================================== Build client version of test service")
    println()
    val taskId1 = distributionClient.graphqlRequest(administratorMutations.buildClientVersion(testServiceName,
        DeveloperDistributionVersion(distributionName, DeveloperVersion.initialVersion),
        ClientDistributionVersion(distributionName, ClientVersion(DeveloperVersion.initialVersion)))).getOrElse {
      sys.error("Can't execute build client version task")
    }
    if (!subscribeTask(distributionClient, taskId1)) {
      sys.error("Execution of build client version task error")
    }
  }

  private def subscribeTask(distributionClient: SyncDistributionClient[SyncSource], taskId: TaskId): Boolean = {
    val source = distributionClient.graphqlSubRequest(administratorSubscriptions.subscribeTaskLogs(taskId)).getOrElse {
      sys.error("Can't subscribe build developer version task")
    }
    while (true) {
      val log = source.next().getOrElse {
        sys.error("Unexpected end of subscription")
      }
      println(log.logLine.line.message)
      for (terminationStatus <- log.logLine.line.terminationStatus) {
        println(s"Build developer version termination status is ${terminationStatus}")
        return terminationStatus
      }
    }
    false
  }
}
