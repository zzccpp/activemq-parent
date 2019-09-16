package cn.zcp.activemq;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Service;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

/**
 * @author zhongchunping
 * @version 1.0
 * @Time 2019-09-16 16:51
 * @describe activemq-parent 消息发送
 */
@Service
public class MsgProducer {

    @Autowired
    private JmsTemplate jmsTemplate;

    void doSendMsg(){

        jmsTemplate.send("springboot-queue", new MessageCreator() {
            @Override
            public Message createMessage(Session session) throws JMSException {

                return session.createTextMessage("springboot-test");
            }
        });
    }
}
