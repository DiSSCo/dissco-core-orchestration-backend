{
  "apiVersion": "apps/v1",
  "kind": "Deployment",
  "metadata": {
    "name": "${pid}-deployment",
    "labels": {
      "app": "${pid}"
    }
  },
  "spec": {
    "replicas": 1,
    "selector": {
      "matchLabels": {
        "app": "${pid}"
      }
    },
    "template": {
      "metadata": {
        "labels": {
          "app": "${pid}"
        }
      },
      "spec": {
        "serviceAccountName":"mas-secret-manager",
        "automountServiceAccountToken": true,
        "containers": [
          {
          "name": "${pid}",
          "image": "${image}:${imageTag}",
          "imagePullPolicy": "Always",
          "env": [
            {
            "name": "MAS_NAME",
            "value": "${name}"
            },
            {
            "name": "MAS_ID",
            "value": "${id}"
            },
            {
            "name": "KAFKA_CONSUMER_HOST",
            "value": "${kafkaHost}"
            },
            {
            "name": "KAFKA_PRODUCER_HOST",
            "value": "${kafkaHost}"
            },
            {
            "name": "KAFKA_CONSUMER_TOPIC",
            "value": "${topicName}"
            },
            {
            "name": "KAFKA_PRODUCER_TOPIC",
            "value": "annotation"
            },
            {
            "name": "KAFKA_CONSUMER_GROUP",
            "value": "group"
            }
          ],
          "securityContext": {
            "allowPrivilegeEscalation": false,
            "runAsNonRoot": true
          }
        }]
      }
    }
  }
}
