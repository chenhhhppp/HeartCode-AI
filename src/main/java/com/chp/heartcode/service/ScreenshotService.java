package com.chp.heartcode.service;

/**
 * @Author: CHP
 * @Description: 截图服务接口
 */
public interface ScreenshotService {

    /**
     * 生成网页截图并上传到对象存储
     *
     * @param webUrl 网页URL
     * @return 截图后的图片访问地址
     */
    String generateAndUploadScreenshot(String webUrl);
}
