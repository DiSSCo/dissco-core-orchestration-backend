apiVersion: batch/v1
kind: Job
metadata:
  name: ${jobName}
spec:
  backoffLimit: 2
  template:
    spec:
      restartPolicy: Never
      containers:
        - name: ${containerName}
          image: ${image}
          resources:
            requests:
              memory: 512M
            limits:
              memory: 512M
          env:
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
                  key: rabbitmq-password
            - name: spring.rabbitmq.password
              valueFrom:
                secretKeyRef:
                  name: aws-secrets
                  key: rabbitmq-username
            - name: webclient.sourceSystemId
              value: ${sourceSystemId}
            - name: spring.datasource.url
              value: jdbc:postgresql://database-1.cbppwfnjypll.eu-west-2.rds.amazonaws.com/dissco
            - name: spring.datasource.username
              valueFrom:
                secretKeyRef:
                  name: db-user-pass
                  key: username
            - name: spring.datasource.password
              valueFrom:
                secretKeyRef:
                  name: db-user-pass
                  key: password
          securityContext:
            runAsNonRoot: true
            allowPrivilegeEscalation: false
          volumeMounts:
            - mountPath: /temp
              name: temp-volume
      volumes:
        - name: temp-volume
          emptyDir: { }