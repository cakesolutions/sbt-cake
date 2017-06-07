pipeline {
  agent {
    label 'sbt-slave'
  }

  environment {
    // Ensure that build scripts recognise the environment they are running within
    CI = 'jenkins'
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
          // TODO: CO-77: add in scalafmt::test target
          // sh "sbt ';headerCheck ;scalastyle ;scalafmt'"
          sh "sbt ';headerCheck ;scalastyle'"
        }
      }
    }

    stage('Test') {
      steps {
        ansiColor('xterm') {
          sh "sbt scripted"
        }
      }
    }
  }
}
