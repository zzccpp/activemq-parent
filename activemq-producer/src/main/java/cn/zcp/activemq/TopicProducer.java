package cn.zcp.activemq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;

/**
 * @author zhongchunping
 * @version 1.0
 * @Time 2019-08-28 9:35
 * @describe activemq-producer
 * 创建一个Topic生产者
 */
public class TopicProducer {
    static Logger logger = LoggerFactory.getLogger(TopicProducer.class);

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
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            //4、创建一个destination
            Topic topic = session.createTopic("first-topic");
            //5、为queue创建一个生产者
            producer = session.createProducer(topic);
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);//设置消息非持久化
            producer.setPriority(5);//消息优先级（优先级分为10个级别,从0(最低)到9(最高).如果不设定优先级，默认级别是4
            //6、发送一个消息(消息类别：查看Message的实现类[byte[],blob,Text,map、object、stream、message(只有消息头与属性)])
            for (int i = 0; i <100 ; i++) {
                TextMessage textMessage = session.createTextMessage("Hello Word"+i);
                //textMessage.setJMSMessageID();JMSMessageID唯一识别每个消息的标识
                textMessage.setStringProperty("zcp","test-属性"+i);//设置消息属性
                //textMessage.setJMSExpiration(5);//未生效
                producer.send(textMessage);
            }
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
