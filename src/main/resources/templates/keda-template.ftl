{
"apiVersion": "keda.sh/v1alpha1",
"kind": "ScaledObject",
  "metadata": {
  "name": "${name}-scaled-object"
  },
  "spec": {
    "minReplicaCount": 0,
    "maxReplicaCount": ${maxReplicas}.0,
    "scaleTargetRef": {
      "name": "${name}-deployment"
    },
    "triggers" : [ {
      "type" : "rabbitmq",
      "metadata" : {
        "mode" : "QueueLength",
        "value" : "1.0",
        "queueName" : "mas-${name}-queue"
      },
      "authenticationRef" : {
        "name" : "keda-trigger-auth-rabbitmq-conn"
      }
    } ]
  }
}