{{- define "service.common.metadata" -}}
name: {{ .service.name }}
{{- end }}

{{- define "service.common.ports" -}}
- name: http
  port: {{ .service.containerPort }}
  targetPort: http
{{- end }}

{{- define "service.common.selectors" -}}
io.kompose.service: {{ .service.name }}
{{- end }}
