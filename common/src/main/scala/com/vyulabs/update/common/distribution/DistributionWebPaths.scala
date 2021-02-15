package com.vyulabs.update.common.distribution

import java.net.URLEncoder

/**
 * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.04.19.
 * Copyright FanDate, Inc.
 */
object DistributionWebPaths {

  val graphqlPathPrefix = "graphql"
  val interactiveGraphqlPathPrefix = "graphiql"
  val uiStaticPathPrefix = "ui"

  val pingPath = "ping"

  val imagePathPrefix = "image"

  val developerVersionPath = "developer-version"
  val clientVersionPath = "client-version"

  val faultReportPath = "fault-report"

  val imageField = "image"
  val faultReportField = "fault-report"

  def encode(pathSegment: String): String = URLEncoder.encode(pathSegment, "utf8")
}
