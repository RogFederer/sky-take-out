package com.sky.service;

import com.sky.dto.SetmealDTO;


public interface SetmealService {

    /**
     * 新增套餐，同时保持套餐和菜品的对应关系
     * @param setmealDTO
     */
    void saveWithDish(SetmealDTO setmealDTO);
}
