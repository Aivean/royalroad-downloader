package com.aivean.royalroad

import java.io.IOException
import java.net.{InetSocketAddress, URI, URL}
import java.nio.charset.StandardCharsets
import java.util.Base64

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import org.scalatest.FunSuite

/**
 * Tests for [[Utils.getDataURIForURL]] that do not depend on external network:
 * a local HTTP server is started on a random port for every test.
 */
class UtilsDataUriLocalTest extends FunSuite {

  // 5x5 red PNG (same image as in UtilsTest)
  private val pngBase64 =
    "iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAIAAAACDbGyAAAAEUlEQVR4nGP8z4ACmBgo4wMAUJEBCfBOwRcAAAAASUVORK5CYII="
  private val pngBytes = Base64.getDecoder.decode(pngBase64)

  private val plainBytes = "just some text".getBytes(StandardCharsets.UTF_8)

  private def withServer[T](routes: Map[String, Array[Byte]])(body: String => T): T = {
    val server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0)
    routes.foreach { case (path, bytes) =>
      server.createContext(path, new HttpHandler {
        override def handle(exchange: HttpExchange): Unit = {
          exchange.sendResponseHeaders(200, bytes.length.toLong)
          val os = exchange.getResponseBody
          try os.write(bytes) finally os.close()
        }
      })
    }
    server.start()
    try body(s"http://127.0.0.1:${server.getAddress.getPort}")
    finally server.stop(0)
  }

  test("content type is detected from stream bytes (PNG signature)") {
    withServer(Map("/image" -> pngBytes)) { base =>
      val uri = Utils.getDataURIForURL(new URL(base + "/image"))
      assert(uri == new URI("data:image/png;base64," + pngBase64))
    }
  }

  test("content type falls back to file extension when stream is not recognized") {
    withServer(Map("/pic.jpg" -> plainBytes, "/pic.png" -> plainBytes, "/pic.gif" -> plainBytes)) { base =>
      val expectedPayload = Base64.getEncoder.encodeToString(plainBytes)
      assert(Utils.getDataURIForURL(new URL(base + "/pic.jpg")).toString ==
        "data:image/jpeg;base64," + expectedPayload)
      assert(Utils.getDataURIForURL(new URL(base + "/pic.png")).toString ==
        "data:image/png;base64," + expectedPayload)
      assert(Utils.getDataURIForURL(new URL(base + "/pic.gif")).toString ==
        "data:image/gif;base64," + expectedPayload)
    }
  }

  test("IOException is thrown when content type cannot be determined") {
    withServer(Map("/file.bin" -> plainBytes)) { base =>
      val e = intercept[IOException] {
        Utils.getDataURIForURL(new URL(base + "/file.bin"))
      }
      assert(e.getMessage.contains("could not get content type"))
    }
  }

  test("binary content larger than one read chunk is encoded completely") {
    // PNG header + 10 KB of payload: forces several 4096-byte reads
    val big = pngBytes ++ Array.tabulate[Byte](10 * 1024)(i => (i % 251).toByte)
    withServer(Map("/big" -> big)) { base =>
      val uri = Utils.getDataURIForURL(new URL(base + "/big")).toString
      assert(uri.startsWith("data:image/png;base64,"))
      val decoded = Base64.getDecoder.decode(uri.stripPrefix("data:image/png;base64,"))
      assert(decoded.sameElements(big))
    }
  }
}
