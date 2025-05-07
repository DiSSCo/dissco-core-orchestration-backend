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
    "triggers" : [ {
      "type" : "rabbitmq",
      "metadata" : {
        "mode" : "QueueLength",
        "value" : "1",
        "queueName" : "${name}-queue"
      },
      "authenticationRef" : {
        "name" : "keda-trigger-auth-rabbitmq-conn"
      }
    } ]
  }
}