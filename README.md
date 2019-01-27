# spring-microservices

# Spring Cloud configuration server

Managing application configuration is critical for microservices running in the cloud because microservice instances need to be launched quickly with minimal human intervention.

**Four main principles about configuration:**

* **Segregate** — We want to completely separate the services configuration informa- tion from the actual physical deployment of a service. Application configura- tion shouldn’t be deployed with the service instance. Instead, configuration information should either be passed to the starting service as environment vari- ables or read from a centralized repository when the service starts.
* **Abstract** — Abstract the access of the configuration data behind a service inter- face. Rather than writing code that directly accesses the service repository (that is, read the data out of a file or a database using JDBC), have the application use a REST-based JSON service to retrieve the configuration data.
* **Centralize** — Because a cloud-based application might literally have hundreds of services, it’s critical to minimize the number of different repositories used to hold configuration information. Centralize your application configuration into as few repositories as possible.
* **Harden** —Because your application configuration information is going to be completely segregated from your deployed service and centralized, it’s critical that whatever solution you utilize can be implemented to be highly available and redundant.