# Лабораторна №7 — інструкція до захисту (Observability + авто-відкат)

Метрики, логи, дашборд і метричний авто-відкат поверх GitOps (ЛР-5) та
Argo Rollouts canary (ЛР-6). Аналізований сервіс — `book-management-service`.

| Компонент | Значення |
|-----------|----------|
| Моніторинг | `kps` (Prometheus/Grafana/Alertmanager), `loki`, `alloy` у ns `monitoring` (через Argo CD) |
| Метрики застосунку | `/actuator/prometheus` → `http_server_requests_seconds_count` / `_bucket` (Micrometer) |
| Rollout | `uni-book-store-book-management` (canary + аналіз success-rate ≥ 95%) |
| Логи | один JSON на рядок у stdout → Loki (поля `level,msg,method,route,status,duration_ms,version`) |

## 0. Підготовка (стек важкий, стартує кілька хвилин)

```bash
minikube start --cpus 4 --memory 8192
```
Якщо кластер перестворювали — відновити з Git (ЛР-5): Argo CD + секрети + `bootstrap/root.yaml`,
і **Grafana-секрет** (пароль не в Git):
```bash
kubectl create namespace monitoring
```
```bash
kubectl -n monitoring create secret generic grafana-admin --from-literal=admin-user=admin --from-literal=admin-password=admin
```
Переконатись, що все Healthy:
```bash
argocd app list
```
Термінали на весь захист:
```bash
kubectl -n monitoring port-forward svc/kps-grafana 3000:80
```
```bash
kubectl -n monitoring port-forward svc/kps-prometheus 9090:9090
```
```bash
kubectl argo rollouts get rollout uni-book-store-book-management -n uni-book-store --watch
```

## 1. Стек моніторингу через Argo CD

```bash
argocd app get kps
```
```bash
kubectl -n monitoring get pods
```
**Побачите:** `kps`, `loki`, `alloy` → `Synced / Healthy`; поди Prometheus, Grafana, Alertmanager,
Loki (SingleBinary), Alloy — `Running`. Grafana відкривається на http://localhost:3000 (`admin`/`admin`).

## 2. Метрики застосунку

Prometheus (http://localhost:9090) → **Status → Targets**: ціль
`serviceMonitor/uni-book-store/uni-book-store-book-management` у стані **UP**.
Запит у Graph:
```promql
sum by (status) (rate(http_server_requests_seconds_count{namespace="uni-book-store"}[1m]))
```
**Побачите:** мітки низької кардинальності (`method`, `uri`, `status`), значення по кожному статусу.

## 3. Логи

Grafana → **Explore** → джерело **Loki**:
```logql
{namespace="uni-book-store"}
```
```logql
{namespace="uni-book-store"} | json | status >= 500
```
```logql
{namespace="uni-book-store"} | json | duration_ms > 200 | line_format "{{.route}} {{.duration_ms}}ms"
```
**Побачите:** структуровані JSON-рядки застосунку; фільтр лишає лише 5xx.

## 4. Дашборд

Grafana → Dashboards → **Uni Book Store — book-management** (приїхав з Git як ConfigMap із
міткою `grafana_dashboard`). Панелі: RPS, частка 5xx, p95, 5xx по подах, CPU/памʼять, логи з Loki.
Щоб показати «як з Git»: видаліть дашборд у UI, і після наступної синхронізації він зʼявиться знову.

## 5. Навантаження

```bash
kubectl run hey --rm -it --restart=Never --image=williamyeh/hey -- -z 3m -c 20 http://uni-book-store-book-management.uni-book-store.svc:8081/api/v1/books
```
Фоновий трафік (потрібен для аналізу в п.6):
```bash
kubectl -n uni-book-store create deployment load --image=curlimages/curl -- sh -c 'while true; do curl -s -o /dev/null http://uni-book-store-book-management:8081/api/v1/books; sleep 0.1; done'
```
```bash
kubectl -n uni-book-store scale deployment load --replicas=3
```
**Побачите:** на дашборді зростають RPS, p95 і CPU. «Інша зміна»: `bookManagement.replicaCount 5 → 8`
через Git (нові поди на панелях CPU/памʼяті), або `kubectl -n uni-book-store delete pod <pod>` (провал/відновлення).

## 6. Автоматичний відкат за метриками

У чарті: `AnalysisTemplate book-management-success-rate` (частка non-5xx ≥ 0.95) підключено
як крок `analysis` у canary (замість ручної паузи).

**Поганий реліз** — у `charts/uni-book-store/values-minikube.yaml` виставити `FAIL_RATE=0.5`:
```bash
cd ~/IdeaProjects/uni-book-store-gitops
```
```yaml
# env:
#   - name: FAIL_RATE
#     value: "0.5"
```
```bash
git commit -am "Bad release: FAIL_RATE=0.5" && git push
```
```bash
kubectl argo rollouts get rollout uni-book-store-book-management -n uni-book-store --watch
```
```bash
kubectl -n uni-book-store get analysisrun
```
```bash
kubectl -n uni-book-store describe analysisrun <name>
```
**Побачите:** canary `setWeight 20 → pause → AnalysisRun **Failed**` (успішних < 95%, бо нові поди
віддають 500) → автоматичний **abort** → `Degraded`, трафік лишився на stable. На дашборді:
сплеск 5xx лише від подів нового ReplicaSet (панель «5xx по подах»). У Loki:
```logql
{namespace="uni-book-store"} | json | status >= 500
```
— рядки лише з подів нової версії (видно за полем `version`).

**Завершити відкат через Git:**
```bash
git revert --no-edit <коміт із FAIL_RATE> && git push
```
```bash
argocd app get uni-book-store --refresh
```
**Побачите:** Rollout → `Healthy` на stable; помилки на дашборді зникають.

(Опційно §6d) Алерт `BookManagementHighErrorRate` (PrometheusRule, >5% 5xx): Prometheus → **Alerts**
(Pending → Firing) або Alertmanager (`port-forward svc/kps-alertmanager 9093`). Через `for: 1m` і
швидкий abort алерт може не встигнути — для показу тимчасово вимкніть аналіз і випустіть погану
версію повністю.

## 7. Порядок живої демонстрації

1. Prometheus → Targets: ціль UP; запит до своєї метрики.
2. Запуск навантаження → RPS/p95/CPU на дашборді.
3. Grafana Explore → логи, фільтр 5xx.
4. Коміт із `FAIL_RATE=0.5` → `AnalysisRun Failed` → abort (повернення на stable) → сплеск 5xx на
   дашборді й у Loki → `git revert`.

Тримайте відкритими Grafana (діапазон 15 хв, автооновлення 10s), `get rollout --watch` і термінал з Git.
Будьте готові написати довільний PromQL/LogQL.

## Корисні запити / дебаг

```promql
sum(rate(http_server_requests_seconds_count{namespace="uni-book-store"}[1m]))
```
```promql
histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{namespace="uni-book-store"}[1m])))
```
```bash
kubectl -n uni-book-store get servicemonitor,analysistemplate,prometheusrule
```
```bash
kubectl argo rollouts get rollout uni-book-store-book-management -n uni-book-store
```

- **Ціль не UP:** порт Service має імʼя `http`; метрики є лише на образі з micrometer (≥ 1.2.0).
- **Аналіз завжди Failed/Error:** немає трафіку (0/0 = NaN) або невірна адреса Prometheus
  (`http://kps-prometheus.monitoring:9090`) — тримайте фоновий `load`.
- **Дашборд не зʼявився:** немає мітки `grafana_dashboard: "1"` або зламаний JSON-відступ у шаблоні.
- **У Loki немає логів:** Alloy relabel бере `app` з `app.kubernetes.io/name`; шукайте за `namespace`.
- **Висока кардинальність:** у мітці `uri`/`route` опинився id замість шаблону маршруту.
