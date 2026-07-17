package com.chp.heartcode.core;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.chp.heartcode.ai.model.HtmlCodeResult;
import com.chp.heartcode.ai.model.MultiFileCodeResult;
import com.chp.heartcode.model.enums.CodeGenTypeEnum;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * @Author: CHP
 * @Description: 代码文件保存器
 */
@Slf4j
public class CodeFileSaver {

    // 文件保存根目录
    private static final String FILE_SAVE_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";

    /**
     * 保存 HtmlCodeResult
     *
     * @param result
     * @return
     */
    public static File saveHtmlCodeResult(HtmlCodeResult result) {
        String baseDirPath = buildUniqueDir(CodeGenTypeEnum.HTML.getValue());
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
        return new File(baseDirPath);
    }

    /**
     * 保存 MultiFileCodeResult
     *
     * @param result
     * @return
     */
    public static File saveMultiFileCodeResult(MultiFileCodeResult result) {
        String baseDirPath = buildUniqueDir(CodeGenTypeEnum.MULTI_FILE.getValue());
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
        writeToFile(baseDirPath, "style.css", result.getCssCode());
        writeToFile(baseDirPath, "script.js", result.getJsCode());
        return new File(baseDirPath);
    }

    /**
     * 构建唯一目录路由：tmp/code_output/{bizType}_{雪花ID}
     *
     * @param bizType
     * @return
     */
    private static String buildUniqueDir(String bizType) {
        String uniqueDirName = StrUtil.format("{}_{}", bizType, IdUtil.getSnowflakeNextIdStr());
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirName;
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

    /**
     * 写入单个文件
     *
     * @param dirPath
     * @param fileName
     * @param content
     */
    private static void writeToFile(String dirPath, String fileName, String content) {

        // todo 如果内容为空，直接跳过，不生产该文件
        if (StrUtil.isBlank(content)) {
            log.warn("内容为空，跳过生成文件：{}", fileName);
            return;
        }

        String filePath = dirPath + File.separator + fileName;
        FileUtil.writeString(content, filePath, "UTF-8");
    }
}
