{
  "apiVersion": "apps/v1",
  "kind": "Deployment",
  "metadata": {
    "name": "${name}-deployment",
    "labels": {
      "app": "${name}"
    }
  },
  "spec": {
    "replicas": 1,
    "selector": {
      "matchLabels": {
        "app": "${name}"
      }
    },
    "template": {
      "metadata": {
        "labels": {
          "app": "${name}"
        }
      },
      "spec": {
        "containers": [
          {
          "name": "${name}",
          "image": "${image}:${imageTag}",
          "imagePullPolicy": "Always",
          "env": [
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
            "value": "annotations"
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
