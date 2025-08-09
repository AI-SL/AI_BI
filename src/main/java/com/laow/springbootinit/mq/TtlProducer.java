package com.laow.springbootinit.mq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class TtlProducer {

    private final static String QUEUE_NAME = "ttl_queue";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        // 建立连接、创建频道
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            String message = "Hello World!";

            /*
            // 设置队列延时
            Map<String, Object> args = new HashMap<String, Object>();
            args.put("x-message-ttl", 5000);
            // 创建队列声明
            channel.queueDeclare(QUEUE_NAME, false, false, false, args);
            */

            // 设置消息延时
            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                    .expiration("1000")
                    .build();

            channel.queueDeclare(QUEUE_NAME, false, false, false, null);

            channel.basicPublish("", "", properties, message.getBytes(StandardCharsets.UTF_8));
            System.out.println(" [x] Sent '" + message + "'");

        }

    }
}
