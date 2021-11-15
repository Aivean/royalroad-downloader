package com.aivean.royalroad

import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL
import net.ruippeixotog.scalascraper.model.Document
import org.jsoup.Connection

import java.io.PrintWriter
import java.net.URLDecoder
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicLong
import scala.concurrent.{Await, Future, duration}
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

  val chapUrls = threads.collect {
    case x if x.startsWith("/") => "https://www.royalroad.com" + x
    case x => x
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

  import scala.concurrent.ExecutionContext.Implicits.global

  // chapter producer, with parallelism limited by the capacity of queue (currently capacity = 4)
  val chapQ = new ArrayBlockingQueue[Option[Future[(String, Document)]]](4, true)
  Future {
    chapUrls.drop(cliArgs.fromChapter() - 1).foreach { u =>
      val uDecoded = URLDecoder.decode(u, "utf-8")
      println(s"downloading: $uDecoded")
      chapQ.put(Some(Future(uDecoded -> retry(backpressure(browser.get(uDecoded))))))
    }
    chapQ.put(None)
  }

  val filename = title.replaceAll("[^\\w\\d]+", "_") + ".html"
  println("Saving as: " + filename)

  val printWriter = new PrintWriter(filename, "UTF-8")
  try {
    printWriter.write(s"""<html><head><meta charset="UTF-8"><title>$title</title></head><body>""")

    def rec(chap: Option[Future[(String, Document)]]): Unit = chap match {
      case None =>
      case Some(f) =>
        Await.result(f, duration.Duration.Inf) match {
          case (url, doc) =>
            println("parsing: " + url)

            // write chapter title to file
            printWriter.write(
              <h1 class="chapter">
                {(doc >?> text(cliArgs.titleQuery()).map(_.trim.stripSuffix(" - " + title)))
                .getOrElse(parsingError("chapter title", cliArgs.titleQuery(), url))}
              </h1>.toString()
            )

            // write chapter content to file
            printWriter.write(
              (doc >?> element(cliArgs.bodyQuery()))
                .getOrElse(parsingError("chapter text", cliArgs.bodyQuery(), url)).outerHtml
            )

            printWriter.write("\n")

            // parse next chapter
            rec(chapQ.take())
        }
    }

    rec(chapQ.take())

  } finally {
    printWriter.write("</body></html>")
    printWriter.close()
  }

  println("done")
}
