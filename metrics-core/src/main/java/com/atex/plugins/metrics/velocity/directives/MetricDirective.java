package com.atex.plugins.metrics.velocity.directives;

import static com.codahale.metrics.MetricRegistry.name;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletContext;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.TemplateInitException;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.ASTBlock;
import org.apache.velocity.runtime.parser.node.Node;
import org.apache.velocity.tools.view.context.ViewContext;

import com.atex.plugins.metrics.MetricsUtil;
import com.atex.plugins.metrics.MetricsUtil.MetricPredicate;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.servlets.MetricsServlet;

/**
 * A #metric directive, you can use like this:
 *
 * <pre>
 *   #metric("uniquename")
 *      ...
 *      complex vm code
 *      ...
 *   #end
 * </pre>
 *
 * or
 *
 * <pre>
 *   #metric()
 *      ...
 *      complex vm code
 *      ...
 *   #end
 * </pre>
 *
 * The template name if found will be prepended to the metric name.
 *
 * @author mnova
 */
public class MetricDirective extends Directive {

    private String templateName;

    @Override
    public String getName() {
        return "metric";
    }

    @Override
    public int getType() {
        return BLOCK;
    }

    @Override
    public void init(final RuntimeServices rs, final InternalContextAdapter context, final Node node)
            throws TemplateInitException {

        super.init(rs, context, node);

        templateName = ((VelocityContext) context.getInternalUserContext()).getCurrentTemplateName();
        if (templateName != null) {
            if (templateName.startsWith("/")) {
                templateName = templateName.substring(1);
            }
            templateName = templateName.replace('/', '.');
        }
    }

    @Override
    public boolean render(final InternalContextAdapter context, final Writer writer, final Node node)
            throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException {

        final MetricRegistry metrics;
        final ServletContext servletContext = ((ViewContext)context.getInternalUserContext()).getServletContext();
        if (servletContext != null) {
            metrics = (MetricRegistry) servletContext.getAttribute(MetricsServlet.METRICS_REGISTRY);
        } else {
            metrics = null;
        }

        String metricName = (templateName != null ? null : "unnamed");

        // loop through all "params"
        for (int idx = 0; idx < node.jjtGetNumChildren(); idx++) {
            if (node.jjtGetChild(idx) != null ) {
                if (!(node.jjtGetChild(idx) instanceof ASTBlock)) {
                    // reading and casting inline parameters
                    if (idx == 0) {
                        metricName = String.valueOf(node.jjtGetChild(idx).value(context));
                    }
                } else {
                    Timer timer = null;

                    if (metrics != null) {
                        if (templateName != null) {
                            if (metricName != null) {
                                metricName = name(templateName, metricName);
                            } else {
                                metricName = templateName;
                            }
                        } else {
                            metricName = name(MetricDirective.class, metricName);
                        }
                        timer = metrics.timer(metricName);
                    }

                    // reading block content and rendering it

                    final int nodeIdx = idx;

                    MetricsUtil.time(timer, new MetricPredicate() {
                        @Override
                        public void apply() throws Exception {
                            node.jjtGetChild(nodeIdx).render(context, writer);
                        }
                    });

                }
            }
        }

        return true;
    }
}
