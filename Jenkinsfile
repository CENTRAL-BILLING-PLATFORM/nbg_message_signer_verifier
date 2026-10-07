```groovy
pipeline {
    agent {
        label 'build-agent'
    }

    environment {
        VERSION = "${BUILD_NUMBER}"

        // IMPORTANT:
        // Verify this path exists on the Jenkins build-agent.
        JAVA_HOME = '/usr/lib/jvm/java-21-openjdk'
        PATH = "${JAVA_HOME}/bin:${env.PATH}"

        DEPLOYMENT_FILE_DIR = './deployment'
        IMAGE_FULL_ADDR = 'registry.ethswitch.et:8443/nbg/nbg-xml-signer-uat'

        MANIFEST_URL = 'github.com/ethswitch/nbg-xml-signer-manifests.git'
        TARGET_BRANCH = 'UAT'
        DEPLOYMENT_FILE = 'deployment.yaml'
        REPOSITORY_URL = "https://${MANIFEST_URL}"

        SLACK_WEBHOOK = credentials('slack-webhook')
    }

    stages {

        stage('Prepare Environment') {
            steps {
                script {

                    env.COMMITTER_NAME = sh(
                        script: """
                            git config --global --add safe.directory '${env.WORKSPACE}'
                            git log -1 --pretty=format:'%an'
                        """,
                        returnStdout: true
                    ).trim()

                    env.COMMIT_MESSAGE = sh(
                        script: "git log -1 --pretty=format:'%s'",
                        returnStdout: true
                    ).trim()

                    def BRANCH_NAME = env.JOB_NAME.tokenize('/')[-1]
                    def JOBNAME = env.JOB_NAME.tokenize('/')[1]

                    def J_NAME = "${JOBNAME}-${BRANCH_NAME}:${env.VERSION}"

                    env.J_NAME = J_NAME
                    env.IMAGE_TAG = env.VERSION
                    env.IMAGE_REPO = env.IMAGE_FULL_ADDR

                    echo "Job Name: ${J_NAME}"
                    echo "Branch: ${BRANCH_NAME}"
                    echo "Committer: ${COMMITTER_NAME}"
                    echo "Commit Message: ${COMMIT_MESSAGE}"
                }
            }
        }

        stage('Verify Build Environment') {
            steps {
                sh '''
                    set -e

                    echo "=========================================="
                    echo "BUILD ENVIRONMENT"
                    echo "=========================================="

                    echo ""
                    echo "JAVA_HOME=$JAVA_HOME"
                    echo "PATH=$PATH"

                    echo ""
                    echo "Java executable:"
                    which java
                    readlink -f "$(which java)"

                    echo ""
                    echo "Java version:"
                    java -version

                    echo ""
                    echo "Javac executable:"
                    which javac
                    readlink -f "$(which javac)"

                    echo ""
                    echo "Javac version:"
                    javac -version

                    echo ""
                    echo "Making Gradle wrapper executable..."
                    chmod +x ./gradlew

                    echo ""
                    echo "Gradle version:"
                    ./gradlew --version

                    echo ""
                    echo "Gradle Java toolchains:"
                    ./gradlew javaToolchains --no-daemon

                    echo ""
                    echo "=========================================="
                    echo "VALIDATING JAVA 21"
                    echo "=========================================="

                    JAVA_MAJOR=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)

                    if [ "$JAVA_MAJOR" != "21" ]; then
                        echo "ERROR: Jenkins agent is NOT running Java 21."
                        echo "Detected Java version: $JAVA_MAJOR"
                        echo "JAVA_HOME: $JAVA_HOME"
                        echo "Java executable: $(readlink -f "$(which java)")"
                        exit 1
                    fi

                    echo "SUCCESS: Java 21 detected."
                '''
            }
        }

        stage('Build War File') {
            steps {
                echo 'Building Spring Boot WAR with Gradle...'

                sh '''
                    set -e

                    chmod +x ./gradlew

                    ./gradlew clean build \
                        -x test \
                        --no-daemon
                '''
            }
        }

        stage('Build and Push Docker Image') {
            steps {
                script {

                    echo "Building Docker image on Jenkins VM agent..."

                    def imageTag = "${IMAGE_FULL_ADDR}:${IMAGE_TAG}"

                    echo "Building image: ${imageTag}"

                    def appImage = docker.build(
                        imageTag,
                        "--build-arg J_NAME=${J_NAME} ."
                    )

                    echo "Pushing Docker image to Harbor registry: ${imageTag}"

                    withCredentials([
                        usernamePassword(
                            credentialsId: 'jenkins-build',
                            usernameVariable: 'HARBOR_USER',
                            passwordVariable: 'HARBOR_PASS'
                        )
                    ]) {

                        docker.withRegistry(
                            "https://${IMAGE_FULL_ADDR.split('/')[0]}",
                            'jenkins-build'
                        ) {
                            appImage.push()
                        }
                    }

                    echo "Docker image successfully built and pushed: ${imageTag}"
                }
            }
        }

        stage('Trigger ManifestUpdate') {
            steps {

                echo "Triggering manifest-updater job..."

                build job: 'manifest-updater',
                    parameters: [
                        string(
                            name: 'IMAGE_TAG',
                            value: "${VERSION}"
                        ),
                        string(
                            name: 'MANIFEST_URL',
                            value: "${MANIFEST_URL}"
                        ),
                        string(
                            name: 'DEPLOYMENT_FILE_DIR',
                            value: "${DEPLOYMENT_FILE_DIR}"
                        ),
                        string(
                            name: 'TARGET_BRANCH',
                            value: "${TARGET_BRANCH}"
                        ),
                        string(
                            name: 'IMAGE_FULL_ADDR',
                            value: "${IMAGE_FULL_ADDR}"
                        ),
                        string(
                            name: 'REPOSITORY_URL',
                            value: "${REPOSITORY_URL}"
                        )
                    ]
            }
        }
    }

    post {

        always {
            script {

                def buildStatus = currentBuild.currentResult ?: 'SUCCESS'

                def statusEmoji =
                    buildStatus == 'SUCCESS' ? '✅' :
                    buildStatus == 'FAILURE' ? '❌' : '⚠️'

                def themeColor =
                    buildStatus == 'SUCCESS' ? 'good' :
                    buildStatus == 'FAILURE' ? 'danger' : 'warning'

                /*
                 * Do not interpolate SLACK_WEBHOOK directly into the Groovy
                 * string. This avoids the Jenkins secret interpolation warning.
                 */
                withCredentials([
                    string(
                        credentialsId: 'slack-webhook',
                        variable: 'SLACK_WEBHOOK_URL'
                    )
                ]) {

                    sh '''
                        set +x

                        curl -s -X POST \
                          -H "Content-type: application/json" \
                          --data @- \
                          "$SLACK_WEBHOOK_URL" <<EOF
{
    "text": "''' + "${statusEmoji}" + ''' Build *''' + "${buildStatus}" + '''* for job *''' + "${env.JOB_NAME}" + '''* (#''' + "${env.BUILD_NUMBER}" + ''')",
    "attachments": [
        {
            "color": "''' + "${themeColor}" + '''",
            "fields": [
                {
                    "title": "Image",
                    "value": "''' + "${env.IMAGE_REPO}" + '''",
                    "short": false
                },
                {
                    "title": "Tag",
                    "value": "''' + "${env.IMAGE_TAG}" + '''",
                    "short": true
                },
                {
                    "title": "Committer",
                    "value": "''' + "${env.COMMITTER_NAME}" + '''",
                    "short": true
                },
                {
                    "title": "Message",
                    "value": "''' + "${env.COMMIT_MESSAGE}" + '''",
                    "short": false
                }
            ]
        }
    ]
}
EOF
                    '''
                }

                echo "Cleaning up workspace..."

                cleanWs()
            }
        }
    }
}
```
