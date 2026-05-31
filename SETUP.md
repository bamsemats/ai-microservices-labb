# AdaptaChat: Setup & Demonstration Guide

This guide provides step-by-step instructions to get the AdaptaChat microservices system running locally in a Kubernetes (Minikube) cluster for testing and demonstration.

## 📋 Prerequisites

Ensure you have the following tools installed:
- **Docker Desktop** (or Docker Engine)
- **Minikube**
- **kubectl**
- **Maven** (to build the backend services)
- **Node.js 18+ & npm** (to build the frontend)
- **Java 21**

---

## 🚀 Quick Start (Kubernetes/Minikube)

This is the recommended way to run the full system with production parity.

### 1. Initialize the Cluster
```powershell
# Start Minikube with sufficient resources
minikube start --cpus 4 --memory 8192

# Enable the Ingress addon (for Gateway routing)
minikube addons enable ingress
```

### 2. Configure Local Secrets
The project uses a `k8s/infrastructure/secrets.yaml` file to manage sensitive data. This file is git-ignored but required for the app to start.
1. Copy the template: `cp k8s/infrastructure/secrets.example.yaml k8s/infrastructure/secrets.yaml`
2. Open `k8s/infrastructure/secrets.yaml` and ensure the values match your local testing needs (the default placeholders are functional for local demo).

### 3. Build & Load Images
To ensure Minikube uses your local code changes, point your terminal to the Minikube Docker daemon:
```powershell
# Windows (PowerShell)
minikube docker-env | Invoke-Expression

# Build all microservices
mvn clean package -DskipTests

# Build Docker images
docker-compose build
```

### 4. Deploy to Kubernetes
Apply the infrastructure (MongoDB, RabbitMQ, Redis, Jaeger) and then the application services:
```powershell
# 1. Apply Infrastructure
kubectl apply -f k8s/infrastructure/

# 2. Apply Application (using the local overlay)
kubectl apply -k k8s/overlays/local
```

### 5. Access the Application
Wait for all pods to be `Running`: `kubectl get pods -w`

```powershell
# Port-forward the frontend to access the UI
kubectl port-forward service/frontend 3000:80
```
Open **[http://localhost:3000](http://localhost:3000)** in your browser.

---

## 🧪 Testing the System

### Initial Admin Setup
On first startup, the system seeds a default admin account based on the values in your `secrets.yaml`:
- **Username**: `admin`
- **Password**: `admin-password-2026` (or whatever is set in secrets.yaml)

### Core Features to Verify
1.  **Registration/Login**: Create a new user account.
2.  **Real-time Chat**: Open two different browsers/incognito tabs and chat between users.
3.  **Semantic Signal Detection**: Type phrases like "watching wagamama on twitch" or "playing elden ring". Observe the dynamic Twitch/YouTube widgets being injected.
4.  **Global Search**: Navigate to the Search tab. Use filters like "Sentiment Theme" or "Date Range" to recover historical signals.
5.  **Admin Dashboard**: Log in as `admin`. Navigate to the Admin Panel to broadcast global messages or perform an "Admin Override" on any user's profile.
6.  **Lumina Fluid UI**: Send emotional messages and watch the global background aura (blur/opacity) shift smoothly.

### E2E Testing (Playwright)
To run automated end-to-end tests:
```powershell
cd frontend
# Optional: Set custom gateway URL if not using default localhost:8080
$env:VITE_GATEWAY_URL="http://localhost:8080" 
npx playwright test
```

---

## 🛠 Troubleshooting

### "ImagePullBackOff" errors
Ensure you ran `minikube docker-env` before building images, or manually load them:
`minikube image load <service-name>:latest`

### Certificates/mTLS issues
The system generates self-signed certificates for gRPC. If services cannot communicate, check the logs:
`kubectl logs -f deployment/auth-service`

### Resetting the Environment
To wipe everything and start fresh:
```powershell
kubectl delete -k k8s/overlays/local
kubectl delete -f k8s/infrastructure/
# Optionally delete Minikube
minikube delete
```
