package com.aivean.royalroad

import java.io.PrintWriter
import java.net.URLDecoder
import java.util.concurrent.atomic.AtomicLong

import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL
import org.jsoup.Connection

import scala.collection.parallel.ForkJoinTaskSupport
import scala.util.{Failure, Success, Try}

object Main extends App {

  val cliArgs = new Args(args)

  import DSL.Extract._
  import DSL._

  val browser = new JsoupBrowser {
    override protected[this] def defaultRequestSettings(conn: Connection): Connection = {
      super.defaultRequestSettings(conn) match {
        case c => c.timeout(60000)
      }
    }
  }

  val doc = browser.get(cliArgs.fictionLink())

  val title = doc >> text("title")

  println("Title: " + title)

  val threads: Seq[String] =
    (doc >> attrs("href")("#chapters a[href^=/fiction/][href*=/chapter/]")).toList.distinct

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

  val chapUrls = {
    val urls = threads.collect {
      case x if x.startsWith("/") => "https://www.royalroad.com" + x
      case x => x
    }.par

    urls.tasksupport = new ForkJoinTaskSupport(new scala.concurrent.forkjoin.ForkJoinPool(4))
    urls
  }

  // delay requests until that time
  val requestTimeLimiter = new AtomicLong()

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

  def retry[T](f: => T, times: Int = 3): T = Try(f) match {
    case Success(res) => res
    case Failure(e) => if (times > 1) retry(f, times - 1)
    else throw e
  }

  val chaps = chapUrls.drop(cliArgs.fromChapter() - 1).map { u =>
    val uDecoded = URLDecoder.decode(u, "utf-8")
    println(s"downloading: $uDecoded")
    uDecoded -> retry(backpressure(browser.get(uDecoded)))
  }.map { case (u, doc) =>
    println("parsing: " + u)

    <h1 class="chapter">
      {(doc >?> text(cliArgs.titleQuery()))
      .getOrElse(parsingError("chapter title", cliArgs.titleQuery(), u))}
    </h1>.toString() +
      (doc >?> element(cliArgs.bodyQuery()))
        .getOrElse(parsingError("chapter text", cliArgs.bodyQuery(), u)).outerHtml
  }.seq

  val filename = title.replaceAll("[^\\w\\d]+", "_") + ".html"
  println("Saving as: " + filename)

  new PrintWriter(filename, "UTF-8") {
    write(
      s"""<html><head><meta charset="UTF-8"><title>$title</title></head><body>
         |${chaps.mkString("\n")}
         |</body>
         |</html>
      """.stripMargin)
    close()
  }

  println("done")
}
