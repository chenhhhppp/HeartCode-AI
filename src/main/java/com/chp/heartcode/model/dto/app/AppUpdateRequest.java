package com.chp.heartcode.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: CHP
 * @Description: 应用更新请求
 */
@Data
public class AppUpdateRequest implements Serializable {

    /**
     * 应用 id
     */
    private Long id;

    /**
     * 应用名称
     */
    private String appName;

    private static final long serialVersionUID = 1L;
}
