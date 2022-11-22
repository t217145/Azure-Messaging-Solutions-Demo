package com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoReceiver.models;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Payment implements Serializable {
    private String bankCode;
    private String currency;
    private String acctNo;
    private double amt;
}