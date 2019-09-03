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
 * 创建一个Listener的消费者,被动消费
 */
public class ListenerConsumer implements MessageListener{
    static Logger logger = LoggerFactory.getLogger(ListenerConsumer.class);
    public static void main(String[] args) {
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
            //参数1、transacted 是否开启事物  2、提交方式[AUTO_ACKNOWLEDGE、CLIENT_ACKNOWLEDGE、DUPS_OK_ACKNOWLEDGE]
            //额外的一种提交方式，SESSION_TRANSACTED 不是在这里传入,而是通过connectionFactory.setTransactedIndividualAck();来设置
            //如果开启了transacted,则必须提交才能
            session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
            //4、创建一个destination
            Queue queue = session.createQueue("first-queue");
            //5、为queue创建一个生产者(ActiveMQDestination)
            consumer = session.createConsumer(queue);
            //6、被动获取消息
            consumer.setMessageListener(new ListenerConsumer());
            Thread.sleep(30000);
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

    @Override
    public void onMessage(Message message) {
        try {
            System.out.println("被动接受消息: " + message);
            //if(message instanceof TextMessage){//获取文本类型的消息
                TextMessage textMessage = (TextMessage) message;
                String text = textMessage.getText();
                System.out.println("Received  Text: " + text);
            /*}else if(message instanceof MapMessage){//获取MAP类型的消息
                Map<String, Object> contentMap = ((ActiveMQMapMessage) message).getContentMap();
                System.out.println("Received  Map: " +contentMap);
            }*/
            //测试当收到第12条的时候，直接提交，12之前的都被确认
            if(text.contains("12"))message.acknowledge();//匹配CLIENT_ACKNOWLEDGE,消息执行完后执行提交
        } catch (JMSException e) {
            logger.error("获取消息异常",e);
        }
    }
}
