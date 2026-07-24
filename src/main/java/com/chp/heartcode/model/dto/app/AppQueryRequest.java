package com.chp.heartcode.model.dto.app;

import com.chp.heartcode.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @Author chp
 * @Description 查询请求类
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AppQueryRequest extends PageRequest implements Serializable {

    /**
     * 应用id
     */
    private Long id;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 应用封面
     */
    private String cover;

    /**
     * 初始化提示
     */
    private String initPrompt;

    /**
     * 代码生成类型
     */
    private String codeGenType;

    /**
     * 部署key
     */
    private String deployKey;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 用户id
     */
    private Long userId;

    private static final long serialVersionUID = 1L;
}
