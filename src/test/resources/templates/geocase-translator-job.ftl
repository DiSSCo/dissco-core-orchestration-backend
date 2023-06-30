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
              value: geoCase
            - name: webclient.endpoint
              value: ${endpoint}
            - name: webclient.query-params
              value: ${query}
            - name: opends.service-name
              value: ${serviceName}
              <#if itemsPerRequest??>
            - name: webclient.items-per-request
              value: ${itemsPerRequest}
              </#if>
            - name: kafka.host
              value: ${kafkaHost}
            - name: kafka.topic
              value: ${kafkaTopic}
          securityContext:
            runAsNonRoot: true
            allowPrivilegeEscalation: false
