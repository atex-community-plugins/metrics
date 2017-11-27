package com.atex.plugins.metrics.polopoly;

import com.atex.plugins.metrics.HealthCheckProvider;
import com.polopoly.search.solr.SolrSearchClient;

/**
 * SolrPublicSearchHealthCheck
 *
 * @author mnova
 */
@HealthCheckProvider(name = "solr_public")
public class SolrPublicSearchHealthCheck extends SolrSearchHealthCheck {

    public SolrPublicSearchHealthCheck() {
        super(SolrSearchClient.DEFAULT_COMPOUND_NAME);
    }

}
