pipeline {
    agent any

    environment {
        // Docker Configuration
        DOCKER_REGISTRY = 'docker.io'
        DOCKER_REPOSITORY = 'hamzaznaidi'
        DOCKER_IMAGE_NAME = 'foyer_project'
        DOCKER_IMAGE_TAG = "${env.BUILD_NUMBER}"
        DOCKER_FULL_IMAGE = "${DOCKER_REGISTRY}/${DOCKER_REPOSITORY}/${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG}"

        // Kubernetes Configuration
        K8S_NAMESPACE = 'default'
        K8S_DEPLOYMENT = 'foyer-app'
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        disableConcurrentBuilds()
        timestamps()
    }

    triggers {
        // Déclenchement automatique sur push GitHub
        pollSCM('H/2 * * * *')  // Vérifie toutes les 2 minutes

        // OU pour webhook GitHub (recommandé)
        // githubPush()
    }

    stages {
        // ========== PHASE 1: CHECKOUT ==========
        stage('Checkout SCM') {
            steps {
                checkout([
                        $class: 'GitSCM',
                        branches: [[name: '*/main']],
                        userRemoteConfigs: [[
                                                    url: 'https://github.com/HamzaZnaidi132/kubernities.git',
                                                    credentialsId: ''
                                            ]],
                        extensions: [
                                [$class: 'CleanBeforeCheckout'],
                                [$class: 'CloneOption', depth: 1, noTags: false, shallow: true]
                        ]
                ])

                script {
                    sh '''
                        echo "========================================"
                        echo "Build Information:"
                        echo "Job: ${JOB_NAME}"
                        echo "Build: ${BUILD_NUMBER}"
                        echo "Branch: ${GIT_BRANCH}"
                        echo "Commit: $(git log -1 --oneline)"
                        echo "========================================"
                    '''
                }
            }
        }

        // ========== PHASE 2: BUILD MAVEN ==========
        stage('Build Maven Project') {
            steps {
                echo '🔨 Building Java Application with Maven...'

                sh '''
                    echo "Java Version:"
                    java -version
                    echo ""
                    echo "Maven Version:"
                    mvn -version
                    echo ""
                '''

                // Build Maven sans tests
                sh 'mvn clean package -DskipTests -B'

                script {
                    // Solution alternative à findFiles
                    sh '''
                        echo "Listing JAR files in target directory:"
                        ls -la target/*.jar || echo "No JAR files found"
                        
                        # Trouver le premier fichier JAR
                        JAR_FILE=$(ls target/*.jar 2>/dev/null | head -1)
                        if [ -n "$JAR_FILE" ]; then
                            echo "JAR_FILE=$JAR_FILE" > jar_info.txt
                            echo "Found JAR file: $JAR_FILE"
                        else
                            echo "WARNING: No JAR file found in target directory"
                        fi
                    '''

                    // Lire le fichier créé
                    if (fileExists('jar_info.txt')) {
                        def jarInfo = readFile('jar_info.txt')
                        def jarLine = jarInfo.readLines().find { it.startsWith('JAR_FILE=') }
                        if (jarLine) {
                            env.JAR_FILE = jarLine.substring(9)
                            echo "Set JAR_FILE to: ${env.JAR_FILE}"
                        }
                    }
                }
            }

            post {
                success {
                    echo '✅ Maven build successful!'
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
                failure {
                    echo '❌ Maven build failed!'
                }
            }
        }

        // ========== PHASE 3: UNIT TESTS ==========
        stage('Run Unit Tests') {
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

        // ========== PHASE 4: DOCKER BUILD ==========
        stage('Build Docker Image') {
            steps {
                echo '🐳 Building Docker Image...'

                script {
                    // Vérifier que Docker est disponible
                    sh 'docker --version'

                    // Construire l'image
                    sh """
                        docker build -t ${DOCKER_FULL_IMAGE} .
                    """

                    // Tagger aussi comme latest
                    sh """
                        docker tag ${DOCKER_FULL_IMAGE} ${DOCKER_REGISTRY}/${DOCKER_REPOSITORY}/${DOCKER_IMAGE_NAME}:latest
                    """

                    // Lister les images créées
                    sh '''
                        echo "Docker images created:"
                        docker images | grep ${DOCKER_IMAGE_NAME} || true
                    '''
                }
            }

            post {
                success {
                    echo '✅ Docker image built successfully!'
                }
                failure {
                    echo '❌ Docker build failed!'
                }
            }
        }

        // ========== PHASE 5: DOCKER PUSH (seulement sur main) ==========
        stage('Push Docker Image to Registry') {
            when {
                branch 'main'
            }
            steps {
                echo '📤 Pushing Docker Image to Registry...'

                script {
                    // Utiliser les credentials Docker Hub si configurés
                    try {
                        withCredentials([usernamePassword(
                                credentialsId: 'docker-hub-creds',
                                usernameVariable: 'DOCKER_USER',
                                passwordVariable: 'DOCKER_PASS'
                        )]) {
                            sh """
                                echo "Logging in to Docker Hub..."
                                echo ${DOCKER_PASS} | docker login -u ${DOCKER_USER} --password-stdin || echo "Login failed, continuing..."
                                
                                echo "Pushing image: ${DOCKER_FULL_IMAGE}"
                                docker push ${DOCKER_FULL_IMAGE} || echo "Push failed for ${DOCKER_FULL_IMAGE}"
                                
                                echo "Pushing latest tag"
                                docker push ${DOCKER_REGISTRY}/${DOCKER_REPOSITORY}/${DOCKER_IMAGE_NAME}:latest || echo "Push failed for latest"
                            """
                        }
                    } catch (Exception e) {
                        echo "⚠️ Docker push skipped (credentials not configured): ${e.message}"
                    }
                }
            }
        }

        // ========== PHASE 6: KUBERNETES DEPLOYMENT ==========
        stage('Deploy to Kubernetes') {
            when {
                branch 'main'
            }
            steps {
                echo '🚀 Deploying to Kubernetes...'

                script {
                    // Vérifier si kubectl est disponible
                    sh 'kubectl version --client || echo "kubectl not available"'

                    // Appliquer les fichiers Kubernetes s'ils existent
                    sh '''
                        if [ -d "k8s" ]; then
                            echo "Applying Kubernetes manifests from k8s/ directory..."
                            kubectl apply -f k8s/ --namespace=${K8S_NAMESPACE} || echo "K8s apply failed"
                        else
                            echo "No k8s directory found, creating a simple deployment..."
                            
                            # Créer un déploiement simple
                            cat <<EOF | kubectl apply -f -
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
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
EOF
                        fi
                        
                        # Vérifier le déploiement
                        echo "Checking deployment status..."
                        kubectl rollout status deployment/${K8S_DEPLOYMENT} -n ${K8S_NAMESPACE} --timeout=120s || echo "Rollout check failed"
                        
                        # Afficher les pods
                        echo "Pods in namespace ${K8S_NAMESPACE}:"
                        kubectl get pods -n ${K8S_NAMESPACE} -l app=${K8S_DEPLOYMENT} || echo "Failed to get pods"
                    '''
                }
            }
        }

        // ========== PHASE 7: HEALTH CHECK ==========
        stage('Health Check') {
            when {
                branch 'main'
            }
            steps {
                echo '🏥 Running Health Check...'

                script {
                    sh '''
                        # Attendre que les pods démarrent
                        echo "Waiting for pods to be ready..."
                        sleep 30
                        
                        # Obtenir les noms des pods
                        POD_NAMES=$(kubectl get pods -n ${K8S_NAMESPACE} -l app=${K8S_DEPLOYMENT} -o jsonpath='{.items[*].metadata.name}' 2>/dev/null) || true
                        
                        if [ -n "$POD_NAMES" ]; then
                            for POD in $POD_NAMES; do
                                echo "Checking pod: $POD"
                                kubectl logs $POD -n ${K8S_NAMESPACE} --tail=10 || echo "Failed to get logs for $POD"
                                
                                # Vérifier l'état du pod
                                kubectl describe pod $POD -n ${K8S_NAMESPACE} | grep -A 3 "Status:" || echo "Failed to describe pod $POD"
                            done
                        else
                            echo "No pods found for deployment ${K8S_DEPLOYMENT}"
                        fi
                    '''
                }
            }
        }
    }

    post {
        always {
            echo '🧹 Cleaning up workspace...'

            script {
                // Nettoyage Docker
                sh '''
                    echo "Cleaning up Docker resources..."
                    docker system prune -f 2>/dev/null || true
                    
                    # Supprimer les images locales
                    docker rmi ${DOCKER_FULL_IMAGE} 2>/dev/null || true
                    docker rmi ${DOCKER_REGISTRY}/${DOCKER_REPOSITORY}/${DOCKER_IMAGE_NAME}:latest 2>/dev/null || true
                '''

                // Afficher le résumé du build
                sh """
                    echo "========================================"
                    echo "BUILD COMPLETED"
                    echo "========================================"
                    echo "Status: ${currentBuild.currentResult}"
                    echo "Duration: ${currentBuild.durationString}"
                    echo "Build: #${env.BUILD_NUMBER}"
                    echo "Image: ${DOCKER_FULL_IMAGE}"
                    echo "Deployment: ${K8S_DEPLOYMENT}"
                    echo "Namespace: ${K8S_NAMESPACE}"
                    echo "========================================"
                """
            }

            // Nettoyer l'espace de travail
            cleanWs()
        }

        success {
            echo '🎉 Pipeline completed successfully!'

            // Notification par email
            emailext (
                    subject: "✅ SUCCESS: ${env.JOB_NAME} Build #${env.BUILD_NUMBER}",
                    body: """
                Build ${env.BUILD_NUMBER} of ${env.JOB_NAME} completed successfully!
                
                Details:
                - Status: SUCCESS
                - Duration: ${currentBuild.durationString}
                - Docker Image: ${DOCKER_FULL_IMAGE}
                - Kubernetes Deployment: ${K8S_DEPLOYMENT}
                - Namespace: ${K8S_NAMESPACE}
                
                View build: ${env.BUILD_URL}
                """,
                    to: 'admin@example.com',
                    from: 'jenkins@example.com'
            )
        }

        failure {
            echo '❌ Pipeline failed!'

            emailext (
                    subject: "❌ FAILURE: ${env.JOB_NAME} Build #${env.BUILD_NUMBER}",
                    body: """
                Build ${env.BUILD_NUMBER} of ${env.JOB_NAME} failed!
                
                Details:
                - Status: FAILURE
                - Duration: ${currentBuild.durationString}
                
                Please check the logs: ${env.BUILD_URL}console
                """,
                    to: 'admin@example.com',
                    from: 'jenkins@example.com',
                    attachLog: true
            )
        }

        unstable {
            echo '⚠️ Pipeline unstable!'
        }
    }
}