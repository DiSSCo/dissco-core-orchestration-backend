{
"apiVersion": "keda.sh/v1alpha1",
"kind": "ScaledObject",
  "metadata": {
  "name": "${name}-scaled-object"
  },
  "spec": {
    "minReplicaCount": 0,
    "maxReplicaCount": ${maxReplicas},
    "scaleTargetRef": {
      "name": "${name}-deployment"
    },
  "triggers": [
    {
      "metadata": {
        "bootstrapServers": "${kafkaHost}",
        "consumerGroup": "group",
        "lagThreshold": "5",
        "topic": "${topicName}"
      },
      "type": "kafka"
    }]
  }
}
