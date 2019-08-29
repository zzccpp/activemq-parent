package cn.zcp.activemq.performance;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;

/**
 * @author zhongchunping
 * @version 1.0
 * @Time 2019-08-28 9:35
 * @describe activemq-producer
 *
 * 性能调整版
 * 官网说明地址：https://activemq.apache.org/version-5-performance-tuning
 * 设置异步发送：
 * |默认设置，消息发送为持久化(这是JMS规范所要求的)，如果发布持久化消息，为阻塞状态（等待消息保存至磁盘及等待消费者消息队列中）才返回
 * |则，如果你要往Topic上发送消息，但无订阅者时，会阻塞，此时可以使用异步的方式发送，对于不需要持久化的消息队列为了提高性能，也可以设置为异步发送
 * 设置ActiveMQConnectionFactory 的 useAsyncSend属性设置为true。
 *
 */
public class Producer1 {
    static Logger logger = LoggerFactory.getLogger(Producer1.class);

    public static void main(String[] args) {
        Connection connection=null;
        Session session= null;
        MessageProducer producer=null;
        try {
            String brockURL = "failover://tcp://192.168.81.240:61616";
            //1、创建connectionFactory
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brockURL);

            //性能：
            connectionFactory.setUseAsyncSend(true);//设置为异步发送

            //2、获取一个连接(ActiveMQConnection)
            connection = connectionFactory.createConnection();
            connection.start();
            //3、创建一个回话(ActiveMQSession)
            //参数1、transacted 是否开启事物  2、提交方式[AUTO_ACKNOWLEDGE、CLIENT_ACKNOWLEDGE、DUPS_OK_ACKNOWLEDGE]
            //额外的一种提交方式，SESSION_TRANSACTED 不是在这里传入,而是通过connectionFactory.setTransactedIndividualAck();来设置
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            //4、创建一个destination
            Queue queue = session.createQueue("first-queue");
            //5、为queue创建一个生产者
            producer = session.createProducer(queue);
            //6、发送一个消息(消息类别：查看Message的实现类[byte[],blob,Text,map、object、stream])
            TextMessage textMessage = session.createTextMessage("UseAsyncSend UseAsyncSend UseAsyncSend over...");
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);//设置消息非持久化,默认情况是消息持久化

            producer.send(textMessage);
            logger.info("producer:send over !");
            session.close();
            connection.close();
        }catch (Exception e){
            logger.error("发送消息异常",e);
        }finally {
            try {
                if(null!=producer)producer.close();
                if(null!=session)session.close();
                if(null!=session)connection.close();
            } catch (JMSException e) {
                logger.error("关闭连接异常",e);
            }
        }
    }
}
