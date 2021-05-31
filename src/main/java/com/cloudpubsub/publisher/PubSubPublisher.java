package com.cloudpubsub.publisher;

import com.cloudpubsub.protobuf.CustomerMessageOuterClass;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

public abstract class PubSubPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(PubSubPublisher.class);

    @Autowired
    private PubSubTemplate pubSubTemplate;

    protected abstract String topic();

    public void publishMessage(MessageModel message) throws ExecutionException, InterruptedException {
        LOG.info("Sending Message to the topic:::");
        String messageId = "messageId " + UUID.randomUUID();
        PubsubMessage.Builder pubsubMessage = PubsubMessage.newBuilder()
                .setData(ByteString.copyFromUtf8(message.getMessage()))
                .setMessageId(messageId);

        if (message.getAttributeMessages() != null && !message.getAttributeMessages().isEmpty()) {
            pubsubMessage.putAllAttributes(message.getAttributeMessages());
        }

        publish(pubsubMessage.build());
    }

    public void publishMessage(CustomerMessageOuterClass.CustomerMessage message) throws ExecutionException, InterruptedException {
        LOG.info("Sending Message to the topic:::");
        // Create pubsub message
        PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
                .setData(message.toByteString())
                .build();
        publish(pubsubMessage);
    }

    private void publish(PubsubMessage pubsubMessage) throws ExecutionException, InterruptedException {
        LOG.info("Publishing to the topic [{}], message [{}]", topic(), pubsubMessage);
        pubSubTemplate.publish(topic(), pubsubMessage).get();
    }
}
