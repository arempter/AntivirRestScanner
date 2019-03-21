package com.arempter.virusscanner.provider

import akka.actor.ActorSystem
import com.arempter.virusscanner.ScannerAPI
import com.arempter.virusscanner.config.ServerSettings
import com.arempter.virusscanner.data.{ AWSMessageEventJsonSupport, Records }
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import spray.json._

import scala.concurrent.{ ExecutionContext, Future }

trait KafkaMessageProvider extends AWSMessageEventJsonSupport with S3 {

  import scala.collection.JavaConverters._

  protected val system: ActorSystem
  implicit val ec: ExecutionContext
  implicit val serverSettings: ServerSettings

  private val createEventsTopic = "create_events"

  private lazy val consumerSettings = Map[String, Object](
    "bootstrap.servers" -> serverSettings.bootstrapServers,
    "group.id" -> serverSettings.groupId,
    "enable.auto.commit" -> serverSettings.autocommit,
    "auto.commit.interval.ms" -> serverSettings.commitInterval
  )

  private lazy val consumer = {
    val c = new KafkaConsumer[String, String](consumerSettings.asJava, new StringDeserializer, new StringDeserializer)
    c.subscribe(List(createEventsTopic).asJava)
    c
  }

  def consumeMessages() = Future {
    val records = consumer.poll(1000)
      .asScala.map(r => r.value().parseJson.convertTo[Records])

    records.foreach(rs => rs.records.map { r =>
      println("got new object to scan: " + r.s3.bucket.name)
      new ScannerAPI(r.s3.bucket.name.split("/")(1), r.s3.`object`.key).scan
    })
    records
  }

}
