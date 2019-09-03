package cn.zcp.activemq.performance;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQPrefetchPolicy;
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
 * 性能调整版
 * 官网说明地址：https://activemq.apache.org/version-5-performance-tuning
 *
 *
 * 对于消费者来说:
 *  1、设置预取值尺寸来提高吞吐量  ActiveMQConnectionFactory 设置 ActiveMQPrefetchPolicy
 *
 *  2、优化的确认
 * 在自动确认模式下(AUTO_ACKNOWLEDGE)消费消息时（在创建消费者会话时设置），ActiveMQ将确认批量收到消息回送给代理（以提高性能）。
 * 批量大小是Consumer的预取限制的50％。通过将ActiveMQ ConnectionFactory上的optimizeAcknowledge属性设置为false(默认)，可以关闭批处理确认
 *
 *  总结：自动提交的情况下, 如果设置了optimizeAcknowledge为ture时，消费端会批量提交，大小=PrefetchPolicy/2
 *
 *  3、直接通过会话消费
 * 默认情况下，Consumer的会话将在单独的线程中将消息分发给使用者。如果您正在使用具有自动确认功能的使用者，
 * 则可以通过将ActiveMQ ConnectionFactory上的alwaysSessionAsync属性设置为false，将消息直接传递给使用者来增加吞吐量
 *
 */
public class Consumer1 {
    static Logger logger = LoggerFactory.getLogger(Consumer1.class);
    public static void main(String[] args) {
        Connection connection=null;
        Session session= null;
        MessageConsumer consumer=null;
        try {
            String brockURL = "failover://tcp://192.168.81.240:61616";
            //1、创建connectionFactory
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brockURL);

            //性能参数:设置预获取数量
            ActiveMQPrefetchPolicy prefetchPolicy = new ActiveMQPrefetchPolicy();
            prefetchPolicy.setQueuePrefetch(1);
            connectionFactory.setPrefetchPolicy(prefetchPolicy);

           // connectionFactory.setOptimizeAcknowledge(true);

            //2、获取一个连接(ActiveMQConnection)
            connection = connectionFactory.createConnection();
            connection.start();
            //3、创建一个回话(ActiveMQSession)
            //参数1、transacted 是否开启事物  2、提交方式[AUTO_ACKNOWLEDGE、CLIENT_ACKNOWLEDGE、DUPS_OK_ACKNOWLEDGE]
            //额外的一种提交方式，SESSION_TRANSACTED 不是在这里传入,而是通过connectionFactory.setTransactedIndividualAck();来设置
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            //4、创建一个destination
            Queue queue = session.createQueue("first-queue");
            //5、为queue创建一个生产者(ActiveMQDestination)[可创建多个消费者]
            consumer = session.createConsumer(queue);
            //6、主动拉取消息
            Message message = consumer.receive();
            System.out.println("message: " + message);
            if(message instanceof TextMessage){//获取文本类型的消息
                TextMessage textMessage = (TextMessage) message;
                String text = textMessage.getText();
                System.out.println("Received  Text: " + text);
            }else if(message instanceof MapMessage){//获取MAP类型的消息
                Map<String, Object> contentMap = ((ActiveMQMapMessage) message).getContentMap();
                System.out.println("Received  Map: " +contentMap);
            }
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
}
