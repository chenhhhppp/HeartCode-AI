package com.chp.heartcode.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: CHP
 * @Description: 用户更新请求
 */
@Data
public class UserUpdateRequest implements Serializable {

    private Long id;

    private String userName;

    private String userAvatar;

    private String userProfile;

    private String userRole;

    private static final long serialVersionUID = 1L;
}
