apiVersion: batch/v1
kind: Job
metadata:
  name: ${jobName}
  namespace: ${namespace}
spec:
  backoffLimit: 2
  template:
    spec:
      restartPolicy: Never
      serviceAccountName: translator-secret-manager
      containers:
        - name: ${containerName}
          image: ${image}
          resources:
            requests:
              memory: 2G
            limits:
              memory: 2G
          env:
            - name: spring.profiles.active
              value: biocase
            - name: spring.rabbitmq.host
              value: rabbitmq-cluster.rabbitmq.svc.cluster.local
            - name: spring.rabbitmq.username
              valueFrom:
                secretKeyRef:
                  name: aws-secrets
                  key: rabbitmq-username
            - name: spring.rabbitmq.password
              valueFrom:
                secretKeyRef:
                  name: aws-secrets
                  key: rabbitmq-password
            - name: application.sourceSystemId
              value: ${sourceSystemId}
          <#if maxItems??>
            - name: application.maxItems
              value: "${maxItems?c}"
          </#if>
            - name: spring.datasource.url
              value: ${database_url}
            - name: spring.datasource.username
              valueFrom:
                secretKeyRef:
                  name: db-secrets
                  key: db-username
            - name: spring.datasource.password
              valueFrom:
                secretKeyRef:
                  name: db-secrets
                  key: db-password
            - name: fdo.digital-media-type
              value: https://doi.org/21.T11148/bbad8c4e101e8af01115
            - name: fdo.digital-specimen-type
              value: https://doi.org/21.T11148/894b1e6cad57e921764e
            - name: JAVA_TOOL_OPTIONS
              value: -XX:MaxRAMPercentage=85
          securityContext:
            runAsNonRoot: true
            allowPrivilegeEscalation: false
          volumeMounts:
            - name: db-secrets
              mountPath: "/mnt/secrets-store/db-secrets"
              readOnly: true
            - name: aws-secrets
              mountPath: "/mnt/secrets-store/aws-secrets"
              readOnly: true
      volumes:
        - name: db-secrets
          csi:
            driver: secrets-store.csi.k8s.io
            readOnly: true
            volumeAttributes:
              secretProviderClass: "db-secrets"
        - name: aws-secrets
          csi:
            driver: secrets-store.csi.k8s.io
            readOnly: true
            volumeAttributes:
              secretProviderClass: "aws-secrets"
