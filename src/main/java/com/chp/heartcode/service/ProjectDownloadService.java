package com.chp.heartcode.service;

import jakarta.servlet.http.HttpServletResponse;

/**
 * @Author: CHP
 * @Description: 项目下载服务接口 —— 将指定路径下的文件打包为 ZIP 下载
 */
public interface ProjectDownloadService {

    /**
     * 将指定项目目录打包为 ZIP 并写入响应流
     *
     * @param projectPath 项目根目录路径
     * @param zipFileName 下载的 ZIP 文件名
     * @param response    HTTP 响应
     */
    void downloadProjectAsZip(String projectPath, String zipFileName, HttpServletResponse response);
}
