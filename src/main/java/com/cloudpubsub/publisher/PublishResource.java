package com.cloudpubsub.publisher;

import com.cloudpubsub.flow.PubsubToBigquery;
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
    public ResponseEntity publishToTopic(@RequestBody MessageModel message){

        try{
            publisher.publishMessage(message);
        } catch (Exception e){
            LOG.error(e.getMessage(), e);
            return new ResponseEntity<>( HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>( HttpStatus.OK);
    }
}
