package com.cloudpubsub.lambda.service;

import com.google.cloud.bigquery.BigQuery;

public class BigQueryService {

    protected final BigQuery bigQuery;

    public BigQueryService(BigQuery bigQuery) {
        this.bigQuery = bigQuery;
    }

}
