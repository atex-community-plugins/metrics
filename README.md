Metrics and Health Checks plugin
================================

This plugin provides a metrics and health checks using [http://metrics.dropwizard.io](http://metrics.dropwizard.io).
You can access an admin menu using `/metrics-admin` and the health checks using `/metrics-admin/healthcheck?pretty=true`.

Every time you access to the health check endpoints, the health checks will be executed, keep in mind this when
writing a new health check.

## Polopoly Version
10.16.3-fp3

you can use version `1.1-RELEASE-10.12.0` which is fully compatible with polopoly 10.12 (you'll need to update the
jetty polopoly plugin to version `7.6.16-polopoly-1.0`).

## Servlet Configuration

You will need to modify the `webapp-dispatcher/pom.xml` (or any web application where you want to run the health checks):

```
    <dependency>
      <groupId>com.atex.plugins</groupId>
      <artifactId>metrics-web</artifactId>
      <version>1.2</version>
    </dependency>
```

This will make the servlets available also on fronts (i.e. you can use that page as a varnish health check).

## Custom health checks

You can write your own custom health checks, to let the plugins knows about them, you'll need to
add the annotation:

```
@HealthCheckProvider(name = "myhealthcheck")
``` 

Where `name` is the name of the health check that will appear in the health checks page.
The health check name must be unique, if it is not an exception will be thrown.

The plugin will use a context-param value to know which packages to scan:

```
  <context-param>
    <param-name>com.atex.plugins.metrics.packagescan.xxxx</param-name>
    <param-value>com.project1.metrics,com.project2.metrics</param-value>
  </context-param>
```

The name must be a unique identifier and must start with `com.atex.plugins.metrics.packagescan.`
so you may use your plugin (or project) name instead of `xxxx`.
The value can be a list of packages separated by `,` (or it can be single package).

If you are using [Guice](https://github.com/google/guice) in your project, than the plugin will
try to use Guice to instantiate the class, if no class are available than a standard newInstance
will be done and Guice will be used to inject the members.

The Guice injector will be get from:

```
final Injector injector = (Injector) servletContext.getAttribute(Injector.class.getName());
``` 

If you add a dependency from:

```
    <dependency>
      <groupId>com.atex.plugins</groupId>
      <artifactId>metrics-core</artifactId>
      <version>1.2</version>
    </dependency>
``` 
 
Then you can inherit from `AbstractHealthCheck`, in this case the `init` method will be
called with the `ServletContext` as parameter.

## Health checks configuration

There are three ways to configure the health checks (in the order they are applied):

1. Using System Properties (that start with `com.atex.plugins.metrics.`)
2. Using a context param specific to the polopoly app (i.e. `com.atex.plugins.metrics.appname.`
where `appname` is the application name like preview, front, and so on)
3. Using a context param (i.e. `com.atex.plugins.metrics.`)

Each health check can be disabled using the property:

`com.atex.plugins.metrics.name.disable`

where `name` is the name of the health check as it appears in the health check page.

## Standard Health Checks

### oom

This health check will keep track of imminent out of memory or lack of cpu time.
A separate thread is started which will spin for 500ms and keep track the start time and the end time,
if the sleep is longer than 500ms it means one of:

- a "Stop The World" event
- lacking of cpu resources

We will keep track of how many of those events happens in a defined interval (default is 5 seconds), if a "longer" wait
happens in an interval of 5 seconds before the previous one, we will increase the oom count to 1 otherwise we will lower
it by 1, if we reach 5 consecutive events we will signal an OOM.

You can control these parameters (do not forget to add `com.atex.plugins.metrics.` as prefix):

name|default value|description
----|-------------|-----------
`oom.pause`|500|the thread sleep time
`oom.minLevel`|5000|the interval we will use to count oom

### jvm

This will keep track of java dead locks

### heartbeat

This health check will use the exposed polopoly heartbeat beans to verify that the heartbeat does not have an high latency.
It will try to identify the default heartbeat interval from:

- `PacemakerComponent` if available.
- if using a servlets 3.0 compliant application server we will look for an `ApplicationHeartbeatFilter` filter.
- a default 1 second interval.

You can control these parameters (do not forget to add `com.atex.plugins.metrics.` as prefix):

name|default value|description
----|-------------|-----------
`heartbeat.interval`|1000|the default heartbeat interval

### couchbase

This health check will verify if the couchbase component is "connected".
In case couchbase is not configured, this health check will be considered always healthy.

### solr_public

This health check use the class `SolrSearchHealthCheck` with the `search_solrClientPublic` application module name
to keep track if the solr is "connected".

You can use `SolrSearchHealthCheck` if you want to keep track of the internal index too.

### diskcache

This health check will fetch the cache setting from the polopoly application and get the files cache and the contents
cache.

The check will be considered unhealthy when the diskspace is less then 512Mb or less than 1%.

You can control these parameters (do not forget to add `com.atex.plugins.metrics.` as prefix):

name|default value|description
----|-------------|-----------
`diskcache.minFreeSpace`|512000000|512Mb
`diskcache.minPct`|1.0|1%

## Metrics

This plugin provides a way to create your own metrics.
Those metrics can be read through a servlet or using jmx, when the context is shutdown the values
will be dumped in the console.

## Velocity directive

If you add `com.atex.plugins.metrics.velocity.directives.MetricDirective` to `velocity.properties`, 
a `#metric` directive will be available that you can use like this:

```
 #metric("uniquename")
     ...
     complex vm code
     ...
 #end
```

or

```
 #metric()
     ...
     complex vm code
     ...
 #end
```

> The template name if found will be prepended to the metric name.

## Metrics

There is `com.atex.plugins.metrics.MetricsUtil` which you can use to automatically track some complex code you may
want to provide, to use it you need to provide a `MetricsRegistryProvider` which will be used to access the metrics
registry.

You can check [http://metrics.dropwizard.io](http://metrics.dropwizard.io/3.2.3/manual/core.html) for a detailed
overview of what you may do.

To use the `MetricsUtil` and so on, you need to add a dependency on `metrics-core`:

```
    <dependency>
      <groupId>com.atex.plugins</groupId>
      <artifactId>metrics-core</artifactId>
      <version>1.w</version>
    </dependency>
``` 

## Code Status
The code in this repository is provided with the following status: **PROJECT**

Under the open source initiative, Atex provides source code for plugin with different levels of support.
There are three different levels of support used. These are:

- EXAMPLE  
The code is provided as an illustration of a pattern or blueprint for how to use a specific feature. Code provided as is.

- PROJECT  
The code has been identified in an implementation project to be generic enough to be useful also in other projects.
This means that it has actually been used in production somewhere, but it comes "as is", with no support attached.
The idea is to promote code reuse and to provide a convenient starting point for customization if needed.

- PRODUCT  
The code is provided with full product support, just as the core Polopoly product itself.
If you modify the code (outside of configuration files), the support is voided.


## License
Atex Polopoly Source Code License
Version 1.0 February 2012

See file **LICENSE** for details