# Чек-лист для захисту лабораторних (k8s + Helm)

TODO-список: що підготувати, що показати і що пояснити. Відмічай `[x]` по мірі готовності.

---

## 0. Перед захистом (підготовка середовища)

- [ ] Docker Desktop запущений (`docker info`)
- [ ] Кластер піднятий (`minikube start --driver=docker`), вузол `Ready`
- [ ] Аддони увімкнені: `minikube addons list | grep -E "ingress|metrics-server"`
- [ ] Образи в кластері: `minikube image ls | grep uni-book-store-backend` (1.0.0 і 1.1.0)
- [ ] Helm встановлений: `helm version` (v3+)
- [ ] Відкриті у вкладках: термінал, `k8s/`, `helm/`, звіти `docs/lab3`, `docs/lab4`

---

## 1. Lab 3 — Kubernetes (що показати)

- [ ] **Структура маніфестів** `k8s/` — по файлу на обʼєкт; пояснити кожен (ns, configmap, secret, pvc, deployment, service, ingress)
- [ ] **Розгортання однією командою:** `kubectl apply -f k8s/`
- [ ] **Обʼєкти:** `kubectl -n uni-book-store get deploy,rs,pods,svc,ingress,pvc`
- [ ] **ConfigMap/Secret у контейнері:** `kubectl -n uni-book-store exec deploy/backend -- printenv DB_HOST SPRING_DATASOURCE_PASSWORD`
- [ ] **Застосунок у браузері:** Swagger UI через `port-forward`
- [ ] **Self-healing:** видалити под → показати, що зʼявився новий
- [ ] **Масштабування:** `scale --replicas=4` → `get endpoints`
- [ ] **Rolling update** 1.0.0 → 1.1.0 (`set image`) + нуль простою
- [ ] **Історія + відкат:** `rollout history` / `rollout undo`
- [ ] **Діагностика:** зламати тег → `ImagePullBackOff` → `describe`/Events → `logs`/`exec` → фікс

Докази (скриншоти): [docs/lab3/REPORT.md](lab3/REPORT.md)

---

## 2. Lab 4 — Helm (що показати)

- [ ] **Структура чарта** `helm/`: `Chart.yaml`, `values.yaml`, `templates/`, `_helpers.tpl`, `NOTES.txt`
- [ ] **version vs appVersion** — пояснити різницю
- [ ] **Шаблонізація:** показати `{{ .Values.x }}` і релізні імена через `include "…fullname"`
- [ ] **Перевірка:** `helm lint helm/`
- [ ] **Рендер без встановлення:** `helm template uni helm/ -f helm/values-dev.yaml`
- [ ] **Два середовища:** `diff` dev vs prod (репліки, ресурси, тег, Ingress)
- [ ] **Пріоритет значень:** `values.yaml < -f < --set` (показати на `--set replicaCount=5`)
- [ ] **Встановлення:** `helm install … -f values-dev.yaml -n … --create-namespace`
- [ ] **Список/статус/значення:** `helm list` / `helm status` / `helm get values --all`
- [ ] **Де стан релізу:** `kubectl get secrets --field-selector type=helm.sh/release.v1`
- [ ] **Життєвий цикл:** `upgrade --set` → `upgrade -f prod` → `history` → `rollback` → `uninstall`
- [ ] **9a — залежність:** `helm dependency update/list`, `--set postgresql.enabled=true`

Докази: [docs/lab4/REPORT.md](lab4/REPORT.md) · гайд: [docs/lab4/GUIDE.md](lab4/GUIDE.md)

---

## 3. Ключові поняття (мати пояснити своїми словами)

- [ ] **Pod / ReplicaSet / Deployment** — ієрархія, хто за що
- [ ] **Service** (ClusterIP / NodePort) — навіщо, як знаходить поди (селектор за мітками)
- [ ] **Ingress** — L7-маршрутизація, потрібен контролер
- [ ] **ConfigMap vs Secret** — різниця, base64 ≠ шифрування
- [ ] **PVC** — навіщо для БД (поди ефемерні)
- [ ] **Проби** liveness / readiness / startup — чим відрізняються
- [ ] **requests vs limits** — для планування vs стеля
- [ ] **Rolling update** — `maxUnavailable` / `maxSurge`, нуль простою
- [ ] **Helm chart / release / revision** — пакет / інсталяція / версія
- [ ] **Де Helm зберігає стан** — Secret `helm.sh/release.v1`

---

## 4. Ймовірні питання + короткі відповіді

**Чому Postgres у кластері з PVC, а не окремо?**
Backend на старті виконує Liquibase + JPA validate — БД потрібна одразу. PVC дає сховище,
що переживає перезапуски пода (поди ефемерні).

**Чому драйвер docker для minikube?**
Весь вузол кластера запускається як один Docker-контейнер — швидко, без VM. Мінус — доступ
ззовні потребує `minikube service` / `tunnel` / `port-forward`.

**Чим Service відрізняється від Ingress?**
Service — стабільний L4-вхід (IP+порт) з балансуванням між подами. Ingress — L7-маршрутизатор
(хости/шляхи, HTTPS), спрямовує трафік на Service.

**Secret безпечний? Це шифрування?**
Ні. Secret — base64 (не шифрування), захист організаційний: окремо від образу, RBAC, не логується.
Реальне шифрування — etcd encryption / Vault.

**Що таке rolling update і як досягли нуль простою?**
Поступова заміна подів: `maxUnavailable: 0` + `maxSurge: 1` — новий под піднімається до того,
як піде старий; readiness-проба гарантує, що трафік іде лише на готові.

**Різниця version vs appVersion у Helm?**
`version` — версія чарта (рецепта), `appVersion` — версія застосунку (образу). Незалежні.

**Пріоритет значень у Helm?**
`values.yaml` < файли `-f` (останній виграє) < `--set` (найвищий).

**Де Helm тримає історію релізів?**
У Secret типу `helm.sh/release.v1` (`sh.helm.release.v1.<реліз>.v<N>`) у namespace релізу —
gzip+base64 знімок. Звідси `history` і `rollback`.

**Що робить rollback з історією?**
Не стирає її — додає НОВУ ревізію, що повторює конфіг обраної.

**Як чарт можна встановити двічі в один кластер?**
Імена обʼєктів будуються з імені релізу (`include "…fullname"` на базі `.Release.Name`),
тож `r1-…` і `r2-…` не конфліктують.

**Навіщо _helpers.tpl?**
DRY: іменовані шаблони (fullname, набір міток) підключаються через `include` в усіх маніфестах.

---

## 5. Міні-демо «за 2 хвилини» (якщо просять показати наживо)

```bash
# Helm: встановити, показати, оновити, відкотити, видалити
helm install uni-book-store helm/ -f helm/values-dev.yaml -n uni-book-store --create-namespace
kubectl -n uni-book-store rollout status deploy/uni-book-store-backend
helm list -n uni-book-store
helm upgrade uni-book-store helm/ -f helm/values-dev.yaml --set backend.image.tag=1.1.0 -n uni-book-store
helm history uni-book-store -n uni-book-store
helm rollback uni-book-store 1 -n uni-book-store
helm uninstall uni-book-store -n uni-book-store
```
