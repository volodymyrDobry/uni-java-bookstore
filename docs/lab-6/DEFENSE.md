# Лабораторна №6 — інструкція до захисту (Argo Rollouts, canary)

Progressive delivery поверх GitOps із ЛР-5. Обрана стратегія — **canary** для
`book-management-service`.

**Чому canary і саме цей сервіс:** `book-management-service` — stateless read-API
каталогу з чистим ендпоінтом версії (`/actuator/info`), за внутрішнім nginx-шлюзом.
Canary дає змогу перевести на нову версію лише частину трафіку (за кількістю реплік —
ClusterIP-Service балансує між stable і canary подами), переконатися на `/actuator/info`
і або `promote`, або `abort` — безпечніше за миттєве перемикання. 5 реплік, щоб
співвідношення версій було видно.

| Параметр | Значення |
|----------|----------|
| GitOps-репо | `github.com/volodymyrDobry/uni-book-store-gitops` |
| Rollout | `uni-book-store-book-management` у ns `uni-book-store` |
| Контролер | `argo-rollouts` (ns `argo-rollouts`, чарт 2.43.8, через Argo CD) |
| Сервіс для перевірки версії | `uni-book-store-book-management:8081/actuator/info` |

---

## 0. Підготовка перед захистом

Плагін CLI:
```bash
brew install argoproj/tap/kubectl-argo-rollouts
```
```bash
kubectl argo rollouts version
```

Образи **щонайменше трьох** версій (stable + для релізу + для abort). Поточна stable —
`1.0.4`. Підготуйте наступні (пуш тегів в app-репо збирає й публікує образ):
```bash
git -C ~/IdeaProjects/uni-java-bookstore tag v1.1.0 && git -C ~/IdeaProjects/uni-java-bookstore push origin v1.1.0
```
```bash
git -C ~/IdeaProjects/uni-java-bookstore tag v1.2.0 && git -C ~/IdeaProjects/uni-java-bookstore push origin v1.2.0
```
> Кожен тег через авто-CI відкриє PR у gitops — **не мерджте** їх завчасно, бампити
> версію будемо вручну під час демо. (Або закрийте ці PR і правте тег руками.)

Термінали, відкриті весь захист:
```bash
kubectl -n argocd port-forward svc/argocd-server 8080:443
```
```bash
kubectl argo rollouts get rollout uni-book-store-book-management -n uni-book-store --watch
```
```bash
kubectl argo rollouts dashboard   # http://localhost:3100
```

---

## Крок 1 — Контролер Argo Rollouts через Argo CD

Показати, що контролер прийшов із Git (окремий Application, sync-wave -1):
```bash
argocd app get argo-rollouts
```
```bash
kubectl -n argo-rollouts get pods
```
```bash
kubectl get crd | grep argoproj
```
```bash
kubectl argo rollouts version
```

**Побачите:** `argo-rollouts` → `Synced / Healthy`; поди контролера `Running`;
CRD `rollouts.argoproj.io` та інші; версія плагіна.

---

## Крок 2–3 — Rollout у чарті + стратегія canary

Показати в Git `charts/uni-book-store/`:
- `templates/book-management-deployment.yaml` обгорнуто `{{- if not .Values.rollout.enabled }}`;
- `templates/book-management-rollout.yaml` — той самий под-спек + `strategy` з values;
- стратегія у `values.yaml` (`rollout.strategy.canary.steps`): `20 → pause{} → 60 → pause 30s → 80 → pause 30s`;
- `values-minikube.yaml`: `rollout.enabled: true`, `bookManagement.replicaCount: 5`.

```bash
kubectl -n uni-book-store get rollout
```
```bash
kubectl argo rollouts get rollout uni-book-store-book-management -n uni-book-store
```
В Argo CD у дереві `uni-book-store` видно об'єкт **Rollout**, його ReplicaSet-и й поди.

**Побачите:** `rollout ... 5/5`, `Status: ✔ Healthy`; у дереві Argo CD — Rollout замість Deployment.

---

## Крок 4 — Реліз через Git (canary)

У **окремому** терміналі запустіть цикл запитів **зсередини кластера** (port-forward
показав би лише один под і розподілу не видно):
```bash
kubectl -n uni-book-store run curl --rm -it --image=curlimages/curl --restart=Never -- \
  sh -c 'while true; do curl -s http://uni-book-store-book-management:8081/actuator/info | grep -o "\"version\":\"[^\"]*\""; sleep 0.5; done'
```

Зробіть реліз — підніміть тег образу в gitops (напр. `1.0.4 → 1.1.0`):
```bash
cd ~/IdeaProjects/uni-book-store-gitops
```
```bash
sed -i '' -E 's/(book-management-service\n[[:space:]]*tag: ")1\.0\.4/\11.1.0/' charts/uni-book-store/values-minikube.yaml   # або вручну у блоці bookManagement
```
```bash
git commit -am "Release book-management 1.1.0" && git push
```
```bash
argocd app get uni-book-store --refresh
```

**Побачите:** у `get rollout --watch` — Rollout переходить у крок `setWeight: 20` і зупиняється
на `pause: {}` зі статусом **Paused**; у циклі запитів **приблизно кожна п'ята** відповідь —
нова версія (`1.1.0`), решта — `1.0.4`; `argocd app get uni-book-store` → `Health: Suspended`.

Promote на ручній паузі (далі таймери 30s самі доведуть до 100%):
```bash
kubectl argo rollouts promote uni-book-store-book-management -n uni-book-store
```
**Побачите:** кроки `60 → 80 → 100`, усі відповіді стають `1.1.0`, Rollout → `Healthy`,
Argo CD → `Synced / Healthy`. (Те саме можна з UI Argo CD: об'єкт Rollout → ⋮ → Resume/Promote-Full.)

---

## Крок 5 — Скасування релізу (abort)

Новий реліз (`1.1.0 → 1.2.0`), дочекатися першої паузи, потім abort:
```bash
cd ~/IdeaProjects/uni-book-store-gitops && git commit -am "Release book-management 1.2.0" && git push   # попередньо змінивши тег на 1.2.0
```
```bash
argocd app get uni-book-store --refresh
```
```bash
kubectl argo rollouts abort uni-book-store-book-management -n uni-book-store
```
```bash
kubectl argo rollouts get rollout uni-book-store-book-management -n uni-book-store
```
```bash
argocd app get uni-book-store
```

**Побачите:** Rollout `Status: ✖ Degraded`, трафік повернувся на stable `1.1.0`;
Argo CD → **`Synced` + `Degraded`**. Пояснення: маніфест у кластері = Git (там тег `1.2.0`),
тому **Synced**; але реліз скасовано й працює стара версія, тому **Degraded**.

Завершуємо відкат **через Git**:
```bash
git log --oneline
```
```bash
git revert --no-edit <коміт із 1.2.0> && git push
```
```bash
argocd app get uni-book-store --refresh
```
```bash
kubectl argo rollouts get rollout uni-book-store-book-management -n uni-book-store
```
**Побачите:** Rollout → `Healthy` на `1.1.0`; Argo CD → `Synced / Healthy`.

> `kubectl argo rollouts retry rollout ...` повторює той самий реліз (якщо проблему
> виправлено поза образом). Але штатний відкат desired-стану — через `git revert`.

---

## Крок 6 — Порядок живої демонстрації

1. Argo CD: `argo-rollouts` Healthy; у `uni-book-store` — об'єкт Rollout.
2. Реліз комітом → пауза (**Suspended/Paused**) → у циклі видно ~20% нової версії → `promote` → `Healthy`.
3. Новий реліз → `abort` на паузі → **Synced + Degraded** → `git revert` → `Healthy`.

Тримайте відкритими `get rollout --watch`, цикл запитів і дашборд/Argo CD.

---

## Дебаг / корисні команди

```bash
kubectl argo rollouts get rollout uni-book-store-book-management -n uni-book-store
```
```bash
kubectl argo rollouts status uni-book-store-book-management -n uni-book-store
```
```bash
kubectl -n uni-book-store get rollout,rs,pods -l app.kubernetes.io/component=book-management
```
```bash
argocd app get uni-book-store
```

- `no matches for kind "Rollout"` / `SyncFailed` — застосунок синхронізувався раніше за CRD.
  Дочекайтеся `argo-rollouts` Healthy і натисніть Sync (у нас у Application є `retry`).
- Одночасно Deployment і Rollout з тим самим селектором — забули `if not .Values.rollout.enabled`
  у `deployment.yaml`.
- Canary «не ділить» трафік — перевіряли через `port-forward` (один под). Запускайте цикл
  **у поді всередині кластера** (як вище).
- Реліз не стартує — Rollout реагує лише на зміну **шаблону пода** (образ/env/мітки),
  а не на зміну `replicas` чи самої стратегії.
