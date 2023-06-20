apiVersion: apps/v1
kind: Deployment
metadata:
  name: ${name}-deployment
  labels:
    app: ${name}
spec:
  replicas: 1
  selector:
    matchLabels:
      app: ${name}
  template:
    metadata:
      labels:
        app: ${name}
    spec:
      containers:
        - name: ${name}
          image: ${image}:${imageTag}
          imagePullPolicy: Always
          ports:
            - containerPort: 80
          env:
            - name: KAFKA_CONSUMER_HOST
              value: kafka.kafka.svc.cluster.local:9092
            - name: KAFKA_PRODUCER_HOST
              value: kafka.kafka.svc.cluster.local:9092
            - name: KAFKA_CONSUMER_TOPIC
              value: plant-organ-detection
            - name: KAFKA_PRODUCER_TOPIC
              value: annotations
            - name: KAFKA_CONSUMER_GROUP
              value: group
          securityContext:
            runAsNonRoot: true
            allowPrivilegeEscalation: false
