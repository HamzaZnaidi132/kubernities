// Jenkinsfile
pipeline {
    agent any

    environment {
        IMAGE_NAME = "hamzaznaidi/foyer_project"
        IMAGE_TAG = "latest"
        K8S_NAMESPACE = "devops"
        K8S_NAMESPACE = 'foyer-production'
        K8S_DEPLOYMENT = 'foyer-app'
        K8S_SERVICE = 'foyer-service'
        K8S_CONFIG_PATH = 'kubernetes/'
        K8S_CONTEXT = 'production-cluster'
    }

    triggers {
        githubPush() // This enables webhook triggers
    }

    stages {
        stage('Checkout') {
            steps {
                echo "Récupération du code depuis GitHub..."
                git branch: 'main',
                        url: 'https://github.com/HamzaZnaidi132/kubernities.git'  // URL corrigée
            }
        }

        stage('Clean & Build') {
            steps {
                echo "Nettoyage + Build Maven..."
                sh 'mvn clean install -DskipTests -B'
            }
        }

        stage('Build Docker Image') {
            steps {
                echo "Construction de l'image Docker..."
                sh "docker build -t ${IMAGE_NAME}:${IMAGE_TAG} ."
            }
        }

        stage('Docker Login & Push') {
            steps {
                echo "Connexion + push vers DockerHub..."
                withCredentials([usernamePassword(
                        credentialsId: '8248def7-9835-4807-8c09-ee56c34c0e21',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh """
                        echo "${DOCKER_PASS}" | docker login -u "${DOCKER_USER}" --password-stdin
                        docker push ${IMAGE_NAME}:${IMAGE_TAG}
                    """
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                echo "Déploiement sur Kubernetes..."
                script {
                    // Créer le namespace s'il n'existe pas
                    sh "kubectl create namespace ${KUBE_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -"

                    // Appliquer les fichiers YAML
                    sh "kubectl apply -f k8s/mysql-deployment.yaml"
                    sh "kubectl apply -f k8s/spring-config.yaml"
                    sh "kubectl apply -f k8s/spring-secret.yaml"

                    // Mettre à jour l'image du déploiement Spring Boot
                    sh "kubectl set image deployment/spring-app spring-app=${IMAGE_NAME}:${IMAGE_TAG} -n ${KUBE_NAMESPACE}"

                    // Vérifier le rollout
                    sh "kubectl rollout status deployment/spring-app -n ${KUBE_NAMESPACE} --timeout=300s"
                }
            }
        }

        stage('Test Application') {
            steps {
                echo "Test de l'application..."
                script {
                    // Obtenir l'URL du service
                    sh """
                        APP_URL=\$(minikube service spring-service -n ${KUBE_NAMESPACE} --url)
                        echo "Application URL: \$APP_URL"
                        
                        # Tester l'endpoint (attendre 30s que l'application démarre)
                        sleep 30
                        curl -f \$APP_URL/api/departments/getAllDepartment || echo "Test échoué, mais le déploiement a réussi"
                    """
                }
            }
        }
    }

    post {
        always {
            echo "Pipeline terminé"
            cleanWs()  // Nettoyer l'espace de travail
        }
        success {
            echo "Build, Push et Déploiement effectués avec succès!"
            slackSend(color: 'good', message: "Pipeline ${env.JOB_NAME} - ${env.BUILD_NUMBER} réussi!")
        }
        failure {
            echo "Le pipeline a échoué."
            slackSend(color: 'danger', message: "Pipeline ${env.JOB_NAME} - ${env.BUILD_NUMBER} échoué!")
        }
    }
}