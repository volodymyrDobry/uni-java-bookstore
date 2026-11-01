#!/usr/bin/env bash
#
# One-command deploy of the whole uni-book-store stack to minikube via Helm.
#
#   ./deploy-helm.sh
#
# It is idempotent and does everything needed:
#   1. ensures the three local images exist in minikube (builds them if missing),
#   2. removes the raw `kubectl apply -f k8s/` stack if present (it would otherwise
#      hold NodePort 30081 and the ingress hosts and block the chart),
#   3. installs/upgrades the Helm release and waits until every pod is ready.
#
# Env overrides:
#   RELEASE   Helm release name            (default: uni-book-store)
#   NAMESPACE target namespace             (default: uni-book-store)
#   VALUES    extra `helm -f` values file  (default: none -> chart values.yaml, ingress on)
#   REBUILD=1 force-rebuild all images even if already present
#   UI_DIR    path to the ui-book-store-client repo (default: ../ui-book-store-client)

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

RELEASE="${RELEASE:-uni-book-store}"
NAMESPACE="${NAMESPACE:-uni-book-store}"
UI_DIR="${UI_DIR:-$(cd "$ROOT/../ui-book-store-client" && pwd)}"
TAG="1.0.0"
KEYCLOAK_HOST="keycloak.local"   # must match ingress.keycloakHost in the Helm values

log() { printf '\n\033[1;34m==> %s\033[0m\n' "$*"; }

have_image() { minikube image ls 2>/dev/null | grep -q "/$1:$2$"; }

# ---------------------------------------------------------------------------
log "1/3  Ensuring images are present in minikube (REBUILD=${REBUILD:-0})"

if [ "${REBUILD:-0}" = "1" ] || ! have_image book-management-service "$TAG"; then
  log "    building book-management-service:$TAG"
  minikube image build -t "book-management-service:$TAG" -f book-management-service/Dockerfile .
fi
if [ "${REBUILD:-0}" = "1" ] || ! have_image order-service "$TAG"; then
  log "    building order-service:$TAG"
  minikube image build -t "order-service:$TAG" -f order-service/Dockerfile .
fi
if [ "${REBUILD:-0}" = "1" ] || ! have_image uni-book-store-frontend "$TAG"; then
  log "    building uni-book-store-frontend:$TAG (OIDC authority -> http://$KEYCLOAK_HOST)"
  docker build -t "uni-book-store-frontend:$TAG" \
    --build-arg VITE_API_URL= \
    --build-arg "VITE_OIDC_AUTHORITY=http://$KEYCLOAK_HOST/realms/uni-book-store" \
    --build-arg VITE_OIDC_CLIENT_ID=uni-book-store-client \
    "$UI_DIR"
  minikube image load "uni-book-store-frontend:$TAG"
fi

# ---------------------------------------------------------------------------
log "2/3  Removing the raw k8s/ stack if present (keeps the namespace)"
# Delete every manifest except the Namespace so it cannot clash with the chart
# on NodePort 30081 or the shared ingress hosts. Safe no-op if nothing is there.
for f in k8s/*.yaml; do
  case "$f" in */00-namespace.yaml) continue ;; esac
  kubectl delete -f "$f" --ignore-not-found >/dev/null 2>&1 || true
done

# A failed/partial release of the same name blocks `upgrade`; start it clean.
if helm status "$RELEASE" -n "$NAMESPACE" >/dev/null 2>&1; then
  if [ "$(helm status "$RELEASE" -n "$NAMESPACE" -o json | grep -o '"status":"[a-z]*"' | head -1)" != '"status":"deployed"' ]; then
    log "    existing release is not healthy — uninstalling before reinstall"
    helm uninstall "$RELEASE" -n "$NAMESPACE" >/dev/null 2>&1 || true
  fi
fi

# ---------------------------------------------------------------------------
log "3/3  helm upgrade --install $RELEASE (waiting for all pods to be ready)"
helm upgrade --install "$RELEASE" ./helm \
  --namespace "$NAMESPACE" --create-namespace \
  ${VALUES:+-f "$VALUES"} \
  --wait --timeout 420s

log "Done. Pods:"
kubectl -n "$NAMESPACE" get pods

cat <<EOF

To open the app:
  1) keep a tunnel running:   sudo minikube tunnel
  2) map the hosts (once):    echo "127.0.0.1 uni-book-store.local $KEYCLOAK_HOST" | sudo tee -a /etc/hosts
  3) browse:                  http://uni-book-store.local/   (login: testuser / password)
EOF
