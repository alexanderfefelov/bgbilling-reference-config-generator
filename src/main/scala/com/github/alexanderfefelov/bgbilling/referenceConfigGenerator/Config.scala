package com.github.alexanderfefelov.bgbilling.referenceConfigGenerator

import better.files.Dsl._
import scopt._
import version._

case class Config(
  inputDir: String = pwd.pathAsString,
  outputDir: String = pwd.pathAsString
)

object Config {

  val parser = new OptionParser[Config](s"java -jar brcg.jar") {
    head(s"${BuildInfo.name} v. ${BuildInfo.version}",
      """
        |
        |Copyright (C) 2018 Alexander Fefelov <https://github.com/alexanderfefelov>
        |This program comes with ABSOLUTELY NO WARRANTY; see LICENSE file for details.
        |This is free software, and you are welcome to redistribute it under certain conditions; see LICENSE file for details.
      """.stripMargin)

    opt[String]('i', "input-directory")
      .valueName("<directory>")
      .action((x, c) => c.copy(inputDir = x))
      .text("Specifies directory containing BGBilling's kernel and modules .jar files. Default value: current directory")

    opt[String]('o', "output-directory")
      .valueName("<directory>")
      .action((x, c) => c.copy(outputDir = x))
      .text("Specifies output directory. Default value: current directory")

    help("help").text("Prints this usage text")
  }

}
