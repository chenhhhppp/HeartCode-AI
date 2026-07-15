package com.chp.heartcode.common;

import lombok.Data;

/**
 * @Author: CHP
 * @Description: 分页请求
 */
@Data
public class PageRequest {

    /**
     * 页号
     */
    private int pageNum = 1;

    /**
     * 每页大小
     */
    private int pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（默认降序）
     */
    private String sortOrder = "descend";
}
