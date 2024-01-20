package com.aivean.royalroad

import com.aivean.royalroad.Args.urlValidationRegex
import org.rogach.scallop._;

class Args(args: Seq[String]) extends ScallopConf(args) {

  banner(
    """Usage:
      | [-t css_query_for_title] [-b css_query_for_chapter_body] https://royalroad.com/fiction/xxxx
      |
      | Override CSS queries (http://www.w3schools.com/cssref/css_selectors.asp) for title or body
      | if RoyalRoad changed it's format and this program works no longer.
      |
      |Options:
      | """.stripMargin)

  val fromChapter = opt[Int](
    descr = "Start download from chapter #. " +
      "Positive values indicate chapters from the beginning. " +
      "(1 means start from the first chapter, 2 means second chapter) " +
      "Negative values indicate chapters from the end " +
      "(-1 means last chapter, -2 means two chapters from the end)",
    default = Some(1)
  )

  val embedImages = opt[Boolean](
    descr = "Enable embedding of images as data URLs",
    short = 'e',
    default = Some(false)
  )

  val includeTitlePage = opt[Boolean](
    descr = "Include title page with fiction title and author name",
    noshort = true,
    default = Some(false)
  )

  val removeWarnings = opt[Boolean](
    descr = "Remove warnings about reporting story if found on Amazon",
    noshort = true,
    default = Some(false)
  )

  val titleQuery = opt[String](
    descr = "CSS selector for chapter title (text of the found element is used)",
    default = Some("title")
  )

  val bodyQuery = opt[String](
    descr = "CSS selector for chapter text body (the whole found element is used)",
    default = Some("div.chapter-content")
  )

  val output = opt[String](
    descr = "Output file path and name." +
      " Supports placeholders:" +
      " {title}, {chapters}, {today}, " +
      " {} - smart separator" +
      " (removed if in the beginning" +
      " or in the end of the file name," +
      " replaced with \"_\" and collapsed otherwise" +
      " e.g. \"{}a{}{}b{}.html\" -> \"a_b.html\")." +
      " Default format is \"{t}{}{c}.html\"",
    short = 'o',
    default = Some("{title}{}{chapters}.html"),
    // sanity check for legal output filename
    validate = fileName => !fileName.matches("[\\\\:*?\"<>|]") && fileName.nonEmpty && !fileName.endsWith("/")
  )

  val fictionLink = trailArg[String](required = true,
    descr = "Fiction URL in format: http://royalroad.com/fiction/xxxx\n" +
      "\tor http[s]://[www.]royalroad.com/fiction/xxxx/fiction-title",
    validate = _.matches(urlValidationRegex))

  verify()
}

object Args {
  val urlValidationRegex = "(https?://(www\\.)?royalroadl?.com/fiction/\\d+)(/.*)?"
}
