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
  val cliArgs = new Args(args)

  def handleFromArg[T](chaps: Seq[T], fromChap: Int): Seq[T] =
    if (fromChap > 0) chaps.drop(fromChap - 1) else if (fromChap < 0) chaps.takeRight(-fromChap) else chaps

  def extractFictionLink(link: String): String = {
    val regex = Args.urlValidationRegex.r
    link match {
      case regex(url, _, _) => url
      case _ => throw new IllegalArgumentException("Invalid fiction link: " + link)
    }
  }

  def embedImageIfNeeded(url: String): Try[String] =
    if (cliArgs.embedImages()) {
      Try(new URL(url)).flatMap(url => Try(retry(getDataURIForURL(url)))).map(_.toString)
    } else Success(url)

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

    if (cliArgs.includeTitlePage()) try {
      // include header with title, author, description and cover image
      val authorName = doc >> text("h4 > span > a")
      val authorProfilePic = doc >> attr("src")("div.avatar-container-general > img")
      val fictionDescription = doc >> text("div.description")
      val fictionImage = doc >> attr("src")("div.cover-art-container > img")
      val fictionLink = extractFictionLink(cliArgs.fictionLink()) // get the fiction page link
      // get the current date and time

      println("Author: " + authorName)
      println("Author Profile Picture: " + authorProfilePic)
      println("Description: " + fictionDescription)
      println("Image: " + fictionImage)
      println("Fiction Link: " + fictionLink)

      def embedIfNeededSilently(url: String) = embedImageIfNeeded(url).getOrElse(url)

      printWriter.write(
        <div class="title-page">
          <h1 class="title">
            <a href={fictionLink}>
              {title}
            </a>
          </h1> <!-- link back to the fiction page -->
          <img class="fiction-image" src={embedIfNeededSilently(fictionImage)}/>
          <h2>by
            {authorName}<img class="author-profile-pic" src={embedIfNeededSilently(authorProfilePic)}/>
          </h2>
          <p class="description">
            {fictionDescription}
          </p>
          <p class="scraping-time">Scraped at:
            {new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())}
          </p> <!-- display the scraping date and time -->
        </div>.toString()
      )
    } catch {
      case e: Exception =>
        println("Failed to include title page: " + e.getMessage)
        e.printStackTrace()
    }

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

              embedImageIfNeeded(imgUrl) match {
                case Success(dataUrl) => img.underlying.attr("src", dataUrl)
                case Failure(e) =>
                  println(s"Failed to convert $imgUrl to data URL")
                  e.printStackTrace()
              }
            case img: JsoupElement => println(s"Warning: image without src attribute: ${img.outerHtml} in $url")
          }
        }

        if (cliArgs.removeWarnings()) {
          // Find all elements with hidden classes from CSS
          val hiddenClassMatcher = Utils.HiddenClassMatcher.fromDocument(doc)

          // Find and remove elements with hidden classes
          chapterContent.select("p,div,span")
            .filter(hiddenClassMatcher.matches)
            .collect {
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
