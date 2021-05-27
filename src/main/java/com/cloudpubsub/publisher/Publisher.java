package com.cloudpubsub.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Publisher extends PubSubPublisher {

    private static Logger LOG = LoggerFactory.getLogger(Publisher.class);

    @Value("${pubsub.topic}")
    public String topic;

    @Override
    protected String topic() {
        return this.topic;
    }
}
