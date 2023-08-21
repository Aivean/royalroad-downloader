package com.aivean.royalroad

import com.aivean.royalroad.Main.extractFictionLink
import org.scalatest.FunSuite

/**
 *
 * @author <a href="mailto:aiveeen@gmail.com">Ivan Zaitsev</a>
 *         2021-04-19
 */
class ArgsTest extends FunSuite {

  test("Url argument validation") {

    for (
      http <- List("http", "https");
      www <- List("", "www.");
      l <- List("", "l")
    ) {
      assert(s"${http}://${www}royalroad${l}.com/fiction/12345".matches(Args.urlValidationRegex))
      assert(s"${http}://${www}royalroad${l}.com/fiction/12345/".matches(Args.urlValidationRegex))
      assert(s"${http}://${www}royalroad${l}.com/fiction/12345/name".matches(Args.urlValidationRegex))
      assert(s"${http}://${www}royalroad${l}.com/fiction/12345/na-me".matches(Args.urlValidationRegex))
      assert(s"${http}://${www}royalroad${l}.com/fiction/12345/name/".matches(Args.urlValidationRegex))
      assert(s"${http}://${www}royalroad${l}.com/fiction/12345/name/chapter/123/chapter-name".matches(Args.urlValidationRegex))
      assert(s"${http}://${www}royalroad${l}.com/fiction/123/name//".matches(Args.urlValidationRegex))
      assert(s"${http}://${www}royalroad${l}.com/fiction/12345//".matches(Args.urlValidationRegex))
      assert(s"${http}://${www}royalroad${l}.com/fiction/123//".matches(Args.urlValidationRegex))

      assert(!s"${http}://${www}royalroad${l}.com/fiction/".matches(Args.urlValidationRegex))
      assert(!s"${http}://${www}royalroad${l}.com/fiction//".matches(Args.urlValidationRegex))
    }
  }

  test("extract fiction link") {
    for (
      http <- List("http", "https");
      www <- List("", "www.");
      l <- List("", "l")
    ) {
      assert(extractFictionLink(s"${http}://${www}royalroad${l}.com/fiction/12345") ==
        s"${http}://${www}royalroad${l}.com/fiction/12345")
      assert(extractFictionLink(s"${http}://${www}royalroad${l}.com/fiction/12345/") ==
        s"${http}://${www}royalroad${l}.com/fiction/12345")
      assert(extractFictionLink(s"${http}://${www}royalroad${l}.com/fiction/12345/name") ==
        s"${http}://${www}royalroad${l}.com/fiction/12345")
      assert(extractFictionLink(s"${http}://${www}royalroad${l}.com/fiction/12345/na-me") ==
        s"${http}://${www}royalroad${l}.com/fiction/12345")
      assert(extractFictionLink(s"${http}://${www}royalroad${l}.com/fiction/12345/name/") ==
        s"${http}://${www}royalroad${l}.com/fiction/12345")
      assert(extractFictionLink(s"${http}://${www}royalroad${l}.com/fiction/12345/name/chapter/123/chapter-name") ==
        s"${http}://${www}royalroad${l}.com/fiction/12345")
      assert(extractFictionLink(s"${http}://${www}royalroad${l}.com/fiction/123/name//") ==
        s"${http}://${www}royalroad${l}.com/fiction/123")
      assert(extractFictionLink(s"${http}://${www}royalroad${l}.com/fiction/12345//") ==
        s"${http}://${www}royalroad${l}.com/fiction/12345")
      assert(extractFictionLink(s"${http}://${www}royalroad${l}.com/fiction/123//") ==
        s"${http}://${www}royalroad${l}.com/fiction/123")
    }
  }
}
