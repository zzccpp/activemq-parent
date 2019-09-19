package cn.zcp.activemq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQMessageConsumer;
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
 * 创建一个简单的消费者
 *
 *
 * ===在非事务型会话中
 * 消息何时被确认取决于创建会话时的应答模式
 * (acknowledgement mode). 有三个可选项
 * Session.AUTO_ACKNOWLEDGE
 * 当客户成功的从 receive 方法返回的时候，或者从MessageListenner.onMessage 方法成功返回的时候，会话自动确认客户收到消息。
 *
 * Session.CLIENT_ACKNOWLEDGE
 * 客户通过调用消息的 acknowledge 方法确认消息。
 * CLIENT_ACKNOWLEDGE 特性
 * 在这种模式中，确认是在会话层上进行，确认一个被消费的消息将自动确认所有已被会话消费的消息。列如，如果
 * 一个消息消费者消费了 10 个消息，然后确认了第 5 个消息，那么 0~5 的消息都会被确认 ->
 * 演示如下：发送端发送 10 个消息，接收端接收 10 个消息，
 * 但是在 i==5 的时候，调用 message.acknowledge()进行
 * 确认，会发现 0~4 的消息都会被确认
 *
 * Session.DUPS_ACKNOWLEDGE
 * 消息延迟确认。指定消息提供者在消息接收者没有确认发
 * 送时重新发送消息，这种模式不在乎接受者收到重复的消 息。
 * 消息的持久化存储
 *
 */
public class SimpleConsumer {
    static Logger logger = LoggerFactory.getLogger(SimpleConsumer.class);
    public static void main(String[] args) {
        Connection connection=null;
        Session session= null;
        MessageConsumer consumer=null;
        try {
            String brockURL = "failover://tcp://localhost:61616";
            //ActiveMQ的客户端只能访问Master的Broker,其他处于Slave的Broker不能访问，所以客户端连接的Broker应该使用failover协议(失败转移)
            brockURL="failover:(tcp://192.168.81.240:61616,tcp://192.168.81.240:61626,tcp://192.168.81.240:61636)?randomize=false";
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
            Queue queue = session.createQueue("first-queue");
            //5、为queue创建一个生产者(ActiveMQDestination)
            /**
             * 源码分析：ActiveMQSession子对象提供了更多的构造方法
             *  1、构建一个ActiveMQMessageConsumer对象，并初始化一些值[org.apache.activemq.ActiveMQMessageConsumer
             *     #ActiveMQMessageConsumer(...)]，发送给broker构造方法createConsumer(Destination destination,
             *     String messageSelector)可以过滤需要消费的内容
             *  2、 this.session.addConsumer(this);
             *      this.session.syncSendPacket(info);
             */
            consumer = session.createConsumer(queue);
            //6、主动拉取消息
            while(true){
                /**
                 * 源码分析:
                 *  1、检查是否还设置有监听，有的话会抛出异常，不允许即主动获取消息又被动监听获取消息
                 *
                 *  clearDeliveredList()
                 * 在上面的sendPullCommand方法中，会先调用clearDeliveredList方法，主要用来清理已经分发的消息链表
                 *  deliveredMessages
                 *  deliveredMessages，存储分发给消费者但还为应答的消息链表
                 *  Ø 如果session是事务的，则会遍历deliveredMessage中的消息放入到previouslyDeliveredMessage中来做重发
                 *  Ø 如果session是非事务的，根据ACK的模式来选择不同的应答操作
                 *
                 *  2、如果prefetch为0并且unconsumedMessages为空，则向broker发送一个pull command
                 *  3、从unconsumedMessages[默认SimplePriorityMessageDispatchChannel对象]中pull阻塞的获取一条消息
                 *     [unconsumedMessage表示未消费的消息，这里面预读取的消息大小为prefetchSize的值]
                 *     [也会判断消息是否过期，过期了,会存放到deliveredMessages中]
                 *     [消息是否重复接收,默认最大重试6次]
                 *  4、发送ack消息给Broker
                 *
                 *
                 *  beforeMessageIsConsumed()
                 *  这里面主要是做消息消费之前的一些准备工作，如果ACK类型不是DUPS_OK_ACKNOWLEDGE或者队列模式（简单
                 * 来说就是除了Topic和DupAck这两种情况），所有的消息先放到deliveredMessages链表的开头。并且如果当前是
                 * 事务类型的会话，则判断transactedIndividualAck，如果为true，表示单条消息直接返回ack。
                 * 否则，调用ackLater，批量应答, client端在消费消息后暂且不发送ACK，而是把它缓存下来(pendingACK)，等到这
                 * 些消息的条数达到一定阀值时，只需要通过一个ACK指令把它们全部确认；这比对每条消息都逐个确认，在性能上
                 * 要提高很多
                 *
                 * afterMessageIsConsumed()
                 * 这个方法的主要作用是执行应答操作，这里面做以下几个操作
                 * Ø 如果消息过期，则返回消息过期的ack
                 * Ø 如果是事务类型的会话，则不做任何处理
                 * Ø 如果是AUTOACK或者（DUPS_OK_ACK且是队列），并且是优化ack操作，则走批量确认ack
                 * Ø 如果是DUPS_OK_ACK，则走ackLater逻辑
                 * Ø 如果是CLIENT_ACK，则执行ackLater
                 *
                 */
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
            }
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
