package com.cloudpubsub.consumer;


import com.cloudpubsub.flow.PubsubToBigquery;
import com.cloudpubsub.protobuf.CustomerMessageOuterClass;
import com.google.pubsub.v1.PubsubMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.support.BasicAcknowledgeablePubsubMessage;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class Consumer extends PubSubConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(Consumer.class);

    @Autowired
    private PubSubTemplate pubSubTemplate;

    @Autowired
    private PubsubToBigquery pubsubToBigquery;

    @Value("${pubsub.subscription}")
    private String subscription;

    @Override
    public String subscription() {
        return this.subscription;
    }

    @Override
    protected void consume(BasicAcknowledgeablePubsubMessage basicAcknowledgeablePubsubMessage) {

        PubsubMessage message = basicAcknowledgeablePubsubMessage.getPubsubMessage();

        try {
//            CustomerMessageOuterClass.CustomerMessage customerMessage = CustomerMessageOuterClass.CustomerMessage.parseFrom(message.getData());
//            LOG.info("message customer : " + customerMessage);
            LOG.info("message: " + message.getData().toStringUtf8());
            LOG.info("Attributes: " + message.getAttributesMap());
        } catch (Exception ex) {
            LOG.error("Error Occured while receiving pubsub message:::::", ex);
        }
        basicAcknowledgeablePubsubMessage.ack();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void subscribe() {
        LOG.info("Subscribing {} to {} ", this.getClass().getSimpleName(), this.subscription());
        pubSubTemplate.subscribe(this.subscription(), this.consumer());
    }
}