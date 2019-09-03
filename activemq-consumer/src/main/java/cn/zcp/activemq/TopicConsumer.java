package cn.zcp.activemq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.util.Map;

/**
 * @author zhongchunping
 * @version 1.0
 * @Time 2019-08-28 11:49
 * @describe activemq-consumer
 * 创建一个简单的订阅者
 *
 * 持久订阅时，客户端向 JMS 服务器注册一个自己身份的 ID，
 * 当这个客户端处于离线时，JMS Provider 会为这个 ID 保
 * 存所有发送到主题的消息，当客户再次连接到 JMS
 * Provider 时，会根据自己的 ID 得到所有当自己处于离线时
 * 发送到主题的消息。
 * 这个身份ID，在代码中的体现就是 connection的 ClientID，
 * 这个其实很好理解，你要想收到朋友发送的 qq 消息，前提
 * 就是你得先注册个 QQ 号，而且还要有台能上网的设备，
 * 电脑或手机。设备就相当于是 clientId 是唯一的；qq 号相
 * 当于是订阅者的名称，在同一台设备上，不能用同一个 qq
 * 号挂 2 个客户端。连接的 clientId 必须是唯一的，订阅者
 * 的名称在同一个连接内必须唯一。这样才能唯一的确定连
 * 接和订阅者。
 */
public class TopicConsumer {
    static Logger logger = LoggerFactory.getLogger(TopicConsumer.class);
    public static void main(String[] args) {
        //[当broker发送消息给给订阅者时,订阅者处理非持久订阅状态,持久订阅者可以收到消息,非持久订阅者不能收到消息]
        //topicConsumer1();//非持久订阅者
        topicConsumer2();//持久订阅者
    }

    /**
     * 非持久订阅者
     *
     */
    public static void topicConsumer1(){
        Connection connection=null;
        Session session= null;
        MessageConsumer consumer=null;
        try {
            String brockURL = "failover://tcp://192.168.81.240:61616";
            //1、创建connectionFactory
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brockURL);
            //2、获取一个连接(ActiveMQConnection)
            connection = connectionFactory.createConnection();
            connection.start();
            //3、创建一个回话(ActiveMQSession)
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            //4、创建一个destination
            Topic topic = session.createTopic("first-topic");
            //5、为queue创建一个生产者(ActiveMQDestination)
            consumer = session.createConsumer(topic);
            //6、监听消息
            consumer.setMessageListener(new ListenerConsumer(){
                @Override
                public void onMessage(Message message) {
                    try {
                        TextMessage textMessage = (TextMessage) message;
                        String text = textMessage.getText();
                        System.out.println("[非持久订阅者]收到订阅消息--Text: " + text);
                    } catch (JMSException e) {
                        logger.error("获取消息异常",e);
                    }
                }
            });
            Thread.sleep(120000);
        }catch (Exception e){
            logger.error("获取消息异常",e);
        }finally {
            try {
                if(null!=consumer)consumer.close();
                if(null!=session)session.close();
                if(null!=session)connection.close();
            } catch (JMSException e) {
                logger.error("关闭连接异常",e);
            }
        }
    }

    /**
     * 持久订阅者：
     * 优点：可以完整获取topic的发布的消息
     * 缺点：如果持久订阅的消息太多,则会溢出
     */
    public static void topicConsumer2(){
        Connection connection=null;
        Session session= null;
        MessageConsumer consumer=null;
        try {
            String brockURL = "failover://tcp://192.168.81.240:61616";
            //1、创建connectionFactory
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brockURL);
            //2、获取一个连接(ActiveMQConnection)
            connection = connectionFactory.createConnection();

            connection.setClientID("zcp-001");//持久化订阅者需要增加

            connection.start();
            //3、创建一个回话(ActiveMQSession)
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            //4、创建一个destination
            Topic topic = session.createTopic("first-topic");
            //5、为topic创建一个持久订阅者
            consumer = session.createDurableSubscriber(topic,"zcp-001");//名字不一样要对应ClientId
            //6、监听消息
            consumer.setMessageListener(new ListenerConsumer(){
                @Override
                public void onMessage(Message message) {
                    try {
                        TextMessage textMessage = (TextMessage) message;
                        String text = textMessage.getText();
                        System.out.println("[持久订阅者]收到订阅消息--Text: " + text);
                    } catch (JMSException e) {
                        logger.error("获取消息异常",e);
                    }
                }
            });
            Thread.sleep(120000);
        }catch (Exception e){
            logger.error("获取消息异常",e);
        }finally {
            try {
                if(null!=consumer)consumer.close();
                if(null!=session)session.close();
                if(null!=session)connection.close();
            } catch (JMSException e) {
                logger.error("关闭连接异常",e);
            }
        }
    }
}
