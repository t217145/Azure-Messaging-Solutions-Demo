package com.cyrus822.manulife.messagingdemo.ServiceBusTopicDemo.services;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

@Service
public class ASBTopicListener {

    private static final String TOPICNAME = "topic";

    @JmsListener(destination = TOPICNAME, containerFactory = "topicJmsListenerContainerFactory", subscription = "demo-sub-with-session")
    public void receiveMessage(byte[] newPaymentB) {
        String newPayment= new String(newPaymentB);
        System.out.println(String.format("Received payment: {%s}", newPayment));
    }      
}