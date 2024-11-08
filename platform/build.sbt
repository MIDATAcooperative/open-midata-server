/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

name := """midata-server"""
organization := "midata.coop"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.13.14"

libraryDependencies += guice

libraryDependencies ++= Seq(
    ws,
    "org.mongodb" % "mongodb-driver-legacy" % "5.2.0",
    "joda-time" % "joda-time" % "2.10.10",
    "ca.uhn.hapi.fhir" % "hapi-fhir-base" % "6.1.2",
    "ca.uhn.hapi.fhir" % "hapi-fhir-structures-dstu3" % "6.1.2",
    "ca.uhn.hapi.fhir" % "hapi-fhir-structures-r4" % "6.1.2",  	    
    "ca.uhn.hapi.fhir" % "hapi-fhir-server" % "6.1.2",
    "org.apache.jena" % "jena-core" % "4.1.0",
    "org.apache.jena" % "jena-arq" % "4.1.0",
    "org.apache.jena" % "jena-tdb" % "4.1.0",
//    "org.thymeleaf" % "thymeleaf" % "3.0.1.RELEASE",
    "com.fasterxml.jackson.core" % "jackson-core" % "2.13.2",
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.13.2",
    "com.fasterxml.jackson.core" % "jackson-annotations" % "2.13.2",
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.13.2",
    "com.typesafe.akka" %% "akka-remote" % "2.6.21",
    "com.typesafe.akka" %% "akka-cluster" % "2.6.21",
    "com.typesafe.akka" %% "akka-cluster-typed" % "2.6.21",
    "com.typesafe.akka" %% "akka-stream" % "2.6.21",
    "com.typesafe.akka" %% "akka-cluster-tools" % "2.6.21",    
    "javax.servlet" % "javax.servlet-api" % "3.1.0",
    "com.typesafe.play" %% "play-mailer" % "9.0.0",
    "com.typesafe.play" %% "play-mailer-guice" % "9.0.0",
    "com.typesafe.play" %% "play-json" % "2.10.6"        
)
routesGenerator := InjectedRoutesGenerator

// Compile the project before generating Eclipse files, so that generated .scala or .class files for views and routes are present
EclipseKeys.preTasks := Seq(compile in Compile, compile in Test)
EclipseKeys.projectFlavor := EclipseProjectFlavor.Java      
EclipseKeys.createSrc := EclipseCreateSrc.ValueSet(EclipseCreateSrc.ManagedClasses, EclipseCreateSrc.ManagedResources) 
