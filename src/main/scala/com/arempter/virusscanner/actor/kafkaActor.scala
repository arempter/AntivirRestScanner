package com.arempter.virusscanner.actor

import akka.actor.Actor
import com.arempter.virusscanner.RestServer.consumeMessages

class kafkaActor extends Actor {
  override def receive: Receive = {
    case "scan" => consumeMessages()
    case _      =>
  }
}
