name := "bgbilling-reference-config-generator"

scalaVersion := "2.12.5"

import com.atlassian.labs.gitstamp.GitStampPlugin._

Seq(gitStampSettings: _*)

lazy val root = (project in file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, buildInfoBuildNumber,
      "builtBy" -> {System.getProperty("user.name")},
      "builtOn" -> {java.net.InetAddress.getLocalHost.getHostName},
      "builtAt" -> {new java.util.Date()},
      "builtAtMillis" -> {System.currentTimeMillis()}
    ),
    buildInfoPackage := "version"
  )

libraryDependencies ++= Seq(
  "com.github.scopt" %% "scopt" % "3.7.0",
  "com.github.pathikrit" %% "better-files" % "3.4.0",
  "org.scala-lang.modules" %% "scala-xml" % "1.1.0",
  "org.apache.commons" % "commons-text" % "1.3"
)

assemblyJarName in assembly := s"${name.value}.jar"

fork := true
