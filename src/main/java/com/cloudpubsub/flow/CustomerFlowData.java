package com.cloudpubsub.flow;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class CustomerFlowData implements Serializable {

    private String firstName;
    private String lastName;
    private String country;
    private int age;

    public CustomerFlowData(PubsubMessage message){
        this.firstName = message.getAttribute("first_name");
        this.lastName = message.getAttribute("last_name");
        this.country = message.getAttribute("country");
        this.age = Integer.parseInt(message.getAttribute("age"));
    }

}
