package com.splitbill.model;

public class ExpenseShare {
    private int id;
    private int expenseId;
    private int userId;
    private String userDisplayName;
    private double shareAmount;

    public ExpenseShare() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getExpenseId() { return expenseId; }
    public void setExpenseId(int expenseId) { this.expenseId = expenseId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUserDisplayName() { return userDisplayName; }
    public void setUserDisplayName(String userDisplayName) { this.userDisplayName = userDisplayName; }

    public double getShareAmount() { return shareAmount; }
    public void setShareAmount(double shareAmount) { this.shareAmount = shareAmount; }
}
