import sbt._
import Keys._
import play.Play.autoImport._
import PlayKeys._
import com.typesafe.sbt.less.Import.LessKeys
import com.typesafe.sbt.web.Import.Assets

object ApplicationBuild extends Build {

  val appName         = "hdc"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here
    javaCore,
    javaWs,
    "org.mongodb" % "mongo-java-driver" % "3.6.3",
    "ca.uhn.hapi.fhir" % "hapi-fhir-base" % "3.2.0",
    "ca.uhn.hapi.fhir" % "hapi-fhir-structures-dstu3" % "3.2.0",  	    
    "ca.uhn.hapi.fhir" % "hapi-fhir-server" % "3.2.0",
    "org.thymeleaf" % "thymeleaf" % "3.0.1.RELEASE",
    "com.fasterxml.jackson.core" % "jackson-core" % "2.8.3",
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.3",
    "com.fasterxml.jackson.core" % "jackson-annotations" % "2.8.3",   
    "com.typesafe.akka" %% "akka-remote" % "2.3.16",
    "com.typesafe.akka" %% "akka-cluster" % "2.3.16",
    "com.typesafe.akka" %% "akka-contrib" % "2.3.16"
  )


  val main = Project(appName, file(".")).enablePlugins(play.PlayJava).settings(
  	// Add your own project settings here
  	version := appVersion,
  	Keys.includeFilter in (Assets, LessKeys.less) := "*.less",
  	Keys.excludeFilter in (Assets, LessKeys.less) := "_*.less",
  	libraryDependencies ++= appDependencies,
  	libraryDependencies ++= Seq(
  	  "com.typesafe.play.plugins" %% "play-plugins-mailer" % "2.3.1"  	  
  	)  	 
  )

}
