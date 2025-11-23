name := "NotiLytics-D2"

version := "1.0"

scalaVersion := "2.13.16"


libraryDependencies ++= Seq(
  guice,
  ws,

  // --- PEKKO CORE ---
  "org.apache.pekko" %% "pekko-actor-typed" % "1.1.2",
  "org.apache.pekko" %% "pekko-stream" % "1.1.2",
  "org.apache.pekko" %% "pekko-actor" % "1.1.2",
  "org.apache.pekko" %% "pekko-slf4j" % "1.1.2",

  // --- THIS IS THE MISSING ONE ---
  "org.apache.pekko" %% "pekko-serialization-jackson" % "1.1.2",

  // --- TESTING ---
  "org.apache.pekko" %% "pekko-actor-testkit-typed" % "1.1.2" % Test,
  "org.mockito" % "mockito-core" % "5.8.0" % Test
)

lazy val root = (project in file("."))
  .enablePlugins(PlayJava, PlayNettyServer)
