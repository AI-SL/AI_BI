package com.laow.springbootinit.mq;

import com.rabbitmq.client.*;

public class DirectConsumer {

  private static final String EXCHANGE_NAME = "direct_exchange";

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();

    // 创建交换机
    channel.exchangeDeclare(EXCHANGE_NAME, "direct");

    String queueName1 = "小网";
    channel.queueDeclare(queueName1, false, false, false, null);
    channel.queueBind(queueName1, EXCHANGE_NAME,"xiaow");

    String queueName2 = "小皮";
    channel.queueDeclare(queueName2, false, false, false, null);
    channel.queueBind(queueName2, EXCHANGE_NAME,"xiaop");


    System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

    DeliverCallback xiaowdeliverCallback = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), "UTF-8");
        System.out.println(" [小网] Received '" +
            delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
    };

    DeliverCallback xiaopdeliverCallback = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), "UTF-8");
        System.out.println(" [小皮] Received '" +
            delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
    };

    channel.basicConsume(queueName1, true, xiaowdeliverCallback, consumerTag -> { });
    channel.basicConsume(queueName2, true, xiaopdeliverCallback, consumerTag -> { });
  }
}