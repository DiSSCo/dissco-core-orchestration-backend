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
            - name: kafka.host
              value: ${kafkaHost}
            - name: kafka.topic
              value: ${kafkaTopic}
            - name: webclient.sourceSystemId
              value: ${sourceSystemId}
            - name: spring.datasource.url
              value: jdbc:postgresql://terraform-20230822143945532600000001.cbppwfnjypll.eu-west-2.rds.amazonaws.com:5432/dissco
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
            - name: fdo.digital-media-object-type
              value: https://doi.org/21.T11148/bbad8c4e101e8af01115
            - name: fdo.digital-specimen-type
              value: https://doi.org/21.T11148/894b1e6cad57e921764e
            - name: JAVA_OPTS
              value: -server -XX:+useContainerSupport -XX:MaxRAMPercentage=75 --illegal-access=deny
          securityContext:
            runAsNonRoot: true
            allowPrivilegeEscalation: false
          volumeMounts:
            - mountPath: /temp
              name: temp-volume
      volumes:
        - name: temp-volume
          emptyDir: { }