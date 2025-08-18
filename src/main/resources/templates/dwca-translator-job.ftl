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
              value: dwca
            - name: dwca.download-file
              value: /temp/darwin.zip
            - name: dwca.temp-folder
              value: /temp/darwin
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
            - name: mas.force-mas-schedule
              value: ${forceMasSchedule?c}
          <#if additionalSpecimenMass??>
            - name: mas.additional-specimen-mass
              value: ${additionalSpecimenMass}
          </#if>
          <#if additionalMediaMass??>
            - name: mas.additional-media-mass
              value: ${additionalMediaMass}
          </#if>
          securityContext:
            runAsNonRoot: true
            allowPrivilegeEscalation: false
          volumeMounts:
            - mountPath: /temp
              name: temp-volume
            - name: db-secrets
              mountPath: "/mnt/secrets-store/db-secrets"
              readOnly: true
            - name: aws-secrets
              mountPath: "/mnt/secrets-store/aws-secrets"
              readOnly: true
      volumes:
        - name: temp-volume
          emptyDir: { }
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
