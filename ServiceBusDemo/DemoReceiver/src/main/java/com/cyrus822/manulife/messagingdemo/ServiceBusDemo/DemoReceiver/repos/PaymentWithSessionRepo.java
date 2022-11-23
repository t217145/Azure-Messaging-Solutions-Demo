package com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoReceiver.repos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoReceiver.models.PaymentWithSession;

public interface PaymentWithSessionRepo extends JpaRepository<PaymentWithSession, Integer> {
    @Query("select p from PaymentWithSession p where p.policyNo = ?1")
    PaymentWithSession findByPolicyNo(int policyNo);
}
