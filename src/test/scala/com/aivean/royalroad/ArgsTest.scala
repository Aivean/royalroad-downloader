package com.aivean.royalroad

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

      assert(!s"${http}://${www}royalroad${l}.com/fiction/12345//".matches(Args.urlValidationRegex))
      assert(!s"${http}://${www}royalroad${l}.com/fiction/".matches(Args.urlValidationRegex))
      assert(!s"${http}://${www}royalroad${l}.com/fiction//".matches(Args.urlValidationRegex))
      assert(!s"${http}://${www}royalroad${l}.com/fiction/123//".matches(Args.urlValidationRegex))
      assert(!s"${http}://${www}royalroad${l}.com/fiction/123/name//".matches(Args.urlValidationRegex))
    }
  }
}
