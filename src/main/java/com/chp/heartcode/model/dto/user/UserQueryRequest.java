package com.chp.heartcode.model.dto.user;

import com.chp.heartcode.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @Author: CHP
 * @Description: 用户查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserQueryRequest extends PageRequest implements Serializable {

    private Long id;

    private String userName;

    private String userAccount;

    private String userProfile;

    private String userRole;

    private static final long serialVersionUID = 1L;
}
