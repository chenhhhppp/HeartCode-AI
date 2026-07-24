package com.chp.heartcode.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.chp.heartcode.constant.UserConstant;
import com.chp.heartcode.exception.BusinessException;
import com.chp.heartcode.exception.ErrorCode;
import com.chp.heartcode.exception.ThrowUtils;
import com.chp.heartcode.mapper.ChatHistoryMapper;
import com.chp.heartcode.model.dto.chathistory.ChatHistoryQueryRequest;
import com.chp.heartcode.model.entity.App;
import com.chp.heartcode.model.entity.ChatHistory;
import com.chp.heartcode.model.entity.User;
import com.chp.heartcode.model.enums.MessageTypeEnum;
import com.chp.heartcode.model.vo.ChatHistoryVO;
import com.chp.heartcode.model.vo.UserVO;
import com.chp.heartcode.service.AppService;
import com.chp.heartcode.service.ChatHistoryService;
import com.chp.heartcode.service.UserService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * 对话历史 服务层实现。
 *
 * @author CHP
 */
@Slf4j
@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {

    @Resource
    @Lazy
    private AppService appService;

    @Resource
    private UserService userService;

    @Override
    public boolean addChatMessage(Long appId, String message, String messageType, Long userId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(messageType), ErrorCode.PARAMS_ERROR, "消息类型不能为空");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        // 验证消息类型是否有效
        MessageTypeEnum messageTypeEnum = MessageTypeEnum.getEnumByValue(messageType);
        ThrowUtils.throwIf(messageTypeEnum == null, ErrorCode.PARAMS_ERROR, "消息类型无效");
        ChatHistory chatHistory = ChatHistory.builder()
                .appId(appId)
                .message(message)
                .messageType(messageType)
                .userId(userId)
                .build();
        return this.save(chatHistory);
    }

    @Override
    public ChatHistory saveUserMessage(Long appId, String message, User loginUser) {
        return saveMessage(appId, message, MessageTypeEnum.USER, loginUser);
    }

    @Override
    public ChatHistory saveAiMessage(Long appId, String message, MessageTypeEnum messageTypeEnum, User loginUser) {
        // 兜底：如果传入的不是 AI / ERROR，默认按 AI 处理
        if (messageTypeEnum == null || messageTypeEnum == MessageTypeEnum.USER) {
            messageTypeEnum = MessageTypeEnum.AI;
        }
        return saveMessage(appId, message, messageTypeEnum, loginUser);
    }

    /**
     * 保存消息的通用方法（内部）
     */
    private ChatHistory saveMessage(Long appId, String message, MessageTypeEnum messageTypeEnum, User loginUser) {
        ChatHistory chatHistory = ChatHistory.builder()
                .appId(appId)
                .message(message)
                .messageType(messageTypeEnum.getValue())
                .userId(loginUser.getId())
                .build();
        boolean saved = this.save(chatHistory);
        if (!saved) {
            log.error("保存对话历史失败：appId={}, type={}, message={}", appId, messageTypeEnum.getValue(), message);
        }
        return chatHistory;
    }

    @Override
    public ChatHistoryVO getChatHistoryVO(ChatHistory chatHistory) {
        if (chatHistory == null) {
            return null;
        }
        ChatHistoryVO vo = new ChatHistoryVO();
        BeanUtil.copyProperties(chatHistory, vo);
        // 关联查询用户信息
        Long userId = chatHistory.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            vo.setUser(userVO);
        }
        return vo;
    }

    @Override
    public List<ChatHistoryVO> getChatHistoryVOList(List<ChatHistory> chatHistoryList) {
        if (CollUtil.isEmpty(chatHistoryList)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息，避免 N+1 查询问题
        Set<Long> userIds = chatHistoryList.stream()
                .map(ChatHistory::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        return chatHistoryList.stream().map(chatHistory -> {
            ChatHistoryVO vo = new ChatHistoryVO();
            BeanUtil.copyProperties(chatHistory, vo);
            vo.setUser(userVOMap.get(chatHistory.getUserId()));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        if (chatHistoryQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chatHistoryQueryRequest.getId();
        String message = chatHistoryQueryRequest.getMessage();
        String messageType = chatHistoryQueryRequest.getMessageType();
        Long appId = chatHistoryQueryRequest.getAppId();
        Long userId = chatHistoryQueryRequest.getUserId();
        LocalDateTime lastCreateTime = chatHistoryQueryRequest.getLastCreateTime();
        String sortField = chatHistoryQueryRequest.getSortField();
        String sortOrder = chatHistoryQueryRequest.getSortOrder();
        // 拼接查询条件（使用条件式 eq/like，避免 null 值生成 IS NULL 导致查询为空）
        queryWrapper.eq("id", id, id != null)
                .like("message", message, StrUtil.isNotBlank(message))
                .eq("messageType", messageType, StrUtil.isNotBlank(messageType))
                .eq("appId", appId, appId != null)
                .eq("userId", userId, userId != null);
        // 游标查询逻辑 - 只使用 createTime 作为游标
        if (lastCreateTime != null) {
            queryWrapper.lt("createTime", lastCreateTime);
        }
        // 排序
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        } else {
            // 默认按创建时间降序排列
            queryWrapper.orderBy("createTime", false);
        }
        return queryWrapper;
    }

    @Override
    public Page<ChatHistoryVO> listMyChatHistoryVOByPage(ChatHistoryQueryRequest chatHistoryQueryRequest, User loginUser) {
        ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long appId = chatHistoryQueryRequest.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        // 查询应用是否存在
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 权限校验：仅应用创建者或管理员可见
        if (!app.getUserId().equals(loginUser.getId()) && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROR, "无权限查看该应用的对话历史");
        }
        long pageNum = chatHistoryQueryRequest.getPageNum();
        long pageSize = chatHistoryQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 50, ErrorCode.PARAMS_ERROR, "每页最多查询 50 条消息");
        QueryWrapper queryWrapper = this.getQueryWrapper(chatHistoryQueryRequest);
        Page<ChatHistory> chatHistoryPage = this.page(Page.of(pageNum, pageSize), queryWrapper);
        // 封装数据
        Page<ChatHistoryVO> voPage = new Page<>(pageNum, pageSize, chatHistoryPage.getTotalRow());
        List<ChatHistoryVO> voList = this.getChatHistoryVOList(chatHistoryPage.getRecords());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public Page<ChatHistoryVO> listChatHistoryVOByPage(ChatHistoryQueryRequest chatHistoryQueryRequest) {
        ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = chatHistoryQueryRequest.getPageNum();
        long pageSize = chatHistoryQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 50, ErrorCode.PARAMS_ERROR, "每页最多查询 50 条消息");
        QueryWrapper queryWrapper = this.getQueryWrapper(chatHistoryQueryRequest);
        Page<ChatHistory> chatHistoryPage = this.page(Page.of(pageNum, pageSize), queryWrapper);
        // 封装数据
        Page<ChatHistoryVO> voPage = new Page<>(pageNum, pageSize, chatHistoryPage.getTotalRow());
        List<ChatHistoryVO> voList = this.getChatHistoryVOList(chatHistoryPage.getRecords());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public boolean deleteByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId);
        return this.remove(queryWrapper);
    }

    @Override
    public Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                                      LocalDateTime lastCreateTime,
                                                      User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "页面大小必须在1-50之间");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        // 验证权限：只有应用创建者和管理员可以查看
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        boolean isCreator = app.getUserId().equals(loginUser.getId());
        ThrowUtils.throwIf(!isAdmin && !isCreator, ErrorCode.NOT_AUTH_ERROR, "无权查看该应用的对话历史");
        // 构建查询条件
        ChatHistoryQueryRequest queryRequest = new ChatHistoryQueryRequest();
        queryRequest.setAppId(appId);
        queryRequest.setLastCreateTime(lastCreateTime);
        QueryWrapper queryWrapper = this.getQueryWrapper(queryRequest);
        // 查询数据（始终查第 1 页，由游标保证连续性）
        return this.page(Page.of(1, pageSize), queryWrapper);
    }

    @Override
    public int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount) {
        try {
            // 起始点为 1 而不是 0，用于排除最新的用户消息
            // 因为对话流程中：用户消息先落库 → AI Service 调用时又会自动把用户消息加进记忆
            // 如果不排除最新一条，会导致该消息在记忆中出现两次
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appId)
                    .orderBy(ChatHistory::getCreateTime, false)
                    .limit(1, maxCount);
            List<ChatHistory> historyList = this.list(queryWrapper);
            if (CollUtil.isEmpty(historyList)) {
                return 0;
            }
            // 反转列表，确保按时间正序（老的在前，新的在后）
            List<ChatHistory> orderedList = new ArrayList<>(historyList);
            java.util.Collections.reverse(orderedList);
            // 先清理历史缓存，防止重复加载
            chatMemory.clear();
            // 按时间顺序添加到记忆中
            int loadedCount = 0;
            for (ChatHistory history : orderedList) {
                String messageType = history.getMessageType();
                String messageContent = history.getMessage();
                if (StrUtil.isBlank(messageContent)) {
                    continue;
                }
                if (MessageTypeEnum.USER.getValue().equals(messageType)) {
                    chatMemory.add(UserMessage.from(messageContent));
                    loadedCount++;
                } else if (MessageTypeEnum.AI.getValue().equals(messageType)) {
                    chatMemory.add(AiMessage.from(messageContent));
                    loadedCount++;
                }
                // ERROR 类型的消息不加入记忆（避免污染 AI 上下文）
            }
            log.info("成功为 appId: {} 加载了 {} 条历史对话到记忆中", appId, loadedCount);
            return loadedCount;
        } catch (Exception e) {
            log.error("加载历史对话失败，appId: {}, error: {}", appId, e.getMessage(), e);
            // 加载失败不影响系统运行，只是没有历史上下文
            return 0;
        }
    }
}
