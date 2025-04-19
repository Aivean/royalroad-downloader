[Royal Road](http://royalroad.com/) book downloader
---

A simple command line tool that downloads fiction from [Royal Road](http://royalroad.com/)
as html. Use [Calibre](http://calibre-ebook.com/) to convert html to any desired format.

![test](https://github.com/Aivean/royalroad-downloader/actions/workflows/test.yml/badge.svg?branch=master)
[![Github All Releases](https://img.shields.io/github/downloads/Aivean/royalroad-downloader/total.svg)](https://github.com/Aivean/royalroad-downloader/releases/latest)
[![GitHub last commit](https://img.shields.io/github/last-commit/Aivean/royalroad-downloader.svg)](https://github.com/Aivean/royalroad-downloader/commits/master)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Features
---

* Downloads complete fiction or specific chapter range from Royal Road as HTML
* Preserves chapter structure and formatting
* Removes content warnings automatically (`--remove-warnings` option)
* Embeds images with `--e` option
* Generates clean HTML output ready for conversion to e-book formats

How to use
---

* download the latest [release .jar](https://github.com/Aivean/royalroad-downloader/releases/latest)
* install the latest java (maybe you have one, check by executing `java -version`)
* execute `java -jar royalroad-downloader-assembly-VERSION.jar https://royalroadl.com/fiction/...`
    (replace royalroad fiction URL with your link and `VERSION` with the corresponding number) 
* the output html file will be created in the current directory
 

Run `java -jar royalroad-downloader-assembly-VERSION.jar --help` to see the full usage reference.


Troubleshooting
---

It may happen that RoyalRoad changes its HTML format and this parser is no longer working.
In that case, please [create an issue](https://github.com/Aivean/royalroad-downloader/issues).
You may also try to work around the problem by overriding [CSS selectors](http://www.w3schools.com/cssref/css_selectors.asp)
that are used to fetch chapter title and body by supplying `-t` and `-b` program arguments.

Default css selectors are:

* `title` for the title
* `div.chapter-content` for the chapter body

Run the program with `--help` argument to see the usage reference.


Building from sources
---

* run `sbt/sbt assembly` in the project directory

Note: build tested to work only with Java 8 (will probably fail on other versions).

Running from sources
--------------------

* run `sbt/sbt --error 'run <arguments>'` in project directory,
    i.e.:

        sbt/sbt --error 'run --help'

Credits
---

* [Scala](http://www.scala-lang.org/) ❤️
* [Scala Scraper](https://github.com/ruippeixotog/scala-scraper)
* [Scallop](https://github.com/scallop/scallop)
