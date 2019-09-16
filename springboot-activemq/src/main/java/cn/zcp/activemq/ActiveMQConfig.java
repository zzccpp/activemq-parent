package cn.zcp.activemq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;

import javax.jms.Queue;
import javax.jms.Topic;

/**
 * @author zhongchunping
 * @version 1.0
 * @Time 2019-09-16 17:36
 * @describe activemq-parent
 *
 * Spring配置类
 */
//@Configuration
public class ActiveMQConfig {

    @Value("${my.queue.name}")
    private String queueName;

    @Value("${my.topic.name}")
    private String TopicName;

    @Value("${spring.activemq.user}")
    private String usrName;

    @Value("${spring.activemq.password}")
    private  String password;

    @Value("${spring.activemq.broker-url}")
    private  String brokerUrl;


    @Bean(name = "springboot-queue")
    public Queue createQueue(){
        return new ActiveMQQueue(queueName);
    }

    @Bean(name = "springboot-topic")
    public Topic createTopic(){
        return new ActiveMQTopic(TopicName);
    }

    @Bean
    public ActiveMQConnectionFactory connectionFactory(){
        return new ActiveMQConnectionFactory(usrName,password,brokerUrl);
    }

    @Bean
    public JmsListenerContainerFactory jmsListenerContainerQueue(ActiveMQConnectionFactory connectionFactory){
        DefaultJmsListenerContainerFactory jmsListenerContainerFactory = new DefaultJmsListenerContainerFactory();

        jmsListenerContainerFactory.setConnectionFactory(connectionFactory);

        return jmsListenerContainerFactory;
    }

    @Bean
    public JmsListenerContainerFactory jmsListenerContainerTopic(ActiveMQConnectionFactory connectionFactory){
        DefaultJmsListenerContainerFactory jmsListenerContainerFactory = new DefaultJmsListenerContainerFactory();
        jmsListenerContainerFactory.setPubSubDomain(true);
        jmsListenerContainerFactory.setConnectionFactory(connectionFactory);

        return jmsListenerContainerFactory;
    }
}
