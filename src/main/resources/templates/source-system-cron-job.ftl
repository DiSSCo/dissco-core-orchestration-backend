apiVersion: batch/v1
kind: CronJob
metadata:
  name: ${jobName}
  namespace: ${namespace}
spec:
  schedule: "${cron}"
  jobTemplate:
    spec:
      template:
        spec:
          containers:
            - name: ${jobName}
              image: ${ jobImage }
              env:
                - name: KEYCLOAK_SERVER
                  value: ${keycloakServer}
                - name: REALM
                  value: dissco
                - name: CLIENT_ID
                  valueFrom:
                    secretKeyRef:
                      name: aws-secrets
                      key: export-client-id
                - name: CLIENT_SECRET
                  valueFrom:
                    secretKeyRef:
                      name: aws-secrets
                      key: export-client-secret
                - name: SOURCE_SYSTEM_ID
                  value: ${sourceSystemId}
                - name: DISSCO_DOMAIN
                  value: ${disscoDomain}
                - name: EXPORT_TYPE
                  value: ${exportType}
          restartPolicy: Never