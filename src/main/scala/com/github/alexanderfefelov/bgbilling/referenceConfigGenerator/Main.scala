package com.github.alexanderfefelov.bgbilling.referenceConfigGenerator

import java.nio.charset.Charset
import java.util.Properties

import better.files._
import org.apache.commons.text.StringEscapeUtils
import org.jsoup.Jsoup

import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks._
import scala.xml._

case class KeyElem(key: String, typ: String, valid: String, default: String, title: String, help: NodeSeq)
case class SubKeyElem(mask: String, parentKey: String, typ: String, valid: String, default: String, title: String, help: NodeSeq)

object Main extends App {

  implicit val charset: Charset = Charset.forName("UTF-8")

  Config.parser.parse(args, Config()) match {
    case Some(config) => justDoIt(config)
    case None =>
  }

  private def justDoIt(config: Config): Unit = {
    val inputDirectory = File(config.inputDir.getAbsolutePath)
    if (!inputDirectory.exists || !inputDirectory.isDirectory || !inputDirectory.isReadable) {
      println(s"Error: $inputDirectory doesn't exist, or is not a directory, or is not readable")
      return
    }
    val outputDirectory = File(config.outputDir.getAbsolutePath)
    if (!outputDirectory.exists || !outputDirectory.isDirectory || !outputDirectory.isWriteable) {
      println(s"Error: $outputDirectory doesn't exist, or is not a directory, or is not writable")
      return
    }
    println(s"Input: $inputDirectory")
    println(s"Output: $outputDirectory")

    val jarFiles = inputDirectory.glob("*.jar").toList
    if (jarFiles.isEmpty) {
      println("Error: Input directory contains no .jar files")
      return
    }
    println(s"${jarFiles.size} .jar file(s) found")
    for (jarFile <- jarFiles) {
      File.usingTemporaryDirectory() { tempDir =>
        breakable {
          print(s"${jarFile.name} ... ")
          val fileName = jarFile.name.stripSuffix(".jar")
          val moduleName = if (fileName == "kernel") "server" else fileName

          // Find and parse MODULE_NAME.properties
          //
          val propertiesRelativePath = s"ru/bitel/bgbilling/properties/$moduleName.properties"
          jarFile.unzipTo(tempDir, e => e.getName == propertiesRelativePath)
          val propertiesFile = tempDir / propertiesRelativePath
          if (!propertiesFile.exists) {
            println(s"$fileName.properties not found")
            break
          }
          var (name, version, buildNumber, buildTime) = ("N/A", "N/A", "N/A", "N/A")
          propertiesFile.fileInputStream { inputStream =>
            val properties = new Properties()
            properties.load(inputStream)
            name = properties.getProperty("name")
            version = properties.getProperty("version")
            buildNumber = properties.getProperty("build.number")
            buildTime = properties.getProperty("build.time")
          }
          print(s"$name ${version}_$buildNumber $buildTime ... ")

          // Find and parse config.xml
          //
          val configXmlRelativePaths = Seq(
            s"ru/bitel/bgbilling/modules/$name/server/config.xml",
            s"bitel/billing/server/$name/config.xml",
            s"ru/bitel/bgbilling/$name/admin/config.xml"
          )
          var configXmlFile = File("""¯\_(ツ)_/¯""")
          breakable {
            for (configXmlRelativePath <- configXmlRelativePaths) {
              jarFile.unzipTo(tempDir, e => e.getName == configXmlRelativePath)
              configXmlFile = tempDir / configXmlRelativePath
              if (configXmlFile.exists) {
                break
              }
            }
          }
          if (!configXmlFile.exists) {
            println("config.xml not found")
            break
          }
          val configXml = XML.loadFile(configXmlFile.pathAsString)
          var keyElems = new ListBuffer[KeyElem]()
          configXml \ "key" foreach { node =>
            val keyElem = KeyElem(
              node \@ "key",
              node \@ "type",
              node \@ "valid",
              node \@ "default",
              node \@ "title",
              node \\ "help"
            )
            keyElems += keyElem
          }
          var subKeyElems = new ListBuffer[SubKeyElem]()
          configXml \ "subKey" foreach { node =>
            val subKeyElem = SubKeyElem(
              node \@ "mask",
              node \@ "parentKey",
              node \@ "type",
              node \@ "valid",
              node \@ "default",
              node \@ "title",
              node \\ "help"
            )
            subKeyElems += subKeyElem
          }

          // Generate reference config
          //
          val referenceConfFile = outputDirectory / s"$name ${version}_$buildNumber (${buildTime.replace(':', '.')}).conf"
          referenceConfFile.overwrite(bannerText(name, version, buildNumber, buildTime))
          for (keyElem <- keyElems.sortBy(_.key)) {
            referenceConfFile.appendLine(keyToText(keyElem))
            for (subKeyElem <- subKeyElems.filter(_.parentKey == keyElem.key).sortBy(_.mask)) {
              referenceConfFile.appendLine(subKeyToText(subKeyElem))
            }
          }
          referenceConfFile.appendLine("# ---[ EOF ]---")

          println("done")
        }
      }
    }
  }

  private def bannerText(name: String, version: String, buildNumber: String, buildTime: String): String = {
    s"""
      |# THIS FILE IS AUTOMATICALLY GENERATED. DO NOT EDIT!
      |
      |# ---------------------------------------------------------------------------------------
      |# Reference config for $name ${version}_$buildNumber ($buildTime)
      |# Generated by <https://github.com/alexanderfefelov/bgbilling-reference-config-generator>
      |# ---------------------------------------------------------------------------------------
      |
    """.stripMargin
  }

  private def keyToText(keyElem: KeyElem): String = {
    s"""
      |# ---[ Key: ${keyElem.key} ]---
      |# Type: ${keyElem.typ}
      |# Valid values: ${keyElem.valid}
      |# Default value: ${keyElem.default}
      ${helpToText(keyElem.help)}
      |# ${keyElem.key} =
    """.stripMargin
  }

  private def subKeyToText(subKeyElem: SubKeyElem): String = {
    s"""
      |# ---[ Sub-key: ${subKeyElem.mask} ]---
      |# Parent key: ${subKeyElem.parentKey}
      |# Type: ${subKeyElem.typ}
      |# Valid values: ${subKeyElem.valid}
      |# Default value: ${subKeyElem.default}
      ${helpToText(subKeyElem.help)}
      |# ${subKeyElem.mask} =
    """.stripMargin
  }

  private def helpToText(help: NodeSeq): String = {
    val s = Jsoup.parse(
      StringEscapeUtils.unescapeHtml4(
        help.toString()
      )
    ).text().trim
    if (!s.isEmpty) {
      s"""|#
        |# $s
        |#"""
    } else {
      "|#"
    }
  }

}
