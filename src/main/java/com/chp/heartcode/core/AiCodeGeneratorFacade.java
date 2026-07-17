package com.chp.heartcode.core;

import com.chp.heartcode.ai.AiCodeGeneratorService;
import com.chp.heartcode.ai.model.HtmlCodeResult;
import com.chp.heartcode.ai.model.MultiFileCodeResult;
import com.chp.heartcode.exception.BusinessException;
import com.chp.heartcode.exception.ErrorCode;
import com.chp.heartcode.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * @Author: CHP
 * @Description: AI 代码生成外观类，组合生成和保存功能
 */
@Slf4j
@Service
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    /**
     * 统一入口：根据类型生成并保存代码
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> generateAndSaveHtmlCode(userMessage);
            case MULTI_FILE -> generateAndSaveMultiFileCode(userMessage);
            default -> {
                String errorMessage = "不支持的生成类型: " + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 生成 HTML 模式的代码并保存
     *
     * @param userMessage 用户提示词
     * @return 保存的目录
     */
    private File generateAndSaveHtmlCode(String userMessage) {
        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
        return CodeFileSaver.saveHtmlCodeResult(result);
    }

    /**
     * 生成多文件模式的代码并保存
     *
     * @param userMessage 用户提示词
     * @return 保存的目录
     */
    private File generateAndSaveMultiFileCode(String userMessage) {
        MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
        return CodeFileSaver.saveMultiFileCodeResult(result);
    }

    // Reactor

    /**
     * 统一入口：根据类型生成并保存代码（流式）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @return 保存的目录
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型不能为空");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> generateAndSaveHtmlCodeStream(userMessage);
            case MULTI_FILE -> generateAndSaveMultiFileCodeStream(userMessage);
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 生成 HTML 模式的代码并保存（流式）
     *
     * @param userMessage 用户提示词
     * @return 保存的目录
     */
    private Flux<String> generateAndSaveHtmlCodeStream(String userMessage) {
        Flux<String> result = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
        // 当流式返回生成代码完成后，再保存代码
        StringBuilder codeBuilder = new StringBuilder();
        // 实时收集代码片段
        return result
                .doOnNext(codeBuilder::append)
                .doOnComplete(() -> {
                    // 流式返回完成后保存代码
                    try {
                        // todo 在 Facade 层剥离 JSON 外壳
                        String rawContent = codeBuilder.toString();

                        String completeHtmlCode = extractAnswerFromJson(rawContent);
                        HtmlCodeResult htmlCodeResult = CodeParser.parseHtmlCode(completeHtmlCode);

                        // todo
                        log.info("大模型返回的完整内容：\n {}", completeHtmlCode);

                        // 保存代码到文件
                        File savedDir = CodeFileSaver.saveHtmlCodeResult(htmlCodeResult);
                        log.info("保存成功，路劲为: {}", savedDir.getAbsolutePath());
                    } catch (Exception e) {
                        log.error("保存失败: {}", e.getMessage());
                    }
                });
    }


    /**
     * 生成 HTML 模式的代码并保存（流式）
     *
     * @param userMessage 用户提示词
     * @return 保存的目录
     */
    private Flux<String> generateAndSaveMultiFileCodeStream(String userMessage) {
        Flux<String> result = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
        // 当流式返回生成代码完成后，再保存代码
        StringBuilder codeBuilder = new StringBuilder();
        return result
                .doOnNext(codeBuilder::append)
                .doOnComplete(() -> {
                    // 流式返回完成后保存代码
                    try {

                        // todo 在 Facade 层剥离 JSON 外壳
                        String rawContent = codeBuilder.toString();

                        String completeMultiFileCode = extractAnswerFromJson(rawContent);
                        MultiFileCodeResult multiFileCodeResult = CodeParser.parseMultiFileCode(completeMultiFileCode);

                        // todo
                        log.info("大模型返回的完整内容：\n {}", completeMultiFileCode);

                        // 保存代码到文件
                        File savedDir = CodeFileSaver.saveMultiFileCodeResult(multiFileCodeResult);
                        log.info("保存成功，路劲为: {}", savedDir.getAbsolutePath());
                    } catch (Exception e) {
                        log.error("保存失败: {}", e.getMessage(), e);
                    }
                });
    }

    // todo 辅助方法：如果大模型返回了 JSON，提取其中的 answer 字段；如果不是 JSON，原样返回
    private String extractAnswerFromJson(String rawContent) {
        try {
            // 使用 Hutool 的 JSONUtil 解析
            cn.hutool.json.JSONObject jsonObject = cn.hutool.json.JSONUtil.parseObj(rawContent);
            if (jsonObject.containsKey("answer")) {
                return jsonObject.getStr("answer");
            }
        } catch (Exception e) {
            // 如果解析失败，说明不是 JSON 格式，直接返回原内容
            log.warn("大模型返回内容非 JSON 格式，按原始 Markdown 处理");
        }
        return rawContent;
    }
}
