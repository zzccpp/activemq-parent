package cn.zcp.activemq;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

/**
 * @author zhongchunping
 * @version 1.0
 * @Time 2019-09-16 18:02
 * @describe activemq-parent
 *
 * 消息消费者
 */
@Service
public class QueueConsumer {

    @JmsListener(destination="springboot-queue")
    public void receiveMessage(String text) {
        System.err.println("[***接收消息***]" + text);
    }
}
