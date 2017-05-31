//enablePlugins(ScalaJSPlugin, WorkbenchPlugin)
enablePlugins(ScalaJSPlugin)

name := "mazeRunner"

version := "0.1-SNAPSHOT"

scalaVersion := "2.12.2"

libraryDependencies ++= Seq(
  "org.scala-js" %%% "scalajs-dom" % "0.9.1"
)
