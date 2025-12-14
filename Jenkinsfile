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

                    // Diagnostic d'abord
                    sh """
                echo "=== Diagnostic du cluster ==="
                echo "1. Vérification des pods :"
                kubectl get pods -n ${namespace}
                echo ""
                
                echo "2. Vérification des services :"
                kubectl get svc -n ${namespace}
                echo ""
                
                echo "3. Vérification des endpoints :"
                kubectl get endpoints -n ${namespace}
                echo ""
                
                echo "4. Obtenir l'URL du service :"
                minikube service list -n ${namespace}
                echo ""
            """

                    // Attendre que le pod soit prêt
                    sh """
                echo "Attente du démarrage du pod..."
                timeout 300 bash -c 'until kubectl get pods -l app=spring-app -n ${namespace} --field-selector=status.phase=Running 2>/dev/null | grep -q Running; do sleep 10; echo "En attente..."; done'
            """

                    // Vérifier les logs du pod
                    sh """
                echo "=== Logs de l'application ==="
                POD_NAME=\$(kubectl get pods -l app=spring-app -n ${namespace} -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)
                if [ -n "\$POD_NAME" ]; then
                    echo "Pod: \$POD_NAME"
                    kubectl logs \$POD_NAME -n ${namespace} --tail=20
                fi
            """

                    // Tester la connectivité interne d'abord
                    sh """
                echo "=== Test de connectivité interne ==="
                POD_NAME=\$(kubectl get pods -l app=spring-app -n ${namespace} -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)
                if [ -n "\$POD_NAME" ]; then
                    echo "Test curl depuis l'intérieur du pod :"
                    kubectl exec \$POD_NAME -n ${namespace} -- curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health || echo "Échec"
                    echo ""
                    
                    echo "Test API depuis l'intérieur du pod :"
                    kubectl exec \$POD_NAME -n ${namespace} -- curl -s http://localhost:8080/api/departments/getAllDepartment || echo "Échec"
                fi
            """

                    // Tester la connectivité externe
                    sh """
                echo "=== Test de connectivité externe ==="
                
                # Méthode 1: Port-forward temporaire
                echo "Méthode 1: Port-forward"
                timeout 30 kubectl port-forward svc/spring-service 8080:8080 -n ${namespace} &
                PF_PID=\$!
                sleep 5
                
                echo "Test via port-forward:"
                curl -s -o /dev/null -w "Code HTTP: %{http_code}\n" http://localhost:8080/actuator/health || echo "Port-forward échoué"
                kill \$PF_PID 2>/dev/null
                echo ""
                
                # Méthode 2: Service NodePort
                echo "Méthode 2: Via NodePort"
                NODE_PORT=\$(kubectl get svc spring-service -n ${namespace} -o jsonpath='{.spec.ports[0].nodePort}' 2>/dev/null)
                MINIKUBE_IP=\$(minikube ip 2>/dev/null || echo "192.168.49.2")
                
                if [ -n "\$NODE_PORT" ]; then
                    echo "NodePort: \$NODE_PORT, Minikube IP: \$MINIKUBE_IP"
                    echo "URL: http://\${MINIKUBE_IP}:\${NODE_PORT}/actuator/health"
                    curl -s -o /dev/null -w "Code HTTP: %{http_code}\n" http://\${MINIKUBE_IP}:\${NODE_PORT}/actuator/health || echo "Échec de connexion"
                else
                    echo "Aucun NodePort trouvé pour le service spring-service"
                fi
            """
                }
            }
        }
    }
}