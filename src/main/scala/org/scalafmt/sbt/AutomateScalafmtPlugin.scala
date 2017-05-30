// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0
package org.scalafmt.sbt

import org.scalafmt.bootstrap.ScalafmtBootstrap
import sbt._
import sbt.Keys._
import sbt.inc.Analysis

// WORKAROUND https://github.com/scalameta/scalafmt/issues/874
// Copyright 2016 - 2017 @hseeberger
object AutomateScalafmtPlugin extends AutoPlugin {

  object autoImport {
    def automateScalafmtFor(configurations: Configuration*): Seq[Setting[_]] =
      configurations.flatMap { c =>
        inConfig(c)(
          Seq(
            compileInputs.in(compile) := {
              scalafmtInc.value
              compileInputs.in(compile).value
            },
            sourceDirectories.in(scalafmtInc) := Seq(scalaSource.value),
            scalafmtInc := {
              val cache = streams.value.cacheDirectory / "scalafmt"
              val include = includeFilter.in(scalafmtInc).value
              val exclude = excludeFilter.in(scalafmtInc).value
              val sources =
                sourceDirectories
                  .in(scalafmtInc)
                  .value
                  .descendantsExcept(include, exclude)
                  .get
                  .toSet
              def format(handler: Set[File] => Unit, msg: String) = {
                def update(
                  handler: Set[File] => Unit,
                  msg: String
                )(
                  in: ChangeReport[File],
                  out: ChangeReport[File]
                ) = {
                  val label = Reference.display(thisProjectRef.value)
                  val files = in.modified -- in.removed
                  Analysis
                    .counted("Scala source", "", "s", files.size)
                    .foreach(
                      count =>
                        streams.value.log.info(s"$msg $count in $label ...")
                    )
                  handler(files)
                  files
                }
                FileFunction.cached(cache)(FilesInfo.hash, FilesInfo.exists)(
                  update(handler, msg)
                )(
                    sources
                  )
              }
              def formattingHandler(files: Set[File]) =
                if (files.nonEmpty) {
                  val filesArg = files.map(_.getAbsolutePath).mkString(",")
                  ScalafmtBootstrap.main(List("--quiet", "-i", "-f", filesArg))
                }
              format(formattingHandler, "Formatting")
              val _ = format(_ => (), "Reformatted") // Recalculate the cache
            }
          )
        )
      }
  }

  private val scalafmtInc =
    taskKey[Unit]("Incrementally format modified sources")

  override def requires = ScalafmtPlugin

  override def trigger = allRequirements

  override def projectSettings =
    (includeFilter.in(scalafmtInc) := "*.scala") +: autoImport
      .automateScalafmtFor(Compile, Test)
}
