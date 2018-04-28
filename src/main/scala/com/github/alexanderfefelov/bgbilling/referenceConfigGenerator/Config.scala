package com.github.alexanderfefelov.bgbilling.referenceConfigGenerator

import java.io.File

import scopt._
import version._

case class Config(
  inputDir: File = new File("."),
  outputDir: File = new File(".")
)

object Config {

  val parser: OptionParser[Config] = new OptionParser[Config](s"java -jar brcg.jar") {
    head(s"${BuildInfo.name} v. ${BuildInfo.version}",
      """
        |
        |Copyright (C) 2018 Alexander Fefelov <https://github.com/alexanderfefelov>
        |This program comes with ABSOLUTELY NO WARRANTY; see LICENSE file for details.
        |This is free software, and you are welcome to redistribute it under certain conditions; see LICENSE file for details.
      """.stripMargin)

    opt[File]('i', "input-directory")
      .valueName("<directory>")
      .required()
      .action((x, c) => c.copy(inputDir = x))
      .text("Specifies directory containing BGBilling's kernel and modules .jar files. This parameter is required")

    opt[File]('o', "output-directory")
      .valueName("<directory>")
      .required()
      .action((x, c) => c.copy(outputDir =  x))
      .text("Specifies output directory. This parameter is required")

    help("help").text("Prints this usage text")
  }

}
