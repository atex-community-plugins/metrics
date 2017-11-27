package com.atex.plugins.metrics;

import com.codahale.metrics.MetricRegistry;

/**
 * A {@link MetricRegistry} provider.
 *
 * @author mnova
 */
public interface MetricsRegistryProvider {

    MetricRegistry getMetricRegistry();

}
