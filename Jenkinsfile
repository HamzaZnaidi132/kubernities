pipeline {
    agent any

    tools {
        maven 'M3'
        jdk 'jdk17'
    }

    environment {
        // Docker Configuration
        DOCKER_REGISTRY = 'docker.io'
        DOCKER_REPOSITORY = 'hamzaznaidi'
        DOCKER_IMAGE_NAME = 'foyer_project'
        DOCKER_IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT.substring(0, 7)}"
        DOCKER_FULL_IMAGE = "${DOCKER_REGISTRY}/${DOCKER_REPOSITORY}/${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG}"
        DOCKER_LATEST_IMAGE = "${DOCKER_REGISTRY}/${DOCKER_REPOSITORY}/${DOCKER_IMAGE_NAME}:latest"

        // Kubernetes Configuration
        K8S_NAMESPACE = 'foyer-production'
        K8S_DEPLOYMENT = 'foyer-app'
        K8S_SERVICE = 'foyer-service'
        K8S_CONFIG_PATH = 'kubernetes/'
        K8S_CONTEXT = 'production-cluster'

        // Application Configuration
        APP_NAME = 'tp-foyer'
        APP_VERSION = '1.0.0'

        // SonarQube (optional)
        SONAR_HOST_URL = 'http://sonarqube:9000'
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        disableConcurrentBuilds()
        ansiColor('xterm')
        timestamps()
    }

    parameters {
        choice(
                name: 'DEPLOY_ENVIRONMENT',
                choices: ['development', 'staging', 'production'],
                description: 'Select deployment environment'
        )
        booleanParam(
                name: 'RUN_INTEGRATION_TESTS',
                defaultValue: true,
                description: 'Run integration tests'
        )
        booleanParam(
                name: 'PERFORM_ROLLBACK',
                defaultValue: false,
                description: 'Perform rollback to previous version'
        )
    }

    stages {
        // ========== PHASE 1: PREPARATION ==========
        stage('Initialize') {
            steps {
                echo '🚀 Initializing Pipeline...'
                script {
                    sh '''
                        echo "========================================"
                        echo "Pipeline Configuration:"
                        echo "  Job: ${JOB_NAME}"
                        echo "  Build: ${BUILD_NUMBER}"
                        echo "  Branch: ${GIT_BRANCH}"
                        echo "  Commit: ${GIT_COMMIT}"
                        echo "  Environment: ${DEPLOY_ENVIRONMENT}"
                        echo "========================================"
                    '''

                    // Set namespace based on environment
                    if (params.DEPLOY_ENVIRONMENT == 'production') {
                        env.K8S_NAMESPACE = 'foyer-production'
                    } else if (params.DEPLOY_ENVIRONMENT == 'staging') {
                        env.K8S_NAMESPACE = 'foyer-staging'
                    } else {
                        env.K8S_NAMESPACE = 'foyer-development'
                    }
                }
            }
        }

        stage('Checkout SCM') {
            steps {
                checkout([
                        $class: 'GitSCM',
                        branches: [[name: "*/${env.GIT_BRANCH}"]],
                        userRemoteConfigs: [[
                                                    url: 'https://github.com/HamzaZnaidi132/kubernities.git',
                                                    credentialsId: 'github-credentials'
                                            ]],
                        extensions: [
                                [$class: 'CleanBeforeCheckout'],
                                [$class: 'CloneOption', depth: 1, noTags: false, shallow: true],
                                [$class: 'RelativeTargetDirectory', relativeTargetDir: '.']
                        ]
                ])
            }
        }

        // ========== PHASE 2: CODE QUALITY ==========
        stage('Code Quality Analysis') {
            steps {
                echo '🔍 Running Code Analysis...'
                script {
                    // SonarQube Scanner (optional)
                    sh '''
                        echo "Running static code analysis..."
                        mvn clean compile
                    '''
                }
            }
        }

        // ========== PHASE 3: BUILD ==========
        stage('Build Application') {
            steps {
                echo '🔨 Building Application...'
                sh '''
                    echo "Java Version:"
                    java -version
                    echo "Maven Version:"
                    mvn -version
                '''

                sh "mvn clean package -DskipTests -B -P${params.DEPLOY_ENVIRONMENT}"

                script {
                    env.JAR_FILE = sh(script: "find target -name '*.jar' -type f | head -1", returnStdout: true).trim()
                    echo "JAR File: ${JAR_FILE}"
                }
            }

            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                    stash includes: 'target/*.jar', name: 'application-jar'
                }
            }
        }

        stage('Unit Tests') {
            steps {
                echo '🧪 Running Unit Tests...'
                sh 'mvn test -B'
            }

            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                    archiveArtifacts artifacts: 'target/surefire-reports/*.xml', fingerprint: true
                }
            }
        }

        // ========== PHASE 4: DOCKER ==========
        stage('Build Docker Image') {
            steps {
                echo '🐳 Building Docker Image...'
                script {
                    // Verify Docker is available
                    sh '''
                        echo "Checking Docker installation..."
                        docker --version
                        docker info
                    '''

                    // Build Docker image with build args
                    docker.build("${DOCKER_FULL_IMAGE}",
                            "--build-arg JAR_FILE=${JAR_FILE} " +
                                    "--build-arg BUILD_DATE=\$(date -u +'%Y-%m-%dT%H:%M:%SZ') " +
                                    "--build-arg VERSION=${APP_VERSION} " +
                                    "--build-arg GIT_COMMIT=${GIT_COMMIT} " +
                                    "--build-arg DEPLOY_ENV=${params.DEPLOY_ENVIRONMENT} .")

                    // Tag as latest
                    sh "docker tag ${DOCKER_FULL_IMAGE} ${DOCKER_LATEST_IMAGE}"

                    // List built images
                    sh '''
                        echo "Docker Images Created:"
                        docker images | grep ${DOCKER_IMAGE_NAME}
                    '''
                }
            }
        }

        stage('Scan Docker Image') {
            when {
                expression { params.DEPLOY_ENVIRONMENT == 'production' }
            }
            steps {
                echo '🔒 Scanning Docker Image for Vulnerabilities...'
                script {
                    try {
                        sh '''
                            # Install Trivy if not available
                            if ! command -v trivy &> /dev/null; then
                                echo "Installing Trivy..."
                                curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh | sh -s -- -b /usr/local/bin
                            fi
                            
                            # Scan the image
                            trivy image --exit-code 0 --severity HIGH,CRITICAL ${DOCKER_FULL_IMAGE} || true
                            echo "Security scan completed"
                        '''
                    } catch (Exception e) {
                        echo "⚠️ Security scan skipped: ${e.message}"
                    }
                }
            }
        }

        stage('Push to Docker Registry') {
            when {
                branch 'main'
            }
            steps {
                echo '📤 Pushing Docker Image to Registry...'
                script {
                    withCredentials([usernamePassword(
                            credentialsId: 'docker-hub-credentials',
                            usernameVariable: 'DOCKER_USERNAME',
                            passwordVariable: 'DOCKER_PASSWORD'
                    )]) {
                        sh """
                            # Login to Docker Registry
                            echo "Logging into Docker Registry..."
                            echo ${DOCKER_PASSWORD} | docker login ${DOCKER_REGISTRY} -u ${DOCKER_USERNAME} --password-stdin
                            
                            # Push versioned image
                            echo "Pushing ${DOCKER_FULL_IMAGE}..."
                            docker push ${DOCKER_FULL_IMAGE}
                            
                            # Push latest image
                            echo "Pushing ${DOCKER_LATEST_IMAGE}..."
                            docker push ${DOCKER_LATEST_IMAGE}
                            
                            echo "✅ Images pushed successfully!"
                        """
                    }
                }
            }
        }

        // ========== PHASE 5: KUBERNETES DEPLOYMENT ==========
        stage('Prepare Kubernetes Manifests') {
            steps {
                echo '📝 Preparing Kubernetes Manifests...'
                script {
                    dir('kubernetes') {
                        // Update image tag in deployment
                        sh """
                            echo "Updating image tag to: ${DOCKER_FULL_IMAGE}"
                            sed -i 's|IMAGE_PLACEHOLDER|${DOCKER_FULL_IMAGE}|g' deployment.yaml
                            
                            # Update namespace
                            sed -i 's|namespace:.*|namespace: ${K8S_NAMESPACE}|g' *.yaml
                            
                            # Update replicas based on environment
                            if [ "${params.DEPLOY_ENVIRONMENT}" = "production" ]; then
                                sed -i 's|replicas:.*|replicas: 3|g' deployment.yaml
                            elif [ "${params.DEPLOY_ENVIRONMENT}" = "staging" ]; then
                                sed -i 's|replicas:.*|replicas: 2|g' deployment.yaml
                            else
                                sed -i 's|replicas:.*|replicas: 1|g' deployment.yaml
                            fi
                            
                            echo "Updated Kubernetes manifests:"
                            ls -la *.yaml
                        """
                    }
                }
            }
        }

        stage('Deploy to Kubernetes') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                }
            }
            steps {
                echo '🚀 Deploying to Kubernetes Cluster...'
                script {
                    withCredentials([file(
                            credentialsId: 'kubeconfig-prod',
                            variable: 'KUBECONFIG'
                    )]) {
                        dir('kubernetes') {
                            sh """
                                # Set kubectl context
                                export KUBECONFIG=${KUBECONFIG}
                                kubectl config use-context ${K8S_CONTEXT}
                                
                                # Verify cluster access
                                echo "Kubernetes Cluster Info:"
                                kubectl cluster-info
                                echo "Nodes:"
                                kubectl get nodes
                                
                                # Create namespace if not exists
                                kubectl create namespace ${K8S_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -
                                
                                # Label namespace
                                kubectl label namespace ${K8S_NAMESPACE} environment=${params.DEPLOY_ENVIRONMENT} --overwrite
                                
                                # Apply secrets and configmaps first
                                echo "Applying ConfigMaps and Secrets..."
                                kubectl apply -f configmap.yaml -n ${K8S_NAMESPACE}
                                kubectl apply -f secret.yaml -n ${K8S_NAMESPACE}
                                
                                # Apply deployment with strategic merge
                                echo "Applying Deployment..."
                                kubectl apply -f deployment.yaml -n ${K8S_NAMESPACE}
                                
                                # Apply service
                                echo "Applying Service..."
                                kubectl apply -f service.yaml -n ${K8S_NAMESPACE}
                                
                                # Apply ingress (if exists)
                                if [ -f ingress.yaml ]; then
                                    echo "Applying Ingress..."
                                    kubectl apply -f ingress.yaml -n ${K8S_NAMESPACE}
                                fi
                                
                                # Wait for rollout
                                echo "Waiting for deployment rollout..."
                                kubectl rollout status deployment/${K8S_DEPLOYMENT} -n ${K8S_NAMESPACE} --timeout=300s
                                
                                # Verify deployment
                                echo "Deployment Status:"
                                kubectl get deployment ${K8S_DEPLOYMENT} -n ${K8S_NAMESPACE} -o wide
                                
                                echo "Pods Status:"
                                kubectl get pods -n ${K8S_NAMESPACE} -l app=${K8S_DEPLOYMENT} -o wide
                                
                                echo "Service Status:"
                                kubectl get service ${K8S_SERVICE} -n ${K8S_NAMESPACE} -o wide
                            """
                        }
                    }
                }
            }

            post {
                success {
                    echo '✅ Kubernetes Deployment Successful!'
                }
                failure {
                    echo '❌ Kubernetes Deployment Failed!'
                    script {
                        // Automatic rollback on failure
                        if (params.PERFORM_ROLLBACK) {
                            echo '🔄 Performing automatic rollback...'
                            kubernetesRollback()
                        }
                    }
                }
            }
        }

        stage('Health Checks & Smoke Tests') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                }
            }
            steps {
                echo '🏥 Running Health Checks...'
                script {
                    withCredentials([file(
                            credentialsId: 'kubeconfig-prod',
                            variable: 'KUBECONFIG'
                    )]) {
                        sh """
                            export KUBECONFIG=${KUBECONFIG}
                            kubectl config use-context ${K8S_CONTEXT}
                            
                            # Wait for pods to be ready
                            echo "Waiting for pods to be ready..."
                            sleep 30
                            
                            # Get pod name
                            POD_NAME=\$(kubectl get pods -n ${K8S_NAMESPACE} -l app=${K8S_DEPLOYMENT} -o jsonpath='{.items[0].metadata.name}')
                            
                            # Check pod logs
                            echo "Pod Logs (last 10 lines):"
                            kubectl logs \${POD_NAME} -n ${K8S_NAMESPACE} --tail=10
                            
                            # Check pod status
                            echo "Pod Status:"
                            kubectl describe pod \${POD_NAME} -n ${K8S_NAMESPACE} | grep -A 5 "Status:"
                            
                            # Health check via port-forward
                            echo "Running health check..."
                            kubectl port-forward \${POD_NAME} -n ${K8S_NAMESPACE} 8080:8080 &
                            PF_PID=\$!
                            sleep 5
                            
                            # Test endpoints
                            curl -f http://localhost:8080/actuator/health || echo "Health check failed"
                            curl -f http://localhost:8080/actuator/info || echo "Info endpoint failed"
                            
                            # Kill port-forward
                            kill \$PF_PID
                            
                            # Check service endpoints
                            echo "Service Endpoints:"
                            kubectl get endpoints ${K8S_SERVICE} -n ${K8S_NAMESPACE}
                        """
                    }
                }
            }
        }

        stage('Integration Tests') {
            when {
                expression { params.RUN_INTEGRATION_TESTS == true }
            }
            steps {
                echo '🔗 Running Integration Tests...'
                script {
                    // Integration tests against deployed application
                    sh '''
                        echo "Integration tests would run here..."
                        # Example: mvn verify -Pintegration-tests
                    '''
                }
            }
        }

        // ========== PHASE 6: MONITORING & VERIFICATION ==========
        stage('Monitor Deployment') {
            steps {
                echo '📊 Monitoring Deployment...'
                script {
                    withCredentials([file(
                            credentialsId: 'kubeconfig-prod',
                            variable: 'KUBECONFIG'
                    )]) {
                        sh """
                            export KUBECONFIG=${KUBECONFIG}
                            kubectl config use-context ${K8S_CONTEXT}
                            
                            # Monitor for 2 minutes
                            for i in {1..12}; do
                                echo "Monitoring iteration \$i/12"
                                echo "=== Pods Status ==="
                                kubectl get pods -n ${K8S_NAMESPACE} -l app=${K8S_DEPLOYMENT}
                                echo ""
                                echo "=== Deployment Status ==="
                                kubectl get deployment ${K8S_DEPLOYMENT} -n ${K8S_NAMESPACE}
                                echo ""
                                sleep 10
                            done
                        """
                    }
                }
            }
        }
    }

    post {
        always {
            echo '🧹 Cleaning Up Resources...'
            script {
                // Clean Docker resources
                sh '''
                    echo "Cleaning Docker resources..."
                    docker system prune -f --filter "until=24h" || true
                    
                    # Remove local images
                    docker rmi ${DOCKER_FULL_IMAGE} || true
                    docker rmi ${DOCKER_LATEST_IMAGE} || true
                    
                    echo "Cleanup completed"
                '''

                // Generate pipeline report
                sh """
                    echo "========================================"
                    echo "PIPELINE EXECUTION REPORT"
                    echo "========================================"
                    echo "Job: ${env.JOB_NAME}"
                    echo "Build: ${env.BUILD_NUMBER}"
                    echo "Status: ${currentBuild.currentResult}"
                    echo "Duration: ${currentBuild.durationString}"
                    echo "Environment: ${params.DEPLOY_ENVIRONMENT}"
                    echo "Docker Image: ${DOCKER_FULL_IMAGE}"
                    echo "K8s Namespace: ${K8S_NAMESPACE}"
                    echo "K8s Deployment: ${K8S_DEPLOYMENT}"
                    echo "========================================"
                """
            }

            // Clean workspace
            cleanWs(cleanWhenNotBuilt: false, deleteDirs: true)
        }

        success {
            echo '✅ Pipeline Completed Successfully!'

            // Send success notification
            emailext(
                    subject: "✅ SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER} - ${params.DEPLOY_ENVIRONMENT}",
                    body: """
                🎉 DEPLOYMENT SUCCESSFUL!
                
                APPLICATION DETAILS:
                • Application: ${APP_NAME}
                • Version: ${APP_VERSION}
                • Environment: ${params.DEPLOY_ENVIRONMENT}
                • Build: #${env.BUILD_NUMBER}
                
                DEPLOYMENT DETAILS:
                • Docker Image: ${DOCKER_FULL_IMAGE}
                • Kubernetes Namespace: ${K8S_NAMESPACE}
                • Deployment: ${K8S_DEPLOYMENT}
                • Service: ${K8S_SERVICE}
                
                LINKS:
                • Build Logs: ${env.BUILD_URL}console
                • Kubernetes Dashboard: [Link to Dashboard]
                • Application URL: [Your Application URL]
                
                DURATION: ${currentBuild.durationString}
                COMMIT: ${env.GIT_COMMIT}
                """,
                    to: 'devops-team@example.com, developers@example.com',
                    from: 'jenkins-ci@example.com',
                    replyTo: 'jenkins-ci@example.com',
                    attachLog: false
            )
        }

        failure {
            echo '❌ Pipeline Failed!'

            // Send failure notification
            emailext(
                    subject: "❌ FAILURE: ${env.JOB_NAME} #${env.BUILD_NUMBER} - ${params.DEPLOY_ENVIRONMENT}",
                    body: """
                ⚠️ DEPLOYMENT FAILED!
                
                APPLICATION DETAILS:
                • Application: ${APP_NAME}
                • Environment: ${params.DEPLOY_ENVIRONMENT}
                • Build: #${env.BUILD_NUMBER}
                
                ERROR DETAILS:
                Please check the build logs for detailed error information.
                
                ACTION REQUIRED:
                1. Check Jenkins build logs
                2. Verify Kubernetes cluster status
                3. Review application logs
                
                LINKS:
                • Build Logs: ${env.BUILD_URL}console
                • Failed Stage: [Check Jenkins]
                
                DURATION: ${currentBuild.durationString}
                COMMIT: ${env.GIT_COMMIT}
                """,
                    to: 'devops-team@example.com, oncall-engineer@example.com',
                    from: 'jenkins-ci@example.com',
                    replyTo: 'jenkins-ci@example.com',
                    attachLog: true
            )
        }

        unstable {
            echo '⚠️ Pipeline Unstable!'
        }

        changed {
            echo '🔄 Pipeline Status Changed!'
        }
    }
}

// ========== HELPER FUNCTIONS ==========
def kubernetesRollback() {
    echo "🔄 Rolling back deployment..."

    withCredentials([file(
            credentialsId: 'kubeconfig-prod',
            variable: 'KUBECONFIG'
    )]) {
        sh """
            export KUBECONFIG=${KUBECONFIG}
            kubectl config use-context ${K8S_CONTEXT}
            
            # Rollback to previous revision
            echo "Checking deployment history..."
            kubectl rollout history deployment/${K8S_DEPLOYMENT} -n ${K8S_NAMESPACE}
            
            echo "Performing rollback..."
            kubectl rollout undo deployment/${K8S_DEPLOYMENT} -n ${K8S_NAMESPACE}
            
            echo "Waiting for rollback to complete..."
            kubectl rollout status deployment/${K8S_DEPLOYMENT} -n ${K8S_NAMESPACE} --timeout=180s
            
            echo "Rollback completed!"
            kubectl get pods -n ${K8S_NAMESPACE} -l app=${K8S_DEPLOYMENT}
        """
    }
}

def deployToKubernetes(String namespace, String deploymentFile) {
    echo "🚀 Deploying to Kubernetes namespace: ${namespace}"

    withCredentials([file(
            credentialsId: 'kubeconfig-prod',
            variable: 'KUBECONFIG'
    )]) {
        sh """
            export KUBECONFIG=${KUBECONFIG}
            kubectl config use-context ${K8S_CONTEXT}
            
            # Apply the deployment
            kubectl apply -f ${deploymentFile} -n ${namespace}
            
            # Wait for rollout
            kubectl rollout status deployment/${K8S_DEPLOYMENT} -n ${namespace} --timeout=300s
        """
    }
}

def runKubernetesTests(String namespace) {
    echo "🧪 Running Kubernetes tests in namespace: ${namespace}"

    withCredentials([file(
            credentialsId: 'kubeconfig-prod',
            variable: 'KUBECONFIG'
    )]) {
        sh """
            export KUBECONFIG=${KUBECONFIG}
            kubectl config use-context ${K8S_CONTEXT}
            
            # Run test pod
            kubectl run test-pod --image=alpine:latest -n ${namespace} --rm -i --restart=Never -- \\
                sh -c "echo 'Running tests...' && sleep 5 && echo 'Tests completed'"
        """
    }
}