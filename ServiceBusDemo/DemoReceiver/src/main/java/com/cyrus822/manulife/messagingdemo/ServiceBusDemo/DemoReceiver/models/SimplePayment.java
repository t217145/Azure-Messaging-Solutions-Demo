package com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoReceiver.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class SimplePayment implements Serializable{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)    
    private int id;
    private int policyNo;
    private String bankCode;
    private String currency;
    private String acctNo;
    private double amt;
}