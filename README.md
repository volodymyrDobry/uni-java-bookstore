# uni-book-store — backend service

Spring Boot (Java 25, Gradle) backend for the uni-book-store project.
CI/CD lives in [`.github/workflows/ci.yml`](.github/workflows/ci.yml); Kubernetes
manifests live in [`k8s/`](k8s/).

---

## Lab 3 — Deployment to Kubernetes (minikube)

The backend is described with a set of Kubernetes manifests and deployed to a
local **minikube** cluster. PostgreSQL runs in-cluster (with a
PersistentVolumeClaim); Keycloak is **not** required to start the app (the JWT
decoder is lazy), so public endpoints (`/actuator/**`, `/swagger-ui/**`) work
without it.

### Objects (one file per object in `k8s/`)

| File | Object | Purpose |
|------|--------|---------|
| `00-namespace.yaml` | Namespace `uni-book-store` | isolates all lab objects |
| `01-configmap.yaml` | ConfigMap `backend-config` | non-sensitive config (env vars) |
| `02-secret.yaml` | Secret `backend-secret` | DB password (demo value) |
| `03-postgres-pvc.yaml` | PersistentVolumeClaim | durable storage for PostgreSQL |
| `04-postgres-deployment.yaml` | Deployment `postgres` | stateful DB component |
| `05-postgres-service.yaml` | Service `postgres` (ClusterIP) | internal DB access |
| `06-backend-deployment.yaml` | Deployment `backend` | 2 replicas, probes, resources |
| `07-backend-service.yaml` | Service `backend` (NodePort 30080) | in-cluster + host access |
| `08-ingress.yaml` | Ingress `backend` | host access via nginx ingress |

### Configuration parameters

**ConfigMap `backend-config`** (injected into the backend as env vars):

| Key | Value | Used for |
|-----|-------|----------|
| `DB_HOST` | `postgres` | `spring.datasource.url` host |
| `DB_PORT` | `5432` | `spring.datasource.url` port |
| `KEYCLOAK_URI` | `http://keycloak:8080` | OAuth2 issuer base |
| `POSTGRES_DB` | `mydatabase` | DB name (app + postgres) |
| `POSTGRES_USER` | `myuser` | DB user (app + postgres) |
| `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE` | `health,info` | actuator exposure |
| `MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED` | `true` | liveness/readiness groups |
| `MANAGEMENT_INFO_ENV_ENABLED` | `true` | expose version in `/actuator/info` |

**Secret `backend-secret`** (base64-encoded at rest; demo values, not real):

| Key | Purpose |
|-----|---------|
| `SPRING_DATASOURCE_PASSWORD` | overrides `spring.datasource.password` |
| `POSTGRES_PASSWORD` | initialises the PostgreSQL container |

### Prerequisites

```bash
# minikube + kubectl (macOS / Homebrew)
brew install minikube kubernetes-cli
```

### 1. Build the two image versions (no `latest` tag)

```bash
docker build --build-arg APP_VERSION=1.0.0 -t uni-book-store-backend:1.0.0 .
docker build --build-arg APP_VERSION=1.1.0 -t uni-book-store-backend:1.1.0 .
```

### 2. Start the cluster and load the images

```bash
minikube start --driver=docker
minikube addons enable metrics-server
minikube addons enable ingress

minikube image load uni-book-store-backend:1.0.0
minikube image load uni-book-store-backend:1.1.0
```

### 3. Deploy — a single command from the directory

```bash
kubectl apply -f k8s/
kubectl -n uni-book-store rollout status deploy/postgres
kubectl -n uni-book-store rollout status deploy/backend
```

### 4. Open the app

```bash
# NodePort via minikube (keep the terminal open — docker driver on macOS):
minikube service backend -n uni-book-store

# or a stable port-forward:
kubectl -n uni-book-store port-forward svc/backend 8088:80
# then open: http://localhost:8088/swagger-ui/index.html
#            http://localhost:8088/actuator/info   -> {"app":{"version":"1.0.0"}}
```

Ingress (optional): add `"$(minikube ip) uni-book-store.local"` to `/etc/hosts`
and open `http://uni-book-store.local/` (run `minikube tunnel` if needed).

### 5. Lifecycle management

```bash
# self-healing
kubectl -n uni-book-store delete pod -l app=backend --field-selector=status.phase=Running | head -1

# scaling
kubectl -n uni-book-store scale deploy/backend --replicas=4
kubectl -n uni-book-store get endpoints backend

# rolling update 1.0.0 -> 1.1.0
kubectl -n uni-book-store set image deploy/backend backend=uni-book-store-backend:1.1.0
kubectl -n uni-book-store rollout status deploy/backend

# history + rollback
kubectl -n uni-book-store rollout history deploy/backend
kubectl -n uni-book-store rollout undo deploy/backend --to-revision=1
```

### 6. Diagnostics (ImagePullBackOff)

```bash
kubectl -n uni-book-store set image deploy/backend backend=uni-book-store-backend:9.9.9-does-not-exist
kubectl -n uni-book-store get pods -l app=backend                 # ImagePullBackOff
kubectl -n uni-book-store describe pod <pod>                      # Events -> cause
kubectl -n uni-book-store logs <healthy-pod>
kubectl -n uni-book-store exec -it <healthy-pod> -- sh            # interactive shell
# fix:
kubectl -n uni-book-store set image deploy/backend backend=uni-book-store-backend:1.0.0
```

### Tear down

```bash
kubectl delete -f k8s/
minikube stop   # or: minikube delete
```
