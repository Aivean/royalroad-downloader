name := "royalroadl-downloader"

version := "1.2.0"

scalaVersion := "2.11.8"

libraryDependencies += "net.ruippeixotog" %% "scala-scraper" % "0.1.2"
libraryDependencies += "org.rogach" %% "scallop" % "1.0.1"

mainClass in assembly := Some("com.aivean.royalroad.Main")