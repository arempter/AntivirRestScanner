package com.arempter.virusscanner.scanengine

import java.io.{ByteArrayInputStream, InputStream}
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import com.arempter.virusscanner.config.ServerSettings
import com.arempter.virusscanner.data.SocketInOut

import scala.io.Source
import scala.util.{Failure, Success, Try}

trait ClamAVClient {

  implicit val serverSettings: ServerSettings
  val SOCKET_TIMEOUT = serverSettings.clamdSocketTimeout
  val as: SocketInOut = getSocketInOut(serverSettings.clamdHost, serverSettings.clamdPort)

  // not used
  def close(): Unit = {
    as.out.write(commandAsBytes("END\0"))
    as.out.flush()
    // check if enough
    as.out.close()
    as.in.close()
  }

  def commandAsBytes(command: String): Array[Byte] = {
    command.getBytes(StandardCharsets.UTF_8)
  }

  def readResponse(inStream: InputStream): String = {
    Source.fromInputStream(inStream).map(_.toString).toList.mkString
  }


  def getSocketInOut(host: String = "localhost", port: Int = 3310): SocketInOut = {
    Try {
      val s = new Socket(host, port)
      s.setSoTimeout(SOCKET_TIMEOUT)
      SocketInOut(s.getInputStream, s.getOutputStream)
    } match {
      case Success(r) => r
      case Failure(_)  => throw new Exception("Failed to connect to ClamAV")
    }
  }

  def ping(): String = {
    as.out.write(commandAsBytes("zPING\0"))
    as.out.flush()
    readResponse(as.in)
  }

  def scanStream(chunk: InputStream): String = {
    val chunkData = Source.fromInputStream(chunk).map(_.toByte).toArray
    val chunkSize = ByteBuffer.allocate(4).putInt(chunkData.length).array()
    // init
    as.out.write(commandAsBytes("zINSTREAM\0"))
    as.out.flush()
    // data
    as.out.write(chunkSize)
    as.out.write(chunkData)
    as.out.write(Array[Byte](0,0,0,0))
    as.out.flush()

    readResponse(as.in)
  }

  def scanStream(chunk: Array[Byte]): String = {
    scanStream(new ByteArrayInputStream(chunk))
  }

}





