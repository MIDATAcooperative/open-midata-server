name := """midata-server"""
organization := "midata.coop"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.12.8"

libraryDependencies += guice

libraryDependencies ++= Seq(
    ws,
    "org.mongodb" % "mongo-java-driver" % "3.6.3",
    "ca.uhn.hapi.fhir" % "hapi-fhir-base" % "3.7.0",
    "ca.uhn.hapi.fhir" % "hapi-fhir-structures-dstu3" % "3.7.0",
    "ca.uhn.hapi.fhir" % "hapi-fhir-structures-r4" % "3.7.0",  	    
    "ca.uhn.hapi.fhir" % "hapi-fhir-server" % "3.7.0",
    "org.thymeleaf" % "thymeleaf" % "3.0.1.RELEASE",
    "com.fasterxml.jackson.core" % "jackson-core" % "2.8.3",
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.3",
    "com.fasterxml.jackson.core" % "jackson-annotations" % "2.8.3",
    "com.typesafe.akka" %% "akka-remote" % "2.5.21",
    "com.typesafe.akka" %% "akka-cluster" % "2.5.21",
    "com.typesafe.akka" %% "akka-contrib" % "2.5.21",
    "com.typesafe.akka" %% "akka-stream" % "2.5.21",
    "com.typesafe.akka" %% "akka-cluster-tools" % "2.5.21",
    "javax.servlet" % "javax.servlet-api" % "3.1.0",
    "com.typesafe.play" %% "play-mailer" % "6.0.1",
    "com.typesafe.play" %% "play-mailer-guice" % "6.0.1",
    "com.typesafe.play" %% "play-json" % "2.6.0"        
)
routesGenerator := InjectedRoutesGenerator

// Compile the project before generating Eclipse files, so that generated .scala or .class files for views and routes are present
EclipseKeys.preTasks := Seq(compile in Compile, compile in Test)
EclipseKeys.projectFlavor := EclipseProjectFlavor.Java      
EclipseKeys.createSrc := EclipseCreateSrc.ValueSet(EclipseCreateSrc.ManagedClasses, EclipseCreateSrc.ManagedResources) 