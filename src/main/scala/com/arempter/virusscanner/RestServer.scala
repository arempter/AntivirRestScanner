package com.arempter.virusscanner

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.arempter.virusscanner.config.ServerSettings
import com.arempter.virusscanner.routes.ScannerRoutes

import scala.io.StdIn

object RestServer extends App with ScannerRoutes {

  implicit val system = ActorSystem("RestScannerAPI")
  implicit val ec  = system.dispatcher
  implicit val mat = ActorMaterializer()

  implicit val serverSettings = ServerSettings(system)

  val bindF = Http().bindAndHandle(allRoutes, serverSettings.listenHost, serverSettings.listenPort)
  StdIn.readLine()
  bindF
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}
