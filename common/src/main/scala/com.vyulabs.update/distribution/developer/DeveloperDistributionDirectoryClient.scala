package com.vyulabs.update.distribution.developer

import java.io._
import java.net.URL

import com.typesafe.config.Config
import com.vyulabs.update.common.Common.{ClientName, VmInstanceId, ServiceName}
import com.vyulabs.update.common.ServiceInstanceName
import com.vyulabs.update.config.ClientConfig
import com.vyulabs.update.info.{DesiredVersions, ServicesVersions, VersionsInfo}
import com.vyulabs.update.distribution.DistributionDirectoryClient
import com.vyulabs.update.state.VmInstancesState
import org.slf4j.Logger
import com.vyulabs.update.config.ClientConfigJson._
import com.vyulabs.update.info.DesiredVersionsJson._
import com.vyulabs.update.info.ServicesVersionsJson._
import com.vyulabs.update.state.VmInstancesStateJson._
import spray.json._

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.04.19.
  * Copyright FanDate, Inc.
  */
class DeveloperDistributionDirectoryClient(val url: URL)(implicit log: Logger) extends DistributionDirectoryClient(url)
    with DeveloperDistributionWebPaths {

  def downloadClientConfig(): Option[ClientConfig] = {
    log.info(s"Download client config")
    val url = makeUrl(getDownloadClientConfigPath())
    downloadToJson(url).map(_.convertTo[ClientConfig])
  }

  def downloadDesiredVersions(common: Boolean = false): Option[DesiredVersions] = {
    log.info(s"Download desired versions")
    val url = makeUrl(getDownloadDesiredVersionsPath(common))
    downloadToJson(url).map(_.convertTo[DesiredVersions])
  }

  def uploadTestedVersions(testedVersions: ServicesVersions): Boolean = {
    log.info(s"Upload tested versions")
    uploadFromJson(makeUrl(uploadTestedVersionsPath),
      testedVersionsName, uploadTestedVersionsPath, testedVersions.toJson)
  }

  def uploadVmInstancesState(instancesState: VmInstancesState): Boolean = {
    uploadFromJson(makeUrl(getUploadInstancesStatePath()),
      instancesStateName, uploadInstancesStatePath, instancesState.toJson)
  }

  def uploadServiceFault(serviceName: ServiceName, faultFile: File): Boolean = {
    uploadFromFile(makeUrl(getUploadServiceFaultPath(serviceName)), serviceFaultName, faultFile)
  }
}
