package com.aivean.royalroad

import java.text.SimpleDateFormat

import org.scalatest.FunSuite

class FictionTest extends FunSuite {

  private val chapters = Seq("c1", "c2", "c3", "c4", "c5")
  private val date = new SimpleDateFormat("yyyy-MM-dd").parse("2026-10-31")

  // ---------- chapterUrls ----------

  test("chapterUrls converts relative links to absolute") {
    assert(Fiction.chapterUrls(Seq("/fiction/1/name/chapter/10/ch")) ==
      List("https://www.royalroad.com/fiction/1/name/chapter/10/ch"))
  }

  test("chapterUrls keeps absolute links as is") {
    val abs = "https://www.royalroad.com/fiction/1/name/chapter/10/ch"
    assert(Fiction.chapterUrls(Seq(abs)) == List(abs))
  }

  test("chapterUrls removes duplicates and preserves order") {
    val hrefs = Seq("/fiction/1/chapter/2", "/fiction/1/chapter/1", "/fiction/1/chapter/2", "/fiction/1/chapter/3")
    assert(Fiction.chapterUrls(hrefs) == List(
      "https://www.royalroad.com/fiction/1/chapter/2",
      "https://www.royalroad.com/fiction/1/chapter/1",
      "https://www.royalroad.com/fiction/1/chapter/3"))
  }

  test("chapterUrls returns empty list for no links") {
    assert(Fiction.chapterUrls(Nil) == Nil)
  }

  // ---------- cleanTitle ----------

  test("cleanTitle strips Royal Road suffix only at the end") {
    assert(Fiction.cleanTitle("Mother of Learning | Royal Road") == "Mother of Learning")
    assert(Fiction.cleanTitle("Mother of Learning") == "Mother of Learning")
    assert(Fiction.cleanTitle("A | Royal Road story") == "A | Royal Road story")
  }

  // ---------- chaptersLabel ----------

  test("chaptersLabel is empty when all chapters are selected") {
    assert(Fiction.chaptersLabel(chapters, chapters) == "")
  }

  test("chaptersLabel describes a range of chapters (1-based)") {
    assert(Fiction.chaptersLabel(chapters, Main.handleFromArg(chapters, 3)) == "chapters_3-5")
    assert(Fiction.chaptersLabel(chapters, Main.handleFromArg(chapters, -2)) == "chapters_4-5")
  }

  test("chaptersLabel describes a single chapter") {
    assert(Fiction.chaptersLabel(chapters, Main.handleFromArg(chapters, -1)) == "chapter_5")
    assert(Fiction.chaptersLabel(chapters, Seq("c2")) == "chapter_2")
  }

  test("chaptersLabel is empty when nothing is selected") {
    assert(Fiction.chaptersLabel(chapters, Nil) == "")
  }

  // ---------- outputFileName ----------

  test("outputFileName with default template and all chapters") {
    assert(Fiction.outputFileName("{title}{}{chapters}.html", "Mother of Learning | Royal Road", "", date) ==
      "Mother_of_Learning.html")
  }

  test("outputFileName with default template and chapter range") {
    assert(Fiction.outputFileName("{title}{}{chapters}.html", "Mother of Learning | Royal Road", "chapters_3-5", date) ==
      "Mother_of_Learning_chapters_3-5.html")
  }

  test("outputFileName substitutes today's date") {
    assert(Fiction.outputFileName("books/{today}{}{title}.html", "Book | Royal Road", "", date) ==
      "books/2026-10-31_Book.html")
  }
}
