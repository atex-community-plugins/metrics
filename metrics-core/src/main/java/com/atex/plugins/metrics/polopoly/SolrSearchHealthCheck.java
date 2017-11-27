package com.atex.plugins.metrics.polopoly;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.servlet.ServletContext;

import com.atex.plugins.metrics.AbstractHealthCheck;
import com.polopoly.application.Application;
import com.polopoly.application.ApplicationComponent;
import com.polopoly.application.servlet.ApplicationServletUtil;
import com.polopoly.search.solr.SearchClient;

/**
 * SolrSearchHealthCheck
 *
 * @author mnova
 */
public class SolrSearchHealthCheck extends AbstractHealthCheck {

    private SearchClient solrSearchClient = null;
    private final String solrCompoundName;

    public SolrSearchHealthCheck(final String solrCompoundName) {
        this.solrCompoundName = checkNotNull(solrCompoundName);
    }

    @Override
    protected void init(final ServletContext servletContext) {
        final Application application = ApplicationServletUtil.getApplication(servletContext);
        solrSearchClient = (SearchClient) application.getApplicationComponent(solrCompoundName);
    }

    @Override
    protected Result check() throws Exception {
        final SearchClient solrSearchClient = getSolrSearchClient();
        if (!isServiceReady((ApplicationComponent) solrSearchClient)) {
            return Result.unhealthy("Solr " + solrCompoundName + " is not ready");
        }
        return Result.healthy();
    }

    private SearchClient getSolrSearchClient() {
        return solrSearchClient;
    }

}
