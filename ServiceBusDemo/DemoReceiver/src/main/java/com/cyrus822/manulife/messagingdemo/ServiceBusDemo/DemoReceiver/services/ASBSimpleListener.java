package com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoReceiver.services;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;
import com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoReceiver.models.Payment;

@Service
public class ASBSimpleListener {

    @JmsListener(destination = "${topic.simple.name}", containerFactory = "topicJmsListenerContainerFactory", subscription = "simple-subscriiption")
    public void receiveTopicMessage(Payment newPayment) {
        System.out.println(String.format("Received payment from Simple Topic by JMS Template: {%s}", newPayment));
    }

    @JmsListener(destination = "${queue.simple.name}", containerFactory = "jmsListenerContainerFactory")
    public void receiveQueueMessage(Payment newPayment) {
        System.out.println(String.format("Received payment from Simple Queue by JMS Template: {%s}", newPayment));
    }    
}