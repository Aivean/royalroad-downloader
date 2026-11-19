package com.aivean.royalroad

import java.text.SimpleDateFormat
import java.util.Date

/**
 * Pure helpers for fiction metadata: chapter links and output file naming.
 * Extracted from [[Main]] so they can be unit tested without network access.
 */
object Fiction {

  val BaseUrl = "https://www.royalroad.com"

  private val TitleSuffix = " | Royal Road"

  /**
   * Removes duplicate chapter links (keeping the first occurrence)
   * and converts relative links (`/fiction/...`) to absolute ones.
   */
  def chapterUrls(hrefs: Iterable[String]): List[String] =
    hrefs.toList.distinct.map(href => if (href.startsWith("/")) BaseUrl + href else href)

  /** Fiction title without the site suffix appended to the page `<title>`. */
  def cleanTitle(pageTitle: String): String = pageTitle.stripSuffix(TitleSuffix)

  /**
   * Label describing the downloaded chapter range, used in the output file name.
   * Empty when all chapters are downloaded,
   * `chapter_N` for a single chapter and `chapters_N-M` for a range (1-based).
   */
  def chaptersLabel(allChapters: Seq[String], selected: Seq[String]): String =
    if (selected.isEmpty || allChapters.size == selected.size) ""
    else {
      val first = allChapters.indexOf(selected.head) + 1
      val last = allChapters.indexOf(selected.last) + 1
      if (first == last) s"chapter_$first" else s"chapters_$first-$last"
    }

  /** Renders output file name from the user template (see `--output` option). */
  def outputFileName(template: String, pageTitle: String, chaptersLabel: String, today: Date): String =
    Utils.renderTemplate(template, Map(
      "title" -> Utils.fileNameSafe(cleanTitle(pageTitle)),
      "chapters" -> chaptersLabel,
      "today" -> new SimpleDateFormat("yyyy-MM-dd").format(today)
    ))
}
