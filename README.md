# spring-microservices

# Configuration management

Managing application configuration is critical for microservices running in the cloud because microservice instances need to be launched quickly with minimal human intervention.

**Four main principles about configuration:**

* **Segregate** — We want to completely separate the services configuration informa- tion from the actual physical deployment of a service. Application configura- tion shouldn’t be deployed with the service instance. Instead, configuration information should either be passed to the starting service as environment vari- ables or read from a centralized repository when the service starts.
* **Abstract** — Abstract the access of the configuration data behind a service inter- face. Rather than writing code that directly accesses the service repository (that is, read the data out of a file or a database using JDBC), have the application use a REST-based JSON service to retrieve the configuration data.
* **Centralize** — Because a cloud-based application might literally have hundreds of services, it’s critical to minimize the number of different repositories used to hold configuration information. Centralize your application configuration into as few repositories as possible.

## Configuration management architecture

Let’s take the four principles (segregate, abstract, centralize, and harden) and see how these four principles apply when the service is bootstrapping.

![config](https://github.com/rgederin/spring-microservices/blob/master/img/config.png)

1. When a microservice instance comes up, it’s going to call a service endpoint to read its configuration information that’s specific to the environment it’s operat- ing in. The connection information for the configuration management (con- nection credentials, service endpoint, and so on) will be passed into the microservice when it starts up.
2. The actual configuration will reside in a repository. Based on the implementation of your configuration repository, you can choose to use different implementa- tions to hold your configuration data. The implementation choices can include files under source control, a relational database, or a key-value data store.
3. The actual management of the application configuration data occurs indepen- dently of how the application is deployed. Changes to configuration manage- ment are typically handled through the build and deployment pipeline where changes of the configuration can be tagged with version information and deployed through the different environments.
4. When a configuration management change is made, the services that use that application configuration data must be notified of the change and refresh their copy of the application data.

## Implementation choices

![config](https://github.com/rgederin/spring-microservices/blob/master/img/config-impl.png)

## Spring cloud config

Spring Cloud Config provides server and client-side support for externalized configuration in a distributed system. With the Config Server you have a central place to manage external properties for applications across all environments. The concepts on both client and server map identically to the Spring Environment and PropertySource abstractions, so they fit very well with Spring applications, but can be used with any application running in any language. As an application moves through the deployment pipeline from dev to test and into production you can manage the configuration between those environments and be certain that applications have everything they need to run when they migrate. The default implementation of the server storage backend uses git so it easily supports labelled versions of configuration environments, as well as being accessible to a wide range of tooling for managing the content. It is easy to add alternative implementations and plug them in with Spring configuration

### Features

**Spring Cloud Config Server features:**

* HTTP, resource-based API for external configuration (name-value pairs, or equivalent YAML content)
* Encrypt and decrypt property values (symmetric or asymmetric)
* Embeddable easily in a Spring Boot application using @EnableConfigServer

**Config Client features (for Spring applications):**

* Bind to the Config Server and initialize Spring Environment with remote property sources
8 Encrypt and decrypt property values (symmetric or asymmetric)

