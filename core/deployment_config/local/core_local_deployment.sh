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
cd ../../envoy
docker build -t envoy:latest -f envoy.Dockerfile .  

# Install all the required Maven dependencies 
cd ../grpc_health_proxy

# Build the gRCP server image
docker buildx build --platform linux/x86_64 -t grpc-health-proxy:latest -f grpc_health_proxy.Dockerfile .
cd ..
docker build -t grpc-server:latest  .


# Apply the K8s deplyment file  
cd deployment_config/local 
kubectl apply -f core_deployment.yaml   

# Expose the service ip for external access
minikube service esp-core-envoy
