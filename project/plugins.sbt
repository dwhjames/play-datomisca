// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository
resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"

// Use the Play sbt plugin for Play projects.  See https://playframework.com/
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.4.6")

addSbtPlugin("me.lessis" % "bintray-sbt" % "0.1.2")
