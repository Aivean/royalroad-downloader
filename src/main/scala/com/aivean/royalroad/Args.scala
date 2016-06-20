package com.aivean.royalroad

import org.rogach.scallop._;

/**
  *
  * @author <a href="mailto:ivan.zaytsev@webamg.com">Ivan Zaytsev</a>
  *         2016-06-19
  */
class Args(args: Seq[String]) extends ScallopConf(args) {

  banner(
    """Usage:
      | [-t css_query_for_title] [-b css_query_for_chapter_body] http://royalroadl.com/fiction/xxxx
      |
      | Override CSS queries (http://www.w3schools.com/cssref/css_selectors.asp) for title or body
      | if RoyalRoad changed it's format and this program works no longer.
      |
      |Options:
      | """.stripMargin)

  val titleQuery = opt[String](
    descr = "CSS selector for chapter title (text of the found element is used)",
    default = Some("div.ccgtheadposttitle")
  )
  val bodyQuery = opt[String](
    descr = "CSS selector for chapter text body (the whole found element is used)",
    default = Some("div.post_body")
  )

  val fictionLink = trailArg[String](required = true,
    descr = "Fiction URL in format: http://royalroadl.com/fiction/xxxx",
    validate = _.matches("http://royalroadl.com/fiction/\\d+"))

  verify()
}
