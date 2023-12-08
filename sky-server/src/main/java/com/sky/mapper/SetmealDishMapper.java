package com.sky.mapper;

import com.sky.annotation.AutoFill;
import com.sky.entity.SetmealDish;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    /**
     * 根据菜品id查询套餐id
     * @param dishIds
     * @return
     */
    List<Long> getSetmealIdsByDishIds(List<Long> dishIds);


    /**
     * 批量插入某一套餐中包含的菜品
     * @param setmealDishes
     */
    void insertBatch(List<SetmealDish> setmealDishes);

    /**
     * 根据套餐id删除套餐菜品表中对应的数据
     * @param setmeal_id
     */
    @Delete("delete from setmeal_dish where setmeal_id=#{setmeal_id}")
    void deleteBySetmealId(Long setmeal_id);

    /**
     * 根据套餐id查询套餐对应的所有菜品
     * @param setmeal_id
     * @return
     */
    @Select("select * from setmeal_dish where setmeal_id=#{setmeal_id}")
    List<SetmealDish> getBySetmealId(Long setmeal_id);
}
