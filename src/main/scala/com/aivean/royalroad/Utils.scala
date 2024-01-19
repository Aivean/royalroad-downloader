package com.aivean.royalroad

import java.io.{BufferedInputStream, ByteArrayOutputStream, IOException}
import java.net._
import java.util.Base64
import java.util.concurrent.atomic.AtomicLong
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
      "without permission" -> 0.6,
      "without consent" -> 0.6,
      "unauthorized" -> 0.6,
      "pilfered" -> 0.5,
      "purloined" -> 0.5,
      "appropriated" -> 0.5,
      "royal road" -> 0.9,
      "story" -> 0.4,
      "narrative" -> 0.4,
      "content" -> 0.4,
      "novel" -> 0.4,
      "tale" -> 0.4,
      "infringement" -> 0.5,
      "violation" -> 0.5,
      "not rightfully" -> 0.5,
      "taken without" -> 0.7,
      "misappropriated" -> 0.6,
      "sightings" -> 0.4,
      "encounter" -> 0.3
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

  def getDataURIForURL(url: URL): URI = withResource(url.openStream()) { is =>
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
