
import sbt._
import sbt.Keys._
import sbt.Project.Initialize

object AWSBuild extends Build {

  lazy val buildSettings = Seq(
    organization := "aws",
    version      := "0.5-SNAPSHOT",
    scalaVersion := "2.10.2",
    scalacOptions ++= Seq("-feature", "-deprecation", "-unchecked"),
    shellPrompt  := CustomShellPrompt.customPrompt
  )

  override lazy val settings =
    super.settings ++
    buildSettings

  lazy val wrap = Project(
    id       = "wrap",
    base     = file("wrap"),
    settings =
      Defaults.defaultSettings ++
      Publish.settings ++
      Seq(
        resolvers ++= Seq(
          "typesafe" at "http://repo.typesafe.com/typesafe/releases",
          "sonatype" at "http://oss.sonatype.org/content/repositories/releases"
        ),
        libraryDependencies ++= Dependencies.wrap
      )
  )

}

object Dependencies {

  object V {
    val awsJavaSDK  = "1.5.5"

    val jodaTime    = "2.2"
    val jodaConvert = "1.3.1"

    val logback     = "1.0.13"
  }

  object Compile {

    val awsJavaSDK = "com.amazonaws" % "aws-java-sdk" % V.awsJavaSDK

    val jodaTime    = "joda-time" % "joda-time"    % V.jodaTime
    val jodaConvert = "org.joda"  % "joda-convert" % V.jodaConvert

    val logback    = "ch.qos.logback" % "logback-classic" % V.logback
  }

  import Compile._

  val wrap = Seq(awsJavaSDK, jodaTime, jodaConvert, logback)

}

object Publish {

  lazy val settings = Seq(
    publishMavenStyle := true,
    publishTo <<= localPublishTo
  )

  // "AWS" at "http://pellucidanalytics.github.com/aws/repository/"
  def localPublishTo: Initialize[Option[Resolver]] = {
    version { v: String =>
      val localPublishRepo = "../datomisca-repo/"
      if (v.trim endsWith "SNAPSHOT")
        Some(Resolver.file("snapshots", new File(localPublishRepo + "/snapshots")))
      else
        Some(Resolver.file("releases",  new File(localPublishRepo + "/releases")))
    }
  }

}

object CustomShellPrompt {

  val Branch = """refs/heads/(.*)\s""".r

  def gitBranchOrSha =
    Process("git symbolic-ref HEAD") #|| Process("git rev-parse --short HEAD") !! match {
      case Branch(name) => name
      case sha          => sha.stripLineEnd
    }

  val customPrompt = { state: State =>

    val extracted = Project.extract(state)
    import extracted._

    (name in currentRef get structure.data) map { name =>
      "[" + scala.Console.CYAN + name + scala.Console.RESET + "] " +
      scala.Console.BLUE + "git:(" +
      scala.Console.RED + gitBranchOrSha +
      scala.Console.BLUE + ")" +
      scala.Console.RESET + " $ "
    } getOrElse ("> ")

  }
}
