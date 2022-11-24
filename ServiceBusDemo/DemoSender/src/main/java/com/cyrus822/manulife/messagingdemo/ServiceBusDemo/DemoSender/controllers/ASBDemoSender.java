package com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoSender.controllers;

import java.util.Map;
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
import com.azure.core.credential.TokenCredential;
import com.azure.core.util.BinaryData;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderAsyncClient;
import com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoSender.models.AuthModel;
import com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoSender.models.Payment;
import com.fasterxml.jackson.databind.ObjectMapper;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.net.URI;

@RestController
@RequestMapping("/basic")
public class ASBDemoSender{

    @Autowired
    private JmsTemplate template;

    @Value("${spring.jms.servicebus.connection-string}")
    private String connStr; 

    @Value("${namespace.url}")
    private String nsUrl;

    private static final String OBJECTTYPE = "com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoReceiver.models.Payment";

/* Basic */
    @PostMapping("/byJMS/{destinationName}")
    public String byJMS(@PathVariable("destinationName")String destinationName, @RequestBody Payment payment) {
        String rtnMsg = "";
        String properties = "{\"SessionId\": \"ctx-01\", \"MessageId\": \"msg01\"}";
        try{
            template.convertAndSend(destinationName, new ObjectMapper().writeValueAsString(payment), jmsMessage -> {
                jmsMessage.setStringProperty("_type", OBJECTTYPE);
                jmsMessage.setStringProperty("JMSXGroupID", "ctx01");
                jmsMessage.setStringProperty("BrokerProperties", properties);
                jmsMessage.setJMSMessageID("msg01");
                return jmsMessage;
            });
            rtnMsg = "Send Success";
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
            Map<String, Object> maps = msg.getApplicationProperties();
            maps.put("_type", OBJECTTYPE);

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
            //Prepare the sender
            RestTemplate rt = new RestTemplate();
            final String baseUrl = String.format(nsUrl, destinationName);
            URI uri = new URI(baseUrl);

            //Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", token);
            headers.set("_type", OBJECTTYPE);
            HttpEntity<Payment> entity = new HttpEntity<>(payment, headers);

            //Call WS
            ResponseEntity<Void> result = rt.postForEntity(uri, entity, Void.class);

            //Get the response
            int rtnCode = result.getStatusCodeValue();

            rtnMsg = (rtnCode == 201) ? "Send Success" : "Send failed";
        } catch (Exception e){
            rtnMsg = "Send failed" + e.getStackTrace();
            e.printStackTrace();
        }
        return rtnMsg;
    }   
    
    @PostMapping("/AuthBySvcPrinciple")
    public String bySdk(@RequestBody AuthModel authModel) {
        String rtnMsg = "";
        AtomicBoolean sampleSuccessful = new AtomicBoolean(false);
        CountDownLatch countdownLatch = new CountDownLatch(1);
                
        try{
            //prepare the Cred.
            TokenCredential credential = new ClientSecretCredentialBuilder().clientId(authModel.getClientId()).tenantId(authModel.getTenantId()).clientSecret(authModel.getClientSecret()).build();

            //prepare the sender
            ServiceBusClientBuilder builder = new ServiceBusClientBuilder().credential(authModel.getFullNameSpace(), credential);
            ServiceBusSenderAsyncClient sender = builder.sender().topicName(authModel.getQueueName()).buildAsyncClient();
            
            //prepare the message
            String paymentJSON = new ObjectMapper().writeValueAsString(new Payment(1, "012", "HKD", "1234567", 12.34));
            ServiceBusMessage msg = new ServiceBusMessage(BinaryData.fromBytes(paymentJSON.getBytes(UTF_8)));
            Map<String, Object> maps = msg.getApplicationProperties();
            maps.put("_type", OBJECTTYPE);

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

}