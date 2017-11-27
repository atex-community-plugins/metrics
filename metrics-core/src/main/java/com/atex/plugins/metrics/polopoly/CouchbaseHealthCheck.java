package com.atex.plugins.metrics.polopoly;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import com.atex.onecms.content.RepositoryClient;
import com.atex.plugins.metrics.AbstractHealthCheck;
import com.atex.plugins.metrics.HealthCheckProvider;
import com.polopoly.application.Application;
import com.polopoly.application.IllegalApplicationStateException;
import com.polopoly.application.servlet.ApplicationServletUtil;

/**
 * CouchbaseHealthCheck
 *
 * @author mnova
 */
@HealthCheckProvider(name = "couchbase")
public class CouchbaseHealthCheck extends AbstractHealthCheck {

    private static final Logger LOGGER = Logger.getLogger(CouchbaseHealthCheck.class.getName());

    private RepositoryClient repositoryClient;

    @Override
    protected void init(final ServletContext servletContext) {
        try {
            final Application application = ApplicationServletUtil.getApplication(servletContext);
            repositoryClient = application.getPreferredApplicationComponent(RepositoryClient.class);
        } catch (IllegalApplicationStateException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    @Override
    protected Result check() throws Exception {
        if (repositoryClient != null) {
            if (!isServiceReady(repositoryClient)) {
                return Result.unhealthy("Couchbase is not ready");
            }
        }
        return Result.healthy();
    }

}
