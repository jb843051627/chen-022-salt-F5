package com.fc.v2.service;

import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.model.auto.TSaltYearTask;
import com.fc.v2.model.custom.SaltRoundResult;

/**
 * 年检到期催办单 Service接口（scheduling-job 形状：周期执行，无增删改查入口）
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITSaltYearTaskService {

    /** 按主键回查条目 */
    TSaltYearTask selectTSaltYearTaskById(Long id);

    /** 现行屏台账（三种情形照常列出，含分不出） */
    List<TSaltYearTask> selectTSaltYearTaskList(QueryWrapper<TSaltYearTask> queryWrapper);

    /** 该时刻可处理的条目（执行窗口内 + 到期 + 尚未处理） */
    List<TSaltYearTask> listDue(Date at);

    /** 执行一次，返回**成功条数**；单条失败跳过继续 */
    int runOnce(Date at);

    /**
     * 按批派——跑一轮（新名字，旧 runOnce 不动）。
     * 挑单、派人、回写、收口三数都只出自这一处出口；
     * 落锤时刻由这一方法自己取，外部不得另传一把尺。
     */
    SaltRoundResult dispatchRound();
}
