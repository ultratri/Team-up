package com.teamup.server.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teamup.server.modules.system.entity.DepartmentMajor;
import com.teamup.server.modules.system.mapper.DepartmentMajorMapper;
import com.teamup.server.modules.system.service.DepartmentMajorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DepartmentMajorServiceImpl implements DepartmentMajorService {

    private final DepartmentMajorMapper mapper;

    @Override
    public Map<String, List<String>> getDepartmentMajorTree() {
        LambdaQueryWrapper<DepartmentMajor> qw = new LambdaQueryWrapper<>();
        qw.eq(DepartmentMajor::getEnabled, 1)
          .orderByAsc(DepartmentMajor::getSortOrder)
          .orderByAsc(DepartmentMajor::getId);

        List<DepartmentMajor> rows = mapper.selectList(qw);
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (DepartmentMajor row : rows) {
            if (row.getDepartment() == null || row.getMajor() == null) continue;
            result.computeIfAbsent(row.getDepartment(), k -> new ArrayList<>()).add(row.getMajor());
        }
        return result;
    }

    @Override
    public List<DepartmentMajor> listAll() {
        LambdaQueryWrapper<DepartmentMajor> qw = new LambdaQueryWrapper<>();
        qw.orderByAsc(DepartmentMajor::getDepartment)
          .orderByAsc(DepartmentMajor::getSortOrder)
          .orderByAsc(DepartmentMajor::getId);
        return mapper.selectList(qw);
    }

    @Override
    public DepartmentMajor create(DepartmentMajor item) {
        mapper.insert(item);
        return item;
    }

    @Override
    public DepartmentMajor update(DepartmentMajor item) {
        mapper.updateById(item);
        return mapper.selectById(item.getId());
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }
}
