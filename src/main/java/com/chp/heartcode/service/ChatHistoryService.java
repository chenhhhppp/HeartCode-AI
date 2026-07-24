package com.chp.heartcode.service;

import com.chp.heartcode.model.dto.chathistory.ChatHistoryQueryRequest;
import com.chp.heartcode.model.entity.ChatHistory;
import com.chp.heartcode.model.entity.User;
import com.chp.heartcode.model.enums.MessageTypeEnum;
import com.chp.heartcode.model.vo.ChatHistoryVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话历史 服务层。
 *
 * @author CHP
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 新增对话历史
     *
     * @param appId       应用 id
     * @param message     消息内容
     * @param messageType 消息类型
     * @param userId      用户 id
     * @return 是否新增成功
     */
    boolean addChatMessage(Long appId, String message, String messageType, Long userId);

    /**
     * 保存用户消息
     *
     * @param appId     应用 id
     * @param message   消息内容
     * @param loginUser 登录用户
     * @return 保存后的对话历史
     */
    ChatHistory saveUserMessage(Long appId, String message, User loginUser);

    /**
     * 保存 AI 消息（成功 / 错误）
     *
     * @param appId           应用 id
     * @param message         消息内容
     * @param messageTypeEnum 消息类型（AI / ERROR）
     * @param loginUser       登录用户
     * @return 保存后的对话历史
     */
    ChatHistory saveAiMessage(Long appId, String message, MessageTypeEnum messageTypeEnum, User loginUser);

    /**
     * 转换为 VO
     *
     * @param chatHistory 对话历史
     * @return VO
     */
    ChatHistoryVO getChatHistoryVO(ChatHistory chatHistory);

    /**
     * 批量转换为 VO
     *
     * @param chatHistoryList 对话历史列表
     * @return VO 列表
     */
    List<ChatHistoryVO> getChatHistoryVOList(List<ChatHistory> chatHistoryList);

    /**
     * 构造查询条件
     *
     * @param chatHistoryQueryRequest 查询请求
     * @return QueryWrapper
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);

    /**
     * 分页查询当前用户（或管理员）可见的某应用对话历史，默认按创建时间倒序（最新在前）
     *
     * @param chatHistoryQueryRequest 查询请求
     * @param loginUser               登录用户
     * @return 对话历史分页
     */
    com.mybatisflex.core.paginate.Page<ChatHistoryVO> listMyChatHistoryVOByPage(ChatHistoryQueryRequest chatHistoryQueryRequest, User loginUser);

    /**
     * 管理员分页查询所有应用的对话历史
     *
     * @param chatHistoryQueryRequest 查询请求
     * @return 对话历史分页
     */
    com.mybatisflex.core.paginate.Page<ChatHistoryVO> listChatHistoryVOByPage(ChatHistoryQueryRequest chatHistoryQueryRequest);

    /**
     * 根据应用 id 删除该应用的所有对话历史（逻辑删除）
     *
     * @param appId 应用 id
     * @return 是否删除成功
     */
    boolean deleteByAppId(Long appId);

    /**
     * 游标分页查询某应用的对话历史（按 createTime 倒序）
     * <p>
     * 首次查询 lastCreateTime 传 null，获取最新的 pageSize 条；
     * 向前加载更多时，把上一页最早一条消息的 createTime 作为 lastCreateTime 传入。
     *
     * @param appId          应用 id
     * @param pageSize       每页大小（1-50）
     * @param lastCreateTime 游标（上一页最早消息的 createTime）
     * @param loginUser      登录用户
     * @return 对话历史分页
     */
    com.mybatisflex.core.paginate.Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                                                             LocalDateTime lastCreateTime,
                                                                             User loginUser);

    /**
     * 加载数据库中的对话历史到 LangChain4j 的对话记忆中。
     * <p>
     * 用于应用启动/重启后恢复 AI 的多轮对话上下文。
     * <p>
     * 注意事项：
     * <ul>
     *   <li>查询起始点 offset=1，排除最新一条用户消息（因为 AI Service 调用时会自动把当前用户消息加进记忆，不排除会重复）</li>
     *   <li>查询结果按时间正序（老的在前、新的在后）添加到记忆中</li>
     *   <li>加载前会先调用 chatMemory.clear() 清理 Redis 中已有的记忆，防止重复加载</li>
     * </ul>
     *
     * @param appId      应用 id
     * @param chatMemory 要填充的对话记忆
     * @param maxCount   最多加载的条数
     * @return 实际加载的消息条数；失败返回 0
     */
    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);
}
