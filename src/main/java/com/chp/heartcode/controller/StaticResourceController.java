package com.chp.heartcode.controller;

import com.chp.heartcode.constant.AppConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author CHP
 * @Description: 静态资源控制器 —— 处理 Vue 项目 dist 目录请求
 * 当请求 /static/vue_project_{id}/dist（不带 index.html）时，
 * 自动返回 dist/index.html，避免 Spring 抛出 NoResourceFoundException。
 */
@Slf4j
@RestController
public class StaticResourceController {

    private static final String CODE_OUTPUT_ROOT_DIR = AppConstant.CODE_OUTPUT_ROOT_DIR;

    /**
     * 处理 Vue 项目 dist 目录请求（不带尾部斜杠）
     * Spring 静态资源处理器对目录请求会抛 NoResourceFoundException，
     * 此 Controller 优先匹配，返回 dist/index.html。
     */
    @GetMapping("/static/vue_project_{id}/dist")
    public ResponseEntity<Resource> vueProjectDistIndex(@PathVariable String id) {
        return serveDistIndex(id, "");
    }

    /**
     * 处理 Vue 项目 dist 目录请求（带尾部斜杠）
     */
    @GetMapping("/static/vue_project_{id}/dist/")
    public ResponseEntity<Resource> vueProjectDistIndexSlash(@PathVariable String id) {
        return serveDistIndex(id, "");
    }

    /**
     * 处理部署应用根路径请求（不带尾部斜杠）
     * Spring 静态资源处理器对目录请求会抛 NoResourceFoundException，
     * 此 Controller 优先匹配，返回 {deployKey}/index.html。
     */
    @GetMapping("/deploy/{key}")
    public ResponseEntity<Resource> deployAppIndex(@PathVariable String key) {
        return serveDeployIndex(key);
    }

    /**
     * 处理部署应用根路径请求（带尾部斜杠）
     */
    @GetMapping("/deploy/{key}/")
    public ResponseEntity<Resource> deployAppIndexSlash(@PathVariable String key) {
        return serveDeployIndex(key);
    }

    /**
     * 读取 dist/index.html 并返回
     */
    private ResponseEntity<Resource> serveDistIndex(String id, String subPath) {
        Path basePath = Paths.get(CODE_OUTPUT_ROOT_DIR).toAbsolutePath().normalize();
        Path targetPath = basePath.resolve("vue_project_" + id).resolve("dist").resolve("index.html").normalize();

        // 安全检查：防止路径穿越
        if (!targetPath.startsWith(basePath)) {
            return ResponseEntity.notFound().build();
        }

        File file = targetPath.toFile();
        if (!file.exists() || !file.isFile()) {
            log.warn("Vue 项目 dist/index.html 不存在: {}", targetPath);
            return ResponseEntity.notFound().build();
        }

        FileSystemResource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .body(resource);
    }

    /**
     * 读取部署目录的 index.html 并返回
     */
    private ResponseEntity<Resource> serveDeployIndex(String key) {
        String deployDir = System.getProperty("user.dir") + File.separator + "tmp" + File.separator + "code_deploy";
        Path basePath = Paths.get(deployDir).toAbsolutePath().normalize();
        Path targetPath = basePath.resolve(key).resolve("index.html").normalize();

        // 安全检查：防止路径穿越
        if (!targetPath.startsWith(basePath)) {
            return ResponseEntity.notFound().build();
        }

        File file = targetPath.toFile();
        if (!file.exists() || !file.isFile()) {
            log.warn("部署应用 index.html 不存在: {}", targetPath);
            return ResponseEntity.notFound().build();
        }

        FileSystemResource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .body(resource);
    }
}
