package com.atex.plugins.metrics;

import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.reflections.Reflections;

import com.atex.plugins.metrics.gc.GCHealthCheck;
import com.atex.plugins.metrics.gc.StopTheWorldChecker;
import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.servlets.HealthCheckServlet;
import com.codahale.metrics.servlets.MetricsServlet;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.inject.Injector;
import com.polopoly.application.servlet.ApplicationNameNotFoundException;


/**
 * MetricsContextListener
 *
 * @author mnova
 */
public class MetricsContextListener implements ServletContextListener {

    private static final Logger LOGGER = Logger.getLogger(MetricsContextListener.class.getName());

    private static final AtomicInteger STW_COUNTER = new AtomicInteger(0);
    private static StopTheWorldChecker STW_THREAD = null;

    @Override
    public void contextInitialized(final ServletContextEvent contextEvent) {
        final ServletContext servletContext = contextEvent.getServletContext();
        final MetricRegistry metrics;
        if (servletContext.getAttribute(MetricsServlet.METRICS_REGISTRY) == null) {

            metrics = new MetricRegistry();

            final JmxReporter reporter = JmxReporter.forRegistry(metrics).build();
            reporter.start();

            servletContext.setAttribute(MetricsServlet.METRICS_REGISTRY, metrics);
        } else {
            metrics = (MetricRegistry) servletContext.getAttribute(MetricsServlet.METRICS_REGISTRY);
        }

        final HealthCheckRegistry healthCheckRegistry;
        if (servletContext.getAttribute(HealthCheckServlet.HEALTH_CHECK_REGISTRY) == null) {
            healthCheckRegistry = new HealthCheckRegistry();
            servletContext.setAttribute(HealthCheckServlet.HEALTH_CHECK_REGISTRY, healthCheckRegistry);
        } else {
            healthCheckRegistry = (HealthCheckRegistry) servletContext.getAttribute(HealthCheckServlet.HEALTH_CHECK_REGISTRY);
        }

        final Parameters params = new Parameters(servletContext);

        if (!params.getBoolean("oom.disable", false)) {
            if (STW_COUNTER.incrementAndGet() == 1) {
                STW_THREAD = new StopTheWorldChecker(
                        params.getLong(
                                "oom.pause",
                                TimeUnit.MILLISECONDS.toMillis(500)),
                        params.getLong(
                                "oom.minLevel",
                                TimeUnit.SECONDS.toMillis(5))
                );
                LOGGER.info("Starting " + STW_THREAD.getClass().getName()  + ": " + STW_THREAD.getId());
                STW_THREAD.start();
            }

            healthCheckRegistry.register("oom", new GCHealthCheck(STW_THREAD));
        }

        discoverHealthChecks(servletContext, healthCheckRegistry);

        /*
        final Collection<Object> leak = new ArrayList<>();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    leak.add(new byte[1024 * 1024]);

                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        break;
                    }

                }
            }
        }).start();
        */
    }

    @Override
    public void contextDestroyed(final ServletContextEvent contextEvent) {

        if (STW_COUNTER.decrementAndGet() == 0 && (STW_THREAD != null)) {
            LOGGER.info("Stopping " + STW_THREAD.getClass().getName()  + ": " + STW_THREAD.getId());
            try {
                STW_THREAD.stopRunning();
                STW_THREAD.interrupt();
                STW_THREAD.join(TimeUnit.SECONDS.toMillis(30));
            } catch (InterruptedException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        final ServletContext servletContext = contextEvent.getServletContext();
        final MetricRegistry metrics = (MetricRegistry) servletContext.getAttribute(MetricsServlet.METRICS_REGISTRY);
        if (metrics != null) {
            final ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
                                                            .convertRatesTo(TimeUnit.SECONDS)
                                                            .convertDurationsTo(TimeUnit.MILLISECONDS)
                                                            .build();
            reporter.report();
        }

    }

    private void discoverHealthChecks(final ServletContext servletContext,
                                      final HealthCheckRegistry registry) {
        final List<String> packages = Lists.newArrayList();
        final Enumeration<String> parameterNames = servletContext.getInitParameterNames();
        while (parameterNames.hasMoreElements()) {
            final String name = parameterNames.nextElement();
            if (name.startsWith("com.atex.plugins.metrics.packagescan.")) {
                final String value = servletContext.getInitParameter(name);
                for (final String p : Splitter.on(",").omitEmptyStrings().split(value)) {
                    if (!packages.contains(p)) {
                        packages.add(p);
                    }
                }
            }
        }

        final Injector injector = (Injector) servletContext.getAttribute(Injector.class.getName());

        final Parameters params = new Parameters(servletContext);

        for (final String p : packages) {
            try {
                LOGGER.info("Scanning of package " + p + " started");
                Reflections reflections = new Reflections(p);
                Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(HealthCheckProvider.class);
                for (final Class<?> k : annotated) {
                    final HealthCheckProvider annotation = k.getAnnotation(HealthCheckProvider.class);
                    final String name = annotation.name();

                    LOGGER.info("found " + name + " on " + k.getName());

                    if (registry.getNames().contains(name)) {
                        final String msg = "An healthcheck with name '" + name + "' has already been registered," +
                                "please change class " + k.getName();
                        LOGGER.log(Level.SEVERE, msg);
                        throw new Error(msg);
                    }
                    if (!params.getBoolean(name + ".disable", false)) {
                        HealthCheck check = null;
                        try {
                            if (injector != null) {
                                try {
                                    check = (HealthCheck) injector.getInstance(k);
                                } catch (Throwable e) {
                                    LOGGER.log(Level.WARNING, "cannot create class " + k.getName() + " using guice: " + e.getMessage());
                                }
                            }
                            if (check == null) {
                                check = (HealthCheck) k.newInstance();
                                if (injector != null) {
                                    injector.injectMembers(check);
                                }
                            }
                        } catch (Throwable e) {
                            LOGGER.log(Level.WARNING, "cannot create class " + k.getName() + " using guice: " + e.getMessage());
                        }
                        if (check != null) {
                            try {
                                if (check instanceof AbstractHealthCheck) {
                                    ((AbstractHealthCheck) check).init(servletContext);
                                }
                                registry.register(name, check);
                            } catch (ApplicationNameNotFoundException e) {
                                LOGGER.log(Level.SEVERE, "cannot initialize " + k.getName() + ": " + e.getMessage());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Cannot scan " + p + ": " + e.getMessage(), e);
            } finally {
                LOGGER.info("Scanning of package " + p + " terminated");
            }
        }
    }

}
