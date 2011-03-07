import sbt._

class Album2Tumblr(info: ProjectInfo) extends DefaultProject(info) {
  val scalatoolsRelease = "Scala Tools Snapshot" at
    "http://scala-tools.org/repo-releases/"

  val httpComponentsVersion = "4.1"
  val metadataExtractorVersion = "2.4.0-beta-1"

  override def libraryDependencies = Set(
    "org.apache.httpcomponents" % "httpclient" % httpComponentsVersion % "compile->default",
    "org.apache.httpcomponents" % "httpmime" % httpComponentsVersion % "compile->default",
    "com.drewnoakes" % "metadata-extractor" % metadataExtractorVersion % "compile->default",
    "ch.qos.logback" % "logback-classic" % "0.9.26",
    "junit" % "junit" % "4.5" % "test->default",
    "org.scala-tools.testing" %% "specs" % "1.6.6" % "test->default"
  ) ++ super.libraryDependencies
}