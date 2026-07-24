package com.chp.heartcode.manager;

import com.chp.heartcode.config.CosClientConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import cn.hutool.core.io.FileUtil;

import jakarta.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * COS对象存储管理器
 *
 * @author yupi
 */
@Component
@Slf4j
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 上传对象
     *
     * @param key  唯一键
     * @param file 文件
     * @return 上传结果
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 上传文件到 COS 并返回访问 URL
     *
     * @param key  COS对象键（完整路径）
     * @param file 要上传的文件
     * @return 文件的访问URL，失败返回null
     */
    public String uploadFile(String key, File file) {
        // 上传文件
        PutObjectResult result = putObject(key, file);
        if (result != null) {
            // 构建访问URL，自动补全 https:// 协议，避免 host 配置缺少协议导致前端无法渲染图片
            String host = cosClientConfig.getHost();
            if (host != null && !host.startsWith("http://") && !host.startsWith("https://")) {
                host = "https://" + host;
            }
            String url = String.format("%s%s", host, key);
            log.info("文件上传COS成功: {} -> {}", file.getName(), url);
            return url;
        } else {
            log.error("文件上传COS失败，返回结果为空");
            return null;
        }
    }

    /**
     * 上传 MultipartFile 到 COS 并返回访问 URL
     * 内部将 MultipartFile 转为临时文件后上传，上传完成后清理临时文件
     *
     * @param key           COS对象键（完整路径）
     * @param multipartFile 前端上传的文件
     * @return 文件的访问URL，失败返回null
     */
    public String uploadFile(String key, MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            return null;
        }
        File tempFile = null;
        try {
            // 将 MultipartFile 写入临时文件
            String originalFilename = multipartFile.getOriginalFilename();
            String suffix = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            tempFile = Files.createTempFile("upload_", suffix).toFile();
            try (InputStream inputStream = multipartFile.getInputStream()) {
                Files.copy(inputStream, tempFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            return uploadFile(key, tempFile);
        } catch (IOException e) {
            log.error("MultipartFile 转临时文件失败", e);
            return null;
        } finally {
            // 清理临时文件
            if (tempFile != null && tempFile.exists()) {
                FileUtil.del(tempFile);
            }
        }
    }
}
