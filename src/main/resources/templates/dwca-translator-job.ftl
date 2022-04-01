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
          env:
            - name: spring.profiles.active
              value: dwca
            - name: webclient.endpoint
              value: ${endpoint}
            - name: dwca.download-file
              value: /temp/darwin.zip
            - name: dwca.temp-folder
              value: /temp/darwin
            - name: kafka.host
              value: ${kafkaHost}
            - name: kafka.topic
              value: ${kafkaTopic}
            - name: opends.service-name
              value: ${serviceName}
          securityContext:
            runAsNonRoot: true
            allowPrivilegeEscalation: false
          volumeMounts:
            - mountPath: /temp
              name: temp-volume
      volumes:
        - name: temp-volume
          emptyDir: { }