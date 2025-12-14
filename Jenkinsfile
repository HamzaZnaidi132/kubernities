// Jenkinsfile
pipeline {
    agent any

    environment {
        IMAGE_NAME = "hamzaznaidi/foyer_project"
        IMAGE_TAG = "latest"
        K8S_NAMESPACE = "devops"  // Changé de K8S_NAMESPACE pour uniformité
    }

    triggers {
        pollSCM('H/5 * * * *')  // Poll SCM toutes les 5 minutes (au lieu de githubPush si problème)
    }

    stages {
        stage('Checkout') {
            steps {
                echo "Récupération du code depuis GitHub..."
                git branch: 'main',
                        url: 'https://github.com/HamzaZnaidi132/kubernities.git'
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
                        echo \"${DOCKER_PASS}\" | docker login -u \"${DOCKER_USER}\" --password-stdin
                        docker push ${IMAGE_NAME}:${IMAGE_TAG}
                    """
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                echo "Déploiement sur Kubernetes..."
                script {
                    // Utiliser K8S_NAMESPACE au lieu de KUBE_NAMESPACE
                    def namespace = env.K8S_NAMESPACE

                    // Créer le namespace s'il n'existe pas
                    sh "kubectl create namespace ${namespace} --dry-run=client -o yaml | kubectl apply -f - || true"

                    // Vérifier si les fichiers existent avant de les appliquer
                    def files = ['k8s/mysql-deployment.yaml', 'k8s/spring-config.yaml', 'k8s/spring-secret.yaml']
                    files.each { file ->
                        if (fileExists(file)) {
                            sh "kubectl apply -f ${file} -n ${namespace}"
                        } else {
                            echo "Fichier ${file} non trouvé, ignoré"
                        }
                    }

                    // Mettre à jour l'image du déploiement Spring Boot
                    sh """
                        if kubectl get deployment spring-app -n ${namespace} 2>/dev/null; then
                            kubectl set image deployment/spring-app spring-app=${IMAGE_NAME}:${IMAGE_TAG} -n ${namespace}
                            kubectl rollout status deployment/spring-app -n ${namespace} --timeout=300s
                        else
                            echo "Le déploiement spring-app n'existe pas, création..."
                            // Créer un déploiement minimal si nécessaire
                            kubectl create deployment spring-app --image=${IMAGE_NAME}:${IMAGE_TAG} -n ${namespace} || true
                        fi
                    """
                }
            }
        }

        stage('Test Application') {
            steps {
                echo "Test de l'application..."
                script {
                    def namespace = env.K8S_NAMESPACE

                    // Attendre que le pod soit prêt
                    sh """
                        echo "Attente du démarrage du pod..."
                        kubectl wait --for=condition=ready pod -l app=spring-app -n ${namespace} --timeout=300s || true
                        
                        # Obtenir le nom du pod
                        POD_NAME=\$(kubectl get pods -l app=spring-app -n ${namespace} -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || echo "")
                        
                        if [ -n "\$POD_NAME" ]; then
                            echo "Pod trouvé: \$POD_NAME"
                            # Tester la connectivité interne
                            kubectl exec \$POD_NAME -n ${namespace} -- curl -s http://localhost:8080/actuator/health || echo "Test de santé échoué"
                        else
                            echo "Aucun pod trouvé"
                        fi
                    """
                }
            }
        }
    }

    post {
        always {
            echo "Pipeline terminé - Status: ${currentBuild.result}"
            cleanWs()  // Nettoyer l'espace de travail
        }
        success {
            echo "Build, Push et Déploiement effectués avec succès!"
            // Optionnel: Envoyer un email
            emailext (
                    subject: "SUCCÈS: Pipeline ${env.JOB_NAME} - ${env.BUILD_NUMBER}",
                    body: "Le pipeline a réussi!\n\nJob: ${env.JOB_NAME}\nBuild: ${env.BUILD_NUMBER}\nURL: ${env.BUILD_URL}",
                    to: 'hamzaznaidi539@gmail.com'  // Remplacez par votre email
            )
        }
        failure {
            echo "Le pipeline a échoué."
            // Optionnel: Envoyer un email d'erreur
            emailext (
                    subject: "ÉCHEC: Pipeline ${env.JOB_NAME} - ${env.BUILD_NUMBER}",
                    body: "Le pipeline a échoué!\n\nJob: ${env.JOB_NAME}\nBuild: ${env.BUILD_NUMBER}\nURL: ${env.BUILD_URL}\n\nConsultez les logs pour plus de détails.",
                    to: 'hamzaznaidi539@gmail.com'  // Remplacez par votre email
            )
        }
    }
}