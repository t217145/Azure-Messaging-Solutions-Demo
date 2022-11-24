package com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoReceiver.services;

import java.util.Map;
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
import com.azure.messaging.servicebus.models.DeadLetterOptions;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;

@Service
public class PeekLockReceiver implements CommandLineRunner {
    
    @Value("${spring.jms.servicebus.connection-string}")
    private String connStr; 

    @Value("${topic.simple.name}")
    private String topicName;    

    @Autowired
    private SimplePaymentRepo repo;

    @Override
    public void run(String... args){
        try{
            Consumer<ServiceBusReceivedMessageContext> processMessage = context -> {
                final ServiceBusReceivedMessage message = context.getMessage();
                int waitFor = 0;
                try{
                    Map<String, Object> maps = message.getApplicationProperties();
                    waitFor = (int)maps.get("waitFor");
                    System.out.printf("%n%n%nWait for %d seconds %n%n%n", waitFor);   
                } catch(Exception e) {
                    //do nth
                    System.out.printf("%n%n%nNo wait for %n%n%n");   
                }

                try {
                    //Cast the JSON to Payment Object
                    Payment payment = null;
                    String paymentJson = new String(message.getBody().toBytes());
                    try{
                        payment = new ObjectMapper().readValue(paymentJson, Payment.class);
                    } catch(Exception e) {
                        //if cast fail, it is useless to retry this message, should put to deadletter queue
                        DeadLetterOptions options = new DeadLetterOptions();
                        options.setDeadLetterReason("Format invalid");
                        options.setDeadLetterErrorDescription(e.getMessage());
                        context.deadLetter(options);
                        System.out.printf("%n%n%nFailed to process the message %s %n%n%n", paymentJson);                  
                    }

                    if(payment != null){
                        //decide whether it is an insert or update action
                        SimplePayment simplePayment = new SimplePayment(0, payment.getPolicyNo(), payment.getBankCode(), payment.getCurrency(), payment.getAcctNo(), payment.getAmt());
                        repo.save(simplePayment);
                        
                        //intentionally pause the process to make the TTL timeout
                        try{
                            Thread.sleep(waitFor * 1000l);
                        } catch(Exception e) {
                            //do nth
                        }
                        context.complete();
                        System.out.printf("Completion of the message %s %n", message.getMessageId());
                    }
                } catch (Exception completionError) {
                    context.abandon();
                    System.out.printf("Failed to process the message %s%n", message.getMessageId());
                    completionError.printStackTrace();
                }
            };
    
            Consumer<ServiceBusErrorContext> processError = errorContext -> System.err.println("Error occurred while receiving message:" + errorContext.getException());
    
            ServiceBusProcessorClient processorClient = new ServiceBusClientBuilder()
                                            .connectionString(connStr)
                                            .processor()
                                            .topicName(topicName)
                                            .subscriptionName("lock-subscription")
                                            .receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
                                            .disableAutoComplete()
                                            .processMessage(processMessage)
                                            .processError(processError)
                                            .disableAutoComplete()
                                            .buildProcessorClient();
    
            processorClient.start();
        } catch (Exception e) {
            //do nth
        }
    }
}
