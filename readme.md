[Royal Road](http://royalroadl.com/) book downloader
---

Nothing fancy, just a simple CLI that imports fiction from  [Royal Road](http://royalroadl.com/)
as html. Use something like [Calibre](http://calibre-ebook.com/) to convert html to any desired format.


How to use
---

* download [release .jar](https://github.com/Aivean/royalroadl-downloader/releases/download/1.0/royalroadl-downloader-assembly-1.0.jar)
* install latest java (maybe you have one, check by executing `java -version`)
* execute `java -jar royalroadl-downloader-assembly-1.0.jar http://royalroadl.com/fiction/xxxx`
* output html file fill be created in current directory


Building from sources
---

* install [sbt](http://www.scala-sbt.org/)
* run `sbt assembly` in project directory


Credits
---

* [Scala](http://www.scala-lang.org/) ❤️
* [Scala Scraper](https://github.com/ruippeixotog/scala-scraper) 