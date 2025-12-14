pipeline {
    agent any

    // Supprimer la section 'tools' si non configurée, ou utiliser directement les commandes

    environment {
        // Docker Configuration
        DOCKER_REGISTRY = 'docker.io'
        DOCKER_REPOSITORY = 'hamzaznaidi'
        DOCKER_IMAGE_NAME = 'foyer_project'
        DOCKER_IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT?.take(7) ?: 'unknown'}"
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
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        disableConcurrentBuilds()
        timestamps()
    }

    stages {
        // ========== PHASE 1: INITIALIZATION ==========
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
                        echo "========================================"
                    '''

                    // Check if Maven and Java are installed
                    sh '''
                        echo "Checking installed tools..."
                        which java || echo "Java not found in PATH"
                        which mvn || echo "Maven not found in PATH"
                        which docker || echo "Docker not found in PATH"
                        which kubectl || echo "kubectl not found in PATH"
                    '''
                }
            }
        }

        stage('Checkout SCM') {
            steps {
                checkout scm
                script {
                    sh '''
                        echo "Repository checked out successfully"
                        git log -1 --oneline
                    '''
                }
            }
        }

        // ========== PHASE 2: BUILD ==========
        stage('Build Application') {
            steps {
                echo '🔨 Building Application...'
                script {
                    sh '''
                        echo "Java Version:"
                        java -version 2>&1 || true
                        echo "Maven Version:"
                        mvn -version 2>&1 || true
                    '''
                }
                sh 'mvn clean package -DskipTests -B'

                script {
                    def jarFile = findFiles(glob: 'target/*.jar')[0]?.name
                    if (jarFile) {
                        env.JAR_FILE = "target/${jarFile}"
                        echo "JAR File: ${env.JAR_FILE}"
                    }
                }
            }

            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
                failure {
                    echo '❌ Build failed!'
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
                }
            }
        }

        // ========== PHASE 3: DOCKER ==========
        stage('Build Docker Image') {
            steps {
                echo '🐳 Building Docker Image...'
                script {
                    // Check Docker
                    sh 'docker --version'

                    // Build with simplified command
                    sh """
                        docker build \
                            -t ${DOCKER_FULL_IMAGE} \
                            -t ${DOCKER_LATEST_IMAGE} \
                            .
                    """

                    // List images
                    sh """
                        echo "Docker images built:"
                        docker images | grep ${DOCKER_IMAGE_NAME} || true
                    """
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
                    // If you have Docker Hub credentials configured in Jenkins
                    withCredentials([usernamePassword(
                            credentialsId: 'docker-hub-creds',
                            usernameVariable: 'DOCKER_USER',
                            passwordVariable: 'DOCKER_PASS'
                    )]) {
                        sh """
                            # Login to Docker Hub
                            echo "${DOCKER_PASS}" | docker login -u "${DOCKER_USER}" --password-stdin
                            
                            # Push images
                            docker push ${DOCKER_FULL_IMAGE}
                            docker push ${DOCKER_LATEST_IMAGE}
                            
                            echo "✅ Images pushed successfully!"
                        """
                    }
                }
            }
        }

        // ========== PHASE 4: KUBERNETES DEPLOYMENT ==========
        stage('Prepare Kubernetes Manifests') {
            when {
                branch 'main'
            }
            steps {
                echo '📝 Preparing Kubernetes Manifests...'
                script {
                    // Create kubernetes directory if it doesn't exist
                    sh 'mkdir -p kubernetes'

                    // Check if deployment file exists, if not create a basic one
                    if (!fileExists('kubernetes/deployment.yaml')) {
                        writeFile file: 'kubernetes/deployment.yaml', text: """
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ${K8S_DEPLOYMENT}
  namespace: ${K8S_NAMESPACE}
  labels:
    app: ${K8S_DEPLOYMENT}
spec:
  replicas: 2
  selector:
    matchLabels:
      app: ${K8S_DEPLOYMENT}
  template:
    metadata:
      labels:
        app: ${K8S_DEPLOYMENT}
    spec:
      containers:
      - name: ${K8S_DEPLOYMENT}
        image: ${DOCKER_FULL_IMAGE}
        ports:
        - containerPort: 8080
"""
                    }

                    // Update image in deployment
                    sh """
                        sed -i.bak 's|image:.*|image: ${DOCKER_FULL_IMAGE}|g' kubernetes/deployment.yaml
                        echo "Updated deployment.yaml with image: ${DOCKER_FULL_IMAGE}"
                    """
                }
            }
        }

        stage('Deploy to Kubernetes') {
            when {
                branch 'main'
            }
            steps {
                echo '🚀 Deploying to Kubernetes...'
                script {
                    // Check if kubectl is available
                    sh 'which kubectl || echo "kubectl not found, skipping deployment"'

                    // Apply Kubernetes manifests
                    sh """
                        # Create namespace if not exists
                        kubectl create namespace ${K8S_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f - || true
                        
                        # Apply deployment
                        kubectl apply -f kubernetes/deployment.yaml || echo "Deployment failed"
                        
                        # Check deployment status
                        kubectl rollout status deployment/${K8S_DEPLOYMENT} -n ${K8S_NAMESPACE} --timeout=180s || echo "Rollout check failed"
                        
                        # Show deployment info
                        kubectl get deployment ${K8S_DEPLOYMENT} -n ${K8S_NAMESPACE} || true
                        kubectl get pods -n ${K8S_NAMESPACE} -l app=${K8S_DEPLOYMENT} || true
                    """
                }
            }
        }

        stage('Health Check') {
            when {
                branch 'main'
            }
            steps {
                echo '🏥 Running Health Check...'
                script {
                    sh """
                        # Wait a bit for pods to start
                        sleep 30
                        
                        # Get pod name
                        POD_NAME=\$(kubectl get pods -n ${K8S_NAMESPACE} -l app=${K8S_DEPLOYMENT} -o jsonpath='{.items[0].metadata.name}' 2>/dev/null) || true
                        
                        if [ -n "\${POD_NAME}" ]; then
                            echo "Checking pod: \${POD_NAME}"
                            kubectl describe pod \${POD_NAME} -n ${K8S_NAMESPACE} | grep -A 5 "Status:" || true
                            kubectl logs \${POD_NAME} -n ${K8S_NAMESPACE} --tail=20 || true
                        else
                            echo "No pods found for deployment ${K8S_DEPLOYMENT}"
                        fi
                    """
                }
            }
        }
    }

    post {
        always {
            echo '🧹 Cleaning up...'
            script {
                // Clean Docker images to save space
                sh '''
                    docker system prune -f 2>/dev/null || true
                    docker rmi ${DOCKER_FULL_IMAGE} 2>/dev/null || true
                    docker rmi ${DOCKER_LATEST_IMAGE} 2>/dev/null || true
                '''

                // Print build summary
                sh """
                    echo "========================================"
                    echo "BUILD SUMMARY"
                    echo "========================================"
                    echo "Job: ${env.JOB_NAME}"
                    echo "Build: ${env.BUILD_NUMBER}"
                    echo "Result: ${currentBuild.currentResult}"
                    echo "Duration: ${currentBuild.durationString}"
                    echo "Docker Image: ${DOCKER_FULL_IMAGE}"
                    echo "K8S Namespace: ${K8S_NAMESPACE}"
                    echo "K8S Deployment: ${K8S_DEPLOYMENT}"
                    echo "========================================"
                """
            }

            // Clean workspace
            cleanWs()
        }

        success {
            echo '✅ Pipeline completed successfully!'

            // Simple notification (if mail is configured)
            emailext (
                    subject: "✅ SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                    body: "Build ${env.BUILD_NUMBER} of ${env.JOB_NAME} completed successfully.\n\nSee: ${env.BUILD_URL}",
                    to: 'admin@example.com'
            )
        }

        failure {
            echo '❌ Pipeline failed!'

            emailext (
                    subject: "❌ FAILURE: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                    body: "Build ${env.BUILD_NUMBER} of ${env.JOB_NAME} failed.\n\nSee logs: ${env.BUILD_URL}console",
                    to: 'admin@example.com'
            )
        }
    }
}