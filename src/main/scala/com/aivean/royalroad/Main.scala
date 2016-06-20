package com.aivean.royalroad

import java.io.PrintWriter

import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.dsl.DSL

object Main extends App {

  val cliArgs = new Args(args)

  import DSL.Extract._
  import DSL._

  val browser = new Browser

  val doc = browser.get(cliArgs.fictionLink())

  val title = doc >> text("h1.fiction-title")

  println("Title: " + title)

  val threads: Seq[String] =
    doc >> attrs("href")("div.chapters li.chapter a[href^=http://royalroadl.com/forum/showthread.php?tid]")

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

  val chaps =
    threads.par.map(u => u -> browser.get(u)).map { case (u, doc) =>
      println("parsing: " + u)

      <h1>
        {(doc >?> text(cliArgs.titleQuery()))
        .getOrElse(parsingError("chapter title", cliArgs.titleQuery(), u))}
      </h1>.toString() +
        (doc >?> element(cliArgs.bodyQuery()))
          .getOrElse(parsingError("chapter text", cliArgs.bodyQuery(), u)).toString
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
