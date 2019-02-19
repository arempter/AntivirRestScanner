package com.arempter.virusscanner.config

import akka.actor.ActorSystem
import com.typesafe.config.Config

class ServerSettings(config: Config) {

  // clamav settings
  val clamdSocketTimeout = config.getInt("clamav.timeout")
  val clamdHost = config.getString("clamav.host")
  val clamdPort = config.getInt("clamav.port")
  val scanDirectoryPrefix = config.getString("clamav.scanDirectoryPrefix")

  // s3 settings
  val s3accessKey = config.getString("s3.accessKey")
  val s3secretKey = config.getString("s3.secretKey")
  private val s3host = config.getString("s3.host")
  private val s3port = config.getInt("s3.port")
  val s3endpoint = s"http://$s3host:$s3port"
  val s3region = config.getString("s3.region")

  // rest server settings
  val listenHost = config.getString("server.host")
  val listenPort = config.getInt("server.port")

  // kafka settings
  val bootstrapServers = config.getString("kafka.bootstrap")
  val groupId = config.getString("kafka.groupid")
  val autocommit = config.getString("kafka.autocommit")
  val commitInterval = config.getString("kafka.commitinterval")

}

object ServerSettings {
  def apply(system: ActorSystem): ServerSettings = new ServerSettings(system.settings.config)
}
