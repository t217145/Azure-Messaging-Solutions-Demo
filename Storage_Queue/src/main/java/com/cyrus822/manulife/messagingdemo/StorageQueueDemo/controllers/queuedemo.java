package com.cyrus822.manulife.messagingdemo.StorageQueueDemo.controllers;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.azure.storage.queue.*;
import com.azure.storage.queue.models.*;
import com.cyrus822.manulife.messagingdemo.StorageQueueDemo.models.Message;

@RestController
public class queuedemo {

    @Value("${queue.connStr}")
    private String connStr;

    @Value("${queue.name}")
    private String qName;    
    
    //C-R-U-D

    @PostMapping("/create")
    public SendMessageResult sendMessage(@RequestBody Message msg){
        try{
            QueueClient queueClient = new QueueClientBuilder()
                                        .connectionString(connStr)
                                        .queueName(qName)
                                        .buildClient();
        
            return queueClient.sendMessage(msg.getMessageContent());
        } catch (QueueStorageException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    //Retrieve

    @GetMapping("/retrieve")
    public List<Message> getMessages(){
        List<Message> msgs = new ArrayList<>();
        
        try {
            QueueClient queueClient = new QueueClientBuilder()
                                        .connectionString(connStr)
                                        .queueName(qName)
                                        .buildClient();

            for(QueueMessageItem msg : queueClient.receiveMessages(3)){
                msgs.add(new Message(msg.getMessageId(), msg.getInsertionTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), msg.getMessageText()));
            }
        } catch (QueueStorageException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        return msgs;
    }

    @GetMapping("/retrievePeek")
    public List<Message> getOneMessages(){
        List<Message> msgs = new ArrayList<>();
        
        try {
            QueueClient queueClient = new QueueClientBuilder()
                                        .connectionString(connStr)
                                        .queueName(qName)
                                        .buildClient();

            for(PeekedMessageItem msg : queueClient.peekMessages(5, null, null)){
                msgs.add(new Message(msg.getMessageId(), msg.getInsertionTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), msg.getMessageText()));
            }
        } catch (QueueStorageException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        return msgs;
    }

    @GetMapping("/retrieveSpecial")
    public List<Message> getMessagesSpecial(){
        List<Message> msgs = new ArrayList<>();
        
        try {
            QueueClient queueClient = new QueueClientBuilder()
                                        .connectionString(connStr)
                                        .queueName(qName)
                                        .buildClient();

            for(QueueMessageItem msg : queueClient.receiveMessages(5, Duration.ofSeconds(10), Duration.ofSeconds(20), null)){
                msgs.add(new Message(msg.getMessageId(), msg.getInsertionTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), msg.getMessageText()));
            }
        } catch (QueueStorageException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        return msgs;
    }    

    //Update
    @PostMapping("/update/{messageId}")
    public Message updateMessage(@PathVariable("messageId") String msgId, @RequestBody Message updatedMsg){
        Message rtnMsg = null;
        
        try {
            QueueClient queueClient = new QueueClientBuilder()
                                        .connectionString(connStr)
                                        .queueName(qName)
                                        .buildClient();

            Optional<QueueMessageItem> qMsg = 
                queueClient.receiveMessages(
                        queueClient.getProperties().getApproximateMessagesCount(),
                        Duration.ofSeconds(10), 
                        Duration.ofSeconds(10), 
                        null
                    )
                    .stream()
                    .filter(msg -> msg.getMessageId().equals(msgId))
                    .findFirst();

            if(qMsg.isPresent()){
                QueueMessageItem item = qMsg.get();
                queueClient.updateMessage(
                    item.getMessageId(),
                    item.getPopReceipt(),
                    updatedMsg.getMessageContent(),
                    Duration.ofSeconds(30)); 

                    rtnMsg = new Message(item.getMessageId(), item.getInsertionTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), item.getMessageText());
            }
        } catch (QueueStorageException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        return rtnMsg;
    }

    //Delete
    @GetMapping("/delete/{messageId}")
    public Message deleteMessage(@PathVariable("messageId") String msgId){
        Message rtnMsg = null;
        
        try {
            QueueClient queueClient = new QueueClientBuilder()
                                        .connectionString(connStr)
                                        .queueName(qName)
                                        .buildClient();

            java.util.Optional<QueueMessageItem> qMsg = 
                queueClient.receiveMessages(
                        queueClient.getProperties().getApproximateMessagesCount(),
                        Duration.ofSeconds(30), 
                        Duration.ofSeconds(30), 
                        null
                    )
                    .stream()
                    .filter(msg -> msg.getMessageId().equals(msgId))
                    .findFirst();

            if(qMsg.isPresent()){
                QueueMessageItem item = qMsg.get();
                queueClient.deleteMessage(
                    item.getMessageId(),
                    item.getPopReceipt()); 

                    rtnMsg = new Message(item.getMessageId(), item.getInsertionTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), item.getMessageText());
            }
        } catch (QueueStorageException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        return rtnMsg;
    }
}