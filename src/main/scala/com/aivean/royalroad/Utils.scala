package com.aivean.royalroad

import java.io.{BufferedInputStream, ByteArrayOutputStream, IOException}
import java.net._
import java.util.Base64
import java.util.concurrent.atomic.AtomicLong
import javax.net.ssl.SSLContext
import javax.security.cert.X509Certificate
import scala.util.{Failure, Success, Try}


/**
 * Misc helper functions
 */
object Utils {

  object WarningFuzzyMatcher {
    val keywords = Map(
      "amazon" -> 1.0,
      "stolen" -> 0.8,
      "theft" -> 0.8,
      "report" -> 0.7,
      "please report" -> 0.61,
      "read the official version" -> 0.8,
      "support the" -> 0.7,
      "without permission" -> 0.6,
      "without author" -> 0.6,
      "without the author" -> 0.6,
      "without consent" -> 0.6,
      "unauthorized" -> 0.6,
      "pilfered" -> 0.5,
      "pirated" -> 0.5,
      "pirate" -> 0.5,
      "lifted" -> 0.5,
      "obtained" -> 0.5,
      "taken" -> 0.5,
      "purloined" -> 0.5,
      "appropriated" -> 0.5,
      "royal road" -> 0.9,
      "story" -> 0.4,
      "narrative" -> 0.4,
      "content" -> 0.4,
      "novel" -> 0.4,
      "tale" -> 0.4,
      "infringement" -> 0.5,
      "unlawfully" -> 0.5,
      "violation" -> 0.5,
      "not rightfully" -> 0.5,
      "taken without" -> 0.7,
      "misappropriated" -> 0.6,
      "sightings" -> 0.4,
      "encounter" -> 0.3,
      "original" -> 0.6,
      "official" -> 0.6,
      "authentic" -> 0.6,
      "genuine" -> 0.6,
      "elsewhere" -> 0.5,
      "different site" -> 0.6,
      "another platform" -> 0.6,
      "another site" -> 0.6,
      "other great novels" -> 0.4,
      "creative writers" -> 0.5,
      "author's preferred platform" -> 0.7,
      "originally published" -> 0.7,
      "true home" -> 0.5,
      "ensure the author gets" -> 0.8,
      "published on a different platform" -> 0.8,
      "seeking out the original" -> 0.8,
      "find the genuine version" -> 0.8,
      "did you know" -> 0.4,
      "favorite authors" -> 0.5,
      "deserves support" -> 0.6,
      "support creators" -> 0.7,
      "read it there" -> 0.5,
      "real experience" -> 0.5,
      "posted elsewhere" -> 0.6,
      "originates from" -> 0.6,
      "help the author" -> 0.7,
      "look for the official" -> 0.7,
      "visit royal road" -> 0.8,
      "discover and support" -> 0.7,
      "ensure author gets credit" -> 0.8,
      "support creativity" -> 0.7,
      "enjoying this book" -> 0.5,
      "seek out the original" -> 0.7,
      "get the support they deserve" -> 0.7,
      "reading their stories" -> 0.5,
      "support the creativity" -> 0.7,
      "by visiting" -> 0.3,
      "can be found on" -> 0.5,
      "check it out there" -> 0.6,
      "find this and other great novels" -> 0.8,
      "author's preferred platform" -> 0.8,
      "support original creators" -> 0.8,
      "help support creative writers" -> 0.8,
      "read it on royal road" -> 0.9,
      "ensure the author gets credit" -> 0.9,
      "searching for the original publication" -> 0.8,
      "book's true home" -> 0.7,
      "novel's true home" -> 0.7,
      "posted elsewhere by the author" -> 0.9,
      "reading the authentic version" -> 0.8,
      "story originates from" -> 0.8,
      "different website" -> 0.6,
      "reading it there" -> 0.6,
      "support creative writers" -> 0.8,
      "finding and reading their stories" -> 0.8,
      "on the original site" -> 0.7,
      "support the author" -> 0.8,
      "searching for the original" -> 0.8,
      "original publication" -> 0.7,
      "true home is a different platform" -> 0.9,
      "support the author by finding it" -> 0.9
    )

    val threshold = 2.5

    def scoreString(s: String): Double = {
      keywords.foldLeft(0.0) {
        case (score, (keyword, value)) =>
          if (s.toLowerCase.contains(keyword)) score + value else score
      }
    }

    def apply(warning: String): Boolean = warning.length < 200 && scoreString(warning) > threshold
  }

  def parsingError(name: String, value: String, url: String): Nothing = {
    throw new IllegalStateException(
      s""" Can't find $name using css query: `$value`
         | Probably it happened because RoyalRoad changed it's html structure.
         | No worries!
         | Check $url
         | and specify correct css selector for $name as command line argument for this program.
         | Check readme or run this program with --help parameter for details:
         |   https://github.com/Aivean/royalroad-downloader/blob/master/readme.md
         | Css selectors reference: http://www.w3schools.com/cssref/css_selectors.asp
      """.stripMargin)
  }

  // delay requests until that time
  private val requestTimeLimiter = new AtomicLong()

  /**
   * Wraps function `f` in retry with exponentially growing delay.
   * Note: delay is implemented as a timestamp in the future, when `f` will be called next
   * and is shared between all calls of `backpressure`.
   * This timestamp is stored in `requestTimeLimiter`.
   */
  def backpressure[T](f: => T, delayMs: Long = 1000): T = {
    while (requestTimeLimiter.get() > System.currentTimeMillis()) {
      Thread.sleep(Math.max(0, System.currentTimeMillis() - requestTimeLimiter.get()))
    }
    Try(f) match {
      case Success(res) => res
      case Failure(e) => e match {
        case statusExp: org.jsoup.HttpStatusException if statusExp.getStatusCode == 429 =>
          import scala.compat.java8.FunctionConverters.asJavaLongUnaryOperator
          requestTimeLimiter.getAndUpdate(
            asJavaLongUnaryOperator(math.max(_: Long, System.currentTimeMillis() + delayMs))
          ) < System.currentTimeMillis()
          Thread.sleep(2000)
          backpressure(f, delayMs * 3 / 2)
        case _ => throw e
      }
    }
  }

  /**
   * Wraps function `f` in the given number of retries.
   */
  def retry[T](f: => T, times: Int = 3): T = Try(f) match {
    case Success(res) => res
    case Failure(e) => if (times > 1) Utils.retry(f, times - 1) else throw e
  }

  def withResource[R <: AutoCloseable, T](resource: => R)(block: R => T): T = {
    var res: Option[R] = None
    try {
      res = Some(resource)
      block(res.get)
    } finally {
      res.foreach(_.close())
    }
  }

  def getDataURIForURL(url: URL): URI = {
    val connection = url.openConnection().asInstanceOf[HttpURLConnection]
    connection.setRequestProperty("User-Agent", "curl/8.4.0")
    connection.setRequestProperty("Host", url.getHost)
    connection.setRequestProperty("Accept", "*/*")

    withResource(connection.getInputStream) { is =>
      val bis = new BufferedInputStream(is)
      val contentType = URLConnection.guessContentTypeFromStream(bis) match {
        case null => // try to guess from url
          val ext = url.toString.split('.').lastOption
          ext match {
            case Some("jpg") => "image/jpeg"
            case Some("png") => "image/png"
            case Some("gif") => "image/gif"
            case _ => null
          }
        case x => x
      }

      if (contentType != null) {
        withResource(new ByteArrayOutputStream()) { os =>
          val chunk = new Array[Byte](4096)
          Stream.continually(bis.read(chunk))
            .takeWhile(_ > 0)
            .foreach(readBytes => os.write(chunk, 0, readBytes))
          os.flush()
          new URI("data:" + contentType + ";base64," +
            Base64.getEncoder.encodeToString(os.toByteArray))
        }
      } else {
        throw new IOException("could not get content type from " + url.toExternalForm)
      }
    }
  }

  def fileNameSafe(s: String): String = s.replaceAll("[^\\w\\d]+", "_")

  def renderTemplate(template: String, replacements: Map[String, String]): String =
    replacements.foldLeft(template) {
        case (acc, (key, value)) =>
          acc.replaceAllLiterally("{" + key + "}", value)
      }.replaceAll("""^(\{})+""", "")
      .replaceAll("""(\{})$""", "")
      .replaceAll("""(\{})(?=\.)""", "")
      .replaceAll("""(_*\{})+""", "_")

  def createDirectoryIfNotExists(path: String): Unit = {
    val dir = new java.io.File(path).getParentFile
    if (dir != null && !dir.exists()) {
      dir.mkdirs()
    }
  }

  def disableSSL(): Unit = {
    import java.security.cert.X509Certificate
    import javax.net.ssl.{SSLContext, TrustManager, X509TrustManager}
    import javax.net.ssl.HttpsURLConnection

    val trustAllCerts: Array[TrustManager] = Array[TrustManager](new X509TrustManager() {
      def getAcceptedIssuers: Array[X509Certificate] = null

      def checkClientTrusted(certs: Array[X509Certificate], authType: String): Unit = {}

      def checkServerTrusted(certs: Array[X509Certificate], authType: String): Unit = {}
    })

    val sc: SSLContext = SSLContext.getInstance("SSL")
    sc.init(null, trustAllCerts, new java.security.SecureRandom)
    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory)
  }

}
