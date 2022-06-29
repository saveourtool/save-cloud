{{- define "service.common.metadata" -}}
name: {{ .service.name }}
{{- end }}

{{- define "service.common.ports" -}}
- name: http
  port: 80
  targetPort: http
{{- end }}

{{- define "service.common.selectors" -}}
io.kompose.service: {{ .service.name }}
{{- end }}
