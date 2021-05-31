package com.cloudpubsub.publisher;

import com.cloudpubsub.avro.model.CustomerMessage;
import com.cloudpubsub.flow.CustomerFlowData;
import com.cloudpubsub.flow.PubsubToBigquery;
import com.cloudpubsub.protobuf.CustomerMessageOuterClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PublishResource {

    private static Logger LOG = LoggerFactory.getLogger(PublishResource.class);

    @Autowired
    private Publisher publisher;

    @PostMapping("api/v1/publish")
    public ResponseEntity publishToTopic(@RequestBody MessageModel message) {

        try {
            publisher.publishMessage(message);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("api/v2/publish")
    public ResponseEntity publishProtoBuffToTopic(@RequestBody MessageModel message) {

        try {
            String firstName = message.getAttributeMessages().get("first_name");
            String lastName = message.getAttributeMessages().get("last_name");
            String country = message.getAttributeMessages().get("country");
            int age = Integer.parseInt(message.getAttributeMessages().get("age"));
            CustomerMessageOuterClass.CustomerMessage customerMessage = CustomerMessageOuterClass.CustomerMessage.newBuilder()
                    .setFirstName(firstName)
                    .setLastName(lastName)
                    .setCountry(country)
                    .setAge(age)
                    .build();
            publisher.publishMessage(customerMessage);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("api/v3/publish")
    public ResponseEntity publishAvroToTopic(@RequestBody MessageModel message) {

        try {
            String firstName = message.getAttributeMessages().get("first_name");
            String lastName = message.getAttributeMessages().get("last_name");
            String country = message.getAttributeMessages().get("country");
            int age = Integer.parseInt(message.getAttributeMessages().get("age"));
            CustomerMessage customerMessage = CustomerMessage
                    .newBuilder()
                    .setCountry(country)
                    .setAge(age)
                    .setFirstName(firstName)
                    .setLastName(lastName).build();

            publisher.publishMessage(customerMessage);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
