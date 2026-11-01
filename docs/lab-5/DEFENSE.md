# Лабораторна №5 — інструкція до захисту (GitOps / Argo CD)

Покроковий сценарій від кроку 3 до кінця під поточний сетап:

| Параметр | Значення |
|----------|----------|
| GitOps-репозиторій | `github.com/volodymyrDobry/uni-book-store-gitops` (public) |
| Namespace застосунку | `uni-book-store` |
| Namespace Argo CD | `argocd` |
| Образи | `ghcr.io/volodymyrdobry/{book-management-service,order-service,uni-bookstore-client}` |
| Хости | `uni-book-store.local` (SPA), `keycloak.local` (Keycloak) |
| Тест-користувач | `testuser` / `password` |
| Нода | minikube, arm64 |

---

## 0. Підготовка перед захистом (заздалегідь, не показується)

Кластер піднятий і є **дві версії** образів (поточна `1.0.3` + нова `1.1.0` для демо оновлення).

```bash
minikube status
```
```bash
kubectl -n argocd get pods
```

Друга версія (видима зміна = номер версії у футері UI та `/actuator/info`):
```bash
git -C ~/IdeaProjects/uni-java-bookstore tag v1.1.0 && git -C ~/IdeaProjects/uni-java-bookstore push origin v1.1.0
```
```bash
git -C ~/IdeaProjects/uni-bookstore-client tag v1.1.0 && git -C ~/IdeaProjects/uni-bookstore-client push origin v1.1.0
```
Дочекатися, доки GitHub Actions опублікують `:1.1.0` (пакети публічні — нові теги теж публічні).

Два термінали, відкриті весь захист:
```bash
kubectl -n argocd port-forward svc/argocd-server 8080:443
```
```bash
sudo minikube tunnel
```

**Побачите:** `Running` усюди; `Forwarding from 127.0.0.1:8080`.

---

## Крок 3 — Argo CD + зміна пароля

```bash
argocd admin initial-password -n argocd
```
```bash
argocd login localhost:8080 --username admin --insecure
```
```bash
argocd account update-password
```
```bash
kubectl -n argocd delete secret argocd-initial-admin-secret
```

UI: **https://localhost:8080** (прийняти самопідписаний сертифікат), логін `admin` + **новий** пароль.

**Побачите:** `'admin' logged in successfully`; старий пароль не працює; в UI — застосунки `root` і `uni-book-store`.

---

## Крок 4 — Застосунок під керуванням Argo CD (App of Apps)

```bash
argocd app list
```
```bash
argocd app get uni-book-store
```
```bash
kubectl -n uni-book-store get pods
```
```bash
helm list -n uni-book-store
```

В UI: `root` → `uni-book-store` → дерево об'єктів, усе **Synced / Healthy**.

**Побачите:**
- `root` і `uni-book-store` → `Synced / Healthy`.
- 8 подів `1/1 Running` (book-management, order-service, frontend, postgres, keycloak, keycloak-db, activemq, localstack).
- `helm list -n uni-book-store` → **порожньо** (Helm лише рендерить шаблони — у цьому суть GitOps).

Живий застосунок:
```bash
open http://uni-book-store.local/
```
**Побачите:** каталог; логін `testuser` / `password`; у футері — `v1.0.3`.

---

## Крок 5 — Оновлення через Git (реліз)

Усі зміни — лише в gitops-репозиторії: `1.0.3 → 1.1.0`.
```bash
cd ~/IdeaProjects/uni-book-store-gitops
```
```bash
sed -i '' -E 's/tag: "1\.0\.3"/tag: "1.1.0"/' charts/uni-book-store/values-minikube.yaml
```
```bash
git commit -am "Release uni-book-store 1.1.0" && git push
```
```bash
argocd app get uni-book-store --refresh
```
```bash
kubectl -n uni-book-store get pods -w
```
```bash
kubectl -n uni-book-store exec deploy/uni-book-store-book-management -- wget -qO- localhost:8081/actuator/info
```

**Побачите:** `OutOfSync → Progressing → Synced / Healthy`; старі поди `Terminating`, нові `Running`; `/actuator/info` → `"version":"1.1.0"`; футер UI → `v1.1.0`.

---

## Крок 6a — Дрейф і selfHeal

**Без selfHeal** (рядок `selfHeal` у `argocd-apps/uni-book-store.yaml` закоментований):
```bash
kubectl -n uni-book-store scale deployment uni-book-store-order-service --replicas=5
```
```bash
argocd app get uni-book-store
```
**Побачите:** `OutOfSync`, але реплік залишається **5** (автосинк лише prune, кластер не чіпається).

**Вмикаємо selfHeal:**
```bash
cd ~/IdeaProjects/uni-book-store-gitops && sed -i '' 's/# selfHeal: true/selfHeal: true/' argocd-apps/uni-book-store.yaml
```
```bash
git commit -am "Enable selfHeal for uni-book-store" && git push
```
```bash
kubectl -n uni-book-store scale deployment uni-book-store-order-service --replicas=5
```
```bash
kubectl -n uni-book-store get deploy uni-book-store-order-service -w
```
**Побачите:** `root` сам застосує зміну до дочірнього Application; після наступного `scale` Argo CD за кілька секунд повертає `replicas` назад до значення з Git (1).

---

## Крок 6b — Synced, але Degraded (неіснуючий тег)

У `charts/uni-book-store/values-minikube.yaml`, блок `bookManagement`, поставте `tag: "9.9.9"`:
```bash
cd ~/IdeaProjects/uni-book-store-gitops
```
```bash
git commit -am "Break: book-management 9.9.9" && git push
```
```bash
argocd app get uni-book-store --refresh
```
```bash
kubectl -n uni-book-store get pods
```
**Побачите:** `Sync Status: Synced` (кластер = Git), але `Health Status: Degraded`/`Progressing`; под book-management у `ImagePullBackOff`.

---

## Крок 6c — Відкат через git revert

```bash
git log --oneline
```
```bash
git revert --no-edit <хеш-комміту-з-9.9.9> && git push
```
```bash
argocd app get uni-book-store --refresh
```
```bash
argocd app history uni-book-store
```
**Побачите:** знову `Synced / Healthy`, поди `Running`; `app history` — список синхронізацій, кожна прив'язана до комміту Git.

> `argocd app rollback` працює лише з вимкненою автосинхронізацією — інакше Argo CD одразу поверне стан із Git. Тому відкочуємося **коммітом** (`git revert`).

---

## Крок 7 — Порядок живої демонстрації (чек-лист)

1. Інтерфейс Argo CD: `root` → `uni-book-store` → дерево, `Synced / Healthy`.
2. Реліз (§5): тег у Git → `commit`/`push` → `Refresh` → нова версія в `/actuator/info` і футері.
3. selfHeal (§6a): `kubectl scale --replicas=5` → Argo CD повертає репліки.
4. Degraded (§6b): тег `9.9.9` → `Synced + Degraded`.
5. Відкат (§6c): `git revert` → `Healthy`, показати `app history`.
6. Готовність: викладач може попросити змінити інше значення (репліки, env) у Git — алгоритм той самий: правка в gitops → `commit`/`push` → `Refresh`.

---

## Дебаг / корисні команди

```bash
argocd app get uni-book-store
```
```bash
kubectl -n uni-book-store describe pod <pod-name> | tail -30
```
```bash
kubectl -n uni-book-store logs deploy/uni-book-store-order-service --tail=50
```
```bash
argocd app get uni-book-store --refresh
```
```bash
argocd app sync uni-book-store
```

- `localhost:80` → **404**: звертайтесь за **хостом** (`http://uni-book-store.local/`), інгрес маршрутизує за Host.
- Перевірити хости: `grep -E "uni-book-store.local|keycloak.local" /etc/hosts`.
- `ImagePullBackOff` після зміни тегу: образ має існувати й бути **публічним** у GHCR, тег у `values-minikube.yaml` = опублікованому.

**Типові помилки:** приватний пакет → `ImagePullBackOff`; `values-minikube.yaml not found` → `valueFiles` відносно `path` чарту; дочірні `Application` мають бути в namespace `argocd`; зміна не приїхала → не той branch/тег або не минув інтервал (тисніть **Refresh**).
