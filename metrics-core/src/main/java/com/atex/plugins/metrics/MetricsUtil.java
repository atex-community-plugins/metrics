package com.atex.plugins.metrics;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

/**
 * Utility methods for metrics.
 *
 * @author mnova
 */
public abstract class MetricsUtil {

    private static final Logger LOGGER = Logger.getLogger(MetricsUtil.class.getName());

    public static void time(final Timer timer, final MetricPredicate predicate) {
        final Timer.Context timerContext;
        if (timer != null) {
            timerContext = timer.time();
        } else {
            timerContext = null;
        }

        try {
            try {
                predicate.apply();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                throw new RuntimeException(e);
            }
        } finally {
            if (timerContext != null) {
                timerContext.stop();
            }
        }
    }

    public static void time(final MetricsRegistryProvider provider, final String timerName, final MetricPredicate predicate) {
        final MetricRegistry metricRegistry = provider.getMetricRegistry();
        final Timer timer = metricRegistry.timer(timerName);
        time(timer, predicate);
    }

    public interface MetricPredicate {

        void apply() throws Exception;

    }
}
