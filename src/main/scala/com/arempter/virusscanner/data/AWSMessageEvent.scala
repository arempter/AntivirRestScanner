package com.arempter.virusscanner.data

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

case class Records(records: List[AWSMessageEvent])

case class UserIdentity(principalId: String)

case class RequestParameters(sourceIPAddress: String)

case class ResponseElements(`x-amz-request-id`: String, `x-amz-id-2`: String)

case class OwnerIdentity(principalId: String)

case class BucketProps(name: String, ownerIdentity: OwnerIdentity, arn: String)

case class ObjectProps(key: String, size: Int, eTag: String, versionId: String, sequencer: String)

case class S3(s3SchemaVersion: String, configurationId: String, bucket: BucketProps, `object`: ObjectProps)

case class AWSMessageEvent(
    eventName: String,
    requestParameters: RequestParameters,
    s3: S3,
    eventSource: String,
    userIdentity: UserIdentity,
    eventVersion: String,
    responseElements: ResponseElements,
    awsRegion: String,
    eventTime: String
)

trait AWSMessageEventJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val ownerIdentityFormat = jsonFormat1(OwnerIdentity)
  implicit val bucketFormat = jsonFormat3(BucketProps)
  implicit val objectPropsFormat = jsonFormat5(ObjectProps)
  implicit val s3Format = jsonFormat4(S3)
  implicit val userIdentityFormat = jsonFormat1(UserIdentity)
  implicit val requestParametersFormat = jsonFormat1(RequestParameters)
  implicit val responseElementsFormat = jsonFormat2(ResponseElements)
  implicit val AWSMessageEventFormat = jsonFormat9(AWSMessageEvent)
  implicit val recordsFormat = jsonFormat1(Records)

}

