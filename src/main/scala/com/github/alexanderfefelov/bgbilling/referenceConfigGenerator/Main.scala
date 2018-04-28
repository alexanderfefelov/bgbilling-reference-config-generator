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
    println(s"Input: ${File(config.inputDir).pathAsString}")
    println(s"Output: ${File(config.outputDir).pathAsString}")

    val jarFiles = File(config.inputDir).glob("*.jar").toList
    if (jarFiles.isEmpty) {
      println("Input directory contains no .jar files")
      return
    }
    println(s"${jarFiles.size} .jar file(s) found")
    for (jarFile <- jarFiles) {
      File.usingTemporaryDirectory() { tempDir =>
        breakable {
          print(s"${jarFile.name} ... ")
          jarFile.unzipTo(tempDir)
          val fileName = jarFile.name.stripSuffix(".jar")
          val moduleName = if (fileName == "kernel") "server" else fileName

          val propertiesFile = tempDir / s"ru/bitel/bgbilling/properties/$moduleName.properties"
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

          val referenceConfFile = config.outputDir / s"$name ${version}_$buildNumber ${buildTime.replace(':', '.')}.conf"
          referenceConfFile
            .overwrite(
              s"""
                |# THIS FILE IS AUTOMATICALLY GENERATED. DO NOT EDIT!
                |
                |# ---------------------------------------------------------------------------------------
                |# Reference config for $name ${version}_$buildNumber $buildTime
                |# Generated by <https://github.com/alexanderfefelov/bgbilling-reference-config-generator>
                |# ---------------------------------------------------------------------------------------
                |
              """.stripMargin
            )

          var configXmlFile = tempDir / s"ru/bitel/bgbilling/modules/$name/server/config.xml"
          if (!configXmlFile.exists) {
            configXmlFile = tempDir / s"bitel/billing/server/$name/config.xml"
            if (!configXmlFile.exists) {
              configXmlFile = tempDir / s"ru/bitel/bgbilling/$name/admin/config.xml"
              if (!configXmlFile.exists) {
                println("config.xml not found")
                break
              }
            }
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

          for (keyElem <- keyElems.sortBy(_.key)) {
            referenceConfFile.appendLine(
              s"""
                 |# ---[ Key: ${keyElem.key} ]---
                 |# Type: ${keyElem.typ}
                 |# Valid values: ${keyElem.valid}
                 |# Default value: ${keyElem.default}
                 |#
                 |# ${helpToText(keyElem.help)}
                 |#
                 |# ${keyElem.key} =
               """.stripMargin
            )

            for (subKeyElem <- subKeyElems.filter(_.parentKey == keyElem.key).sortBy(_.mask)) {
              referenceConfFile.appendLine(
                s"""
                   |# ---[ Sub-key: ${subKeyElem.mask} ]---
                   |# Parent key: ${subKeyElem.parentKey}
                   |# Type: ${subKeyElem.typ}
                   |# Valid values: ${subKeyElem.valid}
                   |# Default value: ${subKeyElem.default}
                   |#
                   |# ${helpToText(subKeyElem.help)}
                   |#
                   |# ${subKeyElem.mask} =
               """.stripMargin
              )
            }
          }

          referenceConfFile.appendLine("# ---[ EOF ]---")
          println("done")
        }
      }
    }
  }

  private def helpToText(help: NodeSeq) = {
    Jsoup.parse(
      StringEscapeUtils.unescapeHtml4(
        help.toString()
      )
    ).text()
  }

}
