package cn.zcp.activemq;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Hello world!
 */
public class AppMain {
    public static void main(String[] args)throws Exception {

        ApplicationContext ac = new ClassPathXmlApplicationContext("classpath:spring-activemq.xml");

        System.in.read();
    }
}
