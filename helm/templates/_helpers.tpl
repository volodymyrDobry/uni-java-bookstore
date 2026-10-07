{{/*
Chart base name (overridable via nameOverride).
*/}}
{{- define "uni-book-store.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Fully qualified app name, built from the RELEASE name so the chart can be
installed multiple times in one cluster without object-name collisions.
*/}}
{{- define "uni-book-store.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
Component-specific object names (release-based).
*/}}
{{- define "uni-book-store.backend.fullname" -}}
{{- printf "%s-backend" (include "uni-book-store.fullname" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "uni-book-store.postgres.fullname" -}}
{{- printf "%s-postgres" (include "uni-book-store.fullname" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Chart name and version, used as a label.
*/}}
{{- define "uni-book-store.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Common labels applied to every object.
*/}}
{{- define "uni-book-store.labels" -}}
helm.sh/chart: {{ include "uni-book-store.chart" . }}
app.kubernetes.io/name: {{ include "uni-book-store.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{/*
Selector labels for the BACKEND component.
*/}}
{{- define "uni-book-store.backend.selectorLabels" -}}
app.kubernetes.io/name: {{ include "uni-book-store.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/component: backend
{{- end -}}

{{/*
Selector labels for the POSTGRES component.
*/}}
{{- define "uni-book-store.postgres.selectorLabels" -}}
app.kubernetes.io/name: {{ include "uni-book-store.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/component: postgres
{{- end -}}
