# Лабораторна робота №4 — усі команди за завданнями (Helm)

Чарт — у [`helm/`](../../helm/). Namespace релізу — `uni-book-store`.

---

## Завдання 1 — Підготувати середовище

```bash
# 1a. Helm 3 + перевірка версії
brew install helm
helm version

# 1b. Кластер
minikube start --driver=docker

# 1c. Переконатись, що Lab 3 розгортається
kubectl apply -f k8s/
kubectl -n uni-book-store get pods

# 1d. Видалити реліз Lab 3 (уникнути конфлікту імен)
kubectl delete namespace uni-book-store
```

## Завдання 2 — Структура чарта

```bash
# 2a. Каталог
mkdir -p helm/templates

# 2b. Chart.yaml (name, description, version, appVersion) — створюється вручну
# 2c. version vs appVersion — пояснення у helm/README.md
```

## Завдання 3 — Маніфести → шаблони

```bash
# 3a-3d: шаблони у helm/templates/ (.Values, релізні імена, Secret з values)
# перевірка:
helm lint helm/
helm template demo helm/ | grep -E '^kind:|^  name:'
```

## Завдання 4 — values.yaml

```bash
# 4a-4b: параметри у helm/values.yaml (дефолти = dev)
# 4c: прапорці enabled — перевірка, що працюють:
helm template demo helm/ --set ingress.enabled=false | grep -c 'kind: Ingress'          # 0
helm template demo helm/ --set postgres.persistence.enabled=false | grep -c 'kind: PersistentVolumeClaim'  # 0
```

## Завдання 5 — _helpers.tpl + NOTES.txt

```bash
# 5a-5c: _helpers.tpl (fullname, labels), include у шаблонах, NOTES.txt
# перевірка рендеру NOTES (без кластера):
helm install demo helm/ --dry-run=client -n uni-book-store | sed -n '/NOTES:/,$p'
```

## Завдання 6 — Два середовища

```bash
# 6a: файли helm/values-dev.yaml та helm/values-prod.yaml

# 6b: різниця без встановлення в кластер
helm template uni helm/ -f helm/values-dev.yaml  > /tmp/dev.yaml
helm template uni helm/ -f helm/values-prod.yaml > /tmp/prod.yaml
diff /tmp/dev.yaml /tmp/prod.yaml

# 6c: пріоритет значень  values.yaml < -f < --set
helm template r helm/ | grep -m1 replicas                                   # 1 (дефолт)
helm template r helm/ -f helm/values-prod.yaml | grep -m1 replicas          # 3 (файл)
helm template r helm/ -f helm/values-prod.yaml --set backend.replicaCount=5 | grep -m1 replicas  # 5 (--set)
```

## Завдання 7 — Перевірити та встановити

```bash
# 7a. Перевірка на помилки
helm lint helm/
helm lint helm/ -f helm/values-dev.yaml

# 7b. Згенерувати маніфести та порівняти з Lab 3
helm template uni-book-store helm/ -f helm/values-dev.yaml > /tmp/helm-rendered.yaml
helm template uni-book-store helm/ -f helm/values-dev.yaml | grep -E '^kind:|^  name:'
grep -E '^kind:|^  name:' k8s/0*.yaml

# 7c. Встановити у власний namespace
helm install uni-book-store helm/ -f helm/values-dev.yaml \
  --namespace uni-book-store --create-namespace
kubectl -n uni-book-store rollout status deploy/uni-book-store-postgres
kubectl -n uni-book-store rollout status deploy/uni-book-store-backend

# 7d. Перелік релізів, статус, значення
helm list -n uni-book-store
helm status uni-book-store -n uni-book-store
helm get values uni-book-store -n uni-book-store            # USER-SUPPLIED
helm get values uni-book-store -n uni-book-store --all      # COMPUTED (усі)

# 7e. Де Helm зберігає стан (Secret helm.sh/release.v1)
kubectl -n uni-book-store get secrets --field-selector type=helm.sh/release.v1
kubectl -n uni-book-store get secret -l owner=helm,name=uni-book-store
kubectl -n uni-book-store get secret sh.helm.release.v1.uni-book-store.v1 \
  -o jsonpath='{.metadata.labels}'; echo
```

## Завдання 8 — Життєвий цикл релізу

```bash
# 8a. Оновити тег образу через --set
helm upgrade uni-book-store helm/ -f helm/values-dev.yaml \
  --set backend.image.tag=1.1.0 -n uni-book-store
kubectl -n uni-book-store get deploy uni-book-store-backend \
  -o jsonpath='{.spec.template.spec.containers[0].image}'; echo

# 8b. Оновити файлом значень іншого середовища
helm upgrade uni-book-store helm/ -f helm/values-prod.yaml -n uni-book-store
kubectl -n uni-book-store get pods,ingress,pvc

# 8c. Історія ревізій
helm history uni-book-store -n uni-book-store

# 8d. Відкат (на ревізію чистого dev; НЕ на rev1 з увімкненою залежністю)
helm rollback uni-book-store 2 -n uni-book-store
helm history uni-book-store -n uni-book-store

# 8e. Видалити реліз однією командою + перевірити, що порожньо
helm uninstall uni-book-store -n uni-book-store
helm list -n uni-book-store
kubectl -n uni-book-store get all,ingress,pvc,configmap,secret
```

## Завдання 9 — На вибір

### 9a — залежність від стороннього чарта (обрано)

```bash
# залежність bitnami/postgresql прописана в Chart.yaml (condition: postgresql.enabled)
helm dependency update helm/        # завантажити у helm/charts/ (+ Chart.lock)
helm dependency list helm/          # список залежностей

# встановити з увімкненою залежністю, налаштованою через значення батьківського чарта
helm install uni-book-store helm/ -f helm/values-dev.yaml \
  -n uni-book-store --create-namespace \
  --set postgresql.enabled=true \
  --set postgresql.auth.username=myuser \
  --set postgresql.auth.password=S3cr3tP@ssw0rd_demo \
  --set postgresql.auth.database=mydatabase

# перевірка
kubectl -n uni-book-store get pods | grep postgresql
helm get values uni-book-store -n uni-book-store | grep -A4 postgresql

# вимкнути назад (щоб не було двох БД; postgresql.enabled=false за замовчуванням)
helm upgrade uni-book-store helm/ -f helm/values-dev.yaml -n uni-book-store
```

### 9b — hook + helm test (альтернатива; потребує тест-шаблонів)

```bash
helm upgrade --install uni-book-store helm/ -f helm/values-dev.yaml -n uni-book-store --create-namespace
helm test uni-book-store -n uni-book-store
helm test uni-book-store -n uni-book-store --logs
```

### 9c — запакувати й опублікувати в OCI-реєстр (альтернатива)

```bash
helm package helm/                               # -> uni-book-store-0.1.0.tgz
echo "$TOKEN" | helm registry login ghcr.io -u volodymyrDobry --password-stdin   # PAT з write:packages
helm push uni-book-store-0.1.0.tgz oci://ghcr.io/volodymyrdobry/charts
helm install uni-book-store oci://ghcr.io/volodymyrdobry/charts/uni-book-store \
  --version 0.1.0 -n uni-book-store --create-namespace
```

## Завдання 10 — Оформити результати

```bash
# 10a. Закомітити чарт
git add helm/ docs/lab4/
git commit -m "Lab-4 | Helm chart + docs"

# 10b. README з таблицею values + команди встановлення — helm/README.md
# 10c. Звіт по пунктах 6-9 — docs/lab4/REPORT.md
```

---

## Корисне: очистка

```bash
helm uninstall uni-book-store -n uni-book-store     # видалити реліз
kubectl -n uni-book-store delete pvc --all          # прибрати томи, якщо лишились
kubectl delete namespace uni-book-store             # повністю очистити namespace
```
