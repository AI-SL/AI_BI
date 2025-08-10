package com.laow.springbootinit.bizmq;

import cn.hutool.json.JSONObject;
import com.laow.springbootinit.common.ErrorCode;
import com.laow.springbootinit.common.TaskStatus;
import com.laow.springbootinit.constant.TextConstant;
import com.laow.springbootinit.exception.BusinessException;
import com.laow.springbootinit.manager.SparkX1Manager;
import com.laow.springbootinit.model.entity.Chart;
import com.laow.springbootinit.service.ChartService;
import com.laow.springbootinit.utils.DataParser;
import com.laow.springbootinit.utils.ExcelUtils;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 消费者
 */
@Component
@Slf4j
public class BiMessageConsumer {

    // 消息队列
    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private ChartService chartService;

    @Resource
    private SparkX1Manager sparkX1Manager;

    /**
     * 指定程序监听的消息队列和确认机制
     *
     * @param message
     * @param channel
     * @param deliveryTag
     */
    @SneakyThrows
    @RabbitListener(queues = {BiMqConstan.BI_QUEUE_NAME}, ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("receiveMessage message = {}", message);
        if (StringUtils.isBlank(message)) {
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, TextConstant.MESSAGE_EMPTY);
        }

        Long chartId = Long.parseLong(message);

        Chart chart = chartService.getById(chartId);

        if (chart == null){
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, TextConstant.CHART_NOT_FOUND);
        }

        Chart updateChart = new Chart();
        updateChart.setId(chart.getId());
        updateChart.setStatus(TaskStatus.RUNNING.getStatus());
        boolean runningResult = chartService.updateById(updateChart);
        if (!runningResult) {
            chartService.handleChartUpdateError(chart.getId(), TextConstant.CHART_UPDATE_RUNNING_FAILED);
            return;
        }

        // 构建用户输入，准备消息
        List<SparkX1Manager.Message> messages = chartService.buildUserInput(chart);

        log.info("buildUserInput: {}", messages);

        try {
            // 非流式调用SparkX1 AI
            JSONObject response = sparkX1Manager.chatCompletion(messages, chart.getUserId().toString());

            String[] results = DataParser.parseOptionDirect(response);
            // 这里对AI生成的内容不符合要求就不进行保存，防止脏数据，导致渲染图像错误
            if (results.length < 3) {
                channel.basicNack(deliveryTag, false, false);
                chartService.handleChartUpdateError(chart.getId(), TextConstant.AI_GENERATION_ERROR);
                return;
            }
            // 解析图表配置
            String genChart = DataParser.extractOptionContent(results[1].trim());
            // 解析返回结论
            String genResult = results[2].trim();

            Chart updateChartResult = new Chart();
            updateChartResult.setId(chart.getId());
            updateChartResult.setGenChart(genChart);
            updateChartResult.setGenResult(genResult);
            updateChartResult.setStatus(TaskStatus.SUCCESS.getStatus());
            // 处理成功后，如果之前又出现错误的情况，会在exeMessage字段存储错误信息，后面处理成功后这里就需要处理
            updateChartResult.setExecMessage("图表生成成功");
            boolean succeedResult = chartService.updateById(updateChartResult);
            if (!succeedResult) {
                channel.basicNack(deliveryTag, false, false);
                chartService.handleChartUpdateError(chart.getId(), TextConstant.CHART_UPDATE_SUCCEED_FAILED);
            }
            log.info("receiveMessage: {}", message);



            // 手动消息确认
            channel.basicAck(deliveryTag, false);
        } catch (BusinessException e) {
            // 处理业务异常（如解析错误）
            log.error("AI处理业务异常，图表ID: {}", chartId, e);
            chartService.handleChartUpdateError(chartId, e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        } catch (Exception e) {
            // 处理其他异常
            log.error("AI处理系统异常，图表ID: {}", chartId, e);
            chartService.handleChartUpdateError(chartId, TextConstant.SYSTEM_ERROR+ "：" + e.getMessage());
            channel.basicNack(deliveryTag, false, true); // 重新入队尝试
        }
    }

}
