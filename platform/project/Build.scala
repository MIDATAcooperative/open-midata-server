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
    "org.mongodb" % "mongo-java-driver" % "3.2.2",
    "org.elasticsearch" % "elasticsearch" % "1.2.1",
    "ca.uhn.hapi.fhir" % "hapi-fhir-base" % "1.5",
    "ca.uhn.hapi.fhir" % "hapi-fhir-structures-dstu2" % "1.5"  	    
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
