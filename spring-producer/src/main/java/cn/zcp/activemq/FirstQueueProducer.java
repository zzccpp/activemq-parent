package cn.zcp.activemq;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

/**
 * @author zhongchunping
 * @version 1.0
 * @Time 2019-09-12 17:35
 * @describe activemq-parent <描述>
 */
@Component
public class FirstQueueProducer {

    @Autowired
    private JmsTemplate jmsTemplate;

    public void sendMsg(){

        //发送消息
        jmsTemplate.send(new MessageCreator() {
            @Override
            public Message createMessage(Session session) throws JMSException {
                TextMessage textMessage = session.createTextMessage("Spring-producer");
                textMessage.setJMSPriority(9);
                return textMessage;
            }
        });
        System.out.println("消息发送完成!");
    }
}
