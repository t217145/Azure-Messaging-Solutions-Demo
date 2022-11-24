package com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoReceiver.repos;

import org.springframework.data.jpa.repository.JpaRepository;
import com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoReceiver.models.SimplePayment;

public interface SimplePaymentRepo extends JpaRepository<SimplePayment, Integer> {
    
}
