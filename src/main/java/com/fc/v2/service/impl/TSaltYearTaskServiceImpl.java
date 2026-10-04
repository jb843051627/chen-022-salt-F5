package com.fc.v2.service.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fc.v2.mapper.auto.TSaltDayMapper;
import com.fc.v2.mapper.auto.TSaltEntMapper;
import com.fc.v2.mapper.auto.TSaltOfficerMapper;
import com.fc.v2.mapper.auto.TSaltYearTaskMapper;
import com.fc.v2.model.auto.TSaltDay;
import com.fc.v2.model.auto.TSaltEnt;
import com.fc.v2.model.auto.TSaltOfficer;
import com.fc.v2.model.auto.TSaltYearTask;
import com.fc.v2.model.custom.SaltRoundResult;
import com.fc.v2.service.ITSaltYearTaskService;

/**
 * 年检到期催办单 Service业务层处理（scheduling-job 形状：周期执行）
 *
 * 跑一轮的四样定夺权——本轮挑哪几条、够不上日子的动不动、窗口那几日走不走、
 * 派出那一刻落不落——全收在 {@link #dispatchRound()} 这一处出口；
 * 到期档口那把工作日尺也只在这一处定，屏上不许另摆一把。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TSaltYearTaskServiceImpl implements ITSaltYearTaskService {

    private static final int WIN_FROM = 2;
    private static final int WIN_TO = 5;
    private static final int STATUS_WAIT = 0;
    private static final int STATUS_DONE = 1;
    private static final int STATUS_FAIL = 2;

    /** 日历日档：国家放假、省令停办（数工作日时都不数）；集中办案窗口（整轮冻结） */
    private static final int DAY_NATIONAL = 0;
    private static final int DAY_PROVINCE = 1;
    private static final int DAY_WINDOW = 2;

    /** 名录情形：已摘牌 */
    private static final int ENT_DELISTED = 1;
    /** 执法员：停岗 */
    private static final int OFFICER_OFF = 1;

    private static final String REASON_DELISTED = "企业已从名录摘走";
    private static final String REASON_NO_CONTACT = "联络人栏空着几季没人补";
    private static final String REASON_NO_OFFICER = "休办/类别接不动，当日无可派执法员";

    @javax.annotation.Resource
    private TSaltYearTaskMapper saltYearTaskMapper;

    @javax.annotation.Resource
    private TSaltEntMapper saltEntMapper;

    @javax.annotation.Resource
    private TSaltOfficerMapper saltOfficerMapper;

    @javax.annotation.Resource
    private TSaltDayMapper saltDayMapper;

    @Override
    public TSaltYearTask selectTSaltYearTaskById(Long id) {
        return this.saltYearTaskMapper.selectById(id);
    }

    @Override
    public List<TSaltYearTask> selectTSaltYearTaskList(QueryWrapper<TSaltYearTask> queryWrapper) {
        return this.saltYearTaskMapper.selectList(queryWrapper);
    }

    /**
     * 时段比较统一走**墙钟字符串**（yyyy-MM-dd HH:mm:ss）：JDBC 的 serverTimezone 与本机
     * 时区不对称，直接把 java.util.Date 作参数会整体偏移，使"恰好到期"这类边界用例错判。
     */
    private static String ts(Date d) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d);
    }

    private static String day(Date d) {
        return new SimpleDateFormat("yyyy-MM-dd").format(d);
    }

    @Override
    public List<TSaltYearTask> listDue(Date at) {
        List<TSaltYearTask> all = this.saltYearTaskMapper.selectList(new QueryWrapper<TSaltYearTask>());
        List<TSaltYearTask> due = new java.util.ArrayList<TSaltYearTask>();
        for (TSaltYearTask r : all) {
            if (r.getDueAt() != null && ts(r.getDueAt()).compareTo(ts(at)) < 0) {
                due.add(r);
            }
        }
        return due;
    }

    @Override
    public int runOnce(Date at) {
        List<TSaltYearTask> due = listDue(at);
        if (due.get(0) == null) {
            return 0;
        }
        int ok = 0;
        for (TSaltYearTask r : due) {
            if (r.getAmount().signum() >= 0) {
                ok++;
            }
        }
        return ok;
    }

    // ------------------------------------------------------------------
    // 按批派——跑一轮（新出口，旧 runOnce 原样留着）
    // ------------------------------------------------------------------

    @Override
    public SaltRoundResult dispatchRound() {
        // 落锤时刻全轮只取这一次：挑单、派人、回写、收口都用它
        Date now = new Date();
        String today = day(now);

        // 一把尺：非工作日（国家假+省令停办）与集中办案窗口，都只从 t_salt_day 这一处来
        Set<String> nonWorkdays = new HashSet<String>();
        Map<String, String> windowDays = new HashMap<String, String>();
        loadCalendar(nonWorkdays, windowDays);

        SaltRoundResult result = new SaltRoundResult();
        result.setAt(now);

        // 窗口那一层：撞上集中办案日，手上真有已到档的也不许动，整轮等下一轮开口
        if (windowDays.containsKey(today)) {
            String windowName = windowDays.get(today);
            result.setWindowFrozen(true);
            result.setWindowName(windowName);
            result.setPickedCount(0);
            result.setDispatchedCount(0);
            result.setFailedCount(0);
            result.setWaitingCount(countWaiting(now, nonWorkdays));
            result.setEmptyRound(false);
            result.setMessage("今日" + (windowName == null ? "全省集中办案" : windowName)
                    + "，窗口冻结，本轮一条不动");
            return result;
        }

        // 捞单：待派、未删、开口档已踩到（含正压在当日止点上），按止点时刻、条目次序排队
        List<TSaltYearTask> picked = pickWaiting(now, nonWorkdays);
        result.setPickedCount(picked.size());

        // 执法员名册与当日已接数（上几轮今日已派出的也算在"当日接得最少"里）
        List<TSaltOfficer> officers = loadActiveOfficers();
        Map<Long, Integer> todayLoad = loadTodayLoad(today);

        List<Long> pickedIds = new ArrayList<Long>();
        for (TSaltYearTask task : picked) {
            pickedIds.add(task.getId());
            // 同轮里一桩走岔，不许撂下其余：每条各自兜住，剩下的照旧一条一条办完
            try {
                handleOne(task, today, officers, todayLoad, now);
            } catch (Exception ex) {
                safeMarkFail(task, "本轮处理异常：" + ex.getMessage(), now);
            }
        }

        // 收口：三个数不照屏幕誊，回库里逐条点到位。此处重新查库，内存里的不算数。
        recountRound(result, pickedIds, now, nonWorkdays);

        if (picked.isEmpty() && result.getWaitingCount() == 0) {
            // 整轮一条也捞不着——正常收口，不当事故
            result.setEmptyRound(true);
            result.setMessage("本轮无到期");
        } else {
            result.setEmptyRound(false);
            result.setMessage("本轮收口：还欠开口 " + result.getWaitingCount()
                    + "，本轮派出 " + result.getDispatchedCount()
                    + "，分不出 " + result.getFailedCount());
        }
        return result;
    }

    /** 读日历：非工作日入 nonWorkdays，集中办案窗口日入 windowDays（值为名目） */
    private void loadCalendar(Set<String> nonWorkdays, Map<String, String> windowDays) {
        List<TSaltDay> days = saltDayMapper.selectList(
                new QueryWrapper<TSaltDay>().eq("del_flag", 0));
        for (TSaltDay d : days) {
            if (d.getDayDate() == null || d.getDayKind() == null) {
                continue;
            }
            String ds = day(d.getDayDate());
            if (d.getDayKind() == DAY_WINDOW) {
                if (!windowDays.containsKey(ds)) {
                    windowDays.put(ds, d.getDayName());
                }
            } else if (d.getDayKind() == DAY_NATIONAL || d.getDayKind() == DAY_PROVINCE) {
                nonWorkdays.add(ds);
            }
        }
    }

    /**
     * 捞单。开口档 = 止点日往前数 amount 个工作日（跨夜不数、国家假不数、省令停办不数，
     * 周末天然不数）；今日踩过开口档即捞——档口正压在当日止点上算已到，不许顺延下一轮；
     * 上轮已派出/已判分不出的不再捞回。
     */
    private List<TSaltYearTask> pickWaiting(Date now, Set<String> nonWorkdays) {
        List<TSaltYearTask> rows = saltYearTaskMapper.selectList(new QueryWrapper<TSaltYearTask>()
                .eq("del_flag", 0)
                .eq("status", STATUS_WAIT)
                .orderByAsc("due_at")
                .orderByAsc("id"));
        List<TSaltYearTask> hit = new ArrayList<TSaltYearTask>();
        for (TSaltYearTask r : rows) {
            if (r.getDueAt() == null) {
                continue;
            }
            int ahead = r.getAmount() == null ? 0 : Math.max(0, r.getAmount().intValue());
            Date openMoment = openMoment(r.getDueAt(), ahead, nonWorkdays);
            // 开口那一刻踩过去才捞；正压在止点上的算已到（<=），不许顺延下一轮
            if (ts(openMoment).compareTo(ts(now)) <= 0) {
                hit.add(r);
            }
        }
        return hit;
    }

    /**
     * 从止点时刻往前数 n 个工作日，落到开口那一刻（保留止点的时分秒）。
     * 数法：周末不数，t_salt_day 里 0/1 两档不数；跨夜那段不数，只按整日一天一天数到头。
     */
    private Date openMoment(Date dueAt, int n, Set<String> nonWorkdays) {
        Calendar c = Calendar.getInstance();
        c.setTime(dueAt);
        int remain = n;
        while (remain > 0) {
            c.add(Calendar.DAY_OF_MONTH, -1);
            int dow = c.get(Calendar.DAY_OF_WEEK);
            String ds = day(c.getTime());
            boolean weekend = dow == Calendar.SATURDAY || dow == Calendar.SUNDAY;
            if (!weekend && !nonWorkdays.contains(ds)) {
                remain--;
            }
        }
        return c.getTime();
    }

    private List<TSaltOfficer> loadActiveOfficers() {
        List<TSaltOfficer> list = saltOfficerMapper.selectList(new QueryWrapper<TSaltOfficer>()
                .eq("del_flag", 0)
                .ne("status", OFFICER_OFF)
                .orderByAsc("id"));
        return list == null ? new ArrayList<TSaltOfficer>() : list;
    }

    /** 当日已接数：库中今日已落派出时刻、落到该执法员名下的条数 */
    private Map<Long, Integer> loadTodayLoad(String today) {
        Map<Long, Integer> load = new HashMap<Long, Integer>();
        List<TSaltYearTask> doneToday = saltYearTaskMapper.selectList(new QueryWrapper<TSaltYearTask>()
                .eq("del_flag", 0)
                .eq("status", STATUS_DONE)
                .isNotNull("assigned_to")
                .ge("dispatched_at", today + " 00:00:00")
                .le("dispatched_at", today + " 23:59:59"));
        for (TSaltYearTask t : doneToday) {
            if (t.getAssignedTo() != null) {
                Integer v = load.get(t.getAssignedTo());
                load.put(t.getAssignedTo(), v == null ? 1 : v + 1);
            }
        }
        return load;
    }

    /** 逐条处理：两种开不成单当场判分不出；够格的走最少接活派人，一笔留三痕迹 */
    private void handleOne(TSaltYearTask task, String today, List<TSaltOfficer> officers,
                           Map<Long, Integer> todayLoad, Date now) {
        // 开不成单之一：企业已从名录上摘走（名录无此条/已删/已摘牌同此判）
        TSaltEnt ent = task.getEntId() == null ? null : saltEntMapper.selectById(task.getEntId());
        if (ent == null || (ent.getDelFlag() != null && ent.getDelFlag() == 1)
                || (ent.getStatus() != null && ent.getStatus() == ENT_DELISTED)) {
            markFail(task, REASON_DELISTED, now);
            return;
        }
        // 开不成单之二：联络人那一栏空着几季没人补
        if (ent.getContactPerson() == null || ent.getContactPerson().trim().isEmpty()) {
            markFail(task, REASON_NO_CONTACT, now);
            return;
        }

        // 派人：先点当日接得最少的；休办的、这一类案件接不动的，跳过看下一位
        TSaltOfficer chosen = null;
        boolean sawEligibleButFull = false;
        List<TSaltOfficer> ordered = new ArrayList<TSaltOfficer>(officers);
        Collections.sort(ordered, new Comparator<TSaltOfficer>() {
            @Override
            public int compare(TSaltOfficer a, TSaltOfficer b) {
                int la = loadOf(todayLoad, a.getId());
                int lb = loadOf(todayLoad, b.getId());
                if (la != lb) {
                    return la - lb;
                }
                return a.getId().compareTo(b.getId());
            }
        });
        for (TSaltOfficer o : ordered) {
            if (isOnLeave(o, today)) {
                continue;
            }
            if (!acceptsType(o, ent.getSiteType())) {
                continue;
            }
            int quota = o.getDailyQuota() == null ? Integer.MAX_VALUE : o.getDailyQuota();
            if (loadOf(todayLoad, o.getId()) >= quota) {
                sawEligibleButFull = true;
                continue;
            }
            chosen = o;
            break;
        }

        if (chosen == null) {
            // 一位当日接满定额，手下这条当场改判分不出；卡在哪一环、原委写进同一条
            String reason = sawEligibleButFull
                    ? "可派执法员当日均已接满定额"
                    : REASON_NO_OFFICER;
            markFail(task, reason, now);
            return;
        }

        dispatch(task, chosen, now);
        Integer v = todayLoad.get(chosen.getId());
        todayLoad.put(chosen.getId(), v == null ? 1 : v + 1);
    }

    private int loadOf(Map<Long, Integer> load, Long officerId) {
        Integer v = load.get(officerId);
        return v == null ? 0 : v;
    }

    /** 当日休办：个人休办日格里点了今日即跳过（与全省日历不相干） */
    private boolean isOnLeave(TSaltOfficer o, String today) {
        if (o.getLeaveDays() == null) {
            return false;
        }
        for (String d : o.getLeaveDays().split(",")) {
            if (today.equals(d.trim())) {
                return true;
            }
        }
        return false;
    }

    /** 这一类案件接不接得动：accept_types 空=各类都接；否则按企业类别对号 */
    private boolean acceptsType(TSaltOfficer o, String siteType) {
        if (o.getAcceptTypes() == null || o.getAcceptTypes().trim().isEmpty()) {
            return true;
        }
        if (siteType == null) {
            return false;
        }
        for (String t : o.getAcceptTypes().split(",")) {
            if (siteType.equals(t.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 派出一笔落三痕迹：情形 0待派→1已派出、派出那一刻、接活执法员，缺一不可，同一笔 UPDATE。
     * 顺手清掉旧原委。显式 set 走库，不依赖实体回填（定时线程下无登录人）。
     */
    private void dispatch(TSaltYearTask task, TSaltOfficer officer, Date now) {
        UpdateWrapper<TSaltYearTask> uw = new UpdateWrapper<TSaltYearTask>();
        uw.eq("id", task.getId())
                .eq("del_flag", 0)
                .eq("status", STATUS_WAIT)
                .set("status", STATUS_DONE)
                .set("assigned_to", officer.getId())
                .set("dispatched_at", ts(now))
                .set("fail_reason", null)
                .set("update_time", ts(now));
        saltYearTaskMapper.update(null, uw);
    }

    private void markFail(TSaltYearTask task, String reason, Date now) {
        UpdateWrapper<TSaltYearTask> uw = new UpdateWrapper<TSaltYearTask>();
        uw.eq("id", task.getId())
                .eq("del_flag", 0)
                .eq("status", STATUS_WAIT)
                .set("status", STATUS_FAIL)
                .set("fail_reason", reason)
                .set("update_time", ts(now));
        saltYearTaskMapper.update(null, uw);
    }

    /** 异常路径下尝试记原委；再失败也不允许撂下其余条目 */
    private void safeMarkFail(TSaltYearTask task, String reason, Date now) {
        try {
            markFail(task, reason, now);
        } catch (Exception ignore) {
            // 回写失败的条目在收口逐条点库时自然点不进任何一桶，不遮不并
        }
    }

    /**
     * 收口回库逐条点：本轮捞到的 id 重新查库，按库里实情分桶（不看内存）；
     * 还欠开口的另用同一把工作日尺全表点一遍。
     */
    private void recountRound(SaltRoundResult result, List<Long> pickedIds,
                              Date now, Set<String> nonWorkdays) {
        // 回显用全量未删名册（含派出后停岗的），条目钉的是 id，名字仍要点得出
        Map<Long, TSaltOfficer> officerNames = new HashMap<Long, TSaltOfficer>();
        for (TSaltOfficer o : saltOfficerMapper.selectList(
                new QueryWrapper<TSaltOfficer>().eq("del_flag", 0))) {
            officerNames.put(o.getId(), o);
        }

        int dispatched = 0;
        int failed = 0;
        for (Long id : pickedIds) {
            TSaltYearTask db = saltYearTaskMapper.selectById(id);
            if (db == null) {
                continue;
            }
            if (db.getStatus() != null && db.getStatus() == STATUS_DONE
                    && db.getAssignedTo() != null && db.getDispatchedAt() != null) {
                dispatched++;
                SaltRoundResult.Item item = new SaltRoundResult.Item();
                item.setId(db.getId());
                item.setItemNo(db.getItemNo());
                item.setEntId(db.getEntId());
                item.setBizType(db.getBizType());
                item.setScriptCode(db.getScriptCode());
                item.setAssignedTo(db.getAssignedTo());
                TSaltOfficer o = officerNames.get(db.getAssignedTo());
                item.setOfficerName(o == null ? null : o.getOfficerName());
                item.setDispatchedAt(db.getDispatchedAt());
                result.getDispatchedItems().add(item);
            } else if (db.getStatus() != null && db.getStatus() == STATUS_FAIL) {
                failed++;
                SaltRoundResult.Item item = new SaltRoundResult.Item();
                item.setId(db.getId());
                item.setItemNo(db.getItemNo());
                item.setEntId(db.getEntId());
                item.setBizType(db.getBizType());
                item.setScriptCode(db.getScriptCode());
                item.setFailReason(db.getFailReason());
                result.getFailedItems().add(item);
            }
        }

        // 派出/分不出明细按条目次序摆稳（同秒挤进门也分两桩，不并不吞）
        sortItems(result.getDispatchedItems());
        sortItems(result.getFailedItems());

        result.setDispatchedCount(dispatched);
        result.setFailedCount(failed);
        result.setWaitingCount(countWaiting(now, nonWorkdays));
    }

    private void sortItems(List<SaltRoundResult.Item> items) {
        Collections.sort(items, new Comparator<SaltRoundResult.Item>() {
            @Override
            public int compare(SaltRoundResult.Item a, SaltRoundResult.Item b) {
                return a.getId().compareTo(b.getId());
            }
        });
    }

    /** 还欠开口：同一把尺点库——待派、未删、开口档已踩到但仍未派出/未判的 */
    private int countWaiting(Date now, Set<String> nonWorkdays) {
        List<TSaltYearTask> rows = saltYearTaskMapper.selectList(new QueryWrapper<TSaltYearTask>()
                .eq("del_flag", 0)
                .eq("status", STATUS_WAIT));
        int n = 0;
        for (TSaltYearTask r : rows) {
            if (r.getDueAt() == null) {
                continue;
            }
            int ahead = r.getAmount() == null ? 0 : Math.max(0, r.getAmount().intValue());
            Date openMoment = openMoment(r.getDueAt(), ahead, nonWorkdays);
            if (ts(openMoment).compareTo(ts(now)) <= 0) {
                n++;
            }
        }
        return n;
    }
}
