name := "akka-persistence-jdbc-test"

organization := "com.github.dnvriend"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.12.2"

// improves type constructor inference with support for partial unification,
// fixing the notorious SI-2712.
scalacOptions += "-Ypartial-unification"

//scalacOptions += "-Ydelambdafy:method"
scalacOptions += "-Ydelambdafy:inline"

val AkkaVersion = "2.4.18"
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % AkkaVersion
libraryDependencies +="com.typesafe.akka" %% "akka-slf4j" % AkkaVersion
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "com.github.dnvriend" %% "akka-persistence-jdbc" % "2.4.18.0"
libraryDependencies += "org.postgresql" % "postgresql" % "42.0.0"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.3"

// testing configuration
fork in Test := true
parallelExecution := false

licenses +=("Apache-2.0", url("http://opensource.org/licenses/apache2.0.php"))

// enable scala code formatting //
import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform

// Scalariform settings
SbtScalariform.autoImport.scalariformPreferences := SbtScalariform.autoImport.scalariformPreferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
  .setPreference(DoubleIndentClassDeclaration, true)

// enable updating file headers //
import de.heikoseeberger.sbtheader.license.Apache2_0

headers := Map(
  "scala" -> Apache2_0("2017", "Dennis Vriend"),
  "conf" -> Apache2_0("2017", "Dennis Vriend", "#")
)

enablePlugins(AutomateHeaderPlugin, SbtScalariform)
