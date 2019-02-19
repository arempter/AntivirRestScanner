package com.arempter.virusscanner.routes

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.arempter.virusscanner.ScannerAPI
import com.arempter.virusscanner.data.S3ObjectMeta
import com.arempter.virusscanner.provider.S3

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait ScannerRoutes extends S3 {
  def getS3ObjectMeta(bucket: String, key: String): Future[List[S3ObjectMeta]]

  implicit val ec: ExecutionContext

  case class S3Object(bucket: String, key: String)
  case class ResponseMessage(response: String)
  implicit val responseMessageFormat = jsonFormat1(ResponseMessage)
  implicit val s3ObjectFormat = jsonFormat2(S3Object)
  implicit val s3ObjectMetaFormat = jsonFormat2(S3ObjectMeta)

  def allRoutes: Route = scanObject ~ getObjectStatus

  def scanObject: Route =
    post {
      path("scan") {
        entity(as[S3Object]) { s3object =>
          onComplete(new ScannerAPI(s3object.bucket, s3object.key).scan) {
            case Success(r) => complete(ResponseMessage("Scan finished: " + r))
            case Failure(ex) => complete(ResponseMessage("Scan failed: " + ex.getMessage))
          }
        }
      }
    }

  def getObjectStatus: Route =
    get {
      pathPrefix("status") {
        path(Segment / Segments) { (s3bucket, s3objects) =>
          onComplete(getS3ObjectMeta(s3bucket, s3objects.mkString("/"))) {
            case Success(r) => complete(r)
            case Failure(ex) => complete(ResponseMessage("getObjectMetadata failed: " + ex.getMessage))
          }
        }
      }
    }

}
