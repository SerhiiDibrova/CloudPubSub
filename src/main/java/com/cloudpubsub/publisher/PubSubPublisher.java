package com.cloudpubsub.publisher;

import com.cloudpubsub.avro.model.CustomerMessage;
import com.cloudpubsub.protobuf.CustomerMessageOuterClass;
import com.cloudpubsub.protobuf.OrderMessageOuterClass;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import org.apache.avro.io.EncoderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;

import org.apache.avro.io.Encoder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

    public void publishMessage(OrderMessageOuterClass.OrderMessage message) throws ExecutionException, InterruptedException {
        LOG.info("Sending Message to the topic:::");
        // Create pubsub message
        PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
                .setData(message.toByteString())
                .build();
        publish(pubsubMessage);
    }

    public void publishMessage(CustomerMessage message) throws ExecutionException, InterruptedException, IOException {
        LOG.info("Sending Message to the topic:::");

        LOG.info("Preparing a BINARY encoder...");
        // Prepare an appropriate encoder for publishing to the topic.
        // Encode the object and write it to the output stream.
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(byteArrayOutputStream, null);
        message.customEncode(encoder);
        encoder.flush();

        // Create pubsub message
        PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
                .setData(ByteString.copyFrom(byteArrayOutputStream.toByteArray()))
                .build();
        publish(pubsubMessage);
    }

    private void publish(PubsubMessage pubsubMessage) throws ExecutionException, InterruptedException {
        LOG.info("Publishing to the topic [{}], message [{}]", topic(), pubsubMessage);
        pubSubTemplate.publish(topic(), pubsubMessage).get();
    }
}
