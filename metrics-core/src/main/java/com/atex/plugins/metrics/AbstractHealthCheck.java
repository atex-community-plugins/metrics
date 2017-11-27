package com.atex.plugins.metrics;

import javax.servlet.ServletContext;

import com.codahale.metrics.health.HealthCheck;
import com.polopoly.application.ApplicationComponent;
import com.polopoly.management.ServiceControl;

/**
 * AbstractHealthCheck
 *
 * @author mnova
 */
public abstract class AbstractHealthCheck extends HealthCheck {

    protected abstract void init(final ServletContext servletContext);

    protected boolean isServiceReady(final ApplicationComponent component) {
        return (component != null) && isServiceReady(component.getServiceControl());
    }

    protected boolean isServiceReady(final ServiceControl service) {
        return (service != null) && service.isConnected() && service.isServing();
    }

}
