package com.arempter.virusscanner

import com.arempter.virusscanner.config.ServerSettings
import com.arempter.virusscanner.provider.S3
import com.arempter.virusscanner.scanengine.ClamAVClient
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContext, Future}

case class S3ObjectMeta(key: String, value: String)

class ScannerAPI(val bucket: String, val key: String)(implicit val ec: ExecutionContext, val serverSettings: ServerSettings)
  extends S3 with ClamAVClient with LazyLogging {

  private def isClean(scanResult: String): Boolean =
    scanResult.contains("OK") || !scanResult.contains("FOUND")

  def scan: Future[String] = Future {
    val result = scanStream(getS3Object(bucket, s"${serverSettings.scanDirectoryPrefix}/$key"))
    isClean(result) match {
      case true  =>
        logger.debug("Scanned file is ok, moving to root of bucket")
        moveS3ObjectTo(bucket, key, serverSettings.scanDirectoryPrefix, "clean")
      case false =>
        logger.debug("Scanned file is infected, moving to contained folder")
        moveS3ObjectTo(bucket, key, "contained", "infected")
    }
  }

}
