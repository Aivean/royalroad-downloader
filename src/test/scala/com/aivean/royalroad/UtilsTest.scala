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


  val reportWarnings = List(
    "A case of theft: this story is not rightfully on Amazon; if you spot it, report the violation.",
    "The narrative has been stolen; if detected on Amazon, report the infringement.",
    "If you come across this story on Amazon, it's taken without permission from the author. Report it.",
    "Stolen from its original source, this story is not meant to be on Amazon; report any sightings.",
    "If you spot this story on Amazon, know that it has been stolen. Report the violation.",
    "If you come across this story on Amazon, be aware that it has been stolen from Royal Road. Please report it.",
    "This story has been stolen from Royal Road. If you read it on Amazon, please report it.",
    "If you encounter this story on Amazon, note that it's taken without permission from the author. Report it.",
    "This tale has been pilfered from Royal Road. If found on Amazon, kindly file a report.",
    "Unauthorized use: this story is on Amazon without permission from the author. Report any sightings.",
    "Stolen from Royal Road, this story should be reported if encountered on Amazon.",
    "This narrative has been purloined without the author's approval. Report any appearances on Amazon.",
    "The author's content has been appropriated; report any instances of this story on Amazon.",
    "The narrative has been taken without permission. Report any sightings.",
    "Royal Road's content has been misappropriated; report any instances of this story if found elsewhere.",
    "The story has been taken without consent; if you see it on Amazon, report the incident.",
    "Stolen novel; please report.",
    "Stolen content alert: this content belongs on Royal Road. Report any occurrences."
  )

  test("amazonRegex") {
    reportWarnings.foreach { w =>
      assert(Utils.WarningFuzzyMatcher(w), "Warning: " + w + " should match amazonRegex\n"+
      s"score: ${Utils.WarningFuzzyMatcher.scoreString(w)} > ${Utils.WarningFuzzyMatcher.threshold}")
    }
  }

}
