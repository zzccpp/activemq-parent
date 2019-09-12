package cn.zcp.activemq;

import org.springframework.stereotype.Service;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

/**
 * @author zhongchunping
 * @version 1.0
 * @Time 2019-09-09 19:02
 * @describe spring-consumer 监听队列
 */
@Service
public class SecondQueueLister implements MessageListener {
    @Override
    public void onMessage(Message message) {

        try {
            TextMessage textMessage = (TextMessage) message;
            String text = textMessage.getText();
            System.out.println("------Second: " + text);
            if(text.contains("20")){//由于没有设置prefect所以都被提交了
                message.acknowledge();
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
