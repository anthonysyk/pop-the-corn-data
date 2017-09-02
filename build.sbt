name := "pop-the-corn-data"

version := "1.0"

scalaVersion := "2.11.7"

lazy val sparkV = "2.0.2"

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkV,
  "org.apache.spark" %% "spark-sql" % sparkV,
  "org.apache.spark" %% "spark-mllib" % sparkV,
  "org.elasticsearch" % "elasticsearch-spark-20_2.11" % "5.5.1",
  "com.chuusai" %% "shapeless" % "2.3.2",
  "org.json4s" % "json4s-jackson_2.11" % "3.5.3",
  "com.quantifind" %% "wisp" % "0.0.4",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.6.0",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.6.0" classifier "models"
)

lazy val circeVersion = "0.8.0"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)