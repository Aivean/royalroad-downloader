package com.aivean.royalroad

import java.io.PrintWriter

import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.dsl.DSL

object Main extends App {

  val url = args.headOption match {
    case Some(u) if u.matches("http://royalroadl.com/fiction/\\d+") => u
    case _ => System.err.println(
      """Provide exactly one program argument:
        |url in format http://royalroadl.com/fiction/xxxx """.stripMargin)
      System.exit(1)
      ""
  }

  import DSL.Extract._
  import DSL._

  val browser = new Browser

  val doc = browser.get(url)

  val title = doc >> text("h1.fiction-title")

  println("Title: " + title)

  val threads: Seq[String] =
    doc >> attrs("href")("div.chapters li.chapter a[href^=http://royalroadl.com/forum/showthread.php?tid]")

  val chaps =
    threads.par.map(u => u -> browser.get(u)).map { case (u, doc) =>
      println("parsing: " + u)

      s"""<h1>${doc >> text("div.largetext")}</h1>""" +
        (doc >> element("div.post_body.postbit_body")).toString
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
