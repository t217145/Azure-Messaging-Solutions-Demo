package com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoReceiver.services;

import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoReceiver.models.Payment;
import com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoReceiver.models.SimplePayment;
import com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoReceiver.repos.SimplePaymentRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;

@Service
public class SimpleNativeTopicListener implements CommandLineRunner {
   
    @Value("${spring.jms.servicebus.connection-string}")
    private String connStr; 

    @Value("${topic.simple.name}")
    private String topicName;    

    @Value("${topic.simple.subscription.name}")
    private String subscriptionName;

    @Autowired
    private SimplePaymentRepo repo;

    @Override
    public void run(String... args){
        try{
            Consumer<ServiceBusReceivedMessageContext> processMessage = context -> {
                final ServiceBusReceivedMessage message = context.getMessage();
                try {
                    //Cast the JSON to Payment Object
                    String paymentJson = new String(message.getBody().toBytes());
                    Payment payment = new ObjectMapper().readValue(paymentJson, Payment.class);

                    //decide whether it is an insert or update action
                    SimplePayment simplePayment = new SimplePayment(0, payment.getPolicyNo(), payment.getBankCode(), payment.getCurrency(), payment.getAcctNo(), payment.getAmt());
                    repo.save(simplePayment);
                    
                    System.out.println(String.format("Insert Payment %s", simplePayment));
                } catch (Exception completionError) {
                    context.abandon();
                    System.out.printf("Completion of the message %s failed%n", message.getMessageId());
                    completionError.printStackTrace();
                }
            };
    
            Consumer<ServiceBusErrorContext> processError = errorContext -> {
                System.err.println("Error occurred while receiving message:" + errorContext.getException());
            };
    
            ServiceBusProcessorClient processorClient = new ServiceBusClientBuilder()
                                            .connectionString(connStr)
                                            .processor()
                                            .topicName(topicName)
                                            .subscriptionName(subscriptionName)
                                            .receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
                                            .processMessage(processMessage)
                                            .processError(processError)
                                            .buildProcessorClient();
    
            processorClient.start();       
        } catch(Exception e) {
            System.out.println("Error when creating processor");
        }
    }
}
