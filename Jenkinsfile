pipeline {
    agent any

    environment {
        IMAGE_NAME = "hamzaznaidi/kubernities_project"
        IMAGE_TAG  = "latest"
        K8S_NAMESPACE = "devops"
    }

    triggers {
        githubPush() // This enables webhook triggers
    }

    stages {
        stage('Checkout') {
            steps {
                echo "Récupération du code depuis GitHub..."
                git branch: 'main', url: 'https://github.com/HamzaZnaidi132/kubernities.git'
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
                withCredentials([usernamePassword(credentialsId: '8248def7-9835-4807-8c09-ee56c34c0e21',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS')]) {
                    sh """
                        echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                        docker push ${IMAGE_NAME}:${IMAGE_TAG}
                    """
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                echo "Déploiement sur Kubernetes..."

                // Mettre à jour l'image dans le déploiement
                sh """
                    kubectl set image deployment/spring-app \
                    spring-app=${IMAGE_NAME}:${IMAGE_TAG} \
                    -n ${K8S_NAMESPACE} \
                    --record
                """

                // Vérifier le rollout
                sh """
                    kubectl rollout status deployment/spring-app \
                    -n ${K8S_NAMESPACE} \
                    --timeout=300s
                """
            }
        }

        stage('Start Minikube for Tests') {
            steps {
                echo "Démarrage de Minikube pour les tests..."
                sh '''
                    # Vérifier et démarrer Minikube si nécessaire
                    if ! minikube status 2>/dev/null | grep -q "Running"; then
                        echo "Démarrage de Minikube..."
                        minikube start --driver=docker --memory=4096 --cpus=2
                        # Attendre que Minikube soit prêt
                        sleep 30
                    else
                        echo "Minikube est déjà démarré"
                    fi
                    
                    # Vérifier l'état de Minikube
                    minikube status
                '''
            }
        }

        stage('Integration Tests') {
            steps {
                echo "Exécution des tests d'intégration..."
                script {
                    // Obtenir l'URL du service
                    def SPRING_URL = sh(script: 'minikube service spring-service -n devops --url', returnStdout: true).trim()
                    echo "URL du service : ${SPRING_URL}"

                    // Attendre que l'application soit prête (max 2 minutes)
                    sh """
                        echo "Attente du démarrage du service..."
                        for i in \$(seq 1 24); do
                            if curl -s -f ${SPRING_URL}/actuator/health > /dev/null 2>&1; then
                                echo "Service disponible après \$((i*5)) secondes"
                                break
                            fi
                            
                            echo "Attente... (\$((i*5))s/120s)"
                            sleep 5
                            
                            if [ \$i -eq 24 ]; then
                                echo "Service non disponible après 120 secondes"
                                echo "Tentative de diagnostic:"
                                kubectl get pods -n devops
                                kubectl logs -n devops deployment/spring-app --tail=20
                                exit 1
                            fi
                        done
                    """

                    // Exécuter les tests d'intégration
                    sh """
                        echo "Test 1: Health endpoint"
                        curl -f ${SPRING_URL}/actuator/health
                        echo ""
                        
                        echo "Test 2: Department endpoint"
                        curl -f ${SPRING_URL}/department/getAllDepartment
                        echo ""
                        
                        echo "Test 3: Création d'un département"
                        curl -X POST ${SPRING_URL}/department/createDepartment \\
                            -H "Content-Type: application/json" \\
                            -d '{"name": "IT", "location": "Tunis"}' || true
                        echo ""
                        
                        echo "Test 4: Récupération des départements"
                        curl -f ${SPRING_URL}/department/getAllDepartment
                        echo ""
                    """
                }
            }
        }

        stage('Verify Deployment') {
            steps {
                echo "Vérification du déploiement..."
                sh """
                    echo "Pods:"
                    kubectl get pods -n ${K8S_NAMESPACE} -o wide
                    
                    echo "Services:"
                    kubectl get svc -n ${K8S_NAMESPACE}
                    
                    echo "Déployments:"
                    kubectl get deployments -n ${K8S_NAMESPACE}
                    
                    echo "Statut du déploiement:"
                    kubectl rollout status deployment/spring-app -n ${K8S_NAMESPACE}
                """
            }
        }
    }

    post {
        always {
            echo "Pipeline terminé"

            // Nettoyage Docker
            sh 'docker system prune -f'

            // Logs pour debug
            sh """
                echo "Derniers logs de l'application:"
                kubectl logs -n ${K8S_NAMESPACE} deployment/spring-app --tail=50 || true
                
                echo "État des ressources:"
                kubectl get all -n ${K8S_NAMESPACE} || true
            """
        }
        success {
            echo "Build et déploiement effectués avec succès!"

            // Notification optionnelle
            sh '''
                echo "Succès à $(date)" >> /tmp/pipeline_success.log
            '''
        }
        failure {
            echo "Le pipeline a échoué."

            // Rollback automatique
            sh """
                echo "Tentative de rollback..."
                kubectl rollout undo deployment/spring-app -n ${K8S_NAMESPACE} || true
                
                echo "Dernier état connu:"
                kubectl describe deployment/spring-app -n ${K8S_NAMESPACE} || true
                
                echo "Logs d'erreur:"
                kubectl logs -n ${K8S_NAMESPACE} deployment/spring-app --tail=100 || true
            """
        }
    }
}