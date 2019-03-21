package com.arempter.virusscanner.provider

import java.time.Instant

import com.amazonaws.auth.{ AWSCredentials, AWSStaticCredentialsProvider, BasicAWSCredentials }
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.model.{ CopyObjectRequest, ObjectMetadata, S3ObjectInputStream }
import com.amazonaws.services.s3.{ AmazonS3, AmazonS3ClientBuilder }
import com.arempter.virusscanner.config.ServerSettings
import com.arempter.virusscanner.data.S3ObjectMeta

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

trait S3 {
  implicit val ec: ExecutionContext
  implicit val serverSettings: ServerSettings

  private def s3Client: AmazonS3 = {
    val credentials: AWSCredentials = new BasicAWSCredentials(serverSettings.s3accessKey, serverSettings.s3secretKey);

    val client: AmazonS3ClientBuilder = AmazonS3ClientBuilder
      .standard()
      .withCredentials(new AWSStaticCredentialsProvider(credentials))
      .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(serverSettings.s3endpoint, serverSettings.s3region));

    client.setPathStyleAccessEnabled(true)
    client.build()
  }

  def getS3Object(bucket: String, key: String): S3ObjectInputStream =
    Try(s3Client.getObject(bucket, key).getObjectContent) match {
      case Success(r)  => r
      case Failure(ex) => throw ex
    }

  import scala.collection.JavaConverters._

  def getS3ObjectMeta(bucket: String, key: String): Future[List[S3ObjectMeta]] =
    Future(
      s3Client.getObjectMetadata(bucket, key)
        .getUserMetadata
        .asScala
        .map { case (k, v) => S3ObjectMeta(k, v) }
        .toList
    )

  def moveS3ObjectTo(bucket: String, key: String, prefix: String, result: String): String = {
    val copyRequest = prefix match {
      case "contained" =>
        new CopyObjectRequest(bucket, s"${serverSettings.scanDirectoryPrefix}/$key", bucket, s"$prefix/$key")
      case "upload" =>
        new CopyObjectRequest(bucket, s"$prefix/$key", bucket, key)
    }
    val newMetadata = new ObjectMetadata()
    newMetadata.addUserMetadata("scanresult", result)
    newMetadata.addUserMetadata("scandate", Instant.now().toString)
    copyRequest.setMetadataDirective("REPLACE")
    copyRequest.setNewObjectMetadata(newMetadata)

    Try(s3Client.copyObject(copyRequest)) match {
      case Success(r) =>
        removeScannedS3Object(bucket, s"${serverSettings.scanDirectoryPrefix}/$key")
        s"ETAG: ${r.getETag}, scan status: $result"
      case Failure(ex) => throw ex
    }
  }

  def removeScannedS3Object(bucket: String, key: String): Boolean = {
    s3Client.deleteObject(bucket, key)
    s3Client.doesObjectExist(bucket, key)
  }

}
