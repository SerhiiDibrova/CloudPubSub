package com.cloudpubsub.publisher;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

@Getter
@Setter
@ToString
public class MessageModel {

    private String message;
    private Map<String, String> attributeMessages;
}
