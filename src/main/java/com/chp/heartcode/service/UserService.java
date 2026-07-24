package com.chp.heartcode.service;

import com.chp.heartcode.model.dto.user.UserProfileUpdateRequest;
import com.chp.heartcode.model.dto.user.UserQueryRequest;
import com.chp.heartcode.model.vo.LoginUserVO;
import com.chp.heartcode.model.vo.UserVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.chp.heartcode.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 用户 服务层。
 *
 * @author CHP
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账号
     * @param userPassword  用户密码
     * @param checkPassword 确认密码
     * @return 注册成功返回用户id，注册失败返回-1
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 加密
     *
     * @param password 密码
     * @return 加密后的密码
     */
    String getEncryptPassword(String password);

    /**
     * 获取脱敏后的登录用户信息
     *
     * @param user 用户
     * @return 登录用户信息
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 用户登录
     *
     * @param userAccount  用户账号
     * @param userPassword 用户密码
     * @param request
     * @return 登录用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取dl
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取用户信息
     *
     * @param user 用户
     * @return 用户信息
     */
    UserVO getUserVO(User user);

    /**
     * 获取用户列表
     *
     * @param userList 用户列表
     * @return 用户列表
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 获取查询包装类
     *
     * @param userQueryRequest 用户查询
     * @return 查询包装类
     */
    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 当前登录用户更新个人信息（仅允许修改昵称、头像、简介等安全字段）
     *
     * @param userProfileUpdateRequest 个人信息更新请求
     * @param loginUser                当前登录用户
     * @return 更新成功返回最新登录用户信息
     */
    LoginUserVO updateMyProfile(UserProfileUpdateRequest userProfileUpdateRequest, User loginUser);
}
