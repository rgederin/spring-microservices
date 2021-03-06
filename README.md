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
- [Client resiliency patterns with Spring Cloud and Netflix Hystrix](#client-resiliency-patterns-with-spring-cloud-and-netflix-hystrix)
    * [Client-side resiliency patterns](#client-side-resiliency-patterns)
    * [Circuit breaker using Hystrix](#circuit-breaker-using-hystrix)
    * [Fallback processing using Hystrix](#fallback-processing-using-hystrix)
    * [Bulkhead pattern using Hystrix](##bulkhead-pattern-using-hystrix)
    * [Hystrix dashboard](#hystrix-dashboard)
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
    1. Using a Spring Cloud service DiscoveryClient
    2. Using Spring Cloud and Ribbon-backed RestTemplate
    3. Using Spring Cloud and Netflix’s Feign client


# Client resiliency patterns with Spring Cloud and Netflix Hystrix

All systems, especially distributed systems, will experience failure. How we build our applications to respond to that failure is a critical part of every software developer’s job. However, when it comes to building resilient systems, most software engineers only take into account the complete failure of a piece of infrastructure or a key service. They focus on building redundancy into each layer of their application using techniques such as clustering key servers, load balancing between services, and segregation of infrastructure into multiple locations.

While these approaches take into account the complete (and often spectacular) loss of a system component, they address only one small part of building resilient sys- tems. When a service crashes, it’s easy to detect that it’s no longer there, and the appli- cation can route around it. However, when a service is running slow, detecting that poor performance and routing around it is extremely difficult because

1. **Degradation of a service can start out as intermittent and build momentum** — The degradation might occur only in small bursts. The first signs of failure might be a small group of users complaining about a problem, until suddenly the applica- tion container exhausts its thread pool and collapses completely.
2. **Calls to remote services are usually synchronous and don’t cut short a long-running call** — The caller of a service has no concept of a timeout to keep the service call from hanging out forever. The application developer calls the service to per- form an action and waits for the service to return.
3. **Applications are often designed to deal with complete failures of remote resources, not partial degradations.** Often, as long as the service has not completely failed, an application will continue to call the service and won’t fail fast. The application will continue to call the poorly behaving service. The calling application or service may degrade gracefully or, more likely, crash because of resource exhaustion. **Resource exhaustion** is when a limited resource such as a thread pool or database connection maxes out and the calling client must wait for that resource to become available.

What’s insidious about problems caused by poorly performing remote services is that they’re not only difficult to detect, but can trigger a cascading effect that can ripple throughout an entire application ecosystem. Without safeguards in place, a single poorly performing service can quickly take down multiple applications. Cloud-based, microservice-based applications are particularly vulnerable to these types of outages because these applications are composed of a large number of fine-grained, distrib-uted services with different pieces of infrastructure involved in completing a user’s transaction.

## Client-side resiliency patterns

Client resiliency software patterns are focused on protecting a remote resource’s (another microservice call or database lookup) client from crashing when the remote resource is failing because that remote service is throwing errors or performing poorly. The goal of these patterns is to allow the client to “fail fast,” not consume valu- able resources such as database connections and thread pools, and prevent the prob- lem of the remote service from spreading “upstream” to consumers of the client.

There are four client resiliency patterns:

1. Client-side load balancing
2. Circuit breakers
3. Fallbacks
4. Bulkheads

![resiliency](https://github.com/rgederin/spring-microservices/blob/master/img/resiliency-1.png)

Figure above demonstrates how these patterns sit between the microservice service consumer and the microservice.

These patterns are implemented in the client calling the remote resource. The implementation of these patterns logically sit between the client consuming the remote resources and the resource itself.

### Client-side load balancing

We introduced the client-side load balancing pattern in the previous section when talking about service discovery. Client-side load balancing involves having the client look up all of a service’s individual instances from a service discovery agent (like Netflix Eureka) and then caching the physical location of said service instances.

Whenever a service consumer needs to call that service instance, the client-side load balancer will return a location from the pool of service locations it’s maintaining.

Because the client-side load balancer sits between the service client and the service consumer, the load balancer can detect if a service instance is throwing errors or behaving poorly. If the client-side load balancer detects a problem, it can remove that service instance from the pool of available service locations and prevent any future service calls from hitting that service instance.

This is exactly the behavior that Netflix’s Ribbon libraries provide out of the box with no extra configuration.

### Circuit breaker

The circuit breaker pattern is a client resiliency pattern that’s modeled after an elec- trical circuit breaker. In an electrical system, a circuit breaker will detect if too much current is flowing through the wire. If the circuit breaker detects a problem, it will break the connection with the rest of the electrical system and keep the downstream components from the being fried.

With a software circuit breaker, when a remote service is called, the circuit breaker will monitor the call. If the calls take too long, the circuit breaker will intercede and kill the call. In addition, the circuit breaker will monitor all calls to a remote resource and if enough calls fail, the circuit break implementation will pop, failing fast and pre- venting future calls to the failing remote resource.

### Fallback processing

With the fallback pattern, when a remote service call fails, rather than generating an exception, the service consumer will execute an alternative code path and try to carry out an action through another means. This usually involves looking for data from another data source or queueing the user’s request for future processing. The user’s call will not be shown an exception indicating a problem, but they may be notified that their request will have to be fulfilled at a later date.

For instance, suppose you have an e-commerce site that monitors your user’s behavior and tries to give them recommendations of other items they could buy. Typi- cally, you might call a microservice to run an analysis of the user’s past behavior and return a list of recommendations tailored to that specific user. However, if the preference service fails, your fallback might be to retrieve a more general list of preferences that’s based off all user purchases and is much more generalized. This data might come from a completely different service and data source.

### How these patterns could help in real life

![resiliency](https://github.com/rgederin/spring-microservices/blob/master/img/resiliency-2.png)

If enough errors on the service have occurred within a certain time period, the cir- cuit breaker will now “trip” the circuit and all calls to Service C will fail without calling Service C.

This tripping of the circuit allows three things to occur:

1. Service B now immediately knows there’s a problem without having to wait for a timeout from the circuit breaker.
2. Service B can now choose to either completely fail or take action using an alter- native set of code (a fallback).
3. Service C will be given an opportunity to recover because Service B isn’t calling it while the circuit breaker has been tripped. This allows Service C to have breathing room and helps prevent the cascading death that occurs when a ser- vice degradation occurs.

Finally, the circuit breaker will occasionally let calls through to a degraded service, and if those calls succeed enough times in a row, the circuit breaker will reset itself.

The key thing a circuit break patterns offers is the ability for remote calls to

1. **Fail fast** — When a remote service is experiencing a degradation, the application will fail fast and prevent resource exhaustion issues that normally shut down the entire application. In most outage situations, it’s better to be partially down rather than completely down.
2. **Fail gracefully** — By timing out and failing fast, the circuit breaker pattern gives the application developer the ability to fail gracefully or seek alternative mecha- nisms to carry out the user’s intent. For instance, if a user is trying to retrieve data from one data source, and that data source is experiencing a service degra- dation, then the application developer could try to retrieve that data from another location.
3. **Recover seamlessly** — With the circuit-breaker pattern acting as an intermediary, the circuit breaker can periodically check to see if the resource being requested is back on line and re-enable access to it without human intervention.

In a large cloud-based application with hundreds of services, this graceful recovery is critical because it can significantly cut down on the amount of time needed to restore service and significantly lessen the risk of a tired operator or application engineer causing greater problems by having them intervene directly (restarting a failed ser- vice) in the restoration of the service.

## Circuit breaker using Hystrix

We’re going to look at implementing Hystrix in two broad categories. In the first cate- gory, you’re going to wrap all calls to your database in the licensing and organization service with a Hystrix circuit breaker. You’re then going to wrap the inter-service calls between the licensing service and the organization service using Hystrix. While these are two different categories calls, you’ll see that the use of Hystrix will be exactly the same. 

Figure below shows what remote resources you’re going to wrap with a Hystrix circuit breaker.

![resiliency](https://github.com/rgederin/spring-microservices/blob/master/img/resiliency-3.png)

Let’s start our Hystrix discussion by showing how to wrap the retrieval of licensing service data from the licensing database using a synchronous Hystrix circuit breaker. With a synchronous call, the licensing service will retrieve its data but will wait for the SQL statement to complete or for a circuit-breaker time-out before continuing processing.

Hystrix and Spring Cloud use the **@HystrixCommand** annotation to mark Java class methods as being managed by a Hystrix circuit breaker. When the Spring framework sees the @HystrixCommand, it will dynamically generate a proxy that will wrapper the method and manage all calls to that method through a thread pool of threads specifically set aside to handle remote calls.

```
    @HystrixCommand
    public License getLicense(String licenseId) {
        randomlyRunLong();

        return licenseRepository.findById(licenseId).get();
    }
```

This doesn’t look like a lot of code, and it’s not, but there is a lot of functionality inside this one annotation. With the use of the @HystrixCommand annotation, any time the getLicense method is called, the call will be wrapped with a Hys-trix circuit breaker. The circuit breaker will interrupt any call to the getLicenses method any time the call takes longer than 1,000 milliseconds.

The beauty of using method-level annotations for tagging calls with circuit-breaker behavior is that it’s the same annotation whether you’re accessing a database or calling a microservice.

Also we could override default timeout:

```
@HystrixCommand(commandProperties = @HystrixProperty(
            name = "execution.isolation.thread.timeoutInMilliseconds",
            value = "15000"))
    public License getLicenseWithOrganizationInfo(String licenseId, String clientType) {
        randomlyRunLong();

        License license = licenseRepository.findById(licenseId).get();

        Organization organization = retrieveOrgInfo(license.getOrganizationId(), clientType);

        return license
                .withOrganizationName(organization.getName())
                .withContactName(organization.getContactName())
                .withContactEmail(organization.getContactEmail())
                .withContactPhone(organization.getContactPhone());
    }
```

## Fallback processing using Hystrix

Part of the beauty of the circuit breaker pattern is that because a “middle man” is between the consumer of a remote resource and the resource itself, you have an opportunity for the developer to intercept a service failure and choose an alternative course of action to take.

In Hystrix, this is known as a fallback strategy and is easily implemented. Let’s see how to build a simple fallback strategy for your licensing database that simply returns a licensing object that says no licensing information is currently available. The following listing demonstrates this.

```
   @HystrixCommand(fallbackMethod = "buildFallbackLicense")
    public License getLicense(String licenseId) {
        randomlyRunLong();

        return licenseRepository.findById(licenseId).get();
    }
    
   private License buildFallbackLicense(String licenseId) {
        return new License()
                .withId(licenseId)
                .withProductName("Sorry no licensing information currently available");
    }    
```

To implement a fallback strategy with Hystrix you have to do two things. First, you need to add an attribute called fallbackMethod to the @HystrixCommand annota- tion. This attribute will contain the name of a method that will be called when Hystrix has to interrupt a call because it’s taking too long.

The second thing you need to do is define a fallback method to be executed. This fallback method must reside in the same class as the original method that was protected by the @HystrixCommand. The fallback method must have the exact same method signature as the originating function as all of the parameters passed into the original method protected by the @HystrixCommand will be passed to the fallback.

## Bulkhead pattern using Hystrix

In a microservice-based application you’ll often need to call multiple microservices to complete a particular task. Without using a bulkhead pattern, the default behavior for these calls is that the calls are executed using the same threads that are reserved for handling requests for the entire Java container. In high volumes, performance problems with one service out of many can result in all of the threads for the Java container being maxed out and waiting to process work, while new requests for work back up. The Java container will eventually crash. The bulkhead pattern segregates remote resource calls in their own thread pools so that a single misbehaving service can be contained and not crash the container.

Hystrix uses a thread pool to delegate all requests for remote services. By default, all Hystrix commands will share the same thread pool to process requests. This thread pool will have 10 threads in it to process remote service calls and those remote services calls could be anything, including REST-service invocations, database calls, and so on. Figure below illustrates this.

![resiliency](https://github.com/rgederin/spring-microservices/blob/master/img/resiliency-4.png)


This model works fine when you have a small number of remote resources being accessed within an application and the call volumes for the individual services are rel- atively evenly distributed. The problem is if you have services that have far higher volumes or longer completion times then other services, you can end up introducing thread exhaustion into your Hystrix thread pools because one service ends up dominating all of the threads in the default thread pool.

Fortunately, Hystrix provides an easy-to-use mechanism for creating bulkheads between different remote resource calls. Figure 5.8 shows what Hystrix managed resources look like when they’re segregated into their own “bulkheads.”

To implement segregated thread pools, you need to use additional attributes exposed through the @HystrixCommand annotation. Let’s look at some code that will

1. Set up a separate thread pool for the getLicensesByOrg() call
2. Set the number of threads in the thread pool
3. Set the queue size for the number of requests that can queue if the individual threads are busy

![resiliency](https://github.com/rgederin/spring-microservices/blob/master/img/resiliency-5.png)


```
@HystrixCommand(
            threadPoolKey = "licensesByOrgThreadPool",
            threadPoolProperties = {
                    @HystrixProperty(name = "coreSize", value = "30"),
                    @HystrixProperty(name = "maxQueueSize", value = "10")})
    public List<License> getLicensesByOrg(String organizationId) {
        return licenseRepository.findByOrganizationId(organizationId);
    }
```

The first thing you should notice is that we’ve introduced a new attribute, thread- Poolkey, to your @HystrixCommand annotation. This signals to Hystrix that you want to set up a new thread pool. If you set no further values on the thread pool, Hystrix sets up a thread pool keyed off the name in the threadPoolKey attribute, but will use all default values for how the thread pool is configured.

To customize your thread pool, you use the threadPoolProperties attribute on the @HystrixCommand. This attribute takes an array of HystrixProperty objects. These HystrixProperty objects can be used to control the behavior of the thread pool. You can set the size of the thread pool by using the coreSize attribute.

You can also set up a queue in front of the thread pool that will control how many requests will be allowed to back up when the threads in the thread pool are busy. This queue size is set by the maxQueueSize attribute. Once the number of requests exceeds the queue size, any additional requests to the thread pool will fail until there is room in the queue.

Note two things about the maxQueueSize attribute. First, if you set the value to -1, a Java SynchronousQueue will be used to hold all incoming requests. A synchronous queue will essentially enforce that you can never have more requests in process then the number of threads available in the thread pool. Setting the maxQueueSize to a value greater than one will cause Hystrix to use a Java LinkedBlockingQueue. The use of a LinkedBlockingQueue allows the developer to queue up requests even if all threads are busy processing requests.

The second thing to note is that the maxQueueSize attribute can only be set when the thread pool is first initialized (for example, at startup of the application). Hystrix does allow you to dynamically change the size of the queue by using the queue- SizeRejectionThreshold attribute, but this attribute can only be set when the maxQueueSize attribute is a value greater than 0.

## Hystrix dashboard

Hystrix has own dashboard which allow monitoring of hystrix comand excution. 

For this we need to add in pom.xml corresponding dependency:

```
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-netflix-hystrix-dashboard</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-actuator</artifactId>
		</dependency>
```

Than add annotation **@EnableHystrixDashboard** to the application class.

And finally we ned to add aditional setting to the application.yml

```
management:
  endpoints:
    web:
      exposure:
        include: hystrix.stream
```

After this you could go to hystrix dashboard in browser

![resiliency](https://github.com/rgederin/spring-microservices/blob/master/img/resiliency-6.png)

Enter hystrix stream value - *http://localhost:8080/actuator/hystrix.stream* and observe hystrix commands

![resiliency](https://github.com/rgederin/spring-microservices/blob/master/img/resiliency-7.png)

## Summary

* When designing highly distributed applications such as a microservice-based application, client resiliency must be taken into account.
* Outright failures of a service (for example, the server crashes) are easy to detect and deal with.
* A single poorly performing service can trigger a cascading effect of resource exhaustion as threads in the calling client are blocked waiting for a service to complete.
* Three core client resiliency patterns are the circuit-breaker pattern, the fallback pattern, and the bulkhead pattern.
* The circuit breaker pattern seeks to kill slow-running and degraded system calls so that the calls fail fast and prevent resource exhaustion.
* The fallback pattern allows you as the developer to define alternative code paths in the event that a remote service call fails or the circuit breaker for the call fails.
* The bulk head pattern segregates remote resource calls away from each other, isolating calls to a remote service into their own thread pool. If one set of ser- vice calls is failing, its failures shouldn’t be allowed to eat up all the resources in the application container.
* Spring Cloud and the Netflix Hystrix libraries provide implementations for the circuit breaker, fallback, and bulkhead patterns.
* The Hystrix libraries are highly configurable and can be set at global, class, and thread pool levels.
