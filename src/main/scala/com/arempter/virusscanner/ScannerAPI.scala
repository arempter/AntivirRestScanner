package com.arempter.virusscanner

import com.arempter.client.data.{ ObjectClean, ObjectInfected }
import com.arempter.client.provider.ClamAVClient
import com.arempter.virusscanner.config.ServerSettings
import com.arempter.virusscanner.provider.S3
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ ExecutionContext, Future }

case class S3ObjectMeta(key: String, value: String)

class ScannerAPI(val bucket: String, val key: String)(implicit val ec: ExecutionContext, val serverSettings: ServerSettings)
  extends S3 with LazyLogging {

  def scan: Future[String] = {
    ClamAVClient().scanStream(getS3Object(bucket, s"${serverSettings.scanDirectoryPrefix}/$key")).map {
      case ObjectClean =>
        logger.debug("Scanned file is ok, moving to root of bucket")
        moveS3ObjectTo(bucket, key, serverSettings.scanDirectoryPrefix, "clean")
      case ObjectInfected =>
        logger.debug("Scanned file is infected, moving to contained folder")
        moveS3ObjectTo(bucket, key, "contained", "infected")
    }
  }

}
