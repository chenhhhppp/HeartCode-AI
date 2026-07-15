package com.chp.heartcode.model.enums;

import cn.hutool.core.util.ObjectUtil;
import lombok.Getter;

/**
 * @Author: CHP
 * @Description: 用户角色枚举
 */
@Getter
public enum UserRoleEnum {

    ADMIN("管理员", "admin"),
    USER("用户", "user");

    private final String text;

    private final String value;

    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value value
     * @return 枚举值
     */
    public static UserRoleEnum getEnumByValue(String value) {
        if (ObjectUtil.isEmpty(value)) {
            return null;
        }
        for (UserRoleEnum anEnum : UserRoleEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
