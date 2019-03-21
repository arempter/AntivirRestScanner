package com.arempter.virusscanner

import akka.actor.{ ActorSystem, Props }
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.arempter.virusscanner.actor.kafkaActor
import com.arempter.virusscanner.config.ServerSettings
import com.arempter.virusscanner.provider.KafkaMessageProvider
import com.arempter.virusscanner.routes.ScannerRoutes

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.io.StdIn

object RestServer extends App with KafkaMessageProvider with ScannerRoutes {

  implicit val system = ActorSystem("RestScannerAPI")
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val mat = ActorMaterializer()

  implicit val serverSettings = ServerSettings(system)

  private val kafkaConsumerActor = system.actorOf(Props[kafkaActor], "kafkaConsumer")

  if (serverSettings.kafkaEnabled)
    system.scheduler.schedule(5.seconds, 5.seconds, kafkaConsumerActor, "scan")

  val bindF = Http().bindAndHandle(allRoutes, serverSettings.listenHost, serverSettings.listenPort)
  StdIn.readLine()
  bindF
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}
