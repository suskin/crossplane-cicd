def call() {
    // The publish pipeline is used to publish a single tagged image version of the stack.
    pipeline {
        agent { label 'upbound-gce' }

        parameters {
            string(name: 'version', defaultValue: '', description: 'The version you are publishing. For example: v0.4.0. If left unspecified, the build will generate one for you.')
        }

        options {
            disableConcurrentBuilds()
            timestamps()
        }

        environment {
            DOCKER = credentials('dockerhub-upboundci')
            CROSSPLANE_CLI_RELEASE = 'master'
        }

        stages {
            stage('Prepare') {
                steps {
                    sh 'mkdir bin'
                    sh "curl -sL https://raw.githubusercontent.com/crossplane/crossplane-cli/${CROSSPLANE_CLI_RELEASE}/bootstrap.sh | env PREFIX=${WORKSPACE} RELEASE=${CROSSPLANE_CLI_RELEASE} bash"
                }
            }
            stage('Promote Release') {

                steps {
                    // The build step turns this into a "dirty" environment from the perspective of `git describe`,
                    // so we set the version once at the beginning and use it for both the build and publish steps.

                    sh """STACK_VERSION=${params.version}
                          STACK_VERSION=\${STACK_VERSION:-\$( git describe --tags --dirty --always )}
                          STACK_VERSION=\${STACK_VERSION} ./bin/kubectl-crossplane-stack-build
                          STACK_VERSION=\${STACK_VERSION} ./bin/kubectl-crossplane-stack-publish
                    """
                }
            }
        }

        post {
            always {
                script {
                    deleteDir()
                }
            }
        }
    }
}
