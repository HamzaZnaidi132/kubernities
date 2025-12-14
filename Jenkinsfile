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
                echo "Test et diagnostic de l'application..."
                script {
                    def namespace = env.K8S_NAMESPACE

                    // Attendre que le pod soit prêt
                    sh """
                echo "=== Attente du démarrage du pod ==="
                for i in {1..30}; do
                    POD_STATUS=\$(kubectl get pods -l app=spring-app -n ${namespace} -o jsonpath='{.items[0].status.phase}' 2>/dev/null || echo "NotFound")
                    if [ "\$POD_STATUS" = "Running" ]; then
                        echo "Pod en cours d'exécution"
                        break
                    fi
                    echo "Attente (tentative \$i/30) - Statut: \$POD_STATUS"
                    sleep 10
                done
            """

                    // Diagnostic complet
                    sh """
                echo "=== Diagnostic complet ==="
                
                # 1. Vérifier les pods
                echo "1. Pods:"
                kubectl get pods -n ${namespace} -o wide
                echo ""
                
                # 2. Vérifier les services
                echo "2. Services:"
                kubectl get svc -n ${namespace}
                echo ""
                
                # 3. Vérifier les endpoints
                echo "3. Endpoints:"
                kubectl get endpoints -n ${namespace}
                echo ""
                
                # 4. Vérifier les logs du pod
                echo "4. Logs du pod:"
                POD_NAME=\$(kubectl get pods -l app=spring-app -n ${namespace} -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)
                if [ -n "\$POD_NAME" ]; then
                    echo "Pod: \$POD_NAME"
                    kubectl logs \$POD_NAME -n ${namespace} --tail=30
                fi
                echo ""
                
                # 5. Vérifier la configuration réseau
                echo "5. Vérification réseau:"
                echo "Minikube IP:"
                minikube ip
                echo ""
                echo "Services exposés:"
                minikube service list -n ${namespace}
                echo ""
            """

                    // Tester via différentes méthodes
                    sh """
                echo "=== Tests de connectivité ==="
                
                # Méthode A: Port-forward (garantie de fonctionner si le pod tourne)
                echo "Méthode A: Port-forward"
                POD_NAME=\$(kubectl get pods -l app=spring-app -n ${namespace} -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)
                if [ -n "\$POD_NAME" ]; then
                    timeout 30 kubectl port-forward pod/\$POD_NAME 8081:8080 -n ${namespace} &
                    PF_PID=\$!
                    sleep 5
                    echo "Test sur localhost:8081..."
                    curl -s -o /dev/null -w "HTTP Code: %{http_code}\\n" http://localhost:8081/actuator/health || echo "Échec du port-forward"
                    kill \$PF_PID 2>/dev/null
                fi
                echo ""
                
                # Méthode B: Tester dans le pod directement
                echo "Méthode B: Test interne au pod"
                kubectl exec \$POD_NAME -n ${namespace} -- curl -s -o /dev/null -w "HTTP Code interne: %{http_code}\\n" http://localhost:8080/actuator/health || echo "Échec interne"
                echo ""
                
                # Méthode C: Tester le service via ClusterIP
                echo "Méthode C: Test via ClusterIP"
                CLUSTER_IP=\$(kubectl get svc spring-service -n ${namespace} -o jsonpath='{.spec.clusterIP}')
                CLUSTER_PORT=\$(kubectl get svc spring-service -n ${namespace} -o jsonpath='{.spec.ports[0].port}')
                if [ -n "\$CLUSTER_IP" ] && [ -n "\$CLUSTER_PORT" ]; then
                    echo "ClusterIP: \$CLUSTER_IP:\$CLUSTER_PORT"
                    # Tester depuis un pod temporaire
                    kubectl run test-curl --image=curlimages/curl -n ${namespace} --rm -i --restart=Never -- curl -s -o /dev/null -w "ClusterIP HTTP: %{http_code}\\n" http://\${CLUSTER_IP}:\${CLUSTER_PORT}/actuator/health || echo "Échec ClusterIP"
                fi
                echo ""
            """

                    // Exposer temporairement le service
                    sh """
                echo "=== Exposition temporaire du service ==="
                echo "Pour accéder à l'application, exécutez:"
                echo "  kubectl port-forward svc/spring-service 8080:8080 -n ${namespace} &"
                echo "  curl http://localhost:8080/actuator/health"
                echo ""
                echo "Ou via NodePort (si configuré):"
                minikube service spring-service -n ${namespace} --url || echo "Service non exposé"
            """
                }
            }
        }
    }
}