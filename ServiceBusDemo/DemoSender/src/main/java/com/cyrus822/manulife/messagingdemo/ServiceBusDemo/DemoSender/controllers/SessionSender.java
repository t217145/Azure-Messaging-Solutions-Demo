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
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoSender.models.Payment;
import com.fasterxml.jackson.databind.ObjectMapper;
import static java.nio.charset.StandardCharsets.UTF_8;

@RestController
@RequestMapping("/session")
public class SessionSender {

    private static final int NUMBER_OF_SESSION_GROUP = 5;

    @Autowired
    private JmsTemplate template;

    @Value("${spring.jms.servicebus.connection-string}")
    private String connStr; 

    @Value("${queue.nosession.name}")
    private String noSessionQueueName;

    @Value("${queue.session.name}")
    private String sessionQueueName;

    /* Without Session Id */
    @PostMapping("/noSessionId")
    public String noSessionId() {
        //It will !sequentially! generate 30 pair of Payment request, which is first create the request for payment, and then update the request. !!! Without Session !!!
        return Send30Message(false, "012", "HKD", "1234567", 12.34);
    }

    /* With Session Id */
    @PostMapping("/withSessionId")
    public String withSessionId() {
        //It will !sequentially! generate 30 pair of Payment request, which is first create the request for payment, and then update the request. !!! With Session !!!
        return Send30Message(true, "001", "USD", "7654321", 43.21);
    }

    @PostMapping("/withSessionIdByJMS/{destinationName}/{sessionId}")
    public String withSessionIdByJMS(@PathVariable("sessionId")String ctxId, @PathVariable("destinationName")String destinationName, @RequestBody Payment payment) {
        String rtnMsg = "";
        try{
            template.convertAndSend(destinationName, new ObjectMapper().writeValueAsString(payment), jmsMessage -> {
                jmsMessage.setStringProperty("JMSXGroupID", ctxId); 
                jmsMessage.setStringProperty("_type", "com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoReceiver.models.Payment");                
                return jmsMessage;
            });
            rtnMsg = String.format("Send Success : session id : {%s}", ctxId);           
        } catch (Exception e){
            rtnMsg = "Send Fail" + e.getStackTrace();
            e.printStackTrace();
        }
        return rtnMsg;
    } 

    private String Send30Message(boolean isSessionEnabled, String bankCode, String currency, String acctNo, double amt){
        String rtnMsg = "";
        String strIsSession = isSessionEnabled ? "with" : "without";
        String queueName = isSessionEnabled ? sessionQueueName : noSessionQueueName;
        try{
            //prepare the sender
            ServiceBusSenderClient senderClient = new ServiceBusClientBuilder().connectionString(connStr).sender().queueName(queueName).buildClient();

            //Generate 30 new + update request
            for(int policyNo = 1; policyNo <= (NUMBER_OF_SESSION_GROUP * 3); policyNo++){
                //First create the new Payment request
                Payment payment = new Payment(policyNo, bankCode, currency, acctNo, amt);
                ServiceBusMessage msg = prepareMessage(payment, isSessionEnabled, "insert");
                senderClient.sendMessage(msg);
                System.out.println(String.format("New payment request with policy number {%d} send to {%s} {%s} Session Id Success", payment.getPolicyNo(), queueName, strIsSession));

                //Then update the Payment request, just add a suffix to the account number to indicate this is an update request
                payment.setAcctNo(payment.getAcctNo() + "-updated");
                ServiceBusMessage msgUpdated = prepareMessage(payment, isSessionEnabled, "update");
                senderClient.sendMessage(msgUpdated);
                System.out.println(String.format("Updated payment request with policy number {%d} send to {%s} {%s} Session Id Success", payment.getPolicyNo(), queueName, strIsSession));                
            }

            senderClient.close();

            rtnMsg = String.format("Send to {%s} {%s} Session Id Success", queueName, strIsSession);
        } catch (Exception e){
            rtnMsg = "Send Fail" + e.getStackTrace();
            e.printStackTrace();
        }
        return rtnMsg;
    }

    private ServiceBusMessage prepareMessage(Payment payment, boolean isSessionEnabled, String actionType){
        ServiceBusMessage msg = null;
        try{
            String paymentJSON = new ObjectMapper().writeValueAsString(payment);
            msg = new ServiceBusMessage(BinaryData.fromBytes(paymentJSON.getBytes(UTF_8)));
            if(isSessionEnabled){
                msg.setSessionId("ctx-" + Integer.toString(payment.getPolicyNo() % NUMBER_OF_SESSION_GROUP));
            }
            Map<String, Object> maps = msg.getApplicationProperties();
            maps.put("_type", "com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoReceiver.models.Payment");
            maps.put("actionType", actionType);   
        } catch (Exception e){
            //do nth
        }
        return msg;
    }
}