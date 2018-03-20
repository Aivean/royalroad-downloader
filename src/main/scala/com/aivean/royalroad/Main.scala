package com.aivean.royalroad

import java.io.PrintWriter
import java.net.URLDecoder

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
         |   https://github.com/Aivean/royalroadl-downloader/blob/master/readme.md
         | Css selectors reference: http://www.w3schools.com/cssref/css_selectors.asp
      """.stripMargin)
  }

  val chapUrls = {
    val urls = threads.collect {
      case x if x.startsWith("/") => "http://royalroadl.com" + x
      case x => x
    }.par

    urls.tasksupport = new ForkJoinTaskSupport(new scala.concurrent.forkjoin.ForkJoinPool(4))
    urls
  }

  def retry[T](f: => T, times: Int = 3): T = Try(f) match {
    case Success(res) => res
    case Failure(e) => if (times > 1) retry(f, times - 1)
    else throw e
  }

  val chaps = chapUrls.map { u =>
    val uDecoded = URLDecoder.decode(u, "utf-8")
    println(s"downloading: $uDecoded")
    uDecoded -> retry(browser.get(uDecoded))
  }.map { case (u, doc) =>
    println("parsing: " + u)

    <h1>
      {(doc >?> text(cliArgs.titleQuery()))
      .getOrElse(parsingError("chapter title", cliArgs.titleQuery(), u))}
    </h1>.toString() +
      (doc >?> element(cliArgs.bodyQuery()))
        .getOrElse(parsingError("chapter text", cliArgs.bodyQuery(), u)).outerHtml
  }.seq

  val filename = title.replaceAll("[^\\w\\d]+", "_") + ".html"
  println("Saving as: " + filename)

  new PrintWriter(filename) {
    write(
      s"""<html><head><title>$title</title></head><body>
         |${chaps.mkString("\n")}
         |</body>
         |</html>
      """.stripMargin)
    close()
  }

  println("done")
}
