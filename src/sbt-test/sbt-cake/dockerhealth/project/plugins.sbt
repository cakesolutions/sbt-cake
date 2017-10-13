// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0

ivyLoggingLevel := UpdateLogging.Quiet

addSbtPlugin(
  "net.cakesolutions" % "sbt-cake" % System.getProperty("plugin.version")
)
