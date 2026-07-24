package com.chp.heartcode.controller;

import com.chp.heartcode.annotation.AuthCheck;
import com.chp.heartcode.common.BaseResponse;
import com.chp.heartcode.common.ResultUtils;
import com.chp.heartcode.constant.UserConstant;
import com.chp.heartcode.exception.ErrorCode;
import com.chp.heartcode.exception.ThrowUtils;
import com.chp.heartcode.model.dto.chathistory.ChatHistoryQueryRequest;
import com.chp.heartcode.model.entity.ChatHistory;
import com.chp.heartcode.model.entity.User;
import com.chp.heartcode.model.vo.ChatHistoryVO;
import com.chp.heartcode.service.ChatHistoryService;
import com.chp.heartcode.service.UserService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 对话历史 控制层。
 *
 * @author CHP
 */
@RestController
@RequestMapping("/chatHistory")
public class ChatHistoryController {

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private UserService userService;

    /**
     * 分页查询当前用户（或管理员）可见的某应用对话历史
     * <p>
     * 进入应用页面时前端调用，默认按创建时间倒序（最新在前），
     * pageNum=1 即获取最新 10 条，向前加载更多历史记录时增加 pageNum。
     *
     * @param chatHistoryQueryRequest 查询请求（必须传 appId）
     * @param request                 请求
     * @return 对话历史分页
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<ChatHistoryVO>> listMyChatHistoryVOByPage(@RequestBody ChatHistoryQueryRequest chatHistoryQueryRequest,
                                                                       HttpServletRequest request) {
        ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Page<ChatHistoryVO> page = chatHistoryService.listMyChatHistoryVOByPage(chatHistoryQueryRequest, loginUser);
        return ResultUtils.success(page);
    }

    /**
     * 管理员分页查询所有对话历史
     *
     * @param chatHistoryQueryRequest 查询请求
     * @return 对话历史分页
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ChatHistory>> listAllChatHistoryByPageForAdmin(@RequestBody ChatHistoryQueryRequest chatHistoryQueryRequest) {
        ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = chatHistoryQueryRequest.getPageNum();
        long pageSize = chatHistoryQueryRequest.getPageSize();
        // 查询数据（默认按 createTime 降序，由 getQueryWrapper 保证）
        QueryWrapper queryWrapper = chatHistoryService.getQueryWrapper(chatHistoryQueryRequest);
        Page<ChatHistory> result = chatHistoryService.page(Page.of(pageNum, pageSize), queryWrapper);
        return ResultUtils.success(result);
    }

    /**
     * 分页查询某个应用的对话历史（游标查询）
     *
     * @param appId          应用ID
     * @param pageSize       页面大小
     * @param lastCreateTime 最后一条记录的创建时间
     * @param request        请求
     * @return 对话历史分页
     */
    @GetMapping("/app/{appId}")
    public BaseResponse<Page<ChatHistory>> listAppChatHistory(@PathVariable Long appId,
                                                              @RequestParam(defaultValue = "10") int pageSize,
                                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastCreateTime,
                                                              HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Page<ChatHistory> result = chatHistoryService.listAppChatHistoryByPage(appId, pageSize, lastCreateTime, loginUser);
        return ResultUtils.success(result);
    }
}
