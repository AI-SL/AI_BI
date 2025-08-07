package com.laow.springbootinit.manager;

import cn.hutool.core.io.IoUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONException;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.laow.springbootinit.config.SparkX1Config;
import lombok.Getter;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 重构后的Spark X1 API客户端工具类
 */
@Service
public class SparkX1Manager {

    @Resource
    private SparkX1Config sparkX1Config;

    /**
     * 非流式调用API
     */
    public JSONObject chatCompletion(List<Message> messages, String userId) {
        String url = sparkX1Config.getBaseUrl() + "/chat/completions";

        JSONObject body = buildRequestBody(messages, userId, false);

        try {
            HttpURLConnection conn = createConnection(url);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes());
                os.flush();
            }

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("API请求失败: " + conn.getResponseCode() + " - " +
                        IoUtil.read(conn.getErrorStream(), "UTF-8"));
            }

            String response = IoUtil.read(conn.getInputStream(), "UTF-8");
            return JSONUtil.parseObj(response);
        } catch (Exception e) {
            throw new RuntimeException("API调用异常", e);
        }
    }

    /**
     * 流式调用API (SSE)
     */
    public void streamChatCompletion(List<Message> messages, String userId, StreamCallback callback) {
        try {
            HttpURLConnection conn = createConnection(sparkX1Config.getBaseUrl() + "/chat/completions");
            conn.setRequestProperty("Accept", "text/event-stream");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(buildRequestBody(messages, userId, true).toString().getBytes());
            }

            // 错误处理增强
            if (conn.getResponseCode() != 200) {
                try (InputStream es = conn.getErrorStream()) {
                    String errorBody = es != null ? IoUtil.read(es, "UTF-8") : "无错误详情";
                    callback.onError(new SparkException(conn.getResponseCode(), errorBody));
                }
                return;
            }

            // SSE解析增强
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data:")) {
                        String eventData = line.substring(5).trim();
                        if ("[DONE]".equals(eventData)) {
                            break;
                        }
                        try {
                            callback.onChunk(JSONUtil.parseObj(eventData));
                        } catch (Exception e) {
                            callback.onError(new JSONException("JSON解析失败: " + eventData, e));
                        }
                    }
                }
                callback.onComplete();
            }
        } catch (Exception e) {
            callback.onError(e);
        }
    }

    // 自定义异常类
    public static class SparkException extends RuntimeException {
        private final int statusCode;

        public SparkException(int statusCode, String body) {
            super("API错误[" + statusCode + "]: " + body);
            this.statusCode = statusCode;
        }
    }

    private HttpURLConnection createConnection(String url) throws Exception {
        URL obj = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + sparkX1Config.getApiKey());
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(120000); // 流式响应需要较长时间
        return conn;
    }

    private JSONObject buildRequestBody(List<Message> messages, String userId, boolean stream) {
        JSONObject body = new JSONObject();
        body.set("model", sparkX1Config.getModel());
        body.set("user", userId);
        body.set("max_tokens", sparkX1Config.getMaxTokens());
        body.set("temperature", sparkX1Config.getTemperature());
        body.set("top_p", sparkX1Config.getTopP());
        body.set("stream", stream);

        JSONArray messagesArray = new JSONArray();
        for (Message msg : messages) {
            JSONObject msgObj = new JSONObject();
            msgObj.set("role", msg.getRole());
            msgObj.set("content", msg.getContent());
            messagesArray.add(msgObj);
        }
        body.set("messages", messagesArray);
        return body;
    }

    @Getter
    public static class Message {
        private final String role;
        private final String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

    }

    public interface StreamCallback {
        void onChunk(JSONObject chunk);

        void onComplete();

        void onError(Throwable t);
    }
}
