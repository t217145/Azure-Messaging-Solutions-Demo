package com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoSender.controllers;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderAsyncClient;
import com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoSender.models.Payment;
import com.fasterxml.jackson.databind.ObjectMapper;
import static java.nio.charset.StandardCharsets.UTF_8;

@RestController
@RequestMapping("/session")
public class SessionSender {

    @Autowired
    private JmsTemplate template;

    @Value("${spring.jms.servicebus.connection-string}")
    private String connStr; 

   /* With Session Id */
   @PostMapping("/withSessionId/{destinationName}/{sessionId}")
   public String withSessionId(@PathVariable("sessionId")String ctxId, @PathVariable("destinationName")String destinationName, @RequestBody Payment payment) {
       String rtnMsg = "";
       AtomicBoolean sampleSuccessful = new AtomicBoolean(false);
       CountDownLatch countdownLatch = new CountDownLatch(1);
               
       try{
           //prepare the sender
           ServiceBusClientBuilder builder = new ServiceBusClientBuilder().connectionString(connStr);
           ServiceBusSenderAsyncClient sender = builder.sender().topicName(destinationName).buildAsyncClient();
           
           //prepare the message
           String paymentJSON = new ObjectMapper().writeValueAsString(payment);
           ServiceBusMessage msg = new ServiceBusMessage(BinaryData.fromBytes(paymentJSON.getBytes(UTF_8))).setSessionId(ctxId);
           ServiceBusMessage batchMsg1 = new ServiceBusMessage(BinaryData.fromBytes(paymentJSON.getBytes(UTF_8))).setSessionId(ctxId + "-batch");
           ServiceBusMessage batchMsg2 = new ServiceBusMessage(BinaryData.fromBytes(paymentJSON.getBytes(UTF_8))).setSessionId(ctxId + "-batch");
           List<ServiceBusMessage> messages = Arrays.asList(batchMsg1, batchMsg2);
           
           //send out the message
           sender.sendMessage(msg).subscribe(
               unused -> System.out.println("Batch sent."),
               error -> System.err.println("Error occurred while publishing message batch: " + error),
               () -> {
                   System.out.println("Batch send complete.");
                   sampleSuccessful.set(true);
               }
           );

           sender.sendMessages(messages).subscribe(
               unused -> System.out.println("Batch sent."),
               error -> System.err.println("Error occurred while publishing message batch: " + error),
               () -> {
                   System.out.println("Batch send complete.");
                   sampleSuccessful.set(true);
               }
           );

           // subscribe() is not a blocking call. We wait here so the program does not end before the send is complete.
           countdownLatch.await(10, TimeUnit.SECONDS);

           // Close the sender.
           sender.close();

           rtnMsg = String.format("Send Success : session id : {%s}", ctxId);           
       } catch (Exception e){
           rtnMsg = "Send Fail" + e.getStackTrace();
           e.printStackTrace();
       }
       return rtnMsg;
   }

   @PostMapping("/withSessionIdByJMS/{destinationName}/{sessionId}")
   public String withSessionIdByJMS(@PathVariable("sessionId")String ctxId, @PathVariable("destinationName")String destinationName, @RequestBody Payment payment) {
       String rtnMsg = "";
       try{
           template.convertAndSend(destinationName, new ObjectMapper().writeValueAsString(payment), jmsMessage -> {
               jmsMessage.setStringProperty("JMSXGroupID", ctxId);                
               return jmsMessage;
           });
           rtnMsg = String.format("Send Success : session id : {%s}", ctxId);           
       } catch (Exception e){
           rtnMsg = "Send Fail" + e.getStackTrace();
           e.printStackTrace();
       }
       return rtnMsg;
   } 
}