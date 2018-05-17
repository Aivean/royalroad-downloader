[Royal Road](http://royalroadl.com/) book downloader
---

Nothing fancy, just a simple CLI that imports fiction from  [Royal Road](http://royalroadl.com/)
as html. Use something like [Calibre](http://calibre-ebook.com/) to convert html to any desired format.


How to use
---

* download latest [release .jar](https://github.com/Aivean/royalroadl-downloader/releases/latest)
* install latest java (maybe you have one, check by executing `java -version`)
* execute `java -jar royalroadl-downloader-assembly-VERSION.jar http://royalroadl.com/fiction/xxxx`
    (replace `VERSION` and `xxxx` with corresponding numbers)
* output html file will be created in current directory


Troubleshooting
---

1. It may happen that RoyalRoad changes it's html format and this parser is no longer working.
   In such case please [create an issue](https://github.com/Aivean/royalroadl-downloader/issues).
   You also can try to work around the problem by overriding [CSS selectors](http://www.w3schools.com/cssref/css_selectors.asp)
that are used to fetch chapter title and body using `-t` and `-b` program arguments.

   Default css selectors are:

   * `title` for title
   * `div.chapter-content` for chapter body

   Run the program with `--help` argument to see usage reference.


2. `Exception in thread "main" java.lang.OutOfMemoryError: Java heap space`
   This may happen on large books. The solution is to give JVM more memory by setting `-Xmx` flag:

        java -Xmx2G -jar ...



Building from sources
---

* install [sbt](http://www.scala-sbt.org/)
* run `sbt assembly` in project directory


Credits
---

* [Scala](http://www.scala-lang.org/) ❤️
* [Scala Scraper](https://github.com/ruippeixotog/scala-scraper)
* [Scallop](https://github.com/scallop/scallop)
