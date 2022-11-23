package com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoReceiver.repos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoReceiver.models.PaymentWithoutSession;

public interface PaymentWithoutSessionRepo extends JpaRepository<PaymentWithoutSession, Integer> {
    @Query("select p from PaymentWithoutSession p where p.policyNo = ?1")
    PaymentWithoutSession findByPolicyNo(int policyNo);
}