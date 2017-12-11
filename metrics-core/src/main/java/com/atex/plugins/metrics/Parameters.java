package com.atex.plugins.metrics;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import com.polopoly.application.servlet.ApplicationNameNotFoundException;
import com.polopoly.application.servlet.ApplicationServletUtil;

/**
 * Parameters
 *
 * @author mnova
 */
public class Parameters {

    private static final Logger LOGGER = Logger.getLogger(Parameters.class.getName());

    private final ServletContext servletContext;

    public Parameters(final ServletContext servletContext) {
        this.servletContext = checkNotNull(servletContext);
    }

    public double getDouble(final String name,
                            final double defaultValue) {
        final String value = getString(name, Double.toString(defaultValue));
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public long getLong(final String name,
                        final long defaultValue) {
        final String value = getString(name, Long.toString(defaultValue));
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(final String name,
                              final boolean defaultValue) {
        final String value = getString(name, Boolean.toString(defaultValue));
        return Boolean.parseBoolean(value);
    }

    public String getString(final String name,
                            final String defaultValue) {

        Object value = System.getProperty("com.atex.plugins.metrics." + name);
        if (value == null) {
            try {
                final String appName = ApplicationServletUtil.getApplicationName(servletContext);
                value = servletContext.getInitParameter("com.atex.plugins.metrics." + appName + "." + name);
            } catch (ApplicationNameNotFoundException e) {
                LOGGER.log(Level.FINE, "cannot get application name from context [" + servletContext.getContextPath() + "]: " + e.getMessage());
            }
            if (value == null) {
                value = servletContext.getInitParameter("com.atex.plugins.metrics." + name);
                if (value != null) {
                    LOGGER.config("getString [" + name + "] from initSelf parameter: " + value);
                }
            } else {
                LOGGER.config("getString [" + name + "] from app initSelf parameter: " + value);
            }
        } else {
            LOGGER.config("getString [" + name + "] from property: " + value);
        }
        if (value != null) {
            return Objects.toString(value);
        } else {
            return defaultValue;
        }

    }

}
