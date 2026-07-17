package com.chp.heartcode.core.saver;

import cn.hutool.core.util.StrUtil;
import com.chp.heartcode.ai.model.MultiFileCodeResult;
import com.chp.heartcode.exception.BusinessException;
import com.chp.heartcode.exception.ErrorCode;
import com.chp.heartcode.model.enums.CodeGenTypeEnum;

/**
 * @Author: CHP
 * @Description: 多文件保存模板
 */
public class MultiFileCodeFileSaverTemplate extends CodeFileSaverTemplate<MultiFileCodeResult> {

    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.MULTI_FILE;
    }

    @Override
    protected void saveFiles(MultiFileCodeResult result, String baseDirPath) {
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
        writeToFile(baseDirPath, "style.css", result.getCssCode());
        writeToFile(baseDirPath, "script.js", result.getJsCode());
    }

    @Override
    protected void validateInput(MultiFileCodeResult result) {
        super.validateInput(result);
        // 至少要有 HTML 代码，CSS 和 JS 可以为空
        if (StrUtil.isBlank(result.getHtmlCode())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML 代码不能为空");
        }
    }
}
