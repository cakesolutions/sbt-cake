package org.scalafmt.sbt

import sbt._
import sbt.Keys._

// WORKAROUND https://github.com/scalameta/scalafmt/issues/923
object ScalafmtPlugin extends AutoPlugin {
  override def trigger: PluginTrigger = allRequirements

  val latestScalafmt = "0.7.0-RC1"

  object autoImport {
    val scalafmt: Command =
      Command.args("scalafmt", "Run scalafmt cli.") {
        case (state, args) =>
          val Right(scalafmt) =
            org.scalafmt.bootstrap.ScalafmtBootstrap.fromVersion(latestScalafmt)
          scalafmt.main("--non-interactive" +: args.toArray)
          state
      }
  }

  override def globalSettings: Seq[Def.Setting[_]] = Seq(
    commands += autoImport.scalafmt
  ) ++ addCommandAlias("scalafmtTest", "scalafmt --test") ++
    addCommandAlias("scalafmtDiffTest", "scalafmt --diff --test") ++
    addCommandAlias("scalafmtDiff", "scalafmt --diff")

}
