package com.aivean.royalroad

import com.aivean.royalroad.Utils._
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL
import net.ruippeixotog.scalascraper.model.Document
import org.jsoup.Connection

import java.io.PrintWriter
import java.net.URLDecoder
import java.util.concurrent.ArrayBlockingQueue
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future, duration}

object Main extends App {

  val cliArgs = new Args(args)

  import DSL.Extract._
  import DSL._

  val browser = new JsoupBrowser {
    override protected[this] def defaultRequestSettings(conn: Connection): Connection = {
      super.defaultRequestSettings(conn) match {
        case c => c.timeout(1.minute.toMillis.toInt)
      }
    }
  }

  val doc = browser.get(cliArgs.fictionLink())
  val title = doc >> text("title")

  println("Title: " + title)

  val threads: Seq[String] =
    (doc >> attrs("href")("#chapters a[href^=/fiction/][href*=/chapter/]")).toList.distinct

  val chapUrls = threads.collect {
    case x if x.startsWith("/") => "https://www.royalroad.com" + x
    case x => x
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

    // recursively process the queue until the end message None is received
    def rec(): Unit = chapQ.take() match {
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
            rec()
        }
    }
    rec()

  } finally {
    printWriter.write("</body></html>")
    printWriter.close()
  }

  println("done")
}
