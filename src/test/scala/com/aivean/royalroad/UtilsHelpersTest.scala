package com.aivean.royalroad

import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

import org.jsoup.HttpStatusException
import org.scalatest.FunSuite

/**
 * Unit tests for helper functions in [[Utils]] that previously had no coverage:
 * retry, backpressure, withResource, parsingError, fileNameSafe, createDirectoryIfNotExists.
 */
class UtilsHelpersTest extends FunSuite {

  private class CountingResource extends AutoCloseable {
    val closed = new AtomicInteger()

    override def close(): Unit = closed.incrementAndGet()
  }

  // ---------- retry ----------

  test("retry returns result of the first successful call without retrying") {
    val calls = new AtomicInteger()
    val res = Utils.retry {
      calls.incrementAndGet()
      "ok"
    }
    assert(res == "ok")
    assert(calls.get() == 1)
  }

  test("retry calls the function again after a failure") {
    val calls = new AtomicInteger()
    val res = Utils.retry({
      if (calls.incrementAndGet() < 3) throw new RuntimeException("fail") else 42
    }, times = 3)
    assert(res == 42)
    assert(calls.get() == 3)
  }

  test("retry rethrows the last exception when all attempts fail") {
    val calls = new AtomicInteger()
    val e = intercept[IllegalStateException] {
      Utils.retry({
        throw new IllegalStateException("attempt " + calls.incrementAndGet())
      }, times = 3)
    }
    assert(calls.get() == 3)
    assert(e.getMessage == "attempt 3")
  }

  test("retry with times = 1 makes exactly one attempt") {
    val calls = new AtomicInteger()
    intercept[RuntimeException] {
      Utils.retry({
        calls.incrementAndGet()
        throw new RuntimeException("fail")
      }, times = 1)
    }
    assert(calls.get() == 1)
  }

  // ---------- backpressure ----------

  test("backpressure returns result when the call succeeds") {
    assert(Utils.backpressure("value") == "value")
  }

  test("backpressure rethrows non-HTTP exceptions immediately") {
    val calls = new AtomicInteger()
    intercept[IllegalArgumentException] {
      Utils.backpressure {
        calls.incrementAndGet()
        throw new IllegalArgumentException("boom")
      }
    }
    assert(calls.get() == 1)
  }

  test("backpressure rethrows HTTP errors other than 429 without retrying") {
    val calls = new AtomicInteger()
    val e = intercept[HttpStatusException] {
      Utils.backpressure {
        calls.incrementAndGet()
        throw new HttpStatusException("Not found", 404, "https://www.royalroad.com/x")
      }
    }
    assert(e.getStatusCode == 404)
    assert(calls.get() == 1)
  }

  test("backpressure retries the call after HTTP 429 (Too Many Requests)") {
    val calls = new AtomicInteger()
    val res = Utils.backpressure({
      if (calls.incrementAndGet() == 1)
        throw new HttpStatusException("Too many requests", 429, "https://www.royalroad.com/x")
      else "done"
    }, delayMs = 10)
    assert(res == "done")
    assert(calls.get() == 2)
  }

  // ---------- withResource ----------

  test("withResource returns block result and closes the resource") {
    val r = new CountingResource
    val res = Utils.withResource(r)(_ => "result")
    assert(res == "result")
    assert(r.closed.get() == 1)
  }

  test("withResource closes the resource when the block throws") {
    val r = new CountingResource
    intercept[RuntimeException] {
      Utils.withResource(r)(_ => throw new RuntimeException("block failed"))
    }
    assert(r.closed.get() == 1)
  }

  test("withResource propagates the original exception if resource creation fails") {
    var blockCalled = false
    val e = intercept[IllegalStateException] {
      Utils.withResource[CountingResource, Unit](throw new IllegalStateException("cannot open")) { _ =>
        blockCalled = true
      }
    }
    assert(e.getMessage == "cannot open")
    assert(!blockCalled)
  }

  // ---------- parsingError ----------

  test("parsingError throws IllegalStateException with selector details") {
    val e = intercept[IllegalStateException] {
      Utils.parsingError("chapter title", "h1.title", "https://www.royalroad.com/fiction/1/chapter/2")
    }
    assert(e.getMessage.contains("chapter title"))
    assert(e.getMessage.contains("`h1.title`"))
    assert(e.getMessage.contains("https://www.royalroad.com/fiction/1/chapter/2"))
  }

  // ---------- fileNameSafe ----------

  test("fileNameSafe keeps word characters untouched") {
    assert(Utils.fileNameSafe("Chapter_12abc") == "Chapter_12abc")
  }

  test("fileNameSafe replaces groups of unsafe characters with a single underscore") {
    assert(Utils.fileNameSafe("Mother of Learning") == "Mother_of_Learning")
    assert(Utils.fileNameSafe("a / b : c") == "a_b_c")
    assert(Utils.fileNameSafe("What?!*<>|\"") == "What_")
  }

  // ---------- createDirectoryIfNotExists ----------

  test("createDirectoryIfNotExists creates all missing parent directories") {
    val tmp = Files.createTempDirectory("rr-test").toFile
    val target = new File(tmp, "a/b/c/book.html")
    Utils.createDirectoryIfNotExists(target.getPath)
    assert(target.getParentFile.isDirectory)
    assert(!target.exists(), "only directories must be created, not the file itself")
  }

  test("createDirectoryIfNotExists does nothing for existing directory or bare file name") {
    val tmp = Files.createTempDirectory("rr-test").toFile
    Utils.createDirectoryIfNotExists(new File(tmp, "book.html").getPath)
    assert(tmp.isDirectory)
    // no parent directory at all - must not throw
    Utils.createDirectoryIfNotExists("book.html")
  }
}
