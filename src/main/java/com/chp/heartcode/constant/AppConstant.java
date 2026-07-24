package com.chp.heartcode.constant;

/**
 * @Author: CHP
 * @Description: APP 常量类
 */
public interface AppConstant {

    /**
     * 精选应用的优先级
     */
    Integer GOOD_APP_PRIORITY = 99;

    /**
     * 默认应用的优先级
     */
    Integer DEFAULT_APP_PRIORITY = 0;

    /**
     * 应用生成目录
     */
    String CODE_OUTPUT_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";

    /**
     * 应用部署目录
     */
    String CODE_DEPLOY_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_deploy";

    /**
     * 应用部署域名（含端口和 context-path，与 StaticResourceConfig 的 /deploy/** 映射对齐）
     */
    String CODE_DEPLOY_HOST = "http://localhost:8123/api/deploy";

    /**
     * 静态资源预览域名（含端口和 context-path，与 StaticResourceConfig 的 /static/** 映射对齐）
     * 用于代码生成后对预览页面截图生成默认封面
     */
    String STATIC_PREVIEW_HOST = "http://localhost:8123/api/static";

}
