package com.chp.heartcode.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: CHP
 * @Description: 用户个人信息更新请求（仅允许修改昵称、头像、简介等安全字段）
 */
@Data
public class UserProfileUpdateRequest implements Serializable {

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    private static final long serialVersionUID = 1L;
}
