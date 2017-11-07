import sbt._
import sbt.Keys._

import net.cakesolutions._

object CakeArtifactoryPlugin extends AutoPlugin {
  override def requires = CakeBuildPlugin
  override def trigger = allRequirements

  val autoImport = CakeArtifactoryKeys
  import autoImport._

  override lazy val buildSettings = Seq(
    artifactory := "artifactory.foo.net",
    artifactoryPath := "foo-maven-local",
    ghe := "github.foo.com",
    gheUser := "adtech",
    gheRepo := (name in ThisBuild).value
  )

  override lazy val projectSettings = Seq(
    publishArtifact in Test := false,
    publishArtifact in makePom := false,
    homepage := Some(
      url(s"https://${ghe.value}/${gheUser.value}/${gheRepo.value}")
    ),
    publishTo := Some(
      "Artifactory" at s"https://${artifactory.value}/${artifactoryPath.value}"
    ),
    credentials ++= {
      (sys.env.get("ARTIFACTORY_USERNAME"), sys.env.get("ARTIFACTORY_PASSWORD")) match {
        case (Some(user), Some(pass)) if user.nonEmpty && pass.nonEmpty =>
          Credentials("Artifactory Realm", artifactory.value, user, pass) :: Nil
        case _ =>
          Nil
      }
    },
    pomExtra := (
      <scm>
        <url>git@{ ghe.value }:{ gheUser.value }/{ gheRepo.value }.git</url>
        <connection>scm:git:git@{ ghe.value }:{ gheUser.value }/{ gheRepo.value }.git</connection>
      </scm>
      <developers>
        <developer>
          <id>{ gheUser.value }</id>
        </developer>
      </developers>
    )
  )
}

object CakeArtifactoryKeys {
  val artifactory = settingKey[String]("The artifactory to publish to")
  val artifactoryPath = settingKey[String]("The artifactory's URL path part")
  val ghe = settingKey[String]("The Github Enterprise install")
  val gheUser = settingKey[String]("Github user")
  val gheRepo = settingKey[String]("Github repo")
}
