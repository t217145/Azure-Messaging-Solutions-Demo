package com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoSender.controllers;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoSender.models.Payment;
import com.fasterxml.jackson.databind.ObjectMapper;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import com.azure.messaging.servicebus.ServiceBusSenderAsyncClient;

@RestController
@RequestMapping("/dedip")
public class DeDipSender {

    private static final String OBJECTTYPE = "com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoReceiver.models.Payment";

    @Value("${spring.jms.servicebus.connection-string}")
    private String connStr; 
    
    @Value("${queue.dedip.name}")
    private String queueName;    

    @PostMapping("/bySdk/{MessageId}")
    public String bySdk(@PathVariable("MessageId") String messageId, @RequestBody Payment payment) {
        String rtnMsg = "";
        AtomicBoolean sampleSuccessful = new AtomicBoolean(false);
        CountDownLatch countdownLatch = new CountDownLatch(1);
                
        try{
            //prepare the sender
            ServiceBusClientBuilder builder = new ServiceBusClientBuilder().connectionString(connStr);
            ServiceBusSenderAsyncClient sender = builder.sender().queueName(queueName).buildAsyncClient();
            
            //prepare the message
            String paymentJSON = new ObjectMapper().writeValueAsString(payment);
            ServiceBusMessage msg = new ServiceBusMessage(BinaryData.fromBytes(paymentJSON.getBytes(UTF_8)));
            Map<String, Object> maps = msg.getApplicationProperties();
            maps.put("_type", OBJECTTYPE);

            //send out the message
            sender.sendMessage(msg).subscribe(
                unused -> System.out.printf("Sending message with Id [%s]%n", messageId),
                error -> System.err.println("Error occurred while publishing message batch: " + error),
                () -> {
                    System.out.printf("Send message with Id [%s] success%n", messageId);
                    sampleSuccessful.set(true);
                }
            );

            countdownLatch.await(10, TimeUnit.SECONDS);
            sender.close();

            rtnMsg = "Send Success";
        } catch (Exception e){
            rtnMsg = "Send Fail" + e.getStackTrace();
            e.printStackTrace();
        }
        return rtnMsg;
    }
}
