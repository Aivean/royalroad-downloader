package com.aivean.royalroad

import org.scalatest.FunSuite

import java.net.{URI, URL}

class UtilsTest extends FunSuite {

  // test url to data uri conversion
  test("urlToDataUri") {
    // small red dot
    val url = new URL("https://github.com/Aivean/royalroad-downloader/assets/2865203/e5e66293-d94e-4377-be85-b5dc77f0fbfa")
    val dataUri = Utils.getDataURIForURL(url)
    assert(dataUri ==
      new URI("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAIAAAACDbGyAAAAEUlEQVR4nGP8z4ACmBgo4wMAUJEBCfBOwRcAAAAASUVORK5CYII="))
  }
}
