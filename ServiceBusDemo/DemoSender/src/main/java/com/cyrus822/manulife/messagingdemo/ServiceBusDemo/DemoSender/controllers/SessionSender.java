package com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoSender.controllers;

import java.util.Map;
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
import com.azure.messaging.servicebus.ServiceBusMessageBatch;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
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

    @Value("${queue.nosession.name}")
    private String noSessionQueueName;

    @Value("${queue.session.name}")
    private String sessionQueueName;

    private static final String OBJECTTYPE = "com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoReceiver.models.Payment";    

    /* Without Session Id */
    @PostMapping("/withoutSessionId")
    public String withoutSessionId() {
        String rtnMsg = "";
                
        try{
            //prepare the sender
            ServiceBusSenderClient senderClient = new ServiceBusClientBuilder().connectionString(connStr).sender().queueName(noSessionQueueName).buildClient();
            
            //prepare the message
            ServiceBusMessageBatch messageBatch = senderClient.createMessageBatch(); 
            
            for(int i=1;i<=30;i++){
                Payment payment = new Payment(i, "001", "HKD", "76543210", 43.21);
                String paymentJSON = new ObjectMapper().writeValueAsString(payment);
                ServiceBusMessage msg = new ServiceBusMessage(BinaryData.fromBytes(paymentJSON.getBytes(UTF_8)));
                Map<String, Object> maps = msg.getApplicationProperties();
                maps.put("_type", OBJECTTYPE);

                if (messageBatch.tryAddMessage(msg)) {
                    continue;
                }

                // The batch is full, so we create a new batch and send the batch.
                senderClient.sendMessages(messageBatch);
                System.out.println("Sent a batch of messages to the queue: " + noSessionQueueName);  
                
                // create a new batch
                messageBatch = senderClient.createMessageBatch();

                // Add that message that we couldn't before.
                if (!messageBatch.tryAddMessage(msg)) {
                    System.err.printf("Message is too large for an empty batch. Skipping. Max size: %s.", messageBatch.getMaxSizeInBytes());
                }                
            }

            if (messageBatch.getCount() > 0) {
                senderClient.sendMessages(messageBatch);
                System.out.println("Sent a batch of messages to the queue: " + noSessionQueueName);
            }
        
            //close the client
            senderClient.close();

            rtnMsg = String.format("Send to {%s} without Session Id Success", noSessionQueueName);
        } catch (Exception e){
            rtnMsg = "Send Fail" + e.getStackTrace();
            e.printStackTrace();
        }
        return rtnMsg;
    }

    /* With Session Id */
    @PostMapping("/withSessionId")
    public String withSessionId() {
        String rtnMsg = "";
                
        try{
            //prepare the sender
            ServiceBusSenderClient senderClient = new ServiceBusClientBuilder().connectionString(connStr).sender().queueName(sessionQueueName).buildClient();
            
            //prepare the message
            ServiceBusMessageBatch messageBatch = senderClient.createMessageBatch(); 
            
            for(int i=1;i<=30;i++){
                Payment payment = new Payment(i, "012", "USD", "01234567", 12.34);
                String paymentJSON = new ObjectMapper().writeValueAsString(payment);
                ServiceBusMessage msg = new ServiceBusMessage(BinaryData.fromBytes(paymentJSON.getBytes(UTF_8))).setSessionId("ctx-" + Integer.toString(payment.getPolicyNo()%10));
                Map<String, Object> maps = msg.getApplicationProperties();
                maps.put("_type", OBJECTTYPE);

                if (messageBatch.tryAddMessage(msg)) {
                    continue;
                }

                // The batch is full, so we create a new batch and send the batch.
                senderClient.sendMessages(messageBatch);
                System.out.println("Sent a batch of messages to the queue: " + sessionQueueName);  
                
                // create a new batch
                messageBatch = senderClient.createMessageBatch();

                // Add that message that we couldn't before.
                if (!messageBatch.tryAddMessage(msg)) {
                    System.err.printf("Message is too large for an empty batch. Skipping. Max size: %s.", messageBatch.getMaxSizeInBytes());
                }                
            }

            if (messageBatch.getCount() > 0) {
                senderClient.sendMessages(messageBatch);
                System.out.println("Sent a batch of messages to the queue: " + sessionQueueName);
            }
        
            //close the client
            senderClient.close();

            rtnMsg = String.format("Send to {%s} without Session Id Success", sessionQueueName);
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
                jmsMessage.setStringProperty("_type", OBJECTTYPE);                
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