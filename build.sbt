lazy val coverageSettings = Seq(
  coverageMinimum := 60,
  coverageFailOnMinimum := false
)

lazy val buildSettings = Seq(
  organization := "com.ithaca",
  scalaOrganization := "org.typelevel",
  scalaVersion := "2.12.0",
  name         := "hephaestus",
  version      := "0.1.0-SNAPSHOT"
)

lazy val commonScalacOptions = Seq(
  "-encoding", "UTF-8",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:experimental.macros",
  "-language:postfixOps",
  "-Ypartial-unification",
  "-Yliteral-types"
)

lazy val commonResolvers = Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots"),
    Resolver.jcenterRepo
)

lazy val commonSettings = Seq(
  resolvers := commonResolvers,
  scalacOptions ++= commonScalacOptions
) ++ coverageSettings ++ buildSettings

lazy val core = (project in file("core")).settings(
  moduleName := "hephaestus-core",
  commonSettings
)

lazy val root = (project in file(".")).aggregate(core)
