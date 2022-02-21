# SCRIPT TO MANAGE THE LOCAL DEPLOYMENT OF THE AGENT DIALOGUE 
# Use the minikube local environment in order to use local images
minikube start
eval $(minikube -p minikube docker-env)

# START CLEANING
kubectl delete deployments esp-core 
kubectl delete services esp-core-envoy
kubectl get pods -n default --no-headers=true | awk '/esp-core/{print $1}'| xargs  kubectl delete -n default pod
docker rmi envoy
docker rmi grpc_server
# END CLEANING 

# Build envoy image 
cd config/
docker build -t envoy:latest -f envoy_updated.Dockerfile .  

# Install all the required Maven dependencies 
cd ..
cd agent-dialogue-core
#mvn clean 
#mvn install

# Build the gRCP server image
docker build -t grpc-server:latest  .

# Apply the K8s deplyment file  
cd ..
cd config/ 
kubectl apply -f deployment-envoy.yaml   

# Expose the service ip for external access
minikube service esp-core-envoy
