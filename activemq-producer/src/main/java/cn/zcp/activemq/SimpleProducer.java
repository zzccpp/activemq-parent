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
 * 创建一个简单生产者
 */
public class SimpleProducer {
    static Logger logger = LoggerFactory.getLogger(SimpleProducer.class);

    public static void main(String[] args) {
        Connection connection=null;
        Session session= null;
        MessageProducer producer=null;
        try {
            String brockURL = "tcp://192.168.81.240:61616";//?jms.sendTimeout=3000   failover://
            //1、创建connectionFactory
            /**源码分析：
             * 1.1、创建ActiveMQConnectionFactory对象,初始化参数
             * 1.2、处理连接后面带的参数，通过反射设置ActiveMQPrefetchPolicy、RedeliveryPolicy、
             *     BlobTransferPolicy、ActiveMQConnectionFactory中成员变量
             */
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brockURL);
            //2、获取一个连接(ActiveMQConnection)
            /**
             * 源码分析:
             * 2.1根据url中Scheme 创建对应的Transport[逻辑：类似于SPI,通过FactoryFinder去META-INF/services/org/apache/
             *    activemq/transport/Scheme获取对应文件中配置通过反射创建TransportFactory对象]，如果这里我们使用的是TCP,则会创建一个TCPTransportFactory,
             *    而后会创建一个TcpTransport[一个线程,创建一个socket客户端连接，并且设置为守护线程]
             *    对TcpTransport进行包装(链式编程)  ResponseCorrelator(MutexTransport(WireFormatNegotiator(InactivityMonitor(TcpTransport
             *    最终会调用org.apache.activemq.transport.tcp.TcpTransport#doStart()方法来去连接服务，并且开启一个线程去读写数据
             * 2.2创建一个createActiveMQConnection，并设置连接的一些属性与监听器等
             */
            connection = connectionFactory.createConnection();
            /**
             * 源码分析：
             * 检查是否连接上服务器,确保连接的信息发送过去
             * 创建一个临时Topic ActiveMQ.Advisory.TempQueue,ActiveMQ.Advisory.TempTopic 可以在管理界面Connections中Connector openwire查看
             */
            connection.start();
            //3、创建一个回话(ActiveMQSession)
            //参数1、transacted 是否开启事物  2、提交方式[AUTO_ACKNOWLEDGE、CLIENT_ACKNOWLEDGE、DUPS_OK_ACKNOWLEDGE]
            //额外的一种提交方式，SESSION_TRANSACTED 不是在这里传入,而是通过connectionFactory.setTransactedIndividualAck();来设置
            /**
             * 源码分析：
             * 创建一个ActiveMQSession对象，如果transacted为true，后面参数没有用(使用默认Session.SESSION_TRANSACTED)
             * 最后把Session信息发送给Broker
             *
             * 下面一段：https://cloud.tencent.com/developer/news/227937
             * 在支持事务的session中，producer发送message时在message中带有transaction ID。broker收到message后判断是否有transaction ID，
             * 如果有就把message保存在transaction store中，等待commit或者rollback消息。所以ActiveMq的事务是针对broker而不是producer的，
             * 不管session是否commit，broker都会收到message。
             * 如果producer发送模式选择了persistent（持久化），那么message过期后会进入死亡队列。在message进入死亡队列之前，
             * ActiveMQ会删除message中的transaction ID，这样过期的message就不在事务中了，不会保存在transaction store中，
             * 会直接进入死亡队列。具体删除transaction ID的地方是在：org.apache.activemq.util.BrokerSupport的doResend，
             * 将transaction ID保存在了originalTransactionID中，删除了transaction ID
             *
             * 在调用session.commit()    session.rollback()时候会向broker发送一条消息
             *
             */
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            //4、创建一个destination
            /**
             * 源码分析：
             * 如果队列名称不是以 ID:开头(创建一个临时队列ActiveMQTempQueue) ,创建一个ActiveMQQueue对象
             */
            Queue queue = session.createQueue("first-queue");
            //5、为queue创建一个生产者
            /**
             * 源码分析：
             * 创建一个ActiveMQMessageProducer对象；
             * 初始化一些参数如
             * WindowSize：producer发送持久化消息是同步发送，发送是阻塞的，直到收到确认。同步发送肯定是有流量控制的。
             *             producer默认是异步发送，异步发送不会等待broker的确认， 所以就需要考虑流量控制了：
             *
             * defaultDeliveryMode：默认消息持久化
             * defaultPriority：消息默认等级4
             *
             * 最后把producer初始化的信息发送给Broker
             */
            producer = session.createProducer(queue);
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);//设置消息非持久化
            producer.setPriority(0);//消息优先级（优先级分为10个级别,从0(最低)到9(最高).如果不设定优先级，默认级别是4. 需要注意的是，JMS provider 并不一定保证按照优先级的 顺序提交消息）
            //producer.setDisableMessageTimestamp(false);
            //producer.setTimeToLive(3000);//一定需要DisableMessageTimestamp为false(默认)才有意义
            //6、发送一个消息(消息类别：查看Message的实现类[byte[],blob,Text,map、object、stream、message(只有消息头与属性)])
            for (int i = 0; i <5 ; i++) {
                TextMessage textMessage = session.createTextMessage("Hello Word"+i);
                //textMessage.setJMSMessageID();JMSMessageID唯一识别每个消息的标识
                textMessage.setStringProperty("zcp","test-属性"+i);//设置消息属性
                /////textMessage.setJMSExpiration(4000);//设置过期具体时间(currentTime+TimeToLive)的值(无效,会被覆盖)
                //textMessage.setJMSRedelivered(true);
                //设置消息可延迟,定时重复发送 http://activemq.apache.org/delay-and-schedule-message-delivery
                //首先需要在activeMQ.xml配置文件中  broker节点中启用schedulerSupport=true[延迟消息不管，是否持久化，当未提交时broker都会先持久化]
                //textMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY,10000);
                /**
                 * 源码分析：org.apache.activemq.ActiveMQSession#send(...)
                 * 1、是否开启事物，如果开启则会生成一个事物ID
                 * 2、设置消息是否持久化
                 * 3、设置消息为非重发
                 * 4、是否设置过期时间(DisableMessageTimestamp需要false、TimeToLive(TTL))【持久化消息保存至默认ActiveMQ.DLQ队列(可配置),非持久话消息直接删除】
                 * ..msg.onSend(); 设置消息体和消息属性为只读，防止篡改
                 * 5、判断消息是进行异步发送还是同步发送，查看条件较多(具体看源码)
                 * [
                 *  如果onComplete没有设置，且发送超时时间小于0，且消息不需要反馈，且连接器不是同步发送模式，
                 *  且消息非持久 化或者连接器是异步发送模式 //或者存在事务id的情况下，走异步发送，否则走同步发送
                 * ]
                 *   其实最终发送都是异步调用next.oneway(command)发送至Broker，只是异步的话直接返回，同步的话，是阻塞等待Broker的返回
                 *   阻塞响应队列responseSlot 在org.apache.activemq.transport.FutureResponse#getResult(int)
                 */
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
