{{- define "common.labels" -}}
io.kompose.service: {{ .service.name }}
version: {{ or .service.dockerTag .Values.dockerTag }}
env: {{ .Values.env }}
{{- end }}

{{- define "pod.common.labels" }}
io.kompose.service: {{ .service.name }}
version: {{ or .service.dockerTag .Values.dockerTag }}
{{- end }}

{{- define "pod.common.annotations" }}
prometheus.io/scrape: 'true'
prometheus.io/path: /actuator/prometheus
{{- if (hasKey .service "managementPort") }}
prometheus.io/port: {{ .service.managementPort | quote }}
{{- end }}
{{- end }}

{{/* Common Linux user configuration for paketo-created containers, where user is cnb:cnb */}}
{{- define "cnb.securityContext" -}}
securityContext:
  runAsUser: 1000
  runAsGroup: 1000
  fsGroup: 1000
{{- end }}

{{/* Common configuration of Kubernetes related things in spring-boot */}}
{{- define "spring-boot.management" -}}
startupProbe:
  # give spring-boot enough time to start
  httpGet:
    path: /actuator/health/liveness
    port: {{ or .managementPort .containerPort }}
  failureThreshold: 30
  periodSeconds: 10
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: {{ or .managementPort .containerPort }}
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: {{ or .managementPort .containerPort }}
lifecycle:
  preStop:
    exec:
      # wait till spring finishes gracefully with `server.shutdown=graceful`
      command: ["sh", "-c", "sleep 10"]
{{- end }}

{{/* Common configuration of deployment for spring-boot microservice */}}
{{- define "spring-boot.common" -}}
image: '{{ .Values.imageRegistry }}/{{ .service.imageName }}:{{ or .service.dockerTag .Values.dockerTag }}'
imagePullPolicy: {{ .Values.pullPolicy }}
ports:
  - name: http
    containerPort:  {{ .service.containerPort }}
  {{ if .service.managementPort }}
  - name: mgmt
    containerPort:  {{ .service.managementPort }}
  {{ end }}
{{- end }}

{{- define "spring-boot.common.env" -}}
- name: SPRING_PROFILES_ACTIVE
  value: {{ or .service.profile .Values.profile }}
{{- end }}

{{- define "spring-boot.config-volume-mount" -}}
name: config-volume
mountPath: /home/cnb/config
{{- end }}

{{- define "spring-boot.config-volume" -}}
name: config-volume
configMap:
  name: {{ .service.name }}-config
  items:
    - key: application.properties
      path: application.properties
{{- end}}

{{- define "spring-boot.sa-token-mount" -}}
name: service-account-projected-token
mountPath: /var/run/secrets/tokens
{{- end }}

{{- define "spring-boot.sa-token-volume" -}}
name: service-account-projected-token
projected:
  sources:
    - serviceAccountToken:
        path: service-account-projected-token
        expirationSeconds: 7200
{{- end}}