package com.laow.springbootinit.utils;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.laow.springbootinit.common.ErrorCode;
import com.laow.springbootinit.exception.BusinessException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DataParser {

    // 正则表达式匹配多种可能的分隔符变体
    private static final Pattern DELIMITER_PATTERN = Pattern.compile(
            "【{5,}|】{5,}|【[】]{4,}|】[【]{4,}"
    );

    // 匹配option对象（包含函数）
    private static final Pattern OPTION_PATTERN = Pattern.compile(
            "\"option\"\\s*:\\s*(\\{.*\\})",
            Pattern.DOTALL
    );

    /**
     * 解析option对象，处理多种分隔符变体
     *
     * @param response
     * @return
     */
    public static String[] parseOptionDirect(JSONObject response) {
        // 提取choices数组
        JSONObject firstChoice = response.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message");

        // 处理option配置
        String optionStr = firstChoice.getStr("content");
        return splitByDelimiters(optionStr);
    }

    /**
     * 使用正则表达式分割字符串，处理多种分隔符变体
     *
     * @param content 包含分隔符的内容
     * @return 分割后的数组
     */
    private static String[] splitByDelimiters(String content) {
        // 尝试查找分隔符
        Matcher matcher = DELIMITER_PATTERN.matcher(content);

        // 查找第一个分隔符
        if (!matcher.find()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "未找到有效的分隔符");
        }
        int firstDelimiterStart = matcher.start();
        int firstDelimiterEnd = matcher.end();

        // 查找第二个分隔符
        if (!matcher.find()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "只找到一个分隔符，需要两个分隔符");
        }
        int secondDelimiterStart = matcher.start();
        int secondDelimiterEnd = matcher.end();

        // 提取各部分内容
        String conclusion = content.substring(0, firstDelimiterStart).trim();
        String chartConfig = content.substring(firstDelimiterEnd, secondDelimiterStart).trim();
        String additional = content.substring(secondDelimiterEnd).trim();

        return new String[]{conclusion, chartConfig, additional};
    }

    /**
     * 增强的option内容提取，处理包含函数的情况
     *
     * @param content 可能包含option的字符串
     * @return 提取到的option内容
     */
    public static String extractOptionContent(String content) {
        // 先尝试正则匹配提取option对象
        String optionContent = extractOptionWithRegex(content);
        if (optionContent != null) {
            // 修复JSON格式
            optionContent = fixCommonJsonErrors(optionContent);

            return optionContent;
        }

        // 尝试解析为JSON对象
        try {
            JSONObject jsonObject = JSONUtil.parseObj(content);

            // 检查是否存在option字段
            if (jsonObject.containsKey("option")) {
                Object optionObj = jsonObject.get("option");
                if (optionObj instanceof JSONObject) {
                    return JSONUtil.toJsonStr((JSONObject) optionObj);
                } else if (optionObj instanceof String) {
                    return (String) optionObj;
                }
                return JSONUtil.toJsonStr(optionObj);
            }

            // 检查是否已经是完整的option配置
            if (isValidOption(content)) {
                return content;
            }
        } catch (Exception e) {
            // 如果解析失败，尝试修复常见的JSON格式错误
            return fixCommonJsonErrors(content);
        }

        // 如果以上方法都失败，返回原始内容
        return content;
    }

    /**
     * 使用正则表达式提取option对象（处理包含函数的情况）
     */
    private static String extractOptionWithRegex(String content) {
        Matcher matcher = OPTION_PATTERN.matcher(content);
        if (matcher.find()) {
            String optionStr = matcher.group(1);
            // 尝试修复option字符串的格式
            return fixCommonJsonErrors(optionStr);
        }
        return null;
    }


    /**
     * 检查是否是有效的option配置
     */
    private static boolean isValidOption(String content) {
        // 检查是否包含必要的ECharts字段
        return content.contains("\"series\"") ||
                content.contains("\"xAxis\"") ||
                content.contains("\"yAxis\"");
    }

    /**
     * 修复常见的JSON格式错误
     *
     * @param jsonString 可能有格式问题的JSON字符串
     * @return 修复后的JSON字符串
     */
    private static String fixCommonJsonErrors(String jsonString) {
        // 1. 修复属性名缺少引号的问题
        String fixed = jsonString.replaceAll("(\\w+)\\s*:", "\"$1\":");

        // 2. 修复单引号问题
        fixed = fixed.replaceAll("'", "\"");

        // 3. 修复注释问题（移除单行注释）
        fixed = fixed.replaceAll("//.*", "");

        // 修复花括号匹配问题，检查是否有多余的花括号，多了的花括号删除最后一个花括号
        fixed = fixBraceMismatch(fixed);

        // 4. 尝试再次解析
        return fixed;
    }

    // 花括号匹配检查
    private static String fixBraceMismatch(String json) {
        int openCount = json.length() - json.replace("{", "").length();
        int closeCount = json.length() - json.replace("}", "").length();

        // 如果开括号比闭括号多，则移除最后一个多余的开括号
        if (openCount > closeCount) {
            int lastOpen = json.lastIndexOf("{");
            if (lastOpen != -1) {
                return json.substring(0, lastOpen) + json.substring(lastOpen + 1);
            }
        }
        if (closeCount > openCount) {
            int lastClose = json.lastIndexOf("}");  // 修正变量名
            if (lastClose != -1) {
                return json.substring(0, lastClose);
            }
        }

        return json;
    }

    // 统一调用入口
    public static void main(String[] args) {
        // 测试不同的分隔符变体
        String[] testCases = {
                "分析结论【【【【【图表配置】】】】】附加说明",
                "分析结论【】【【【图表配置】】】】附加说明",
                "分析结论】】】】】图表配置【【【【【附加说明",
                "分析结论】】】】】图表配置【】【【【附加说明"
        };

        for (String testCase : testCases) {
            try {
                System.out.println("测试用例: " + testCase);
                String[] parts = splitByDelimiters(testCase);
                System.out.println("结论: " + parts[0]);
                System.out.println("配置: " + parts[1]);
                System.out.println("附加: " + parts[2]);
                System.out.println("---------------------");
            } catch (BusinessException e) {
                System.err.println("解析失败: " + e.getMessage());
            }
        }

        // 测试JSON提取
        String responseString = "{\n" +
                "  \"option\": {\n" +
                "    \"dom\": \"chartContainer\",\n" +
                "    \"title\": {\n" +
                "      \"text\": \"房屋面积与价格关系分析\",\n" +
                "      \"left\": \"center\"\n" +
                "    },\n" +
                "    \"tooltip\": {\n" +
                "      \"trigger\": \"axis\",\n" +
                "      \"formatter\": function(params) {\n" +
                "        return `${params[0].name}<br/>面积: ${params[0].value}㎡<br/>价格: ${params[1].value}万元<br/>学区: ${params[2].value}`;\n" +
                "      }\n" +
                "    },\n" +
                "    \"legend\": {\n" +
                "      \"data\": [\"一等学区\", \"二等学区\", \"普通学区\"],\n" +
                "      \"top\": \"10\",\n" +
                "      \"right\": \"10\"\n" +
                "    },\n" +
                "    \"grid\": {\n" +
                "      \"left\": \"3%\",\n" +
                "      \"right\": \"4%\",\n" +
                "      \"bottom\": \"3%\",\n" +
                "      \"containLabel\": true\n" +
                "    },\n" +
                "    \"xAxis\": {\n" +
                "      \"type\": \"value\",\n" +
                "      \"name\": \"面积(平方米)\",\n" +
                "      \"axisLine\": {\n" +
                "        \"show\": true,\n" +
                "        \"lineStyle\": {\n" +
                "          \"color\": \"#999\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"yAxis\": {\n" +
                "      \"type\": \"value\",\n" +
                "      \"name\": \"价格(万元)\",\n" +
                "      \"axisLine\": {\n" +
                "        \"show\": true,\n" +
                "        \"lineStyle\": {\n" +
                "          \"color\": \"#999\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"series\": [{\n" +
                "      \"type\": \"line\",\n" +
                "      \"data\": [\n" +
                "        [98, 850, \"一等学区\"],\n" +
                "        [75, 620, \"二等学区\"],\n" +
                "        [120, 980, \"一等学区\"],\n" +
                "        [89, 480, \"二等学区\"],\n" +
                "        [82, 520, \"一等学区\"],\n" +
                "        [105, 320, \"普通学区\"],\n" +
                "        [135, 780, \"一等学区\"],\n" +
                "        [68, 210, \"普通学区\"],\n" +
                "        [92, 195, \"二等学区\"],\n" +
                "        [118, 380, \"普通学区\"]\n" +
                "      ],\n" +
                "      \"encode\": {\n" +
                "        \"x\": 0,\n" +
                "        \"y\": 1,\n" +
                "        \"itemName\": 2\n" +
                "      },\n" +
                "      \"symbolSize\": 10,\n" +
                "      \"lineStyle\": {\n" +
                "        \"width\": 3\n" +
                "      },\n" +
                "      \"areaStyle\": {},\n" +
                "      \"itemStyle\": {\n" +
                "        \"color\": function(params) {\n" +
                "          const colorMap = {\n" +
                "            \"一等学区\": \"#FF6B6B\",\n" +
                "            \"二等学区\": \"#4ECDC4\",\n" +
                "            \"普通学区\": \"#45B7D1\"\n" +
                "          };\n" +
                "          return colorMap[params.data[2]];\n" +
                "        }\n" +
                "      }\n" +
                "    }],\n" +
                "    \"dataset\": {\n" +
                "      \"source\": [\n" +
                "        [\"北京\", \"海淀区\", \"三室两厅\", 98, 850, \"2024-03-15\", \"一等学区\"],\n" +
                "        [\"上海\", \"浦东新区\", \"两室一厅\", 75, 620, \"2024-02-28\", \"二等学区\"],\n" +
                "        [\"深圳\", \"南山区\", \"四室两厅\", 120, 980, \"2024-01-10\", \"一等学区\"],\n" +
                "        [\"广州\", \"天河区\", \"三室一厅\", 89, 480, \"2024-04-05\", \"二等学区\"],\n" +
                "        [\"杭州\", \"西湖区\", \"两室两厅\", 82, 520, \"2024-03-22\", \"一等学区\"],\n" +
                "        [\"成都\", \"高新区\", \"三室两厅\", 105, 320, \"2024-02-18\", \"普通学区\"],\n" +
                "        [\"南京\", \"鼓楼区\", \"四室两厅\", 135, 780, \"2024-04-12\", \"一等学区\"],\n" +
                "        [\"武汉\", \"江汉区\", \"两室一厅\", 68, 210, \"2024-01-30\", \"普通学区\"],\n" +
                "        [\"西安\", \"雁塔区\", \"三室一厅\", 92, 195, \"2024-03-08\", \"二等学区\"],\n" +
                "        [\"重庆\", \"渝北区\", \"复式公寓\", 118, 380, \"2024-02-05\", \"普通学区\"]\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}";
        // 假设已获取API响应

        String option = extractOptionContent(responseString);
        System.out.println("提取的配置: " + option);
    }
}
