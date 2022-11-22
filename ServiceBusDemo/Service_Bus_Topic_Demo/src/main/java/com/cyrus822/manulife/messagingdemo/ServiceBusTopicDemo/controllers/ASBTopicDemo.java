package com.cyrus822.manulife.messagingdemo.ServiceBusTopicDemo.controllers;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderAsyncClient;
import com.cyrus822.manulife.messagingdemo.ServiceBusTopicDemo.models.Payment;
import com.fasterxml.jackson.databind.ObjectMapper;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.URI;

@RestController
@RequestMapping("/send")
public class ASBTopicDemo {

    @Autowired
    private JmsTemplate template;

    @Value("${spring.jms.servicebus.connection-string}")
    private String connStr; 

    @Value("${namespace.url}")
    private String nsUrl;

/* Basic */
    @PostMapping("/byJMS/{destinationName}/{sessionId}")
    public String byJMS(@PathVariable("sessionId")String ctxId, @PathVariable("destinationName")String destinationName, @RequestBody Payment payment) {
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

    @PostMapping("/bySdk/{destinationName}")
    public String bySdk(@PathVariable("destinationName")String destinationName, @RequestBody Payment payment) {
        String rtnMsg = "";
        AtomicBoolean sampleSuccessful = new AtomicBoolean(false);
        CountDownLatch countdownLatch = new CountDownLatch(1);
                
        try{
            //prepare the sender
            ServiceBusClientBuilder builder = new ServiceBusClientBuilder().connectionString(connStr);
            ServiceBusSenderAsyncClient sender = builder.sender().topicName(destinationName).buildAsyncClient();
            
            //prepare the message
            String paymentJSON = new ObjectMapper().writeValueAsString(payment);
            ServiceBusMessage msg = new ServiceBusMessage(BinaryData.fromBytes(paymentJSON.getBytes(UTF_8)));

            //send out the message
            sender.sendMessage(msg).subscribe(
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

            rtnMsg = "Send Success";
        } catch (Exception e){
            rtnMsg = "Send Fail" + e.getStackTrace();
            e.printStackTrace();
        }
        return rtnMsg;
    }

    @PostMapping("/byHTTP/{destinationName}/{token}")
    public String byHTTP(@PathVariable("destinationName")String destinationName, @PathVariable("token")String token, @RequestBody Payment payment) {
        String rtnMsg = "";
                
        try{
            //prepare the sender
            RestTemplate rt = new RestTemplate();
            final String baseUrl = String.format(nsUrl, destinationName);
            URI uri = new URI(baseUrl);

            // create headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", token);
            headers.set("_type", "com.cyrus822.manulife.messagingdemo.ServiceBusTopicDemo.models.Payment");
            HttpEntity<Payment> entity = new HttpEntity<>(payment, headers);

            ResponseEntity<Void> result = rt.postForEntity(uri, entity, Void.class);
            

            int rtnCode = result.getStatusCodeValue();

            rtnMsg = (rtnCode == 201) ? "Send Success" : "Send failed";
        } catch (Exception e){
            rtnMsg = "Send failed" + e.getStackTrace();
            e.printStackTrace();
        }
        return rtnMsg;
    }    

}