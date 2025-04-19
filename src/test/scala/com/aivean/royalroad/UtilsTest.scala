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

  // test url to data uri conversion
  test("urlToDataUriImgur") {
    val url = new URL("https://i.imgur.com/kp112Ad.png")
    val dataUri = Utils.getDataURIForURL(url)
    assert(dataUri.toString.startsWith("data:image/png;base64,"))
  }


  test("renderTemplate") {
    assert(Utils.renderTemplate(
      "{title}_{chapters}_{date}",
      Map("title" -> "MyTitle", "chapters" -> "1-2", "date" -> "2022-01-01")
    ) == "MyTitle_1-2_2022-01-01")

    assert(Utils.renderTemplate(
      "{title}{}{chapters}{}{date}",
      Map("title" -> "MyTitle", "chapters" -> "1-2", "date" -> "2022-01-01")
    ) == "MyTitle_1-2_2022-01-01")

    assert(Utils.renderTemplate(
      "{}{title}{}{chapters}{}{date}{}",
      Map("title" -> "MyTitle", "chapters" -> "", "date" -> "2022-01-01")
    ) == "MyTitle_2022-01-01")

    assert(Utils.renderTemplate(
      "{}{title}{}{chapters}{}{date}{}.html{}",
      Map("title" -> "MyTitle", "chapters" -> "", "date" -> "2022-01-01")
    ) == "MyTitle_2022-01-01.html")

    assert(Utils.renderTemplate(
      "{}{oops}{}",
      Map()
    ) == "{oops}")

  }

}
