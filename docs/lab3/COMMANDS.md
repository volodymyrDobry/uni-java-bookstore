# Лабораторна робота №3 — Усі команди з прив'язкою до завдань

Шпаргалка з командами, які використовувались під час виконання лаби (backend-сервіс
**uni-book-store**, кластер **minikube**). Namespace проєкту — `uni-book-store`.

> Порада: щоб не писати `-n uni-book-store` щоразу, задайте namespace за замовчуванням:
> ```bash
> kubectl config set-context --current --namespace=uni-book-store
> ```
> Нижче namespace вказано явно, щоб команди працювали й без цього налаштування.

---

## Завдання 1 — Підготувати образ застосунку

Збірка двох версій образу (без тега `latest`; версія закладається build-аргументом):

```bash
docker build --build-arg APP_VERSION=1.0.0 -t uni-book-store-backend:1.0.0 .
docker build --build-arg APP_VERSION=1.1.0 -t uni-book-store-backend:1.1.0 .
```

Перевірка, що образи зібрані й версії відрізняються:

```bash
docker images uni-book-store-backend
docker inspect uni-book-store-backend:1.0.0 --format '{{range .Config.Env}}{{println .}}{{end}}' | grep INFO_APP_VERSION
docker inspect uni-book-store-backend:1.1.0 --format '{{range .Config.Env}}{{println .}}{{end}}' | grep INFO_APP_VERSION
```

---

## Завдання 2 — Розгорнути локальний кластер

Встановлення minikube (kubectl ставиться як залежність):

```bash
brew install minikube
minikube version
kubectl version --client
```

Запуск кластера з docker-драйвером:

```bash
minikube start --driver=docker
```

Перевірка стану вузла та системних подів (п. 2c):

```bash
kubectl get nodes
kubectl get nodes -o wide
kubectl get pods -n kube-system
```

Увімкнення доповнень (п. 2d):

```bash
minikube addons enable metrics-server
minikube addons enable ingress
minikube addons list | grep -E "metrics-server|ingress"
kubectl get pods -n ingress-nginx
```

Завантаження локальних образів у кластер:

```bash
minikube image load uni-book-store-backend:1.0.0
minikube image load uni-book-store-backend:1.1.0
minikube image ls | grep uni-book-store-backend
```

Корисне:

```bash
minikube ip                 # IP вузла
minikube dashboard          # веб-UI Kubernetes (тримає термінал зайнятим)
minikube dashboard --url    # лише посилання на UI
docker ps                   # побачити вузол як контейнер "minikube"
kubectl config current-context   # на який кластер націлений kubectl
```

---

## Завдання 3 — Створити окремий namespace

```bash
kubectl apply -f k8s/00-namespace.yaml
kubectl get namespaces
```

(Рекомендовано) зробити namespace типовим:

```bash
kubectl config set-context --current --namespace=uni-book-store
kubectl config view --minify | grep namespace:
```

---

## Завдання 4 — Описати маніфести застосунку

Маніфести в каталозі `k8s/` (по файлу на об'єкт):

| Файл | Об'єкт |
|------|--------|
| `00-namespace.yaml` | Namespace |
| `01-backend-configmap.yaml` | ConfigMap |
| `02-backend-secret.yaml` | Secret |
| `03-postgre-pvc.yaml` | PersistentVolumeClaim |
| `04-postgres-deployment.yaml` | Deployment (Postgres) |
| `05-postgre-service.yaml` | Service (Postgres, ClusterIP) |
| `06-backend-deployment.yaml` | Deployment (backend, 2 репліки) |
| `07-backend-service.yaml` | Service (backend, NodePort) |
| `08-backend-ingress.yaml` | Ingress |

Перевірка синтаксису без застосування:

```bash
kubectl apply -f k8s/ --dry-run=client
```

Довідка по будь-якому полю маніфеста:

```bash
kubectl explain deployment.spec.strategy
kubectl explain deployment.spec.template.spec.containers.resources
```

---

## Завдання 5 — Розгорнути застосунок

Застосувати всі маніфести однією командою (п. 5a):

```bash
kubectl apply -f k8s/
```

Дочекатись готовності:

```bash
kubectl -n uni-book-store rollout status deploy/postgres
kubectl -n uni-book-store rollout status deploy/backend
```

Показати створені об'єкти (п. 5b):

```bash
kubectl -n uni-book-store get pods
kubectl -n uni-book-store get deploy
kubectl -n uni-book-store get replicaset
kubectl -n uni-book-store get service
kubectl -n uni-book-store get deploy,rs,pods,svc,ingress,pvc   # усе разом
```

Відкрити застосунок з хоста (п. 5c):

```bash
# варіант А — стабільний port-forward (тримати термінал відкритим):
kubectl -n uni-book-store port-forward svc/backend 8088:80
# далі у браузері:
#   http://localhost:8088/swagger-ui/index.html
#   http://localhost:8088/actuator/health   -> {"status":"UP"}
#   http://localhost:8088/actuator/info      -> {"app":{"version":"1.0.0"}}

# варіант Б — через minikube (випадковий порт, тримати відкритим):
minikube service backend -n uni-book-store --url
```

Довести, що ConfigMap і Secret потрапили в контейнер (п. 5d):

```bash
kubectl -n uni-book-store exec deploy/backend -- \
  printenv DB_HOST DB_PORT KEYCLOAK_URI SPRING_DATASOURCE_PASSWORD INFO_APP_VERSION
kubectl -n uni-book-store get configmap backend-config -o yaml
kubectl -n uni-book-store get secret backend-secret -o jsonpath='{.data.SPRING_DATASOURCE_PASSWORD}' | base64 -d; echo
```

---

## Завдання 6 — Керування життєвим циклом

### 6a. Самовідновлення

```bash
kubectl -n uni-book-store get pods -l app=backend
kubectl -n uni-book-store delete pod -l app=backend --field-selector=status.phase=Running | head -1
kubectl -n uni-book-store get pods -l app=backend -w     # новий под створюється автоматично
```

### 6b. Масштабування

```bash
kubectl -n uni-book-store scale deploy/backend --replicas=4
kubectl -n uni-book-store rollout status deploy/backend
kubectl -n uni-book-store get pods -l app=backend -o wide
kubectl -n uni-book-store get endpoints backend          # усі IP подів = балансування
kubectl top pods -n uni-book-store                        # навантаження (metrics-server)
```

### 6c. Оновлення (rolling update 1.0.0 → 1.1.0)

```bash
# задати причину для історії (необов'язково, але корисно):
kubectl -n uni-book-store annotate deploy/backend \
  kubernetes.io/change-cause="Update image to 1.1.0" --overwrite

# замінити образ:
kubectl -n uni-book-store set image deploy/backend backend=uni-book-store-backend:1.1.0

# показати процес:
kubectl -n uni-book-store rollout status deploy/backend
kubectl -n uni-book-store get pods -w                     # у другому терміналі
kubectl -n uni-book-store get rs                          # стара RS -> 0, нова -> 2

# перевірити результат:
kubectl -n uni-book-store get deploy backend -o jsonpath='{.spec.template.spec.containers[0].image}'; echo
curl http://localhost:8088/actuator/info                  # {"app":{"version":"1.1.0"}}
```

### 6d. Історія оновлень та відкат

```bash
kubectl -n uni-book-store rollout history deploy/backend
kubectl -n uni-book-store rollout history deploy/backend --revision=1   # деталі ревізії
kubectl -n uni-book-store rollout undo deploy/backend --to-revision=1   # відкат
kubectl -n uni-book-store rollout status deploy/backend
```

### 6e. Доступність під час оновлення (нуль простою)

```bash
# під час rolling update паралельно опитувати health:
while true; do curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8088/actuator/health; sleep 0.3; done
```

---

## Завдання 7 — Виконати діагностику

### 7a. Навмисна помилка → ImagePullBackOff

```bash
kubectl -n uni-book-store set image deploy/backend backend=uni-book-store-backend:9.9.9-does-not-exist
kubectl -n uni-book-store get pods -l app=backend          # ErrImagePull -> ImagePullBackOff
kubectl -n uni-book-store get pods -l app=backend -w
```

### 7b. Знайти причину (опис пода + Events)

```bash
kubectl -n uni-book-store describe pod <проблемний-под>     # прокрутити до секції Events
kubectl -n uni-book-store get events --field-selector involvedObject.name=<проблемний-под> --sort-by=.lastTimestamp
```

### 7c. Логи контейнера та вхід у контейнер

```bash
kubectl -n uni-book-store logs <здоровий-под>
kubectl -n uni-book-store logs <здоровий-под> --tail=20
kubectl -n uni-book-store logs <здоровий-под> --previous    # логи попередньої (впалої) спроби
kubectl -n uni-book-store exec -it <здоровий-под> -- sh     # інтерактивний shell
```

### 7d. Виправити й довести до робочого стану

```bash
kubectl -n uni-book-store set image deploy/backend backend=uni-book-store-backend:1.0.0
kubectl -n uni-book-store rollout status deploy/backend
kubectl -n uni-book-store get pods -l app=backend          # лише Running 1/1
curl http://localhost:8088/actuator/health                 # {"status":"UP"}
```

---

## Завдання 8 — Оформити результати

```bash
git add k8s/ Dockerfile README.md docs/
git commit -m "Lab-3 | Kubernetes manifests, README and report"
git push -u origin labs/lab-3-k8s
```

---

## Корисне: очистка та повний reset

Видалити весь namespace з усіма об'єктами (повний чистий старт):

```bash
kubectl delete namespace uni-book-store
kubectl apply -f k8s/            # підняти заново (00-namespace.yaml створить ns)
```

Видалити лише об'єкти з маніфестів:

```bash
kubectl delete -f k8s/
```

Зупинити / видалити кластер:

```bash
minikube stop                    # зупинити (дані зберігаються)
minikube delete                  # видалити кластер повністю
```
