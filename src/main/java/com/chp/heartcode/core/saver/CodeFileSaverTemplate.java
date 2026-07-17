package com.chp.heartcode.core.saver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.chp.heartcode.exception.BusinessException;
import com.chp.heartcode.exception.ErrorCode;
import com.chp.heartcode.model.enums.CodeGenTypeEnum;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * @Author: CHP
 * @Description: 抽象代码文件保存器 - 模板方法模式
 */
@Slf4j
public abstract class CodeFileSaverTemplate<T> {

    // 文件保存根目录
    private static final String FILE_SAVE_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";

    /**
     * 模板方法：保存代码的标准流程
     *
     * @param result 代码结果对象
     * @return 保存的目录
     */
    public final File saveCode(T result) {
        // 1. 验证输入
        validateInput(result);
        // 2. 构建唯一目录
        String baseDirPath = buildUniqueDir();
        // 3. 保存文件（具体实现由之类提供）
        saveFiles(result, baseDirPath);
        // 4. 返回目录文件对象
        return new File(baseDirPath);
    }

    /**
     * 验证输入参数（由之类实现）
     *
     * @param result 代码结果对象
     */
    protected void validateInput(T result) {
        if (result == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "代码结果对象不能为空");
        }
    }

    /**
     * 构建唯一目录路径：tmp/code_output/{bizType}_{雪花ID}
     *
     * @return 目录路径
     */
    protected final String buildUniqueDir() {
        String codeType = getCodeType().getValue();
        String uniqueDirName = StrUtil.format("{}_{}", codeType, IdUtil.getSnowflakeNextIdStr());
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirName;
        FileUtil.mkdir(dirPath);
        return dirPath;
    }


    /**
     * 写入单个文件的工具方法
     *
     * @param dirPath  目录路径
     * @param fileName 文件名
     * @param content  文件内容
     */
    protected final void writeToFile(String dirPath, String fileName, String content) {

        // todo 如果内容为空，直接跳过，不生产该文件
        if (StrUtil.isBlank(content)) {
            log.warn("内容为空，跳过生成文件：{}", fileName);
            return;
        }

        if (StrUtil.isNotBlank(content)) {
            String filePath = dirPath + File.separator + fileName;
            FileUtil.writeString(content, filePath, "UTF-8");
        }
    }

    /**
     * 获取代码类型（由子类实现）
     *
     * @return 代码生成类型
     */
    protected abstract CodeGenTypeEnum getCodeType();

    /**
     * 保存文件（由子类实现）
     *
     * @param result      生成结果
     * @param baseDirPath 保存目录路径
     */
    protected abstract void saveFiles(T result, String baseDirPath);
}
