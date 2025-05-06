apiVersion: keda.sh/v1alpha1
kind: ScaledObject
metadata:
  name: ${name}-scaled-object
spec:
  scaleTargetRef:
    name: ${name}-deployment
  minReplicaCount: 0
  maxReplicaCount:  ${maxReplicas}
  triggers:
    - type: rabbitmq
      metadata:
        mode: QueueLength
        value: "1"
        queueName: ${topicName}-queue
      authenticationRef:
        name: keda-trigger-auth-rabbitmq-conn
