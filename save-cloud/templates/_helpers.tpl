{{- define "deployment.props" -}}
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: {{ .containerPort }}
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port:  {{ .containerPort }}
lifecycle:
  preStop:
    exec:
      # wait till spring finishes gracefully with `server.shutdown=graceful`
      command: ["sh", "-c", "'sleep 10'"]
{{- end }}