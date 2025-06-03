apiVersion: apps/v1
kind: Deployment
metadata:
  labels:
    app: ${pid}
  name: ${pid}-deployment
  namespace: machine-annotation-services
spec:
  replicas: 1
  selector:
    matchLabels:
      app: ${pid}
  template:
    metadata:
      labels:
        app: ${pid}
    spec:
      containers:
      - name: ${pid}
        image: ${image}:${imageTag}
        imagePullPolicy: Always
        env:
        - name: MAS_NAME
          value: ${name}
        - name: MAS_ID
          value: ${id}
        - name: RABBITMQ_HOST
          value: rabbitmq-cluster.rabbitmq.svc.cluster.local
        - name: RABBITMQ_QUEUE
          value: ${topicName}-queue
        - name: RABBITMQ_USER
          valueFrom:
            secretKeyRef:
              name: aws-secrets
              key: rabbitmq-username
        - name: RABBITMQ_PASSWORD
          valueFrom:
            secretKeyRef:
              name: aws-secrets
              key: rabbitmq-password
        - name: RUNNING_ENDPOINT
          value: https://dev.dissco.tech/api/v1/mjr
        - name: GEOPICK_USER
          valueFrom:
            secretKeyRef:
              key: geopick-user
              name: mas-secrets
        - name: GEOPICK_PASSWORD
          valueFrom:
            secretKeyRef:
              key: geopick-password
              name: mas-secrets
        securityContext:
          allowPrivilegeEscalation: false
          runAsNonRoot: true
        volumeMounts:
        - mountPath: /temp
          name: temp-volume
        - mountPath: /mnt/secrets-store/mas-secrets
          name: mas-secrets
          readOnly: true
        - name: aws-secrets
          mountPath: "/mnt/secrets-store/aws-secrets"
          readOnly: true
      volumes:
      - emptyDir: {}
        name: temp-volume
      - name: mas-secrets
        csi:
          driver: secrets-store.csi.k8s.io
          readOnly: true
          volumeAttributes:
            secretProviderClass: mas-secrets
      - name: aws-secrets
        csi:
          driver: secrets-store.csi.k8s.io
          readOnly: true
          volumeAttributes:
            secretProviderClass: "aws-secrets"
