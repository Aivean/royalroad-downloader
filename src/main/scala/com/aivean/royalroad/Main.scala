package com.aivean.royalroad

import java.io.PrintWriter

import net.ruippeixotog.scalascraper.browser.{Browser, JsoupBrowser}
import net.ruippeixotog.scalascraper.dsl.DSL
import org.jsoup.Connection

import scala.collection.parallel.ForkJoinTaskSupport

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
    (doc >> attrs("href")("#chapters a[href^=/fiction/chapter/]")).toList.distinct

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

  val chaps = chapUrls.map { u =>
    println(s"downloading: $u")
    u -> browser.get(u)
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
