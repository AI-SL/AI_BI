package com.laow.springbootinit.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class TopicConsumer {

    private static final String EXCHANGE_NAME = "topic_exchange";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, "topic");
        String queueName1 = "前端";
        channel.queueDeclare(queueName1, false, false, false, null);
        channel.queueBind(queueName1, EXCHANGE_NAME, "*.*.前端");

        String queueName2 = "后端";
        channel.queueDeclare(queueName2, false, false, false, null);
        channel.queueBind(queueName2, EXCHANGE_NAME, "*.*.后端");

        String queueName3 = "产品";
        channel.queueDeclare(queueName3, false, false, false, null);
        channel.queueBind(queueName3, EXCHANGE_NAME, "*.产品.*");


        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        DeliverCallback qianduandeliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [前端] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };

        DeliverCallback houduanliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [后端] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };
        DeliverCallback chanpinliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [产品] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };

        channel.basicConsume(queueName1, true, qianduandeliverCallback, consumerTag -> {
        });
        channel.basicConsume(queueName2, true, houduanliverCallback, consumerTag -> {
        });
        channel.basicConsume(queueName3, true, chanpinliverCallback, consumerTag -> {
        });
    }
}