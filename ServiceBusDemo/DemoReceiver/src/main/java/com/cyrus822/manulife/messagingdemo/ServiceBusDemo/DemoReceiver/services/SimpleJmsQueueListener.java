package com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoReceiver.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;
import com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoReceiver.models.Payment;
import com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoReceiver.repos.SimplePaymentRepo;
import com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoReceiver.models.SimplePayment;

@Service
public class SimpleJmsQueueListener {
    @Autowired
    private SimplePaymentRepo repo;

    //topicJmsListenerContainerFactory for topic
    @JmsListener(destination = "${queue.simple.name}", containerFactory = "jmsListenerContainerFactory")
    public void receiveQueueMessage(Payment newPayment) {
        System.out.println(String.format("Received payment from Simple Queue by JMS Template: {%s}", newPayment));
        //Save to DB
        SimplePayment payment = new SimplePayment(0, newPayment.getPolicyNo(), newPayment.getBankCode(), newPayment.getCurrency(), newPayment.getAcctNo(), newPayment.getAmt());
        repo.save(payment);
    }
}
