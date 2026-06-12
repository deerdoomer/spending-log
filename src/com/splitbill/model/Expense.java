package com.splitbill.model;

import java.util.List;

public class Expense {
    private int id;
    private int groupId;
    private int payerId;
    private String payerDisplayName;
    private double amount;
    private String description;
    private String expenseDate;
    private List<ExpenseShare> shares;

    public Expense() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getGroupId() { return groupId; }
    public void setGroupId(int groupId) { this.groupId = groupId; }

    public int getPayerId() { return payerId; }
    public void setPayerId(int payerId) { this.payerId = payerId; }

    public String getPayerDisplayName() { return payerDisplayName; }
    public void setPayerDisplayName(String payerDisplayName) { this.payerDisplayName = payerDisplayName; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getExpenseDate() { return expenseDate; }
    public void setExpenseDate(String expenseDate) { this.expenseDate = expenseDate; }

    public List<ExpenseShare> getShares() { return shares; }
    public void setShares(List<ExpenseShare> shares) { this.shares = shares; }
}
