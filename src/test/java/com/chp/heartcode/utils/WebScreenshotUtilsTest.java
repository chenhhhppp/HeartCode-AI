package com.chp.heartcode.utils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @Author: CHP
 * @Description: 网页截图工具类测试
 */
@Slf4j
@SpringBootTest
public class WebScreenshotUtilsTest {

    @Test
    void saveWebPageScreenshot() {
        String testUrl = "https://pvp.qq.com/";
        String webPageScreenshot = WebScreenshotUtils.saveWebPageScreenshot(testUrl);
        log.info("截图文件路径: {}", webPageScreenshot);
        Assertions.assertNotNull(webPageScreenshot);
    }
}
