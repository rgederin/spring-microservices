# spring-microservices

# Table of Contents

- [Configuration management](#configuration-management)
    * [Configuration management architecture](#configuration-management-architecture)
    * [Implementation choices](#implementation-choices)
    * [Spring cloud config](#spring-cloud-config)
- [Service Discovery](#service-discovery)
    * [Non-cloud service discovery](#non-cloud-service-discovery)
    * [Service discovery in the cloud](#service-discovery-in-the-cloud)
    * [Service discovery in action using Spring and Netflix Eureka and Ribbon](#service-discovery-in-action-using-spring-and-netflix-eureka-and-ribbon)
    * [Using service discovery to look up a service](#using-service-discovery-to-look-up-a-service)
    * [Summary](#summary)


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
* Encrypt and decrypt property values (symmetric or asymmetric)


# Service Discovery

In any distributed architecture, we need to find the physical address of where a machine is located. This concept has been around since the beginning of distrib- uted computing and is known formally as service discovery. Service discovery can be something as simple as maintaining a property file with the addresses of all the remote services used by an application, or something as formalized (and compli- cated) as a UDDI (Universal Description, Discovery, and Integration) repository.

Service discovery is critical to microservice, cloud-based applications for two key reasons. First, it offers the application team the ability to quickly horizontally scale up and down the number of service instances running in an environment. The service consumers are abstracted away from the physical location of the service via service dis- covery. Because the service consumers don’t know the physical location of the actual service instances, new service instances can be added or removed from the pool of available services.

This ability to quickly scale services without disrupting the service consumers is an extremely powerful concept, because it moves a development team used to building monolithic, single-tenant (for example, one customer) applications away from think- ing about scaling only in terms of adding bigger, better hardware (vertical scaling) to the more powerful approach to scaling by adding more servers (horizontal scaling).

A monolithic approach usually drives development teams down the path of over- buying their capacity needs. Capacity increases come in clumps and spikes and are rarely a smooth steady path. Microservices allow us to scale up/down new service instances. Service discovery helps abstract that these deployments are occurring away from the service consumer.

The second benefit of service discovery is that it helps increase application resil- iency. When a microservice instance becomes unhealthy or unavailable, most service discovery engines will remove that instance from its internal list of available services. The damage caused by a down service will be minimized because the service discovery engine will route services around the unavailable service.

## Non-cloud service discovery

Whenever you have an application calling resources spread across multiple servers, it needs to locate the physical location of those resource. In the non-cloud world, this service location resolution was often solved through a combination of DNS and a net- work load balancer.

An application needs to invoke a service located in another part of the organiza- tion. It attempts to invoke the service by using a generic DNS name along with a path that uniquely represents the service that the application was trying to invoke. The DNS name would resolve to a commercial load balancer, such as the popular F5 load balancer (http://f5.com) or an open source load balancer such as HAProxy (http:// haproxy.org).

![discovery](https://github.com/rgederin/spring-microservices/blob/master/img/service-discovery-traditional.png)

The load balancer, upon receiving the request from the service consumer, locates the physical address entry in a routing table based on the path the user was trying to access. This routing table entry contains a list of one or more servers hosting the ser- vice. The load balancer then picks one of the servers in the list and forwards the request onto that server.

Each instance of a service is deployed to one or more application servers. The number of these application servers was often static (for example, the number of application servers hosting a service didn’t go up and down) and persistent (for exam- ple, if a server running an application server crashed, it would be restored to the same state it was at the time of the crash, and would have the same IP and configuration that it had previously.)

To achieve a form of high availability, a secondary load balancer is sitting idle and pinging the primary load balancer to see if it’s alive. If it isn’t alive, the secondary load balancer becomes active, taking over the IP address of the primary load balancer and beginning serving requests.

While this type of model works well with applications running inside of the four walls of a corporate data center and with a relatively small number of services running on a group of static servers, **it doesn’t work well for cloud-based microservice applications.** Reasons for this include:

1. *Single point of failure* — While the load balancer can be made highly available, it’s a single point of failure for your entire infrastructure. If the load balancer goes down, every application relying on it goes down too. While you can make a load balancer highly available, load balancers tend to be centralized chokepoints within your application infrastructure.
2. *Limited horizontal scalability* — By centralizing your services into a single cluster of load balancers, you have limited ability to horizontally scale your load-balancing infrastructure across multiple servers. Many commercial load balancers are con- strained by two things: their redundancy model and licensing costs. Most com- mercial load balancers use a hot-swap model for redundancy so you only have a single server to handle the load, while the secondary load balancer is there only for fail-over in the case of an outage of the primary load balancer. You are, in essence, constrained by your hardware. Second, commercial load balancers also have restrictive licensing models geared toward a fixed capacity rather than a more variable model.
3. *Statically managed* - Most traditional load balancers aren’t designed for rapid registration and de-registration of services. They use a centralized database to store the routes for rules and the only way to add new routes is often through the vendor’s proprietary API.
4. *Complex* — Because a load balancer acts as a proxy to the services, service con- sumer requests have to have their requests mapped to the physical services. This translation layer often added a layer of complexity to your service infra- structure because the mapping rules for the service have to be defined and deployed by hand. In a traditional load balancer scenario, this registration of new service instances was done by hand and not at startup time of a new ser- vice instance.


## Service discovery in the cloud

The solution for a cloud-based microservice environment is to use a service-discovery mechanism that’s

* *Highly available* — Service discovery needs to be able to support a “hot” cluster- ing environment where service lookups can be shared across multiple nodes in a service discovery cluster. If a node becomes unavailable, other nodes in the cluster should be able to take over.
* *Peer-to-peer* — Each node in the service discovery cluster shares the state of a ser- vice instance.
* *Load balanced* — Service discovery needs to dynamically load balance requests across all service instances to ensure that the service invocations are spread across all the service instances managed by it. In many ways, service discovery replaces the more static, manually managed load balancers used in many early web application implementations.
* *Resilient* — The service discovery’s client should “cache” service information locally. Local caching allows for gradual degradation of the service discovery feature so that if service discovery service does become unavailable, applications can still function and locate the services based on the information main- tained in its local cache.
* *Fault-tolerant* — Service discovery needs to detect when a service instance isn’t healthy and remove the instance from the list of available services that can take client requests. It should detect these faults with services and take action with- out human intervention.

## The architecture of service discovery

To begin our discussion around service discovery architecture, we need to understand four concepts. These general concepts are shared across all service discovery imple- mentations:

* *Service registration* — How does a service register with the service discovery agent?
* *Client lookup of service address* — What’s the means by which a service client looks up service information?
* *Information sharing* — How is service information shared across nodes?
* *Health monitoring* — How do services communicate their health back to the service discovery agent?


Figure below shows the flow of these four bullets and what typically occurs in a service discovery pattern implementation.

![discovery](https://github.com/rgederin/spring-microservices/blob/master/img/service-discovery-cloud-1.png)

In figure above, one or more service discovery nodes have been started. These service discovery instances are usually unique and don’t have a load balancer that sits in front of them.

As service instances start up, they’ll register their physical location, path, and port that they can be accessed by with one or more service discovery instances. While each instance of a service will have a unique IP address and port, each service instance that comes up will register under the same service ID. A service ID is nothing more than a key that uniquely identifies a group of the same service instances.

A service will usually only register with one service discovery service instance. Most service discovery implementations use a peer-to-peer model of data propagation where the data around each service instance is communicated to all the other nodes in the cluster.

Depending on the service discovery implementation, the propagation mechanism might use a hard-coded list of services to propagate to or use a multi-casting protocol like the “gossip” or “infection-style” protocol to allow other nodes to “discover” changes in the cluster.

Finally, each service instance will push to or have pulled from its status by the service discovery service. Any services failing to return a good health check will be removed from the pool of available service instances.

Once a service has registered with a service discovery service, it’s ready to be used by an application or service that needs to use its capabilities. Different models exist for a client to “discover” a service. A client can rely solely on the service discovery engine to resolve service locations each time a service is called. With this approach, the ser- vice discovery engine will be invoked every time a call to a registered microservice instance is made. Unfortunately, this approach is brittle because the service client is completely dependent on the service discovery engine to be running to find and invoke a service.

### Client side load balancing

A more robust approach is to use what’s called client-side load balancing.

In this model, when a consuming actor needs to invoke a service

1. It will contact the service discovery service for all the service instances a service consumer is asking for and then cache data locally on the service consumer’s machine.
2. Each time a client wants to call the service, the service consumer will look up the location information for the service from the cache. Usually client-side caching will use a simple load balancing algorithm like the “round-robin” load balancing algorithm to ensure that service calls are spread across multiple service instances.
3. The client will then periodically contact the service discovery service and refresh its cache of service instances. The client cache is eventually consistent, but there’s always a risk that between when the client contacts the service discovery instance for a refresh and calls are made, calls might be directed to a service instance that isn’t healthy. If, during the course of calling a service, the service call fails, the local service discovery cache is invalidated and the service discovery client will attempt to refresh its entries from the service discovery agent.

![discovery](https://github.com/rgederin/spring-microservices/blob/master/img/service-discovery-cloud-2.png)

## Service discovery in action using Spring and Netflix Eureka and Ribbon

Now you’re going to implement service discovery by setting up a service discovery agent and then registering two services with the agent. You’ll then have one service call another service by using the information retrieved by service discovery. Spring Cloud offers multiple methods for looking up information from a service discovery agent. We’ll also walk through the strengths and weakness of each approach.

Once again, the Spring Cloud project makes this type of setup trivial to undertake. You’ll use Spring Cloud and Netflix’s Eureka service discovery engine to implement your service discovery pattern. For the client-side load balancing you’ll use Spring Cloud and Netflix’s Ribbon libraries.

![discovery](https://github.com/rgederin/spring-microservices/blob/master/img/service-discovery-cloud-3.png)

When the licensing service is invoked, it will call the organization service to retrieve the organization information associated with the designated organization ID. The actual resolution of the organization service’s location will be held in a service discovery registry. For this example, you’ll register two instances of the organization service with a service discovery registry and then use client-side load balancing to look up and cache the registry in each service instance. Figure 4.4 shows this arrangement:

1. As the services are bootstrapping, the licensing and organization services will also register themselves with the Eureka Service. This registration process will tell Eureka the physical location and port number of each service instance along with a service ID for the service being started.
2. When the licensing service calls to the organization service, it will use the Netflix Ribbon library to provide client-slide load balancing. Ribbon will contact the Eureka service to retrieve service location information and then cache it locally.
3. Periodically, the Netflix Ribbon library will ping the Eureka service and refresh its local cache of service locations.

Any new organization services instance will now be visible to the licensing service locally, while any non-healthy instances will be removed from the local cache

## Using service discovery to look up a service

We now have the organization service registered with Eureka. You can also have the licensing service call the organization service without having direct knowledge of the location of any of the organization services. The licensing service will look up the physical location of the organization by using Eureka.

For our purposes, we’re going to look at three different Spring/Netflix client libraries in which a service consumer can interact with Ribbon. These libraries will move from the lowest level of abstraction for interacting with Ribbon to the highest. The libraries we’ll explore include

* Spring Discovery client
* Spring Discovery client enabled RestTemplate  
* Netflix Feign client


### Looking up service instances with Spring DiscoveryClient

The Spring DiscoveryClient offers the lowest level of access to Ribbon and the services registered within it. Using the DiscoveryClient, you can query for all the services registered with the ribbon client and their corresponding URLs.

```
@SpringBootApplication
@EnableEurekaClient
@EnableDiscoveryClient
public class LicensingServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(LicensingServiceApplication.class, args);
	}
}

@Component
@RequiredArgsConstructor
public class OrganizationDiscoveryClient {

    private final DiscoveryClient discoveryClient;

    public Optional<Organization> getOrganization(String organizationId) {
        RestTemplate restTemplate = new RestTemplate();
        List<ServiceInstance> instances = discoveryClient.getInstances("organizationservice");

        if (instances.isEmpty()) {
            return Optional.empty();
        }

        String serviceUri = String.format("%s/v1/organizations/%s", instances.get(0).getUri().toString(), organizationId);

        ResponseEntity<Organization> restExchange =
                restTemplate.exchange(
                        serviceUri,
                        HttpMethod.GET,
                        null, Organization.class, organizationId);

        return Optional.of(restExchange.getBody());
    }
}
```

The first item of interest in the code is the DiscoveryClient. This is the class you’ll use to interact with Ribbon. To retrieve all instances of the organization services regis- tered with Eureka, you can use the getInstances() method, passing in the key of service you’re looking for, to retrieve a list of ServiceInstance objects.

The ServiceInstance class is used to hold information about a specific instance of a service including its hostname, port and URI.

In code above, you take the first ServiceInstance class in your list to build a target URL that can then be used to call your service. Once you have a target URL, you can use a standard Spring RestTemplate to call your organization service and retrieve data.

**The DiscoveryClient and real life**

I’m walking through the DiscoveryClient to be completed in our discussion of building service consumers with Ribbon. The reality is that you should only use the Discovery- Client directly when your service needs to query Ribbon to understand what services and service instances are registered with it. There are several problems with this code including the following:

You aren’t taking advantage of Ribbon’s client side load-balancing—By calling the Dis- coveryClient directly, you get back a list of services, but it becomes your responsibility to choose which service instances returned you’re going to invoke.

You’re doing too much work—Right now, you have to build the URL that’s going to be used to call your service. It’s a small thing, but every piece of code that you can avoid writing is one less piece of code that you have to debug.

Observant Spring developers might have noticed that you’re directly instantiating the RestTemplate class in the code. This is antithetical to normal Spring REST invoca- tions, as normally you’d have the Spring Framework inject the RestTemplate the class using it via the @Autowired annotation.

You instantiated the RestTemplate class in listing 4.8 because once you’ve enabled the Spring DiscoveryClient in the application class via the @EnableDiscovery- Client annotation, all RestTemplates managed by the Spring framework will have a Ribbon-enabled interceptor injected into them that will change how URLs are cre- ated with the RestTemplate class. Directly instantiating the RestTemplate class allows you to avoid this behavior.

In summary, there are better mechanisms for calling a Ribbon-backed service.

### Invoking services with Ribbon-aware Spring RestTemplate

Next, we’re going to see an example of how to use a RestTemplate that’s Ribbon- aware. This is one of the more common mechanisms for interacting with Ribbon via Spring. To use a Ribbon-aware RestTemplate class, you need to define a Rest- Template bean construction method with a Spring Cloud annotation called @Load- Balanced. 

```
@SpringBootApplication
@EnableEurekaClient
@EnableDiscoveryClient
public class LicensingServiceApplication {

	@LoadBalanced
	@Bean
	public RestTemplate getRestTemplate(){
		return new RestTemplate();
	}

	public static void main(String[] args) {
		SpringApplication.run(LicensingServiceApplication.class, args);
	}
}

@Component
@RequiredArgsConstructor
public class OrganizationRestTemplateClient {

    private final RestTemplate restTemplate;

    public Organization getOrganization(String organizationId) {
        ResponseEntity<Organization> restExchange =
                restTemplate.exchange(
                        "http://organizationservice/v1/organizations/{organizationId}",
                        HttpMethod.GET,
                        null, Organization.class, organizationId);

        return restExchange.getBody();
    }
}
```

This code should look somewhat similar to the previous example, except for two key differences. First, the Spring Cloud DiscoveryClient is nowhere in sight. Second, the URL being used in the restTemplate.exchange() call should look odd to you:

```
restTemplate.exchange(
  "http://organizationservice/v1/organizations/{organizationId}",
   HttpMethod.GET,
 null, Organization.class, organizationId);
```
 
The server name in the URL matches the application ID of the organizationservice key that you registered the organization service with in Eureka: http://{applicationid}/v1/organizations/{organizationId}

The Ribbon-enabled RestTemplate will parse the URL passed into it and use what- ever is passed in as the server name as the key to query Ribbon for an instance of a ser- vice. The actual service location and port are completely abstracted from the developer.

In addition, by using the RestTemplate class, Ribbon will round-robin load bal- ance all requests among all the service instances.

### Invoking services with Netflix Feign client

An alternative to the Spring Ribbon-enabled RestTemplate class is Netflix’s Feign client library. The Feign library takes a different approach to calling a REST service by having the developer first define a Java interface and then annotating that interface with Spring Cloud annotations to map what Eureka-based service Ribbon will invoke. The Spring Cloud framework will dynamically generate a proxy class that will be used to invoke the targeted REST service. There’s no code being written for calling the ser- vice other than an interface definition.

```
@SpringBootApplication
@EnableEurekaClient
@EnableFeignClients
public class LicensingServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(LicensingServiceApplication.class, args);
	}
}

@FeignClient("organizationservice")
public interface OrganizationFeignClient {

    @RequestMapping(
            method= RequestMethod.GET,
            value="/v1/organizations/{organizationId}",
            consumes="application/json")
    Organization getOrganization(@PathVariable("organizationId") String organizationId);
}
```

You start the Feign example by using the @FeignClient annotation and passing it the name of the application id of the service you want the interface to represent. Next you’ll define a method, getOrganization(), in your interface that can be called by the client to invoke the organization service.

How you define the getOrganization() method looks exactly like how you would expose an endpoint in a Spring Controller class. First, you’re going to define a @RequestMapping annotation for the getOrganization() method that will map the HTTP verb and endpoint that will be exposed on the organization service invocation. Second, you’ll map the organization ID passed in on the URL to an organizationId parameter on the method call, using the @PathVariable annota- tion. The return value from the call to the organization service will be automatically mapped to the Organization class that’s defined as the return value for the getOrganization() method.

To use the OrganizationFeignClient class, all you need to do is autowire and use it. The Feign Client code will take care of all the coding work for you.

## Summary

* The service discovery pattern is used to abstract away the physical location of services.
* A service discovery engine such as Eureka can seamlessly add and remove ser- vice instances from an environment without the service clients being impacted.
* Client-side load balancing can provide an extra level of performance and resiliency by caching the physical location of a service on the client making the service call.
* Eureka is a Netflix project that when used with Spring Cloud, is easy to set upand configure.
* You used three different mechanisms in Spring Cloud, Netflix Eureka, and Netflix Ribbon to invoke a service. These mechanisms included
    – Using a Spring Cloud service DiscoveryClient
    – Using Spring Cloud and Ribbon-backed RestTemplate
    – Using Spring Cloud and Netflix’s Feign client