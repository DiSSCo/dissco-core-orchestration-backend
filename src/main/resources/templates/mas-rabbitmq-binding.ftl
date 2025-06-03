{
    "apiVersion": "rabbitmq.com/v1beta1",
    "kind": "Binding",
    "metadata": {
        "name": "mas-${name}-binding",
        "namespace": "machine-annotation-services"
    },
    "spec": {
        "destination": "mas-${name}-queue",
        "destinationType": "queue",
        "routingKey": "${name}",
        "source": "${exchangeName}",
        "rabbitmqClusterReference": {
            "name": "rabbitmq-cluster",
            "namespace": "rabbitmq"
        },
        "vhost": "/"
    }
}