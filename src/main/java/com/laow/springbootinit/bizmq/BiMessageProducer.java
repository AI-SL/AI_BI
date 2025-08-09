package com.laow.springbootinit.bizmq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 自定义消息发送者
 */
@Component
public class BiMessageProducer {

    // 消息队列
    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送消息
     * @param message
     */
    public void sendMessage(String message) {
        rabbitTemplate.convertAndSend(BiMqConstan.BI_EXCHANGE_NAME, BiMqConstan.BI_ROUTING_KEY, message);
    }
}
