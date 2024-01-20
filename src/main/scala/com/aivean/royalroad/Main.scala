package com.aivean.royalroad

import com.aivean.royalroad.Utils._
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.browser.JsoupBrowser.JsoupElement
import net.ruippeixotog.scalascraper.dsl.DSL
import net.ruippeixotog.scalascraper.model.Document
import org.jsoup.Connection

import java.io.PrintWriter
import java.net.{URL, URLDecoder}
import java.util.concurrent.ArrayBlockingQueue
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future, duration}
import scala.util.{Failure, Success, Try}

object Main extends App {

  print ("Royalroad downloader v" + VersionInfo.version + "\n")

  def handleFromArg[T](chaps: Seq[T], fromChap: Int): Seq[T] =
    if (fromChap > 0) chaps.drop(fromChap - 1) else if (fromChap < 0) chaps.takeRight(-fromChap) else chaps

  def extractFictionLink(link: String): String = {
    val regex = Args.urlValidationRegex.r
    link match {
      case regex(url, _, _) => url
      case _ => throw new IllegalArgumentException("Invalid fiction link: " + link)
    }
  }

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

  val doc = browser.get(extractFictionLink(cliArgs.fictionLink()))
  val title = doc >> text("title")

  println("Title: " + title)

  val threads: Seq[String] =
    (doc >> attrs("href")("#chapters a[href^=/fiction/][href*=/chapter/]")).toList.distinct

  val chapUrls = threads.collect {
    case x if x.startsWith("/") => "https://www.royalroad.com" + x
    case x => x
  }

  val chapUrlsConstrained = handleFromArg(chapUrls, cliArgs.fromChapter())
  if (chapUrlsConstrained.isEmpty) {
    println("No chapters found")
    System.exit(1)
  }

  import scala.concurrent.ExecutionContext.Implicits.global

  // chapter producer, with parallelism limited by the capacity of queue (currently capacity = 4)
  val chapQ = new ArrayBlockingQueue[Option[Future[(String, Document)]]](4, true)
  Future {
    chapUrlsConstrained.foreach { u =>
      val uDecoded = URLDecoder.decode(u, "utf-8")
      println(s"downloading: $uDecoded")
      chapQ.put(Some(Future(uDecoded -> retry(backpressure(browser.get(uDecoded))))))
    }
    chapQ.put(None)
  }

  val filename = renderTemplate(cliArgs.output(), Map(
    "title" -> fileNameSafe(title.stripSuffix(" | Royal Road")),
    "chapters" -> {
      // when chapter range is specified, add it to the filename
      if (chapUrls.size != chapUrlsConstrained.size) {
        val firstChapter = chapUrls.indexOf(chapUrlsConstrained.head)
        val lastChapter = chapUrls.indexOf(chapUrlsConstrained.last)
        if (firstChapter == lastChapter) "chapter_" + (firstChapter + 1) else
          "chapters_" + (firstChapter + 1) + "-" + (lastChapter + 1)
      } else ""
    },
    "today" -> {
      new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date())
    }
  ))

  println("Saving as: " + filename)

  createDirectoryIfNotExists(filename)
  val printWriter = new PrintWriter(filename, "UTF-8")
  try {
    printWriter.write(s"""<html><head><meta charset="UTF-8"><title>$title</title></head><body>""")

    // process the queue until the end message None is received
    Stream.continually(chapQ.take())
      .takeWhile(_.isDefined)
      .flatMap(_.map(Await.result(_, duration.Duration.Inf)))
      .foreach { case (url, doc) =>
        println("parsing: " + url)

        // write chapter title to file
        printWriter.write(
          <h1 class="chapter">
            {(doc >?> text(cliArgs.titleQuery()).map(_.trim.stripSuffix(" - " + title)))
            .getOrElse(parsingError("chapter title", cliArgs.titleQuery(), url))}
          </h1>.toString()
        )

        // get chapter content
        val chapterContent = (doc >?> element(cliArgs.bodyQuery()))
          .getOrElse(parsingError("chapter text", cliArgs.bodyQuery(), url))

        if (cliArgs.embedImages()) {
          // find all image elements in the chapter content
          // replace all image elements with their data URI
          chapterContent.select("img").collect {
            case img: JsoupElement if img.hasAttr("src") =>
              val imgUrl = img.attr("src")
              println("embedding image: " + imgUrl)
              Try(new URL(imgUrl)) match {
                case Success(url) =>
                  Try(retry(getDataURIForURL(url))) match {
                    case Success(dataUrl) => img.underlying.attr("src", dataUrl.toString)
                    case Failure(e) =>
                      println(s"Failed to convert $imgUrl to data URL")
                      e.printStackTrace()
                  }
                case Failure(_) => println(s"Invalid URL: $imgUrl")
              }
            case img: JsoupElement => println(s"Warning: image without src attribute: ${img.outerHtml} in $url")
          }
        }

        if (cliArgs.removeWarnings()) {
          // find all paragraphs
          val paragraphs = chapterContent.select("p")
          // find all paragraphs that contain the warning
          val warningParagraphs = paragraphs.filter(p => Utils.WarningFuzzyMatcher(p.text))
          // remove all warning paragraphs
          warningParagraphs.collect {
            case p: JsoupElement =>
              println("removing warning: " + p.text)
              p.underlying.remove()
          }
        }

        // write chapter content to file
        printWriter.write(chapterContent.outerHtml)

        printWriter.write("\n")
      }


  } finally {
    printWriter.write("</body></html>")
    printWriter.close()
  }

  println("done")
  println("Saved: " + filename)
}
