package com.splitbill.model;

/**
 * 转账确认记录 — 对应 transfers 表
 * 表示一笔已确认的结算转账（X 付给 Y 的钱，Y 已确认收到）
 */
public class Transfer {
    private int id;
    private int groupId;
    private int fromUserId;
    private String fromUserName;
    private int toUserId;
    private String toUserName;
    private double amount;
    private int confirmedBy;
    private String confirmedAt;

    public Transfer() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getGroupId() { return groupId; }
    public void setGroupId(int groupId) { this.groupId = groupId; }

    public int getFromUserId() { return fromUserId; }
    public void setFromUserId(int fromUserId) { this.fromUserId = fromUserId; }

    public String getFromUserName() { return fromUserName; }
    public void setFromUserName(String fromUserName) { this.fromUserName = fromUserName; }

    public int getToUserId() { return toUserId; }
    public void setToUserId(int toUserId) { this.toUserId = toUserId; }

    public String getToUserName() { return toUserName; }
    public void setToUserName(String toUserName) { this.toUserName = toUserName; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public int getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(int confirmedBy) { this.confirmedBy = confirmedBy; }

    public String getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(String confirmedAt) { this.confirmedAt = confirmedAt; }
}
