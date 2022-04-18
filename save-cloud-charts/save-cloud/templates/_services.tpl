{{- define "service.common" -}}
apiVersion: v1
kind: Service
metadata:
  name: {{ .service.name }}
spec:
  ports:
    - name: http
      port: {{ .service.containerPort }}
      targetPort: http
  selector:
    io.kompose.service: {{ .service.name }}
{{- end }}
