# Orchestration Service

Orchestration backend to start translators and manage machine annotation services. 

# Documentation

Documentation is available via swagger endpoints: 

* Test: https://dev-orchestration.dissco.tech/api/docs/swagger-ui/index.html
* Acceptance: https://acc.orchestration.dissco.tech/api/docs/swagger-ui/index.html


# Running Locally
## Requirements

Running locally requires:

- Access to the rabbitmq cluster via localhost:5672
    - `kubectl port-forward -n rabbitmq rabbitmq-cluster-server-0 5672`
- Access to the relational database (IP address is whitelisted)

## Domain Object generation

DiSSCo uses JSON schemas to generate domain objects (e.g. Digital Specimens, Digital Media, etc)
based on the openDS specification. These files are stored in the
`/target/generated-sources/jsonschema2pojo directory`, and must be generated before running locally.
The following steps indicate how to generate these objects.

### Importing Up To-Date JSON Schemas

The JSON schemas are stored in `/resources/json-schemas`. The source of truth for JSON schemas is
the [DiSSCO Schemas Site](https://schemas.dissco.tech/schemas/fdo-type/). If the JSON schema has
changed, the changes can be downloaded using the maven runner script.

1. **Update the pom.xml**: The exec-maven-plugin in the pom indicated which version of the schema to
   download. If the version has changed, update the pom.
2. **Run the exec plugin**: Before the plugin can be run, the code must be compiled. Run the
   following in the terminal (or via the IDE interface):

```
mvn compile 
mvn exec:java
```

### Building POJOs

DiSSCo uses the [JsonSchema2Pojo](https://github.com/joelittlejohn/jsonschema2pojo) plugin to
generate domain objects based on our JSON Schemas. Once the JSON schemas have been updated, you can
run the following from the terminal (or via the IDE interface):

```
mvn clean
mvn jsonschema2pojo:generate
```

## Application Properties

### Mandatory properties

```properties
spring.rabbitmq.username=RabbitMQ username
spring.rabbitmq.password=RabbitMQ password
spring.rabbitmq.host=localhost (default)
spring.datasource.url=database url (starting with jdbc:postgresql://)
spring.datasource.username=database username
spring.datasource.password=database password
spring.security.oauth2.resourceserver.jwt.issuer-uri=Keycloak base uri
spring.security.oauth2.client.provider.dissco.token-uri=token issuer endpoint
spring.security.oauth2.client.registration.dissco.client-secret=client secret
spring.security.oauth2.client.registration.dissco.client-id=client id
spring.security.oauth2.client.registration.dissco.authorization-grant-type=keycloak grant type
application.baseUrl=base url of application (e.g. https://dev-orchestration.dissco.tech"
mas.runningEndpoint=backend endpoint for MAS indicate job is running (e.g. https://dev.dissco.tech/api/v1/mjr)
endpoint.handleEndpoint=endpoint to create handles (e.g. https://dev.dissco.tech/handle-manager/api/pids/v1/)
s3.access-key=s3 access key 
s3.access-secret=s3 access key secret
```

### Optional Properties

Users can manually set exchanges and routing keys for the translator to publish to; however, these
are already defined in the translator code, and do not need to be set.

```properties
translator-job.rabbitmq.exchangeName= desired exchange
translator-job.rabbitmq.routingKeyName= desired routing key
```



