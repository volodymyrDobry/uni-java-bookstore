# kubectl — шпаргалка команд

Довідник команд для захисту лабораторних (k8s / Helm). Namespace проєкту — `uni-book-store`.

> Порада: `kubectl config set-context --current --namespace=uni-book-store` — щоб не писати `-n` щоразу.
> Нижче `-n uni-book-store` вказано явно.

---

## 1. Контекст і кластер

```bash
kubectl config current-context          # на який кластер націлений kubectl
kubectl config get-contexts             # усі контексти
kubectl cluster-info                    # адреси control-plane
kubectl get nodes                       # вузли кластера
kubectl get nodes -o wide               # + IP, ОС, runtime
kubectl version                         # версії клієнта і сервера
kubectl api-resources                   # усі типи обʼєктів + їхні короткі імена
```

## 2. Namespaces

```bash
kubectl get namespaces                  # усі namespace
kubectl create namespace <ns>           # створити
kubectl delete namespace <ns>           # видалити (каскадно все всередині!)
kubectl config set-context --current --namespace=<ns>   # зробити типовим
```

## 3. Перегляд обʼєктів (get)

```bash
kubectl get pods -n uni-book-store                 # поди
kubectl get pods -n uni-book-store -o wide         # + вузол, IP, образ
kubectl get deploy,rs,pods,svc,ingress,pvc -n uni-book-store   # кілька типів разом
kubectl get all -n uni-book-store                  # основні обʼєкти
kubectl get pods -A                                # по всіх namespace
kubectl get pods -l app=backend -n uni-book-store  # за міткою (label selector)
kubectl get pods -w -n uni-book-store              # стежити наживо (Ctrl+C вихід)
kubectl get endpoints <svc> -n uni-book-store      # IP подів за Service (балансування)
```

Формати виводу:
```bash
kubectl get pod <pod> -o yaml -n uni-book-store     # повний YAML обʼєкта
kubectl get pod <pod> -o json -n uni-book-store     # JSON
kubectl get deploy backend -n uni-book-store \
  -o jsonpath='{.spec.template.spec.containers[0].image}'   # витягти одне поле
```

## 4. Діагностика (describe / events / logs / exec)

```bash
kubectl describe pod <pod> -n uni-book-store        # повний опис + секція Events (причини!)
kubectl get events -n uni-book-store --sort-by=.lastTimestamp   # події namespace
kubectl logs <pod> -n uni-book-store                # логи контейнера
kubectl logs <pod> --tail=20 -n uni-book-store      # останні 20 рядків
kubectl logs <pod> -f -n uni-book-store             # стежити наживо
kubectl logs <pod> --previous -n uni-book-store     # логи попередньої (впалої) спроби
kubectl logs deploy/backend -n uni-book-store       # логи через Deployment (сам обере под)
kubectl exec -it <pod> -n uni-book-store -- sh      # інтерактивний shell у контейнері
kubectl exec deploy/backend -n uni-book-store -- printenv   # виконати команду в поді
```

*Петля діагностики:* `get pods` → `describe` (Events) → `logs` → `exec` → фікс → `rollout status`.

## 5. Створення та видалення (apply / delete)

```bash
kubectl apply -f k8s/                        # застосувати всі маніфести з каталогу
kubectl apply -f k8s/ --dry-run=client       # перевірити без змін у кластері
kubectl apply -f file.yaml                   # один файл
kubectl delete -f k8s/                        # видалити описані обʼєкти
kubectl delete pod <pod> -n uni-book-store    # видалити под (ReplicaSet створить новий)
kubectl delete pod <pod> --force --grace-period=0 -n uni-book-store   # форсувати
```

## 6. Зміни та масштабування

```bash
kubectl scale deploy/backend --replicas=4 -n uni-book-store         # масштабування
kubectl set image deploy/backend backend=img:1.1.0 -n uni-book-store # змінити образ
kubectl edit deploy/backend -n uni-book-store                        # відкрити в редакторі
kubectl annotate deploy/backend kubernetes.io/change-cause="..." --overwrite -n uni-book-store
```

## 7. Rollout (оновлення, історія, відкат)

```bash
kubectl rollout status deploy/backend -n uni-book-store     # дочекатись завершення
kubectl rollout history deploy/backend -n uni-book-store    # історія ревізій
kubectl rollout undo deploy/backend -n uni-book-store       # відкат на крок назад
kubectl rollout undo deploy/backend --to-revision=1 -n uni-book-store  # на конкретну
kubectl rollout restart deploy/backend -n uni-book-store    # перезапуск (rolling)
```

## 8. Ресурси та метрики

```bash
kubectl top nodes -n uni-book-store         # навантаження вузлів (потрібен metrics-server)
kubectl top pods -n uni-book-store          # навантаження подів
```

## 9. Доступ до застосунку

```bash
kubectl port-forward svc/backend 8088:80 -n uni-book-store   # проброс порту на localhost
# далі http://localhost:8088/actuator/health
```

## 10. Секрети та конфіги

```bash
kubectl get configmap <name> -o yaml -n uni-book-store
kubectl get secret <name> -o yaml -n uni-book-store
kubectl get secret <name> -n uni-book-store \
  -o jsonpath='{.data.KEY}' | base64 -d        # розкодувати значення Secret
```

---

## Корисні прапорці (працюють з більшістю команд)

| Прапорець | Що робить |
|-----------|-----------|
| `-n <ns>` | namespace |
| `-A` | по всіх namespace |
| `-o wide` | більше колонок (вузол, IP, образ) |
| `-o yaml` / `-o json` | повний обʼєкт |
| `-o jsonpath='{...}'` | витягти одне поле |
| `-l key=value` | фільтр за міткою (label selector) |
| `--field-selector` | фільтр за полем (напр. `status.phase=Running`) |
| `-w` | watch — стежити за змінами наживо |
| `--dry-run=client` | не застосовувати, лише перевірити |
| `-f <файл/каталог>` | джерело маніфестів |

---

## minikube (локальний кластер)

```bash
minikube start --driver=docker       # запустити
minikube status                      # стан
minikube stop                        # зупинити
minikube delete                      # видалити кластер
minikube ip                          # IP вузла
minikube addons enable ingress       # аддон ingress
minikube addons enable metrics-server
minikube image load <img>            # завантажити локальний образ у кластер
minikube image ls                    # образи в кластері
minikube service <svc> -n <ns> --url # URL сервісу з хоста
minikube tunnel                      # тунель для Ingress/LoadBalancer (docker driver)
minikube dashboard                   # веб-UI
```

## Helm (Lab 4)

```bash
helm lint helm/                                   # перевірка чарта
helm template <rel> helm/ -f helm/values-dev.yaml # рендер локально
helm install <rel> helm/ -f vals -n <ns> --create-namespace
helm upgrade <rel> helm/ -f vals [--set k=v] -n <ns>
helm list -n <ns>                                 # релізи
helm status <rel> -n <ns>                         # статус + NOTES
helm get values <rel> -n <ns> [--all]             # значення релізу
helm history <rel> -n <ns>                        # ревізії
helm rollback <rel> <N> -n <ns>                   # відкат
helm uninstall <rel> -n <ns>                      # видалення
helm dependency update helm/                      # залежності
helm package helm/                                # запакувати у .tgz
```
