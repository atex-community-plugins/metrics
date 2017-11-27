package com.atex.plugins.metrics.polopoly;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.RoundingMode;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;

import com.atex.plugins.metrics.AbstractHealthCheck;
import com.atex.plugins.metrics.HealthCheckProvider;
import com.atex.plugins.metrics.Parameters;
import com.google.common.math.DoubleMath;
import com.polopoly.application.Application;
import com.polopoly.application.IllegalApplicationStateException;
import com.polopoly.application.PacemakerComponent;
import com.polopoly.application.PacemakerSettings;
import com.polopoly.application.servlet.ApplicationHeartbeatFilter;
import com.polopoly.application.servlet.ApplicationServletUtil;

/**
 * PolopolyHeartbeatHealthCheck
 *
 * @author mnova
 */
@HealthCheckProvider(name = "heartbeat")
public class PolopolyHeartbeatHealthCheck extends AbstractHealthCheck {

    private static final Logger LOGGER = Logger.getLogger(PolopolyHeartbeatHealthCheck.class.getName());

    private static final String MBEAN_NAME_FMT = "com.polopoly:host=*,application=%s,detailLevel=INFO,name=HeartbeatMonitor";

    private MBeanServer mBeanServer = null;
    private Long interval = null;
    private long counter = 0;

    private ServletContext servletContext;
    private long defaultInterval;

    @Override
    protected void init(final ServletContext servletContext) {
        this.servletContext = checkNotNull(servletContext);
        this.defaultInterval = new Parameters(servletContext)
                .getLong(
                        "heartbeat.interval",
                        TimeUnit.SECONDS.toMillis(1));
    }

    @Override
    protected Result check() throws Exception {
        final String mBeanName = getMBeanName();
        try {
            final MBeanServer mbeanServer = getMBeanServer();
            final ObjectName pat = new ObjectName(mBeanName);
            final Set<ObjectName> names = mbeanServer.queryNames(pat, null);
            final Iterator<ObjectName> iter = names.iterator();
            if (iter.hasNext()) {
                final Long latency = (Long) mbeanServer.getAttribute(iter.next(), "TimeSinceLastHeartbeat");
                final long threshold = getThreshold();
                if ((threshold > 0) && (latency > threshold)) {
                    counter += 1;
                    if (counter >= 5) {
                        return Result.unhealthy("Latency is " + latency + " which is higher than " + threshold);
                    }
                } else {
                    if (counter > 0) {
                        counter -= 1;
                    }
                }
            } else {
                LOGGER.log(Level.SEVERE, "MBean " + mBeanName + " HeartbeatMonitor not found");
            }
            return Result.healthy();
        } catch (JMException e) {
            LOGGER.log(Level.SEVERE, "Could not read " + mBeanName + " TimeSinceLastHeartbeat", e);
            return Result.unhealthy(e);
        }
    }

    private String getMBeanName() {
        final String appName = ApplicationServletUtil.getApplicationName(servletContext);
        return String.format(MBEAN_NAME_FMT, appName);
    }

    private long getThreshold() {
        if (interval == null) {
            interval = getThresholdInternal();
        }
        return interval;
    }

    private long getThresholdInternal() {
        try {
            final Application application = ApplicationServletUtil.getApplication(servletContext);
            final PacemakerComponent pacemaker = application.getPreferredApplicationComponent(PacemakerComponent.class);
            final PacemakerSettings settings = pacemaker.getPacemakerSettings();
            if (settings.isEnabled() && settings.isFixedRate()) {
                return DoubleMath.roundToLong(settings.getInterval(), RoundingMode.HALF_UP);
            }
        } catch (IllegalApplicationStateException e) {
            try {
                final Map<String, ? extends FilterRegistration> registrations = servletContext.getFilterRegistrations();
                for (Entry<String, ? extends FilterRegistration> entry : registrations.entrySet()) {
                    final FilterRegistration registration = entry.getValue();
                    Class<? extends Filter> clazz = null;
                    try {
                        clazz = (Class<? extends Filter>) Class.forName(registration.getClassName());
                        if (ApplicationHeartbeatFilter.class.isAssignableFrom(clazz)) {
                            final String timerTypeString = registration.getInitParameter("heartbeat.scheduleType");
                            if ("fixedRate".equals(timerTypeString)) {
                                final double interval = getTimeConfigValue(
                                        registration,
                                        "heartbeat.interval",
                                        1000L
                                );
                                return DoubleMath.roundToLong(interval, RoundingMode.HALF_UP);
                            }
                            break;
                        }
                    } catch (ClassNotFoundException e1) {
                        LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
                    }
                }
            } catch (Throwable e1) {
                LOGGER.log(Level.WARNING, "Cannot get filter configuration, using default, error: " + e1.getMessage(), e1);
            }

            return defaultInterval;
        }
        return 0;
    }

    private long getTimeConfigValue(final FilterRegistration config,
                                    final String initParameterName,
                                    final long defaultValue) {
        String initParameter = config.getInitParameter(initParameterName);
        if (initParameter == null) {
            return defaultValue;
        } else {
            try {
                return (long)(Double.parseDouble(initParameter) * 1000.0D);
            } catch (NumberFormatException e) {
                LOGGER.warning("Could not read parameter " + initParameterName + ". Not a valid double. Using default value.");
                return defaultValue;
            }
        }
    }

    private MBeanServer getMBeanServer() {
        if (mBeanServer == null) {
            mBeanServer = checkNotNull(ApplicationServletUtil.findMBeanServer());
        }
        return mBeanServer;
    }

}
