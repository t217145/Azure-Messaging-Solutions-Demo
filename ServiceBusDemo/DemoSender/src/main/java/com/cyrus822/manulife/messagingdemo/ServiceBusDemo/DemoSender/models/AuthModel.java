package com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoSender.models;

import java.io.Serializable;
import lombok.Data;

@Data
public class AuthModel implements Serializable {
    private String clientId;
    private String clientSecret;
    private String tenantId;
    private String fullNameSpace;
    private String queueName;
}
