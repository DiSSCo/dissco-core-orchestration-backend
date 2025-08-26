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
