package com.laow.springbootinit.common;

/**
 * 自定义错误码
 *
 * @author <a href="https://github.com/AI-SL">laow</a>
 * @from laow
 */
public enum TaskStatus {

    WAIT("wait"),
    RUNNING("running"),
    SUCCESS("succeed"),
    FAILED("failed");

    private final String status;

    TaskStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}