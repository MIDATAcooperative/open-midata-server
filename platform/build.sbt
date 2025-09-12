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

scalaVersion := "2.13.16"

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
    "org.apache.pekko" %% "pekko-remote" % "1.1.5",
    "org.apache.pekko" %% "pekko-cluster" % "1.1.5",
    "org.apache.pekko" %% "pekko-cluster-typed" % "1.1.5",
    "org.apache.pekko" %% "pekko-stream" % "1.1.5",
    "org.apache.pekko" %% "pekko-cluster-tools" % "1.1.5",   
    "org.apache.pekko" %% "pekko-serialization-jackson" % "1.1.5",    
    "javax.servlet" % "javax.servlet-api" % "3.1.0",
    "org.playframework" %% "play-mailer" % "10.1.0",
    "org.playframework" %% "play-mailer-guice" % "10.1.0",
    "org.playframework" %% "play-json" % "3.0.5",    
    "com.github.bastiaanjansen" % "otp-java" % "2.1.0",
    "io.nayuki" % "qrcodegen" % "1.8.0"
)
routesGenerator := InjectedRoutesGenerator

// Compile the project before generating Eclipse files, so that generated .scala or .class files for views and routes are present
EclipseKeys.preTasks := Seq(Compile / compile, Test / compile)
EclipseKeys.projectFlavor := EclipseProjectFlavor.Java      
EclipseKeys.createSrc := EclipseCreateSrc.ValueSet(EclipseCreateSrc.ManagedClasses, EclipseCreateSrc.ManagedResources) 
