package com.atex.plugins.metrics.jvm;

import com.atex.plugins.metrics.HealthCheckProvider;
import com.codahale.metrics.health.jvm.ThreadDeadlockHealthCheck;

/**
 * JVMThreadDeadlockHealthCheck
 *
 * @author mnova
 */
@HealthCheckProvider(name = "jvm")
public class JVMThreadDeadlockHealthCheck extends ThreadDeadlockHealthCheck {

}
