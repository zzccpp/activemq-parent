package cn.zcp.activemq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.util.Arrays;


/**
 * @author zhongchunping
 * @version 1.0
 * @Time 2019-08-28 9:35
 * @describe activemq-producer
 * 创建一个简单生产者
 */
public class SessionProducer {
    static Logger logger = LoggerFactory.getLogger(SessionProducer.class);

    public static void main(String[] args) {

        Connection connection=null;
        Session session= null;
        MessageProducer producer=null;
        try {
            String brockURL = "failover://tcp://192.168.81.240:61616";
            //1、创建connectionFactory
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brockURL);
            //2、获取一个连接(ActiveMQConnection)
            connection = connectionFactory.createConnection();
            connection.start();
            //3、创建一个回话(ActiveMQSession)
            //参数1、transacted 是否开启事物  2、提交方式[AUTO_ACKNOWLEDGE、CLIENT_ACKNOWLEDGE、DUPS_OK_ACKNOWLEDGE]
            //额外的一种提交方式，SESSION_TRANSACTED 不是在这里传入,而是通过connectionFactory.setTransactedIndividualAck();来设置
            session = connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
            //4、创建一个destination
            Queue queue = session.createQueue("first-queue");
            //5、为queue创建一个生产者
            producer = session.createProducer(queue);
            //6、发送一个消息(消息类别：查看Message的实现类[byte[],blob,Text,map、object、stream])
            //发送一个Map消息,{obj=[123123, 213234], 1111=zcp11, xxx=zcp}
            ActiveMQMapMessage message = new ActiveMQMapMessage();
            message.setString("xxx","zcp");
            message.setString("1111","zcp11");
            message.setObject("obj", Arrays.asList("123123","444444444"));
            //使用session提交，延迟消息都会持久化，不会丢失
            message.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY,30000);
            producer.send(message);
            session.commit();
            logger.info("producer:send over !");
        }catch (Exception e){
            logger.error("发送消息异常",e);
            try {
                session.rollback();
            } catch (JMSException e1) {
                e1.printStackTrace();
            }
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
