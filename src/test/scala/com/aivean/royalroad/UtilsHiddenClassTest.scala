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

}