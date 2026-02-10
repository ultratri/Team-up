package com.teamup.server.modules.system.service;

import com.teamup.server.modules.system.entity.DepartmentMajor;

import java.util.List;
import java.util.Map;

public interface DepartmentMajorService {

    /**
     * 公共：返回院系->专业列表（仅 enabled=1），按 sortOrder 排序
     */
    Map<String, List<String>> getDepartmentMajorTree();

    /**
     * 管理端：返回全部记录（含禁用），按 department + sortOrder + id 排序
     */
    List<DepartmentMajor> listAll();

    DepartmentMajor create(DepartmentMajor item);

    DepartmentMajor update(DepartmentMajor item);

    void delete(Long id);
}
