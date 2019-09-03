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
             */
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            //4、创建一个destination
            Queue queue = session.createQueue("first-queue");
            //5、为queue创建一个生产者
            producer = session.createProducer(queue);
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);//设置消息非持久化
            producer.setPriority(0);//消息优先级（优先级分为10个级别,从0(最低)到9(最高).如果不设定优先级，默认级别是4. 需要注意的是，JMS provider 并不一定保证按照优先级的 顺序提交消息）
            //6、发送一个消息(消息类别：查看Message的实现类[byte[],blob,Text,map、object、stream、message(只有消息头与属性)])
            for (int i = 0; i <10 ; i++) {
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
