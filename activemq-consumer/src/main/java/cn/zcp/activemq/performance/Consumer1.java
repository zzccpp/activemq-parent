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
 *
 * 几种ACK回复源码查看 org.apache.activemq.ActiveMQMessageConsumer#afterMessageIsConsumed(..)
 *
 * 源码分析：：
 * AUTO_ACKNOWLEDGE：如果设置了optimizeAcknowledge，则会在ackCounter + deliveredCounter >= (info.getPrefetchSize() * .65) || (optimizeAcknowledgeTimeOut > 0 && System.currentTimeMillis() >= (optimizeAckTimestamp + optimizeAcknowledgeTimeOut))
 * 条件满足时进行批量确认.STANDARD_ACK_TYPE告诉Broker删除
 *
 * CLIENT_ACKNOWLEDGE：客户端自动确认，无论同步/异步，MQ均不发送STANDARD_ACK_TYPE，直到message.acknowledge调用确认消息；如果在client端未确认消息个数达到prefetchSize * 0.5，会补送一个ACK_TYPE为DELIVERED_ACK_TYPE的确认指令，这会触发broker端 可继续push消息到client端。
 *
 * DUPS_OK_ACKNOWLEDGE：调用STANDARD_ACK_TYPE来告诉Broker删除
 *
 * SESSION_TRANSACTED：事务开启后，在session.commit()之前，所有消费的消息，要么全部正常确认，要么全部redelivery。在基于GROUP(消息分组)场景下特别适合。
 *
 * INDIVIDUAL_ACKNOWLEDGE：确认时机和CLIENT_ACKNOWLEDGE几乎一样；当消息消费成功后，调用message.acknowledege来确认此消息(单条)，而CLIENT_ACKNOWLEDGE模式message.acknowledge()方法将导致整个session中所有消息被确认(批量确认)。
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

            connectionFactory.setOptimizeAcknowledge(true);//设置对AUTO_ACKNOWLEDGE有批量确认优化效果

            //2、获取一个连接(ActiveMQConnection)
            connection = connectionFactory.createConnection();
            connection.start();
            //3、创建一个回话(ActiveMQSession)
            //参数1、transacted 是否开启事物  2、提交方式[AUTO_ACKNOWLEDGE、CLIENT_ACKNOWLEDGE、DUPS_OK_ACKNOWLEDGE]
            //额外的一种提交方式，SESSION_TRANSACTED 不是在这里传入,而是通过connectionFactory.setTransactedIndividualAck();来设置
            session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
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
                /**
                 * 源码分析：org.apache.activemq.ActiveMQMessageConsumer#createActiveMQMessage(...)
                 *          在ActiveMQMessageConsumer#afterMessageIsConsumed方法中调用
                 *
                 * 1、session.isClientAcknowledge()  调用session下面[[所有]]的Consumer，提交现在所有已经获取的消息
                 * 2、session.isIndividualAcknowledge() 表示只确认"单条消息"确认.
                 */
                textMessage.acknowledge();
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
/**
 *
 * ACK_MODE
 * 通过前面的源码分析，基本上已经知道了消息的消费过程，以及消息的批量获
 * 取和批量确认，那么接下来再了解下消息的确认过程
 * AUTO_ACKNOWLEDGE = 1 自动确认
 * CLIENT_ACKNOWLEDGE = 2 客户端手动确认
 * DUPS_OK_ACKNOWLEDGE = 3 自动批量确认
 * SESSION_TRANSACTED = 0 事务提交并确认
 * 虽然 Client 端指定了 ACK 模式,但是在 Client 与 broker 在交换 ACK 指令的时
 * 候,还需要告知 ACK_TYPE,ACK_TYPE 表示此确认指令的类型，不同的
 * ACK_TYPE 将传递着消息的状态，broker 可以根据不同的 ACK_TYPE 对消息进
 * 行不同的操作。
 *
 *
 * ACK_TYPE
 * DELIVERED_ACK_TYPE = 0 消息"已接收"，但尚未处理结束
 * STANDARD_ACK_TYPE = 2 "标准"类型,通常表示为消息"处理成功"，broker 端可以删除消息了
 * POSION_ACK_TYPE = 1 消息"错误",通常表示"抛弃"此消息，比如消息重发多
 * 次后，都无法正确处理时，消息将会被删除或者 DLQ(死信队列)
 * REDELIVERED_ACK_TYPE = 3 消息需"重发"，比如 consumer 处理消息时抛出了异常，broker 稍后会重新发送此消息
 * INDIVIDUAL_ACK_TYPE = 4 表示只确认"单条消息",无论在任何 ACK_MODE 下
 * UNMATCHED_ACK_TYPE = 5 在 Topic 中，如果一条消息在转发给“订阅者”
 * EXPIRED_ACK_TYPE==6  消息过期
 * 时，发现此消息不符合 Selector 过滤条件，那么此消息将 不会转发给订阅
 * 者，消息将会被存储引擎删除(相当于在 Broker 上确认了消息)。
 *
 */