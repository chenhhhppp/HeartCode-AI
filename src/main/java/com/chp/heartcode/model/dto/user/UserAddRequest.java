package com.chp.heartcode.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: CHP
 * @Description: 用户添加请求
 */
@Data
public class UserAddRequest implements Serializable {

    private String userName;

    private String userAccount;

    private String userAvatar;

    private String userProfile;

    private String userRole;

    private static final long serialVersionUID = 1L;
}
