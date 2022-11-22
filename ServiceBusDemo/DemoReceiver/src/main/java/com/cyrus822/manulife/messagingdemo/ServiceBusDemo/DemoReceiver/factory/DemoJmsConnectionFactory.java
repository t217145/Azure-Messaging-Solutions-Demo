package com.cyrus822.manulife.messagingdemo.ServiceBusDemo.DemoReceiver.factory;

import javax.jms.ConnectionFactory;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;

public class DemoJmsConnectionFactory {
    
    @Bean(name = "sessionTopicListenerFactory")
    public JmsListenerContainerFactory<?> sessionTopicListenerFactory(ConnectionFactory connectionFactory, DefaultJmsListenerContainerFactoryConfigurer configurer){
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
		factory.setSubscriptionShared(false);
        factory.setPubSubDomain(true);
        factory.setSubscriptionDurable(true);
		factory.setSessionTransacted(true);
        return factory;
    }

    @Bean(name = "sessionQueueListenerFactory")
    public JmsListenerContainerFactory<?> sessionQueueListenerFactory(ConnectionFactory connectionFactory, DefaultJmsListenerContainerFactoryConfigurer configurer){
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
		factory.setSessionTransacted(true);
        return factory;
    }	
}