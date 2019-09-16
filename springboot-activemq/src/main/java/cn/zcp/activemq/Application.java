package cn.zcp.activemq;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class Application {
    static Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {

        logger.info("启动....");
        ConfigurableApplicationContext ca = SpringApplication.run(Application.class, args);

        String[] beanNames = ca.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            System.out.println("BeanDefinition...."+beanName);
        }

        MsgProducer producer = ca.getBean(MsgProducer.class);

        producer.doSendMsg();

        logger.info("发送消息完成....");
    }
}
