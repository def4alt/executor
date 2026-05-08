{{/*
Expand the name of the chart.
*/}}
{{- define "executor.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
Truncated at 63 chars because some Kubernetes name fields are limited to 63.
*/}}
{{- define "executor.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version for use in chart labels.
*/}}
{{- define "executor.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels.
*/}}
{{- define "executor.labels" -}}
helm.sh/chart: {{ include "executor.chart" . }}
{{ include "executor.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels (used in spec.selector.matchLabels).
*/}}
{{- define "executor.selectorLabels" -}}
app.kubernetes.io/name: {{ include "executor.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use.
*/}}
{{- define "executor.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "executor.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Image reference with optional registry and tag.
*/}}
{{- define "executor.image" -}}
{{- $registry := .Values.image.registry | default "docker.io" }}
{{- $repository := .Values.image.repository }}
{{- $tag := .Values.image.tag | default (printf "v%s" .Chart.AppVersion) }}
{{- printf "%s/%s:%s" $registry $repository $tag }}
{{- end }}

{{/*
Return the appropriate apiVersion for ingress.
*/}}
{{- define "executor.ingress.apiVersion" -}}
{{- if and (.Capabilities.APIVersions.Has "networking.k8s.io/v1") (semverCompare ">=1.19-0" .Capabilities.KubeVersion.Version) }}
{{- print "networking.k8s.io/v1" }}
{{- else if .Capabilities.APIVersions.Has "networking.k8s.io/v1beta1" }}
{{- print "networking.k8s.io/v1beta1" }}
{{- else }}
{{- print "extensions/v1beta1" }}
{{- end }}
{{- end }}

{{/*
Return the path type for ingress.
*/}}
{{- define "executor.ingress.pathType" -}}
{{- if semverCompare ">=1.18-0" $.Capabilities.KubeVersion.Version }}
{{- print "Prefix" }}
{{- end }}
{{- end }}
