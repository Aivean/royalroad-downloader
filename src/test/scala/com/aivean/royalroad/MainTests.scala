package com.aivean.royalroad

import com.aivean.royalroad.Main.handleFromArg
import org.scalatest.FunSuite

class MainTests extends FunSuite {

  test("handleFromArg should drop elements when fromChap is positive") {
    val chaps = Seq(1, 2, 3, 4, 5)
    val result = handleFromArg(chaps, 3)
    assert(result == Seq(3, 4, 5))
  }

  test("handleFromArg should take right elements when fromChap is negative") {
    val chaps = Seq(1, 2, 3, 4, 5)
    val result = handleFromArg(chaps, -2)
    assert(result == Seq(4, 5))
  }

  test("handleFromArg should return the same sequence when fromChap is zero") {
    val chaps = Seq(1, 2, 3, 4, 5)
    val result = handleFromArg(chaps, 0)
    assert(result == chaps)
  }

  test("handleFromArg should handle an empty sequence") {
    val chaps = Seq.empty[Int]
    val result = handleFromArg(chaps, 2)
    assert(result.isEmpty)
  }

  // If you want to go further and test with different types
  test("handleFromArg should handle sequence of Strings") {
    val chaps = Seq("A", "B", "C")
    val result = handleFromArg(chaps, -1)
    assert(result == Seq("C"))
  }
}
