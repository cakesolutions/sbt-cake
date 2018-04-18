pipeline {
  agent {
    label 'sbt-slave'
  }

  environment {
    // Ensure that build scripts recognise the environment they are running within
    CI = 'jenkins'
    // Use the git SHA to gain some integration test isolation
    DOCKER_COMPOSE_PROJECT_NAME = sh(returnStdout: true, script: "git rev-parse --verify HEAD").trim()
  }

  stages {
    stage('Compile') {
      steps {
        ansiColor('xterm') {
          sh "sbt ';clean ;compile ;doc'"
        }
      }
    }

    stage('Verification') {
      steps {
        ansiColor('xterm') {
          sh "sbt ';headerCheck ;scalastyle ;sbt:scalafmt::test ;scalafmt::test'"
        }
      }
    }

    stage('Test') {
      steps {
        ansiColor('xterm') {
          script {
            // In CI environments, we use the eth0 or local-ipv4 address of the slave
            // instead of localhost
            def dockerip = sh(returnStdout: true, script:  $/wget http://169.254.169.254/latest/meta-data/local-ipv4 -qO-/$).trim()
            withEnv(["CI_HOST=$dockerip"]) {
              sh "sbt scripted"
            }
          }
        }
      }
    }
  }
}
