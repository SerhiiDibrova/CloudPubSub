package com.cloudpubsub.lambda;

import com.cloudpubsub.protobuf.OrderMessageOuterClass;
import com.cloudpubsub.publisher.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublishResource {

    private static Logger LOG = LoggerFactory.getLogger(PublishResource.class);

    @Autowired
    private Publisher publisher;

    @PostMapping("api/v1/publish/order")
    public ResponseEntity publishToTopic(@RequestBody OrderModel order) {
        try {
            OrderMessageOuterClass.OrderMessage customerMessage = OrderMessageOuterClass.OrderMessage.newBuilder()
                    .setType(order.type)
                    .setCreated(order.created)
                    .setState(order.state)
                    .setAmount(order.amount)
                    .build();
            publisher.publishMessage(customerMessage);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}
