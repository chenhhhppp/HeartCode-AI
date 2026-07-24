package com.chp.heartcode.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * @Author: CHP
 * @Description: 静态资源配置
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 获取项目根目录
        String userDir = System.getProperty("user.dir");

        // 代码生成输出目录映射
        String outputDir = userDir + File.separator + "tmp" + File.separator + "code_output";
        registry.addResourceHandler("/static/**")
                .addResourceLocations("file:" + outputDir.replace("\\", "/") + "/")
                .setCachePeriod(0);

        // 代码部署目录映射
        String deployDir = userDir + File.separator + "tmp" + File.separator + "code_deploy";
        registry.addResourceHandler("/deploy/**")
                .addResourceLocations("file:" + deployDir.replace("\\", "/") + "/")
                .setCachePeriod(0);

        System.out.println("静态资源配置已加载:");
        System.out.println("  /static/** -> " + outputDir);
        System.out.println("  /deploy/** -> " + deployDir);
    }
}
