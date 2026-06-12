package com.splitbill.model;

public class UserBalance {
    private int userId;
    private String displayName;
    private double totalShare;   // 总消费额（分摊）
    private double balance;      // 净收支（正 = 应收，负 = 应付）

    public UserBalance() {}

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public double getTotalShare() { return totalShare; }
    public void setTotalShare(double totalShare) { this.totalShare = totalShare; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
}
