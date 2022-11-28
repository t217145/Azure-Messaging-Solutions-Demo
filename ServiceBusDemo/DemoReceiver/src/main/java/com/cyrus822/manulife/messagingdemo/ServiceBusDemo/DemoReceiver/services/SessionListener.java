package com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoReceiver.services;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoReceiver.models.Payment;
import com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoReceiver.models.PaymentWithSession;
import com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoReceiver.models.PaymentWithoutSession;
import com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoReceiver.repos.PaymentWithSessionRepo;
import com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoReceiver.repos.PaymentWithoutSessionRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient;
import com.azure.messaging.servicebus.ServiceBusSessionReceiverAsyncClient;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;

@Service
public class SessionListener implements CommandLineRunner {

    private static final int NUMBER_OF_SESSION_GROUP = 5;
    
    @Value("${spring.jms.servicebus.connection-string}")
    private String connStr; 

    @Value("${queue.nosession.name}")
    private String noSessionQueue;

    @Value("${queue.session.name}")
    private String sessionQueue;    

    @Autowired
    private PaymentWithoutSessionRepo noSessionRepo;

    @Autowired
    private PaymentWithSessionRepo sessionRepo;    

    @Override
    public void run(String... args){
        try{
            for(int i=1; i<=NUMBER_OF_SESSION_GROUP; i++){
                /* Without Session Control */
                prepareNonSessionProcessor(String.format("non-session-processor-%d", i), noSessionQueue);

                /* With Session Control */
                prepareSessionProcessor(String.format("session-processor-%d", i), i, sessionQueue);
            }         
        } catch(Exception e) {
            System.out.println("Error when creating processor");
        }
    }

    /* Util */
    private void prepareNonSessionProcessor(String processorName, String queueName){
        Consumer<ServiceBusReceivedMessageContext> processMessage = context -> {
            final ServiceBusReceivedMessage message = context.getMessage();
            try {
                //Cast the JSON to Payment Object
                String paymentJson = new String(message.getBody().toBytes());
                Payment payment = new ObjectMapper().readValue(paymentJson, Payment.class);
                //decide whether it is an insert or update action
                if(isInsert(message)){
                    insertPaymentWithoutSession(payment, processorName);
                } else {
                    updateWithoutSession(payment, processorName);
                }
                //intentionally pause the process randomly
                pauseRandom();
            } catch (Exception completionError) {
                context.abandon();
                System.out.printf("Completion of the message %s failed%n", message.getMessageId());
                completionError.printStackTrace();
            }
        };

        Consumer<ServiceBusErrorContext> processError = errorContext -> System.err.println("Error occurred while receiving message:" + errorContext.getException());

        ServiceBusProcessorClient processorClient = new ServiceBusClientBuilder()
                                        .connectionString(connStr)
                                        .processor()
                                        .queueName(queueName)
                                        .receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
                                        .processMessage(processMessage)
                                        .processError(processError)
                                        .buildProcessorClient();

        processorClient.start();
    }

    private void prepareSessionProcessor(String processorName, int id, String queueName){
        AtomicBoolean sampleSuccessful = new AtomicBoolean(true);
        CountDownLatch countdownLatch = new CountDownLatch(1);
        String ctxName = "ctx-" + (id % NUMBER_OF_SESSION_GROUP);
        try{
            // Create a receiver.
            ServiceBusSessionReceiverAsyncClient sessionReceiver = new ServiceBusClientBuilder()
                .connectionString(connStr)
                .sessionReceiver()
                .receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
                .queueName(queueName)
                .buildAsyncClient();

            Mono<ServiceBusReceiverAsyncClient> receiverMono = sessionReceiver.acceptSession(ctxName);

            Flux.usingWhen(receiverMono,
                receiver -> {
                    receiver.setSessionState("new".getBytes(StandardCharsets.UTF_8));
                    return receiver.receiveMessages();
                },
                receiver -> Mono.fromRunnable(receiver::close))
                .subscribe(message -> {
                    try{
                        //decide whether it is an insert or update action
                        String paymentJson = new String(message.getBody().toBytes());
                        Payment payment = new ObjectMapper().readValue(paymentJson, Payment.class);
                        //decide whether it is an insert or update action
                        if(isInsert(message)){
                            insertPaymentWithSession(payment, processorName);
                        } else {
                            updateWithSession(payment, processorName);
                        }                        
                        //intentionally pause the process randomly
                        pauseRandom();
                    }catch(Exception e){
                        //do nth
                    }                        
                }, error -> {
                    System.err.println("Error occurred: " + error);
                    sampleSuccessful.set(false);
                });
            
            countdownLatch.await(2, TimeUnit.SECONDS);         
        }catch(Exception e){
            //do nth
        }
    }    

    private void pauseRandom(){
        try{
            Thread.sleep(new Random(System.currentTimeMillis()).nextInt(1500));
        } catch(Exception e) {
            //do nth
        }      
    }

    //Transform and Save to DB
    private void insertPaymentWithSession(Payment payment, String processorName){
        PaymentWithSession newPayment = new PaymentWithSession();
        newPayment.setAcctNo(payment.getAcctNo());
        newPayment.setAmt(payment.getAmt());
        newPayment.setBankCode(payment.getBankCode());
        newPayment.setCurrency(payment.getCurrency());
        newPayment.setPolicyNo(payment.getPolicyNo());
        newPayment.setProcessorName(processorName);
        
        System.out.println(String.format("Insert Payment with session processed by processor [%s]: {%s}", processorName, newPayment));
        sessionRepo.save(newPayment);  
    }

    private void updateWithSession(Payment payment, String processorName){
        PaymentWithSession dbPayment = sessionRepo.findByPolicyNo(payment.getPolicyNo());
        if(dbPayment != null){
            dbPayment.setAcctNo(payment.getAcctNo());
            dbPayment.setProcessorName(processorName);
            sessionRepo.save(dbPayment);
            System.out.println(String.format("Update Payment with session processed by processor [%s]: {%s}", processorName, payment));
        } else {
            System.err.printf("Update Payment request with policy number {%d} not found!%n", payment.getPolicyNo());
        }
    }

    //Transform and Save to DB
    private void insertPaymentWithoutSession(Payment payment, String processorName){
        PaymentWithoutSession newPayment = new PaymentWithoutSession();
        newPayment.setAcctNo(payment.getAcctNo());
        newPayment.setAmt(payment.getAmt());
        newPayment.setBankCode(payment.getBankCode());
        newPayment.setCurrency(payment.getCurrency());
        newPayment.setPolicyNo(payment.getPolicyNo());
        newPayment.setProcessorName(processorName);
        
        System.out.println(String.format("Insert Payment without session processed by processor [%s]: {%s}", processorName, newPayment));
        noSessionRepo.save(newPayment);  
    }    
    
    private void updateWithoutSession(Payment payment, String processorName){
        PaymentWithSession dbPayment = sessionRepo.findByPolicyNo(payment.getPolicyNo());
        if(dbPayment != null){
            dbPayment.setAcctNo(payment.getAcctNo());
            dbPayment.setProcessorName(processorName);
            sessionRepo.save(dbPayment);
            System.out.println(String.format("Update Payment without session processed by processor [%s]: {%s}", processorName, payment));
        } else {
            System.err.printf("Update Payment request with policy number {%d} not found!%n", payment.getPolicyNo());
        }
    }        

    private boolean isInsert(ServiceBusReceivedMessage msg){
        Map<String, Object> maps = msg.getApplicationProperties();
        String actionType = (String)(maps.get("actionType"));
        return actionType.equals("insert");
    }
}