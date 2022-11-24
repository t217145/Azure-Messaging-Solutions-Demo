package com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoSender.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
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
@RequestMapping("/durable")
public class DurableSender {

    private static final String OBJECTTYPE = "com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoReceiver.models.Payment";

    @Value("${spring.jms.servicebus.connection-string}")
    private String connStr; 
    
    @Value("${topic.simple.name}")
    private String topicName;

    @PostMapping("send10Message")
    public String send10Message() {
        String rtnMsg = "";
        AtomicBoolean sampleSuccessful = new AtomicBoolean(false);
        CountDownLatch countdownLatch = new CountDownLatch(1);
                
        try{
            //prepare the sender
            ServiceBusClientBuilder builder = new ServiceBusClientBuilder().connectionString(connStr);
            ServiceBusSenderAsyncClient sender = builder.sender().topicName(topicName).buildAsyncClient();
            
            List<ServiceBusMessage> msgs = new ArrayList<>();

            //prepare the message
            for(int i=1;i<=10;i++){
                Payment payment = new Payment(i, "012", "HKD", "1234567", 12.34);
                String paymentJSON = new ObjectMapper().writeValueAsString(payment);
                ServiceBusMessage msg = new ServiceBusMessage(BinaryData.fromBytes(paymentJSON.getBytes(UTF_8)));
                Map<String, Object> maps = msg.getApplicationProperties();
                maps.put("_type", OBJECTTYPE);
                msgs.add(msg);
            }

            //send out the message
            sender.sendMessages(msgs).subscribe(
                unused -> System.out.println("Sending batch message"),
                error -> System.err.println("Error occurred while publishing message batch: " + error),
                () -> {
                    System.out.println("Send batch message success");
                    sampleSuccessful.set(true);
                }
            );

            countdownLatch.await(10, TimeUnit.SECONDS);
            sender.close();

            rtnMsg = "Send Success 10 messages to " + topicName;
        } catch (Exception e){
            rtnMsg = "Send Fail" + e.getStackTrace();
            e.printStackTrace();
        }
        return rtnMsg;
    }
}