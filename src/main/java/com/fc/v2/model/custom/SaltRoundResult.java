package com.fc.v2.model.custom;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 年检派单"跑一轮"的收口回话。
 * 屏幕上的三个数（还欠开口/本轮派出/分不出）与逐条两处痕迹，
 * 只许取自这一趟回话，不许别处另摆一把尺。
 *
 * @author fuce
 * @date 2026-10-04
 */
public class SaltRoundResult implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 本轮落锤时刻 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date at;

    /** 是否撞在全省集中办案窗口（整轮冻结，已到档也不动） */
    private boolean windowFrozen;

    /** 窗口名目 */
    private String windowName;

    /** 本轮实际捞到、逐条走过的条目数 */
    private int pickedCount;

    /** 收口三数：还欠开口的（到期仍待派） */
    private int waitingCount;

    /** 收口三数：本轮派出的 */
    private int dispatchedCount;

    /** 收口三数：分不出的 */
    private int failedCount;

    /** 派出逐条（回库点出来的两处痕迹：执法员 + 派出时刻） */
    private List<Item> dispatchedItems = new ArrayList<Item>();

    /** 分不出逐条（回库点出来的原委） */
    private List<Item> failedItems = new ArrayList<Item>();

    /** 整轮一条也没捞着的正常收口 */
    private boolean emptyRound;

    /** 收口一句话，空轮回"本轮无到期" */
    private String message;

    /** 逐条回话 */
    public static class Item implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long id;
        private String itemNo;
        private Long entId;
        private Integer bizType;
        private String scriptCode;
        private Long assignedTo;
        private String officerName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date dispatchedAt;
        private String failReason;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getItemNo() {
            return itemNo;
        }

        public void setItemNo(String itemNo) {
            this.itemNo = itemNo;
        }

        public Long getEntId() {
            return entId;
        }

        public void setEntId(Long entId) {
            this.entId = entId;
        }

        public Integer getBizType() {
            return bizType;
        }

        public void setBizType(Integer bizType) {
            this.bizType = bizType;
        }

        public String getScriptCode() {
            return scriptCode;
        }

        public void setScriptCode(String scriptCode) {
            this.scriptCode = scriptCode;
        }

        public Long getAssignedTo() {
            return assignedTo;
        }

        public void setAssignedTo(Long assignedTo) {
            this.assignedTo = assignedTo;
        }

        public String getOfficerName() {
            return officerName;
        }

        public void setOfficerName(String officerName) {
            this.officerName = officerName;
        }

        public Date getDispatchedAt() {
            return dispatchedAt;
        }

        public void setDispatchedAt(Date dispatchedAt) {
            this.dispatchedAt = dispatchedAt;
        }

        public String getFailReason() {
            return failReason;
        }

        public void setFailReason(String failReason) {
            this.failReason = failReason;
        }
    }

    public Date getAt() {
        return at;
    }

    public void setAt(Date at) {
        this.at = at;
    }

    public boolean isWindowFrozen() {
        return windowFrozen;
    }

    public void setWindowFrozen(boolean windowFrozen) {
        this.windowFrozen = windowFrozen;
    }

    public String getWindowName() {
        return windowName;
    }

    public void setWindowName(String windowName) {
        this.windowName = windowName;
    }

    public int getPickedCount() {
        return pickedCount;
    }

    public void setPickedCount(int pickedCount) {
        this.pickedCount = pickedCount;
    }

    public int getWaitingCount() {
        return waitingCount;
    }

    public void setWaitingCount(int waitingCount) {
        this.waitingCount = waitingCount;
    }

    public int getDispatchedCount() {
        return dispatchedCount;
    }

    public void setDispatchedCount(int dispatchedCount) {
        this.dispatchedCount = dispatchedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public List<Item> getDispatchedItems() {
        return dispatchedItems;
    }

    public void setDispatchedItems(List<Item> dispatchedItems) {
        this.dispatchedItems = dispatchedItems;
    }

    public List<Item> getFailedItems() {
        return failedItems;
    }

    public void setFailedItems(List<Item> failedItems) {
        this.failedItems = failedItems;
    }

    public boolean isEmptyRound() {
        return emptyRound;
    }

    public void setEmptyRound(boolean emptyRound) {
        this.emptyRound = emptyRound;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
