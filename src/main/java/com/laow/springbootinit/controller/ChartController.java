package com.laow.springbootinit.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.laow.springbootinit.annotation.AuthCheck;
import com.laow.springbootinit.common.BaseResponse;
import com.laow.springbootinit.common.DeleteRequest;
import com.laow.springbootinit.common.ErrorCode;
import com.laow.springbootinit.common.ResultUtils;
import com.laow.springbootinit.constant.UserConstant;
import com.laow.springbootinit.exception.BusinessException;
import com.laow.springbootinit.exception.ThrowUtils;
import com.laow.springbootinit.manager.RedisLimiterManager;
import com.laow.springbootinit.manager.SparkX1Manager;
import com.laow.springbootinit.model.dto.chart.*;
import com.laow.springbootinit.model.entity.Chart;
import com.laow.springbootinit.model.entity.User;
import com.laow.springbootinit.model.vo.BiResponse;
import com.laow.springbootinit.service.ChartService;
import com.laow.springbootinit.service.UserService;
import com.laow.springbootinit.utils.DataParser;
import com.laow.springbootinit.utils.ExcelUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 图表接口
 *
 * @author <a href="https://github.com/AI-SL">laow</a>
 * @from laow
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;


    @Resource
    private SparkX1Manager sparkX1Manager;

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    // region 增删改查

    /**
     * 创建
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);
        User loginUser = userService.getLoginUser(request);

        chart.setUserId(loginUser.getId());
        chart.setChartData(JSONUtil.toJsonStr(chart.getChartData()));
        chart.setChartType(chart.getChartType());
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param chartUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);
        long id = chartUpdateRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Chart> getChartById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }

    /**
     * 分页获取列表（仅管理员）
     *
     * @param chartQueryRequest
     * @return
     */
    @PostMapping("/admin/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                chartService.getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                     HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                chartService.getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                       HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                chartService.getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    // endregion

    /**
     * 编辑（用户）
     *
     * @param chartEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartEditRequest, chart);
        User loginUser = userService.getLoginUser(request);
        long id = chartEditRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 智能生成图表(异步)
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen")
    public BaseResponse<BiResponse> genChartByAi(@RequestPart("file") MultipartFile multipartFile,
                                                 GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();

        // 校验
        ThrowUtils.throwIf(StringUtils.isAnyBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isAnyBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");

        // 校验文件
        //获取文件大小
        long size = multipartFile.getSize();
        //获取文件名
        String originalFilename = multipartFile.getOriginalFilename();
        // 1M的大小
        final long ONE_M = 1 * 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_M, ErrorCode.PARAMS_ERROR, "文件超过1MB");
        // 校验文件后缀
        String fileSuffix = FileUtil.extName(originalFilename);
        final List<String> validFileSuffixList = Arrays.asList("xlsx", "xls");
        ThrowUtils.throwIf(!validFileSuffixList.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "文件格式错误");

        // 获取当前登录用户ID
        User loginUser = userService.getLoginUser(request);

        // 限流判断，每个用户一个限流器
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());

        final String prompt = "你是一名专业的数据分析师和前端开发专家（精通ECharts V5）。接下来，请严格根据我按以下格式提供的任务执行分析：\n" +
                "\n" +
                "---\n" +
                "分析需求：\n" +
                "{请清晰、具体地描述你的数据分析目标或问题。例如：“分析过去一年每月销售额趋势”，“比较不同产品线在各地区的市场份额”，“识别客户购买行为中的关键模式”，“计算关键绩效指标KPI如平均订单价值、客户生命周期价值”等。}\n" +
                "原始数据：\n" +
                "{提供标准的CSV格式文本数据。要求：\n" +
                "使用逗号`,`分隔字段。\n" +
                "包含逗号或换行符的字段必须用双引号`\"`包裹。\n" +
                "缺失值请用空字符串`\"\"`或`null`表示。\n" +
                "第一行必须为列名（表头）。\n" +
                "确保数据格式正确无误。}\n" +
                "\n" +
                "请基于提供的“分析需求”和“原始数据”：\n" +
                "1. 进行必要的数据处理(如清洗、筛选、聚合、排序、计算新指标等)。\n" +
                "2. 选择最合适的ECharts图表类型进行数据可视化或者根据用户的指定进行可视化。\n" +
                "3. 生成分析结论。\n" +
                "\n" +
                "你的输出必须且仅包含以下两部分，绝对不要有任何其他内容（如注释、解释、问候语、Markdown代码块标记等，特别是【【【【【这样的两组分隔符输出时要按照我给的样式进行输出，不要发生改变）：**\n" +
                "\n" +
                "【【【【【\n" +
                "{完整且可直接替换option后能运行的ECharts V6 `option`配置代码。要求：\n" +
                "可以直接用到<ReactECharts option={option}/>中。" +
                "代码必须是**纯粹**的Json对象,只需要option对应的数据，比如\\{\"option\":\\{对应参数\\}\\}类似这样的数据，我需要的是你给出里面option后面的对应的数据，包括外面的\\{对应参数\\}这样的格式。" +
                "必须包含所有必要的数据处理逻辑(在`dataset`或`series.data`中体现)。\n" +
                "图表默认渲染在`dom: 'chartContainer'`元素上(用户需在HTML中准备)，如果有图例，请统一放在图的右上角,不要给图表设置名称。\n" +
                "确保可视化方案**合理、清晰、有效**地服务于分析需求。}" +
                "返回的option配置示例(不要使用new echarts.graphic.LinearGradient这个渐变的函数)" +
                "对option的配置进行校验，然后要确定没有错误，ECharts使用时可以显示图像" +
                "如果在校验的时候发现存在错误或new echarts.graphic.LinearGradient函数，请重新生成，确保没有这个函数并且保持与下面的示例格式相同。\n" +
                "{\n" +
                "  xAxis: {\n" +
                "    type: 'category',\n" +
                "    data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']\n" +
                "  },\n" +
                "  yAxis: {\n" +
                "    type: 'value'\n" +
                "  },\n" +
                "  series: [\n" +
                "    {\n" +
                "      data: [150, 230, 224, 218, 135, 147, 260],\n" +
                "      type: 'line'\n" +
                "    }\n" +
                "  ]\n" +
                "};\n" +
                "【【【【【\n" +
                "{**具体、清晰、有洞察力**的数据分析结论。要求：\n" +
                "* **严格基于**处理后的数据和生成的可视化结果。\n" +
                "* 详细阐述关键发现、主要趋势、显著模式、异常点或重要比较。\n" +
                "* 结论应直接回应“分析需求”中提出的目标或问题。\n" +
                "* 避免模糊、笼统的陈述。}" +
                "* 回答的结论示例：{\n" +
                "  \"关键发现\": [\n" +
                "    \"学区等级显著影响房屋定价，呈现明显的梯度特征：一等学区平均单价783.33万元，较二等学区高出93.4%，较普通学区高出216.3%\",\n" +
                "    \"二等学区与普通学区价差相对收窄，两者差价仅157.5万元，反映中等教育资源的市场估值弹性较低\",\n" +
                "    \"所有样本均来自2024年1-4月挂牌数据，未体现季节性波动因素，建议结合历史同期数据验证长期趋势\"\n" +
                "  ],\n" +
                "  \"主要趋势\": [\n" +
                "    \"教育资源质量与房价呈强正相关，优质学区溢价能力突出，一等学区房源均价接近普通学区的3倍\",\n" +
                "    \"学区分级直接影响市场定价体系，形成清晰的三级价格梯队，与我国现行教育资源配置政策高度吻合\"\n" +
                "  ],\n" +
                "  \"异常点提示\": [\n" +
                "    \"成都高新区（普通学区）出现320万元的较高挂牌价，需核查该区域是否存在特殊配套资源或政策利好\",\n" +
                "    \"武汉江汉区（普通学区）210万元的低价与同级别重庆渝北区380万元存在显著差异，建议关注城市能级和地段因素的影响\"\n" +
                "  ]\n" +
                "}\n" +
                "\n" +
                "**重要原则：**\n" +
                "* **纯净输出：** 除了上述两部分，**绝对不生成任何多余字符**。\n" +
                "* **可行性检查：** 如果数据格式错误、需求无法理解或基于提供数据无法实现需求，**立即停止并清晰告知用户问题所在**，不生成代码和结论。\n" +
                "* **实事求是：** 仅分析用户提供的数据，不虚构数据或进行超出数据支持范围的推测。";
        // 构造用户输入
        StringBuilder userInput = new StringBuilder();

        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType))
            userGoal += "，请使用" + chartType;
        userInput.append(prompt).append("\n");
        userInput.append("分析需求：").append(userGoal).append("\n");

        // 读取到用户上传的excel文件，进行一个处理
        String csvData = ExcelUtils.exceltoCsv(multipartFile);
        userInput.append("原始数据：").append(csvData).append("\n");
        // 插入数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setStatus("wait");
        chart.setUserId(loginUser.getId());
        boolean saveResult = chartService.updateById(chart);
        if (!saveResult) {
            handleChartUpdateError(chart.getId(), "保存图表原始数据失败");
        }
        Chart updateChart = new Chart();
        updateChart.setId(chart.getId());
        updateChart.setStatus("running");
        boolean runningResult = chartService.updateById(updateChart);
        if (!runningResult) {
            handleChartUpdateError(chart.getId(), "更新图表执行中状态失败");
            BiResponse biResponse = new BiResponse();
            biResponse.setChartId(chart.getId());

            return ResultUtils.success(biResponse);
        }
        // 准备消息
        List<SparkX1Manager.Message> messages = new ArrayList<>();
        messages.add(new SparkX1Manager.Message("user", userInput.toString()));

        // 非流式调用SparkX1 AI
        JSONObject response = sparkX1Manager.chatCompletion(messages, loginUser.getId().toString());

        String[] results = DataParser.parseOptionDirect(response);
        // 这里对AI生成的内容不符合要求就不进行保存，防止脏数据，导致渲染图像错误
        if (results.length < 3) {
            handleChartUpdateError(chart.getId(), "SparkX1 AI生成错误");
            BiResponse biResponse = new BiResponse();
            biResponse.setChartId(chart.getId());

            return ResultUtils.success(biResponse);
        }
        // 解析图表配置
        String genChart = DataParser.extractOptionContent(results[1].trim());
        // 解析返回结论
        String genResult = results[2].trim();

        // 插入数据库
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chart.getId());
        updateChartResult.setGenChart(genChart);
        updateChartResult.setGenResult(genResult);
        updateChartResult.setStatus("succeed");
        boolean succeedResult = chartService.updateById(updateChartResult);
        if (!succeedResult) {
            handleChartUpdateError(chart.getId(), "更新图表已完成状态失败");
        }
        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());

        return ResultUtils.success(biResponse);
    }

    /**
     * 智能生成图表(异步)
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen")
    public BaseResponse<BiResponse> genChartByAiAsync(@RequestPart("file") MultipartFile multipartFile,
                                                 GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();

        // 校验
        ThrowUtils.throwIf(StringUtils.isAnyBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isAnyBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");

        // 校验文件
        //获取文件大小
        long size = multipartFile.getSize();
        //获取文件名
        String originalFilename = multipartFile.getOriginalFilename();
        // 1M的大小
        final long ONE_M = 1 * 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_M, ErrorCode.PARAMS_ERROR, "文件超过1MB");
        // 校验文件后缀
        String fileSuffix = FileUtil.extName(originalFilename);
        final List<String> validFileSuffixList = Arrays.asList("xlsx", "xls");
        ThrowUtils.throwIf(!validFileSuffixList.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "文件格式错误");

        // 获取当前登录用户ID
        User loginUser = userService.getLoginUser(request);

        // 限流判断，每个用户一个限流器
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());

        final String prompt = "你是一名专业的数据分析师和前端开发专家（精通ECharts V5）。接下来，请严格根据我按以下格式提供的任务执行分析：\n" +
                "\n" +
                "---\n" +
                "分析需求：\n" +
                "{请清晰、具体地描述你的数据分析目标或问题。例如：“分析过去一年每月销售额趋势”，“比较不同产品线在各地区的市场份额”，“识别客户购买行为中的关键模式”，“计算关键绩效指标KPI如平均订单价值、客户生命周期价值”等。}\n" +
                "原始数据：\n" +
                "{提供标准的CSV格式文本数据。要求：\n" +
                "使用逗号`,`分隔字段。\n" +
                "包含逗号或换行符的字段必须用双引号`\"`包裹。\n" +
                "缺失值请用空字符串`\"\"`或`null`表示。\n" +
                "第一行必须为列名（表头）。\n" +
                "确保数据格式正确无误。}\n" +
                "\n" +
                "请基于提供的“分析需求”和“原始数据”：\n" +
                "1. 进行必要的数据处理(如清洗、筛选、聚合、排序、计算新指标等)。\n" +
                "2. 选择最合适的ECharts图表类型进行数据可视化或者根据用户的指定进行可视化。\n" +
                "3. 生成分析结论。\n" +
                "\n" +
                "你的输出必须且仅包含以下两部分，绝对不要有任何其他内容（如注释、解释、问候语、Markdown代码块标记等，特别是【【【【【这样的两组分隔符输出时要按照我给的样式进行输出，不要发生改变）：**\n" +
                "\n" +
                "【【【【【\n" +
                "{完整且可直接替换option后能运行的ECharts V6 `option`配置代码。要求：\n" +
                "可以直接用到<ReactECharts option={option}/>中。" +
                "代码必须是**纯粹**的Json对象,只需要option对应的数据，比如\\{\"option\":\\{对应参数\\}\\}类似这样的数据，我需要的是你给出里面option后面的对应的数据，包括外面的\\{对应参数\\}这样的格式。" +
                "必须包含所有必要的数据处理逻辑(在`dataset`或`series.data`中体现)。\n" +
                "图表默认渲染在`dom: 'chartContainer'`元素上(用户需在HTML中准备)，如果有图例，请统一放在图的右上角,不要给图表设置名称。\n" +
                "确保可视化方案**合理、清晰、有效**地服务于分析需求。}" +
                "返回的option配置示例(不要使用new echarts.graphic.LinearGradient这个渐变的函数)" +
                "对option的配置进行校验，然后要确定没有错误，ECharts使用时可以显示图像" +
                "如果在校验的时候发现存在错误或new echarts.graphic.LinearGradient函数，请重新生成，确保没有这个函数并且保持与下面的示例格式相同。\n" +
                "{\n" +
                "  xAxis: {\n" +
                "    type: 'category',\n" +
                "    data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']\n" +
                "  },\n" +
                "  yAxis: {\n" +
                "    type: 'value'\n" +
                "  },\n" +
                "  series: [\n" +
                "    {\n" +
                "      data: [150, 230, 224, 218, 135, 147, 260],\n" +
                "      type: 'line'\n" +
                "    }\n" +
                "  ]\n" +
                "};\n" +
                "【【【【【\n" +
                "{**具体、清晰、有洞察力**的数据分析结论。要求：\n" +
                "* **严格基于**处理后的数据和生成的可视化结果。\n" +
                "* 详细阐述关键发现、主要趋势、显著模式、异常点或重要比较。\n" +
                "* 结论应直接回应“分析需求”中提出的目标或问题。\n" +
                "* 避免模糊、笼统的陈述。}" +
                "* 回答的结论示例：{\n" +
                "  \"关键发现\": [\n" +
                "    \"学区等级显著影响房屋定价，呈现明显的梯度特征：一等学区平均单价783.33万元，较二等学区高出93.4%，较普通学区高出216.3%\",\n" +
                "    \"二等学区与普通学区价差相对收窄，两者差价仅157.5万元，反映中等教育资源的市场估值弹性较低\",\n" +
                "    \"所有样本均来自2024年1-4月挂牌数据，未体现季节性波动因素，建议结合历史同期数据验证长期趋势\"\n" +
                "  ],\n" +
                "  \"主要趋势\": [\n" +
                "    \"教育资源质量与房价呈强正相关，优质学区溢价能力突出，一等学区房源均价接近普通学区的3倍\",\n" +
                "    \"学区分级直接影响市场定价体系，形成清晰的三级价格梯队，与我国现行教育资源配置政策高度吻合\"\n" +
                "  ],\n" +
                "  \"异常点提示\": [\n" +
                "    \"成都高新区（普通学区）出现320万元的较高挂牌价，需核查该区域是否存在特殊配套资源或政策利好\",\n" +
                "    \"武汉江汉区（普通学区）210万元的低价与同级别重庆渝北区380万元存在显著差异，建议关注城市能级和地段因素的影响\"\n" +
                "  ]\n" +
                "}\n" +
                "\n" +
                "**重要原则：**\n" +
                "* **纯净输出：** 除了上述两部分，**绝对不生成任何多余字符**。\n" +
                "* **可行性检查：** 如果数据格式错误、需求无法理解或基于提供数据无法实现需求，**立即停止并清晰告知用户问题所在**，不生成代码和结论。\n" +
                "* **实事求是：** 仅分析用户提供的数据，不虚构数据或进行超出数据支持范围的推测。";
        // 构造用户输入
        StringBuilder userInput = new StringBuilder();

        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType))
            userGoal += "，请使用" + chartType;
        userInput.append(prompt).append("\n");
        userInput.append("分析需求：").append(userGoal).append("\n");

        // 读取到用户上传的excel文件，进行一个处理
        String csvData = ExcelUtils.exceltoCsv(multipartFile);
        userInput.append("原始数据：").append(csvData).append("\n");

        // 插入数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setStatus("wait");
        chart.setUserId(loginUser.getId());

        boolean saveResult = chartService.save(chart);
        if (!saveResult) {
            handleChartUpdateError(chart.getId(), "保存图表原始数据失败");
        }

        // todo 要处理任务队列满了后抛异常的情况
        CompletableFuture.runAsync(() -> {

            Chart updateChart = new Chart();
            updateChart.setId(chart.getId());
            updateChart.setStatus("running");
            boolean runningResult = chartService.updateById(updateChart);
            if (!runningResult) {
                handleChartUpdateError(chart.getId(), "更新图表执行中状态失败");
                return;
            }
            // 准备消息
            List<SparkX1Manager.Message> messages = new ArrayList<>();
            messages.add(new SparkX1Manager.Message("user", userInput.toString()));

            // 非流式调用SparkX1 AI
            JSONObject response = sparkX1Manager.chatCompletion(messages, loginUser.getId().toString());

            String[] results = DataParser.parseOptionDirect(response);
            // 这里对AI生成的内容不符合要求就不进行保存，防止脏数据，导致渲染图像错误
            if (results.length < 3) {
                handleChartUpdateError(chart.getId(), "SparkX1 AI生成错误");
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
            updateChartResult.setStatus("succeed");
            boolean succeedResult = chartService.updateById(updateChartResult);
            if (!succeedResult) {
                handleChartUpdateError(chart.getId(), "更新图表已完成状态失败");
            }
        }, threadPoolExecutor);

        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());

        return ResultUtils.success(biResponse);
    }

    private void handleChartUpdateError(Long chartId, String execMessage) {
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chartId);
        updateChartResult.setStatus("failed");
        updateChartResult.setExecMessage(execMessage);
        boolean result = chartService.updateById(updateChartResult);
        if (!result) {
            log.error("更新图表状态失败" + chartId + ", " + execMessage);
        }
    }

}
