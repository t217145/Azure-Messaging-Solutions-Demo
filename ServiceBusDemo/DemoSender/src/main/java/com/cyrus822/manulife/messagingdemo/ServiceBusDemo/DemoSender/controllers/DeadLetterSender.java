package com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoSender.controllers;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import static java.nio.charset.StandardCharsets.UTF_8;
import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderAsyncClient;

@RestController
@RequestMapping("/dlq")
public class DeadLetterSender {

    private static final String OBJECTTYPE = "com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoReceiver.models.Payment";

    @Value("${spring.jms.servicebus.connection-string}")
    private String connStr; 
    
    @Value("${topic.simple.name}")
    private String topicName;

    @PostMapping("/invalid")
    public String withSessionIdByJMS( @RequestBody String invalidContent) {
        String rtnMsg = "";
        AtomicBoolean sampleSuccessful = new AtomicBoolean(false);
        CountDownLatch countdownLatch = new CountDownLatch(1);
                
        try{
            //prepare the sender
            ServiceBusClientBuilder builder = new ServiceBusClientBuilder().connectionString(connStr);
            ServiceBusSenderAsyncClient sender = builder.sender().topicName(topicName).buildAsyncClient();
            
            //prepare the message
            String paymentJSON = invalidContent;
            ServiceBusMessage msg = new ServiceBusMessage(BinaryData.fromBytes(paymentJSON.getBytes(UTF_8)));
            Map<String, Object> maps = msg.getApplicationProperties();
            maps.put("_type", OBJECTTYPE);
            maps.put("isLock", "true"); 
            maps.put("waitFor", 1);             

            //send out the message
            sender.sendMessage(msg).subscribe(
                unused -> System.out.println("Message sent."),
                error -> System.err.println("Error occurred while publishing message : " + error),
                () -> {
                    System.out.println("Message send complete.");
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
