package com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoReceiver.services;

import java.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;
import com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoReceiver.models.Payment;
import com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoReceiver.models.PaymentWithSession;
import com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoReceiver.repos.PaymentWithSessionRepo;

@Service
public class SessionListener {
    
    @Autowired
    private PaymentWithSessionRepo repo;

    /* Without Session Control */
    @JmsListener(destination = "${queue.nosession.name}", containerFactory = "jmsListenerContainerFactory")
    public void receiveTopicMessage1(Payment payment) {
        String processor = "p1";
        processPayment(payment, processor);
        try{
            Thread.sleep(new Random(System.currentTimeMillis()).nextInt(800));
        } catch(Exception e) {
            //do nth
        }        
    }

    @JmsListener(destination = "${queue.nosession.name}", containerFactory = "jmsListenerContainerFactory")
    public void receiveTopicMessage2(Payment payment) {
        String processor = "p2";
        processPayment(payment, processor);
        try{
            Thread.sleep(new Random(System.currentTimeMillis()).nextInt(800));
        } catch(Exception e) {
            //do nth
        }         
    }
    
    @JmsListener(destination = "${queue.nosession.name}", containerFactory = "jmsListenerContainerFactory")
    public void receiveTopicMessage3(Payment payment) {
        String processor = "p3";
        processPayment(payment, processor);
        try{
            Thread.sleep(new Random(System.currentTimeMillis()).nextInt(800));
        } catch(Exception e) {
            //do nth
        }         
    }
    
    @JmsListener(destination = "${queue.nosession.name}", containerFactory = "jmsListenerContainerFactory")
    public void receiveTopicMessage4(Payment payment) {
        String processor = "p4";
        processPayment(payment, processor); 
        try{
            Thread.sleep(new Random(System.currentTimeMillis()).nextInt(800));
        } catch(Exception e) {
            //do nth
        }               
    }
    
    @JmsListener(destination = "${queue.nosession.name}", containerFactory = "jmsListenerContainerFactory")
    public void receiveTopicMessage5(Payment payment) {
        String processor = "p5";
        processPayment(payment, processor);
        try{
            Thread.sleep(new Random(System.currentTimeMillis()).nextInt(800));
        } catch(Exception e) {
            //do nth
        }        
    }

    //https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/servicebus/azure-messaging-servicebus/src/samples/java/com/azure/messaging/servicebus/ReceiveNamedSessionAsyncSample.java

    /* Util */
    private void processPayment(Payment payment, String processorName){
        PaymentWithSession newPayment = new PaymentWithSession();
        newPayment.setAcctNo(payment.getAcctNo());
        newPayment.setAmt(payment.getAmt());
        newPayment.setBankCode(payment.getBankCode());
        newPayment.setCurrency(payment.getCurrency());
        newPayment.setPolicyNo(payment.getPolicyNo());
        
        System.out.println(String.format("Received payment from Non-Session Queue by processor [%s]: {%s}", processorName, newPayment));
        repo.save(newPayment);  
    }

}