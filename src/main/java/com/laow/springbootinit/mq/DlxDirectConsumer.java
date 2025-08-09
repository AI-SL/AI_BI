package com.laow.springbootinit.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.util.HashMap;
import java.util.Map;

public class DlxDirectConsumer {

    private static final String DEAD_EXCHANGE_NAME = "dxl-direct_exchange";

    private static final String WORK_EXCHANGE_NAME = "direct2_exchange";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        // 创建交换机
        channel.exchangeDeclare(WORK_EXCHANGE_NAME, "direct");

        // 自定死信队列参数
        Map<String, Object> args = new HashMap<String, Object>();
        // 要绑定到那个交换机
        args.put("x-dead-letter-exchange", DEAD_EXCHANGE_NAME);
        // 指定死信要转发到那个死信队列
        args.put("x-dead-letter-routing-key", "waibao");

        String queueName1 = "小猫";
        channel.queueDeclare(queueName1, false, false, false, args);
        channel.queueBind(queueName1, WORK_EXCHANGE_NAME, "xiaom");

        // 指定死信队列的参数
        Map<String, Object> args2 = new HashMap<String, Object>();
        args2.put("x-dead-letter-exchange", DEAD_EXCHANGE_NAME);
        args2.put("x-dead-letter-routing-key", "laoban");

        String queueName2 = "小够";
        channel.queueDeclare(queueName2, false, false, false, args2);
        channel.queueBind(queueName2, WORK_EXCHANGE_NAME, "xiaog");


        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        DeliverCallback xiaomdeliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            // 拒绝
            channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
            System.out.println(" [小猫] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };

        DeliverCallback xiaogdeliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
            System.out.println(" [小够] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };

        channel.basicConsume(queueName1, false, xiaomdeliverCallback, consumerTag -> {
        });
        channel.basicConsume(queueName2, false, xiaogdeliverCallback, consumerTag -> {
        });


    }
}