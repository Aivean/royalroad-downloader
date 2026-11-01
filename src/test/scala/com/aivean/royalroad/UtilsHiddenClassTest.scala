package com.aivean.royalroad

import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import org.scalatest.FunSuite

class UtilsHiddenClassTest extends FunSuite {

  test("RegEx pattern correctly extracts class names from CSS") {
    // Direct test of the regex pattern used in extractHiddenClassNames
    val css1 = ".cmFmYTQ2MGQxNDE2MzQ1NDY4YjliNDA0ODg5YWI1YjQ5{display:none;speak:never;}"
    val css2 = ".anotherHiddenClass { display: none; color: red; }"
    val css3 = ".normalClass { color: blue; }" // This shouldn't match

    val combinedCss = css1 + "\n" + css2 + "\n" + css3

    // Use the same regex pattern as in Utils.HiddenClassMatcher.extractHiddenClassNames
    val cssPattern = Utils.HiddenClassMatcher.cssPattern
    val matches = cssPattern.findAllMatchIn(combinedCss).map(_.group(1)).toSet

    assert(matches.contains("cmFmYTQ2MGQxNDE2MzQ1NDY4YjliNDA0ODg5YWI1YjQ5"))
    assert(matches.contains("anotherHiddenClass"))
    assert(matches.size === 2)
  }

  test("HiddenClassMatcher.extractHiddenClassNames finds classes with display:none in HTML") {
    val testHtml =
      """
      <html>
        <head>
          <style>
            .cmFmYTQ2MGQxNDE2MzQ1NDY4YjliNDA0ODg5YWI1YjQ5{
                display: none;
                speak: never;
            }
          </style>
          <style>
            .anotherHiddenClass {
              color: red;
              display: none;
            }
            .normalClass {
              color: blue;
            }
          </style>
        </head>
        <body>
          <div class="cmFmYTQ2MGQxNDE2MzQ1NDY4YjliNDA0ODg5YWI1YjQ5">Hidden warning message</div>
          <div class="anotherHiddenClass">Another hidden message</div>
          <div class="normalClass">Normal content</div>
        </body>
      </html>
      """

    val browser = JsoupBrowser()
    val doc = browser.parseString(testHtml)

    val hiddenClasses = Utils.HiddenClassMatcher.extractHiddenClassNames(doc)

    // Check that we found the hidden classes
    assert(hiddenClasses.contains("cmFmYTQ2MGQxNDE2MzQ1NDY4YjliNDA0ODg5YWI1YjQ5"),
           s"Hidden classes found: ${hiddenClasses.mkString(", ")}")
    assert(hiddenClasses.contains("anotherHiddenClass"),
           s"Hidden classes found: ${hiddenClasses.mkString(", ")}")
    assert(hiddenClasses.size === 2)
  }

  test("HiddenClassMatcher detects elements with hidden classes") {
    val testHtml =
      """
      <html>
        <body>
          <div class="cmFmYTQ2MGQxNDE2MzQ1NDY4YjliNDA0ODg5YWI1YjQ5">Hidden warning message</div>
          <div class="normal">Normal content</div>
        </body>
      </html>
      """

    val browser = JsoupBrowser()
    val doc = browser.parseString(testHtml)

    // Create matcher with predefined class names
    val matcher = Utils.HiddenClassMatcher.fromClassNames(Set("cmFmYTQ2MGQxNDE2MzQ1NDY4YjliNDA0ODg5YWI1YjQ5"))

    // Get the elements
    val elements = doc >> elementList("div")

    // First element should be detected as a warning due to its class
    assert(matcher.matches(elements(0)))

    // Second element should not be detected as a warning
    assert(!matcher.matches(elements(1)))
  }

  test("findHiddenClasses returns only classes with display:none") {
    val css = ".a{display:none} .b { color: red } .c-d_e {\n  margin: 0;\n  display : none;\n}"
    assert(Utils.HiddenClassMatcher.findHiddenClasses(css) == Set("a", "c-d_e"))
    assert(Utils.HiddenClassMatcher.findHiddenClasses("") == Set.empty[String])
  }

  test("HiddenClassMatcher matches element having a hidden class among several classes") {
    val doc = JsoupBrowser().parseString(
      """<html><body>
        |  <p class="text  hiddenOne other">warning</p>
        |  <p class="text other">normal</p>
        |  <p>no class</p>
        |  <p class="   ">blank class</p>
        |</body></html>""".stripMargin)
    val matcher = Utils.HiddenClassMatcher.fromClassNames(Set("hiddenOne"))
    val ps = doc >> elementList("p")

    assert(ps.map(matcher.matches) == List(true, false, false, false))
  }

  test("HiddenClassMatcher.fromDocument finds and matches hidden elements end-to-end") {
    val doc = JsoupBrowser().parseString(
      """<html><head><style>.rr-warn{display:none;speak:never;}</style></head>
        |<body><div class="chapter-content">
        |  <p>Chapter text</p>
        |  <span class="rr-warn">Stolen from Royal Road, please report</span>
        |  <p>More text</p>
        |</div></body></html>""".stripMargin)
    val matcher = Utils.HiddenClassMatcher.fromDocument(doc)
    val hidden = (doc >> elementList("p,div,span")).filter(matcher.matches).map(_.text)

    assert(hidden == List("Stolen from Royal Road, please report"))
  }

  test("HiddenClassMatcher matches nothing when the document has no hidden classes") {
    val doc = JsoupBrowser().parseString(
      "<html><head><style>.x{color:red}</style></head><body><p class='x'>t</p></body></html>")
    val matcher = Utils.HiddenClassMatcher.fromDocument(doc)
    assert(Utils.HiddenClassMatcher.extractHiddenClassNames(doc).isEmpty)
    assert(!(doc >> elementList("p")).exists(matcher.matches))
  }

}