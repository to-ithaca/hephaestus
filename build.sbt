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
  scalacOptions ++= commonScalacOptions,
  libraryDependencies ++= Seq(
    "com.hackoeur" % "jglm" % "1.0.0",
    "org.typelevel" %% "cats-core" % "0.9.0",
    "org.scodec" %% "scodec-stream" % "1.0.1",
    "org.scodec" %% "scodec-cats" % "0.2.0",
    "org.scalatest" %% "scalatest" % "3.0.1" % "test"
  )
) ++ coverageSettings ++ buildSettings


lazy val core = (project in file("core"))
  .settings(
    moduleName := "hephaestus-core",
    commonSettings,
    target in javah := (target in LocalProject("native")).value / "include"
).dependsOn(native % Runtime)

lazy val native = (project in file("native"))
  .enablePlugins(JniNative)
  .settings(
    moduleName := "hephaestus-native",
    buildSettings,
    sourceDirectory in nativeCompile := baseDirectory.value,
    compile in Compile := {
      Def.sequential(
        javah in LocalProject("core"),
        nativeCompile
      ).value
      (compile in Compile).value
    }
)

lazy val samples = (project in file("samples"))
  .settings(
    moduleName := "hephaestus-samples",
    buildSettings
).dependsOn(core, native)

lazy val root = (project in file(".")).aggregate(core, native, samples)
