//package com.laow.springbootinit.utils;
//
//import cn.hutool.json.JSONObject;
//import com.laow.springbootinit.manager.SparkX1Manager;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import java.util.ArrayList;
//import java.util.List;
//
//@Component
//public class shili {
//
//    @Autowired
//    private SparkX1Manager client;
//    public static void main(String[] args) {
//
//
//// 准备消息
//        List<SparkX1Manager.Message> messages = new ArrayList<>();
//        messages.add(new SparkX1Manager.Message("user", "你好，介绍一下你自己"));
//
//// 流式调用
//        client.streamChatCompletion(messages, "user123", new SparkX1Manager.StreamCallback() {
//            private final StringBuilder fullResponse = new StringBuilder();
//
//            @Override
//            public void onChunk(JSONObject chunk) {
//                // 处理每个数据块
//                JSONObject choice = chunk.getJSONArray("choices").getJSONObject(0);
//                JSONObject delta = choice.getJSONObject("delta");
//
//                // 处理推理内容
//                if (delta.containsKey("reasoning_content")) {
//                    String reasoning = delta.getStr("reasoning_content");
//                    System.out.print("[推理] " + reasoning);
//                }
//
//                // 处理实际回复内容
//                if (delta.containsKey("content")) {
//                    String content = delta.getStr("content");
//                    System.out.print(content);
//                    fullResponse.append(content);
//                }
//            }
//
//            @Override
//            public void onComplete() {
//                System.out.println("\n\n------ 完整回复 ------\n" + fullResponse);
//            }
//
//            @Override
//            public void onError(Throwable t) {
//                System.err.println("请求出错: " + t.getMessage());
//                t.printStackTrace();
//            }
//        });
//
//// 非流式调用示例
//        JSONObject response = client.chatCompletion(messages, "user123");
//        System.out.println("非流式响应: " + response);
//    }
//}
