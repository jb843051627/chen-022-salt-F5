package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TSaltYearTask;
import com.fc.v2.model.custom.SaltRoundResult;
import com.fc.v2.service.ITSaltYearTaskService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 年检到期催办单 Controller（scheduling-job 形状：现行屏 + 跑一轮出口）
 *
 * 屏幕上的收口三数与逐条两处痕迹，只取自 dispatchRound() 这一趟回话；
 * 控制器不另算日子、不手填数目。
 *
 * @author fuce
 * @date 2026-10-04
 */
@Api(value = "年检到期催办单")
@Controller
@RequestMapping("/saltYearTask")
public class SaltYearTaskController extends BaseController {

    private final String prefix = "admin/saltYearTask";

    @Autowired
    private ITSaltYearTaskService saltYearTaskService;

    @ApiOperation(value = "年检现行屏跳转", notes = "年检现行屏跳转")
    @GetMapping("/view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    /**
     * 现行屏：待派/已派出/分不出三种情形照常都列着——分不出的条目不遮住，
     * 只是本轮不朝它动手。可按情形过滤，缺省列全部。
     */
    @Log(title = "年检催办现行屏", action = "list")
    @ApiOperation(value = "现行屏台账", notes = "三种情形照常列出")
    @GetMapping("/list")
    @ResponseBody
    public ResultTable list(@RequestParam(value = "status", required = false) Integer status) {
        QueryWrapper<TSaltYearTask> queryWrapper = new QueryWrapper<TSaltYearTask>()
                .eq("del_flag", 0)
                .orderByAsc("due_at")
                .orderByAsc("id");
        if (status != null) {
            queryWrapper.eq("status", status);
        }
        // 分不出的条目不遮住，三种情形照常列着；本处不挑单、不算日子
        startPage();
        com.github.pagehelper.PageInfo<TSaltYearTask> page =
                new com.github.pagehelper.PageInfo<TSaltYearTask>(
                        saltYearTaskService.selectTSaltYearTaskList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    /**
     * 跑一轮：四样定夺权（挑哪几条、够不上日子动不动、窗口走不走、派出时刻落不落）
     * 全在服务层这一处；屏幕只原样接回话。
     */
    @Log(title = "年检催办跑一轮", action = "round")
    @ApiOperation(value = "按批派跑一轮", notes = "服务层唯一出口，返回收口三数与逐条痕迹")
    @PostMapping("/round")
    @ResponseBody
    public AjaxResult round() {
        SaltRoundResult result = saltYearTaskService.dispatchRound();
        return AjaxResult.success(200, result.getMessage(), result);
    }
}
