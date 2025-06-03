{
    "apiVersion": "rabbitmq.com/v1beta1",
    "kind": "Queue",
    "metadata": {
        "name": "mas-${name}-queue",
        "namespace": "machine-annotation-services"
    },
    "spec": {
        "durable": true,
        "name": "mas-${name}-queue",
        "type": "quorum",
        "vhost": "/",
        "rabbitmqClusterReference": {
            "name": "rabbitmq-cluster",
            "namespace": "rabbitmq"
        }
    }
}