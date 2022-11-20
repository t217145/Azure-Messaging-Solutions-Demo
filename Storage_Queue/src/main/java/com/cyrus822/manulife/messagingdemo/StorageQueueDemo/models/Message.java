package com.cyrus822.manulife.messagingdemo.StorageQueueDemo.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Message {
   private String messageId;
   private String sessionId;
   private String messageContent; 
}