package com.laow.springbootinit.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.laow.springbootinit.annotation.AuthCheck;
import com.laow.springbootinit.bizmq.BiMessageProducer;
import com.laow.springbootinit.common.*;
import com.laow.springbootinit.constant.PromptConstant;
import com.laow.springbootinit.constant.TextConstant;
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

    @Resource
    private BiMessageProducer biMessageProducer;

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
        ThrowUtils.throwIf(StringUtils.isAnyBlank(goal), ErrorCode.PARAMS_ERROR, TextConstant.FILE_GOAL_EMPTY);
        ThrowUtils.throwIf(StringUtils.isAnyBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, TextConstant.FILE_NAME_TOO_LONG);

        // 校验文件
        //获取文件大小
        long size = multipartFile.getSize();
        //获取文件名
        String originalFilename = multipartFile.getOriginalFilename();
        // 1M的大小
        final long ONE_M = 1 * 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_M, ErrorCode.PARAMS_ERROR, TextConstant.FILE_SIZE_EXCEEDED);
        // 校验文件后缀
        String fileSuffix = FileUtil.extName(originalFilename);
        final List<String> validFileSuffixList = Arrays.asList("xlsx", "xls");
        ThrowUtils.throwIf(!validFileSuffixList.contains(fileSuffix), ErrorCode.PARAMS_ERROR, TextConstant.FILE_FORMAT_ERROR);

        // 获取当前登录用户ID
        User loginUser = userService.getLoginUser(request);

        // 限流判断，每个用户一个限流器
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());

        // 读取文件
        String csvData = ExcelUtils.exceltoCsv(multipartFile);

        // 插入数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setStatus(TaskStatus.WAIT.getStatus());
        chart.setUserId(loginUser.getId());
        boolean saveResult = chartService.save(chart);
        if (!saveResult) {
            chartService.handleChartUpdateError(chart.getId(), TextConstant.CHART_SAVE_ORIGINAL_FAILED);
            BiResponse biResponse = new BiResponse();
            biResponse.setChartId(chart.getId());

            return ResultUtils.success(biResponse);
        }
        Chart updateChart = new Chart();
        updateChart.setId(chart.getId());
        updateChart.setStatus(TaskStatus.RUNNING.getStatus());
        boolean runningResult = chartService.updateById(updateChart);
        if (!runningResult) {
            chartService.handleChartUpdateError(chart.getId(), TextConstant.CHART_UPDATE_RUNNING_FAILED);
            BiResponse biResponse = new BiResponse();
            biResponse.setChartId(chart.getId());

            return ResultUtils.success(biResponse);
        }
        // 准备消息
        List<SparkX1Manager.Message> messages = chartService.buildUserInput(chart);

        // 非流式调用SparkX1 AI
        JSONObject response = sparkX1Manager.chatCompletion(messages, loginUser.getId().toString());

        String[] results = DataParser.parseOptionDirect(response);
        // 这里对AI生成的内容不符合要求就不进行保存，防止脏数据，导致渲染图像错误
        if (results.length < 3) {
            chartService.handleChartUpdateError(chart.getId(), TextConstant.AI_GENERATION_ERROR + ": " + request.toString());
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
        updateChartResult.setStatus(TaskStatus.SUCCESS.getStatus());
        boolean succeedResult = chartService.updateById(updateChartResult);
        if (!succeedResult) {
            chartService.handleChartUpdateError(chart.getId(), TextConstant.CHART_UPDATE_SUCCEED_FAILED);
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
    @PostMapping("/gen/async")
    public BaseResponse<BiResponse> genChartByAiAsync(@RequestPart("file") MultipartFile multipartFile,
                                                 GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();

        // 校验
        ThrowUtils.throwIf(StringUtils.isAnyBlank(goal), ErrorCode.PARAMS_ERROR, TextConstant.FILE_GOAL_EMPTY);
        ThrowUtils.throwIf(StringUtils.isAnyBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, TextConstant.FILE_NAME_TOO_LONG);

        // 校验文件
        //获取文件大小
        long size = multipartFile.getSize();
        //获取文件名
        String originalFilename = multipartFile.getOriginalFilename();
        // 1M的大小
        final long ONE_M = 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_M, ErrorCode.PARAMS_ERROR, TextConstant.FILE_SIZE_EXCEEDED);
        // 校验文件后缀
        String fileSuffix = FileUtil.extName(originalFilename);
        final List<String> validFileSuffixList = Arrays.asList("xlsx", "xls");
        ThrowUtils.throwIf(!validFileSuffixList.contains(fileSuffix), ErrorCode.PARAMS_ERROR, TextConstant.FILE_FORMAT_ERROR);

        // 获取当前登录用户ID
        User loginUser = userService.getLoginUser(request);

        // 限流判断，每个用户一个限流器
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());


        // 读取到用户上传的excel文件，进行一个处理
        String csvData = ExcelUtils.exceltoCsv(multipartFile);


        // 插入数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setStatus(TaskStatus.WAIT.getStatus());
        chart.setUserId(loginUser.getId());

        boolean saveResult = chartService.save(chart);
        if (!saveResult) {
            chartService.handleChartUpdateError(chart.getId(), TextConstant.CHART_SAVE_ORIGINAL_FAILED);
        }

        // todo 要处理任务队列满了后抛异常的情况
        CompletableFuture.runAsync(() -> {

            Chart updateChart = new Chart();
            updateChart.setId(chart.getId());
            updateChart.setStatus(TaskStatus.RUNNING.getStatus());
            boolean runningResult = chartService.updateById(updateChart);
            if (!runningResult) {
                chartService.handleChartUpdateError(chart.getId(), TextConstant.CHART_UPDATE_RUNNING_FAILED);
                return;
            }
            // 准备消息
            List<SparkX1Manager.Message> messages = chartService.buildUserInput(chart);

            // 非流式调用SparkX1 AI
            JSONObject response = sparkX1Manager.chatCompletion(messages, loginUser.getId().toString());

            String[] results = DataParser.parseOptionDirect(response);
            // 这里对AI生成的内容不符合要求就不进行保存，防止脏数据，导致渲染图像错误
            if (results.length < 3) {
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
            boolean succeedResult = chartService.updateById(updateChartResult);
            if (!succeedResult) {
                chartService.handleChartUpdateError(chart.getId(), TextConstant.CHART_UPDATE_SUCCEED_FAILED);
            }
        }, threadPoolExecutor);

        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());

        return ResultUtils.success(biResponse);
    }

    /**
     * 智能生成图表(分布式)
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/async/mq")
    public BaseResponse<BiResponse> genChartByAiAsyncMq(@RequestPart("file") MultipartFile multipartFile,
                                                      GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();

        // 校验
        ThrowUtils.throwIf(StringUtils.isAnyBlank(goal), ErrorCode.PARAMS_ERROR, TextConstant.MESSAGE_EMPTY);
        ThrowUtils.throwIf(StringUtils.isAnyBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, TextConstant.FILE_NAME_TOO_LONG);

        // 校验文件
        //获取文件大小
        long size = multipartFile.getSize();
        //获取文件名
        String originalFilename = multipartFile.getOriginalFilename();
        // 1M的大小
        final long ONE_M = 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_M, ErrorCode.PARAMS_ERROR, TextConstant.FILE_SIZE_EXCEEDED);
        // 校验文件后缀
        String fileSuffix = FileUtil.extName(originalFilename);
        final List<String> validFileSuffixList = Arrays.asList("xlsx", "xls");
        ThrowUtils.throwIf(!validFileSuffixList.contains(fileSuffix), ErrorCode.PARAMS_ERROR, TextConstant.FILE_FORMAT_ERROR);

        // 获取当前登录用户ID
        User loginUser = userService.getLoginUser(request);

        // 限流判断，每个用户一个限流器
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());


        // 读取到用户上传的excel文件，进行一个处理
        String csvData = ExcelUtils.exceltoCsv(multipartFile);

        // 插入数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setStatus(TaskStatus.WAIT.getStatus());
        chart.setUserId(loginUser.getId());

        boolean saveResult = chartService.save(chart);
        if (!saveResult) {
            chartService.handleChartUpdateError(chart.getId(), TextConstant.CHART_SAVE_ORIGINAL_FAILED);
        }


        Long newChartId = chart.getId();
        biMessageProducer.sendMessage(String.valueOf(newChartId));

        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());

        return ResultUtils.success(biResponse);
    }

}
