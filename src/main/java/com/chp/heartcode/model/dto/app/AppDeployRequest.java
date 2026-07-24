package com.chp.heartcode.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author CHP
 * @Description 部署请求类
 */
@Data
public class AppDeployRequest implements Serializable {

    private Long appId;

    private static final long serialVersionUID = 1L;
}
