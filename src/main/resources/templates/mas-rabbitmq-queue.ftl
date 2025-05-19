{
    "apiVersion": "rabbitmq.com/v1beta1",
    "kind": "Queue",
    "metadata": {
        "name": "mas-${name}-queue",
        "namespace": "rabbitmq"
    },
    "spec": {
        "autoDelete": false,
        "durable": true,
        "name": "mas-${name}-queue",
        "type": "quorum",
        "rabbitmqClusterReference": {
            "name": "rabbitmq-cluster"
        }
    }
}