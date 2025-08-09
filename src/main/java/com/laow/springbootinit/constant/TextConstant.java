package com.laow.springbootinit.constant;

/**
 * 系统文本常量
 * 包含系统中使用的各种错误提示和消息文本
 */
public interface TextConstant {

    // 分隔符相关错误信息
    /** 分隔符数量不足 */
    String DELIMITER_NOT_ENOUGH = "只找到一个分隔符，需要两个分隔符";
    /** 未找到有效分隔符 */
    String DELIMITER_NOT_FOUND = "未找到有效的分隔符";

    // 文件相关错误信息
    /** 目标为空 */
    String FILE_GOAL_EMPTY = "目标为空";
    /** 名称过长 */
    String FILE_NAME_TOO_LONG = "名称过长";
    /** 文件超过大小限制 */
    String FILE_SIZE_EXCEEDED = "文件超过1MB";
    /** 文件格式错误 */
    String FILE_FORMAT_ERROR = "文件格式错误";

    // 消息队列相关错误信息
    /** 消息为空 */
    String MESSAGE_EMPTY = "消息为空";

    // 图表相关错误信息
    /** 图表不存在 */
    String CHART_NOT_FOUND = "图表不存在";
    /** 图表状态更新失败 - 执行中 */
    String CHART_UPDATE_RUNNING_FAILED = "更新图表执行中状态失败";
    /** 图表状态更新失败 - 已完成 */
    String CHART_UPDATE_SUCCEED_FAILED = "更新图表已完成状态失败";
    /** 图表原始数据保存失败 */
    String CHART_SAVE_ORIGINAL_FAILED = "保存图表原始数据失败";

    // 系统及AI相关错误信息
    /** 系统内部异常 */
    String SYSTEM_ERROR = "系统内部异常";
    /** AI生成错误 */
    String AI_GENERATION_ERROR = "SparkX1 AI生成错误";
}
