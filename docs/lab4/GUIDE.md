# Лабораторна робота №4 — повний гайд (Helm)

Покроковий розбір усієї лаби: до кожного завдання — команди, пояснення й трохи теорії.
Застосунок — backend **uni-book-store** (Spring Boot) + PostgreSQL. Чарт — у [`helm/`](../../helm/).

## Трохи теорії для старту

**Helm** — пакетний менеджер для Kubernetes. **Чарт** — це пакет (шаблони + значення),
який Helm перетворює на обʼєкти кластера й керує ними як єдиним **релізом**.

- **Шаблони** (`templates/*.yaml`) — маніфести, де жорсткі значення замінені на `{{ .Values.x }}`.
- **values.yaml** — значення за замовчуванням.
- **Реліз** — конкретна встановлена інсталяція чарта (з іменем і namespace).
- **Ревізія** — версія релізу; кожен `upgrade`/`rollback` створює нову ревізію.

Навіщо: одна команда замість багатьох `kubectl apply`, параметризація під середовища,
версіонування й життєвий цикл (`install`/`upgrade`/`rollback`/`uninstall`).

---

## Завдання 1 — Підготувати середовище

### 1a. Встановити Helm 3 і перевірити версію
```bash
brew install helm
helm version        # потрібна мажорна версія 3+
```
*Теорія:* Helm 3 — лише клієнт (CLI), працює з кластером через ваш `kubectl`-контекст;
нічого в кластер ставити не треба (на відміну від Helm 2 з Tiller).

### 1b. Запустити кластер minikube
```bash
minikube start --driver=docker
```

### 1c. Переконатися, що маніфести Lab 3 розгортаються
```bash
kubectl apply -f k8s/
kubectl -n uni-book-store get pods
```

### 1d. Видалити реліз Lab 3 (уникнути конфлікту імен)
```bash
kubectl delete namespace uni-book-store
```
*Навіщо:* Helm позначає свої обʼєкти мітками `app.kubernetes.io/managed-by=Helm`.
Якщо обʼєкт з таким імʼям уже існує й не належить Helm — установка впаде. Чистимо старе.

---

## Завдання 2 — Створити структуру чарта

### 2a. Каталог чарта
```bash
mkdir -p helm/templates
```

### 2b. Chart.yaml
```yaml
apiVersion: v2
name: uni-book-store
description: Helm chart for the uni-book-store backend and its PostgreSQL database
type: application
version: 0.1.0
appVersion: "1.0.0"
```
*Теорія:* `apiVersion: v2` — формат для Helm 3; `type: application` — звичайний застосунок.

### 2c. version vs appVersion
- **`version`** — версія **чарта** (рецепта): шаблони/values.
- **`appVersion`** — версія **застосунку** (backend-образу).
Детальніше — у [helm/README.md](../../helm/README.md).

---

## Завдання 3 — Перенести маніфести в шаблони

### 3a. Кожен маніфест → окремий файл у `templates/`
`configmap.yaml`, `secret.yaml`, `postgres-pvc.yaml`, `postgres-deployment.yaml`,
`postgres-service.yaml`, `backend-deployment.yaml`, `backend-service.yaml`, `ingress.yaml`.
*Примітка:* Namespace у шаблони не переносимо — його створює `--create-namespace`.

### 3b. Жорсткі значення → `.Values`
Приклад (backend-deployment):
```yaml
replicas: {{ .Values.backend.replicaCount }}
image: "{{ .Values.backend.image.repository }}:{{ .Values.backend.image.tag | default .Chart.AppVersion }}"
```
*Теорія:* `.Values` — дерево значень із values.yaml; `| default` — функція Sprig для дефолту.

### 3c. Імена обʼєктів з імені релізу (щоб ставити двічі)
Через іменований шаблон у `_helpers.tpl`:
```yaml
name: {{ include "uni-book-store.backend.fullname" . }}
```
`fullname` будується з `.Release.Name`, тож `r1-…` і `r2-…` не конфліктують.

### 3d. Secret — значення з values
```yaml
stringData:
  SPRING_DATASOURCE_PASSWORD: {{ .Values.postgres.auth.password | quote }}
```

Перевірка:
```bash
helm lint helm/
helm template demo helm/ | grep -E '^kind:|^  name:'
```

---

## Завдання 4 — Описати values.yaml

### 4a. Винести у значення ключові параметри
Образ (repo/tag), репліки, тип/порт сервісу, ресурси, конфіг застосунку, Ingress — усе в [values.yaml](../../helm/values.yaml).

### 4b. Дефолти = dev
1 репліка, тег = `appVersion`, скромні ресурси.

### 4c. Прапорці `enabled` для необовʼязкових обʼєктів
```yaml
ingress:
  enabled: true
postgres:
  persistence:
    enabled: true
```
Перевірка, що прапорець працює:
```bash
helm template demo helm/ --set ingress.enabled=false | grep -c 'kind: Ingress'   # 0
```
*Теорія:* у шаблоні обгортка `{{- if .Values.ingress.enabled }} … {{- end }}` вмикає/вимикає обʼєкт.

---

## Завдання 5 — Спільні шаблони та підказка

### 5a. `_helpers.tpl` — іменовані шаблони
`uni-book-store.fullname` (повне імʼя), `uni-book-store.labels` (набір міток),
`*.selectorLabels` (селектори per-component).
*Теорія:* `{{- define "name" -}} … {{- end -}}` оголошує шаблон; викликається через `include`.

### 5b. Підключити через `include`
```yaml
labels:
  {{- include "uni-book-store.labels" . | nindent 4 }}
```
`nindent 4` — відступ 4 пробіли (щоб YAML лишався валідним).

### 5c. NOTES.txt
`templates/NOTES.txt` виводиться після `helm install` — як відкрити застосунок.
Текст адаптивний (Ingress / NodePort / port-forward).

---

## Завдання 6 — Два середовища

### 6a. values-dev.yaml / values-prod.yaml
Відрізняються: репліки (1↔3), ресурси, тег (1.0.0↔1.1.0), Ingress (off↔on).

### 6b. Різниця без встановлення в кластер
```bash
helm template uni helm/ -f helm/values-dev.yaml  > /tmp/dev.yaml
helm template uni helm/ -f helm/values-prod.yaml > /tmp/prod.yaml
diff /tmp/dev.yaml /tmp/prod.yaml
```
*Теорія:* `helm template` рендерить локально, у кластер нічого не йде.

### 6c. Пріоритет значень
```
values.yaml  <  -f файл  <  --set
```
```bash
helm template r helm/ -f helm/values-prod.yaml --set backend.replicaCount=5 | grep -m1 replicas
```

---

## Завдання 7 — Перевірити та встановити чарт

### 7a. Перевірка на помилки
```bash
helm lint helm/
helm lint helm/ -f helm/values-dev.yaml
```

### 7b. Переглянути маніфести й порівняти з Lab 3
```bash
helm template uni-book-store helm/ -f helm/values-dev.yaml > /tmp/helm-rendered.yaml
helm template uni-book-store helm/ -f helm/values-dev.yaml | grep -E '^kind:|^  name:'
grep -E '^kind:|^  name:' k8s/0*.yaml     # для порівняння
```

### 7c. Встановити у власний namespace
```bash
helm install uni-book-store helm/ -f helm/values-dev.yaml \
  --namespace uni-book-store --create-namespace
kubectl -n uni-book-store rollout status deploy/uni-book-store-backend
```

### 7d. Перелік релізів, статус, значення
```bash
helm list -n uni-book-store
helm status uni-book-store -n uni-book-store
helm get values uni-book-store -n uni-book-store --all
```

### 7e. Де Helm зберігає стан
```bash
kubectl -n uni-book-store get secrets --field-selector type=helm.sh/release.v1
```
*Теорія:* Helm 3 зберігає знімок кожної ревізії як Secret `sh.helm.release.v1.<реліз>.v<N>`
(gzip+base64). Звідси — історія й rollback.

---

## Завдання 8 — Життєвий цикл релізу

### 8a. Оновити тег образу через `--set`
```bash
helm upgrade uni-book-store helm/ -f helm/values-dev.yaml \
  --set backend.image.tag=1.1.0 -n uni-book-store
```

### 8b. Оновити файлом іншого середовища
```bash
helm upgrade uni-book-store helm/ -f helm/values-prod.yaml -n uni-book-store
```

### 8c. Історія ревізій
```bash
helm history uni-book-store -n uni-book-store
```

### 8d. Відкат
```bash
helm rollback uni-book-store 1 -n uni-book-store
```
*Теорія:* rollback не «стирає» історію — додає НОВУ ревізію, що повторює конфіг обраної.

### 8e. Видалити реліз однією командою
```bash
helm uninstall uni-book-store -n uni-book-store
kubectl -n uni-book-store get all,ingress,pvc,configmap,secret
```

---

## Завдання 9 — На вибір: 9a (залежність від стороннього чарта)

У `Chart.yaml`:
```yaml
dependencies:
  - name: postgresql
    version: "15.x.x"
    repository: "https://charts.bitnami.com/bitnami"
    condition: postgresql.enabled
```
Команди:
```bash
helm dependency update helm/     # завантажує subchart у helm/charts/
helm dependency list helm/
helm install uni-book-store helm/ -n uni-book-store --create-namespace \
  --set postgresql.enabled=true
```
*Теорія:* залежність (subchart) живе в `charts/`, фіксується у `Chart.lock`, а
налаштовується через значення батьківського чарта (простір імен ключів = імʼя залежності,
тут `postgresql.*`). Умова `condition` вмикає/вимикає залежність прапорцем.

---

## Завдання 10 — Оформити результати

### 10a. Закомітити чарт
```bash
git add helm/ docs/lab4/
git commit -m "Lab-4 | Helm chart + docs"
```

### 10b. README з таблицею values і командами
Див. [helm/README.md](../../helm/README.md).

### 10c. Звіт по пунктах 6–9
Див. [docs/lab4/REPORT.md](REPORT.md) (зі скриншотами).

---

## Шпаргалка команд Helm

```bash
helm lint <chart>                       # перевірка чарта
helm template <rel> <chart> -f vals      # рендер локально
helm install <rel> <chart> -f vals -n <ns> --create-namespace
helm upgrade <rel> <chart> -f vals [--set k=v] -n <ns>
helm list -n <ns>                        # релізи
helm status <rel> -n <ns>                # статус + NOTES
helm get values <rel> -n <ns> [--all]    # значення релізу
helm history <rel> -n <ns>               # ревізії
helm rollback <rel> <revision> -n <ns>   # відкат
helm uninstall <rel> -n <ns>             # видалення
helm dependency update <chart>           # залежності
helm package <chart>                     # запакувати у .tgz
```
