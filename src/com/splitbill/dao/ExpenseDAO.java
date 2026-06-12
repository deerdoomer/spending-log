package com.splitbill.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.splitbill.model.Expense;
import com.splitbill.model.ExpenseShare;
import com.splitbill.model.Settlement;

public class ExpenseDAO {

    /** 添加账单（含分摊明细） */
    public boolean addExpense(int groupId, int payerId, double amount, String description,
                              List<Integer> shareUserIds) {
        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            // 插入账单
            String sql1 = "INSERT INTO expenses (group_id, payer_id, amount, description) VALUES (?, ?, ?, ?)";
            PreparedStatement ps1 = conn.prepareStatement(sql1, Statement.RETURN_GENERATED_KEYS);
            ps1.setInt(1, groupId);
            ps1.setInt(2, payerId);
            ps1.setDouble(3, amount);
            ps1.setString(4, description);
            ps1.executeUpdate();

            int expenseId;
            try (ResultSet rs = ps1.getGeneratedKeys()) {
                if (!rs.next()) { conn.rollback(); return false; }
                expenseId = rs.getInt(1);
            }
            ps1.close();

            // 平摊金额
            double shareAmount = amount / shareUserIds.size();
            // 处理分钱时的精度问题：最后一个人承担余数
            double totalShared = 0;
            String sql2 = "INSERT INTO expense_shares (expense_id, user_id, share_amount) VALUES (?, ?, ?)";
            PreparedStatement ps2 = conn.prepareStatement(sql2);

            for (int i = 0; i < shareUserIds.size(); i++) {
                int userId = shareUserIds.get(i);
                double sAmount = (i == shareUserIds.size() - 1)
                        ? Math.round((amount - totalShared) * 100.0) / 100.0
                        : Math.round(shareAmount * 100.0) / 100.0;
                totalShared += sAmount;

                ps2.setInt(1, expenseId);
                ps2.setInt(2, userId);
                ps2.setDouble(3, sAmount);
                ps2.addBatch();
            }
            ps2.executeBatch();
            ps2.close();

            conn.commit();
            return true;
        } catch (SQLException e) {
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            return false;
        } finally {
            try { if (conn != null) conn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
            DBUtil.close(conn);
        }
    }

    /** 获取群组的所有账单 */
    public List<Expense> getExpensesByGroup(int groupId) {
        List<Expense> list = new ArrayList<>();
        String sql = "SELECT e.*, u.display_name AS payer_name " +
                     "FROM expenses e JOIN users u ON e.payer_id = u.id " +
                     "WHERE e.group_id = ? ORDER BY e.expense_date DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Expense e = new Expense();
                    e.setId(rs.getInt("id"));
                    e.setGroupId(rs.getInt("group_id"));
                    e.setPayerId(rs.getInt("payer_id"));
                    e.setPayerDisplayName(rs.getString("payer_name"));
                    e.setAmount(rs.getDouble("amount"));
                    e.setDescription(rs.getString("description"));
                    e.setExpenseDate(rs.getString("expense_date"));
                    e.setShares(getSharesByExpense(e.getId()));
                    list.add(e);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** 获取某个账单的分摊明细 */
    private List<ExpenseShare> getSharesByExpense(int expenseId) {
        List<ExpenseShare> list = new ArrayList<>();
        String sql = "SELECT es.*, u.display_name AS user_name " +
                     "FROM expense_shares es JOIN users u ON es.user_id = u.id " +
                     "WHERE es.expense_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, expenseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ExpenseShare s = new ExpenseShare();
                    s.setId(rs.getInt("id"));
                    s.setExpenseId(rs.getInt("expense_id"));
                    s.setUserId(rs.getInt("user_id"));
                    s.setUserDisplayName(rs.getString("user_name"));
                    s.setShareAmount(rs.getDouble("share_amount"));
                    list.add(s);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** 获取已确认转账对各用户净收支的调整值（正=对方欠减少，负=债权减少） */
    private Map<Integer, Double> getTransferAdjustments(int groupId) {
        Map<Integer, Double> adj = new HashMap<>();
        String sql = "SELECT from_user_id, to_user_id, amount FROM transfers WHERE group_id=?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int fromUid = rs.getInt("from_user_id");
                    int toUid = rs.getInt("to_user_id");
                    double amount = rs.getDouble("amount");
                    // 付款人债务减少 → balance +amount
                    adj.merge(fromUid, amount, Double::sum);
                    // 收款人债权减少 → balance -amount
                    adj.merge(toUid, -amount, Double::sum);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return adj;
    }

    /**
     * 计算群组中的结算方案
     * 返回一个列表，表示谁欠谁多少钱（已扣除已确认转账）
     */
    public List<Settlement> calculateSettlements(int groupId) {
        // 1. 计算每个人应该付的总金额（所有分摊之和）
        Map<Integer, Double> shouldPay = new HashMap<>();
        String sql1 = "SELECT es.user_id, SUM(es.share_amount) AS total_share " +
                      "FROM expense_shares es " +
                      "JOIN expenses e ON es.expense_id = e.id " +
                      "WHERE e.group_id = ? GROUP BY es.user_id";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql1)) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    shouldPay.put(rs.getInt("user_id"), rs.getDouble("total_share"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }

        // 2. 计算每个人已经付的总金额
        Map<Integer, Double> paid = new HashMap<>();
        String sql2 = "SELECT e.payer_id, SUM(e.amount) AS total_paid " +
                      "FROM expenses e WHERE e.group_id = ? GROUP BY e.payer_id";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql2)) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    paid.put(rs.getInt("payer_id"), rs.getDouble("total_paid"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }

        // 3. 计算每个人的净收支（正=别人欠他，负=他欠别人）
        Map<Integer, Double> balance = new HashMap<>();
        // 先拿到所有出现过的用户
        java.util.Set<Integer> allUsers = new java.util.HashSet<>();
        allUsers.addAll(shouldPay.keySet());
        allUsers.addAll(paid.keySet());

        for (int uid : allUsers) {
            double p = paid.getOrDefault(uid, 0.0);
            double s = shouldPay.getOrDefault(uid, 0.0);
            balance.put(uid, Math.round((p - s) * 100.0) / 100.0);
        }

        // 3b. 用已确认转账调整余额（已转账的部分视为已结清）
        Map<Integer, Double> transferAdj = getTransferAdjustments(groupId);
        for (Map.Entry<Integer, Double> entry : transferAdj.entrySet()) {
            int uid = entry.getKey();
            double adj = entry.getValue();
            balance.merge(uid, adj, (oldVal, adjVal) ->
                Math.round((oldVal + adjVal) * 100.0) / 100.0);
        }

        // 4. 用贪心算法生成结算方案（正数的收钱，负数的付钱）
        List<Settlement> settlements = new ArrayList<>();
        List<Map.Entry<Integer, Double>> creditors = new ArrayList<>();  // 应收
        List<Map.Entry<Integer, Double>> debtors = new ArrayList<>();    // 应付

        for (Map.Entry<Integer, Double> entry : balance.entrySet()) {
            if (entry.getValue() > 0.01) {
                creditors.add(entry);
            } else if (entry.getValue() < -0.01) {
                debtors.add(entry);
            }
        }

        // 获取用户姓名的 map
        com.splitbill.dao.UserDAO userDAO = new com.splitbill.dao.UserDAO();

        int ci = 0, di = 0;
        while (ci < creditors.size() && di < debtors.size()) {
            double creditAmount = creditors.get(ci).getValue();
            double debitAmount = -debtors.get(di).getValue();
            double settleAmount = Math.min(creditAmount, debitAmount);
            settleAmount = Math.round(settleAmount * 100.0) / 100.0;

            if (settleAmount >= 0.01) {
                int fromUid = debtors.get(di).getKey();
                int toUid = creditors.get(ci).getKey();

                String fromName = userDAO.findById(fromUid).getDisplayName();
                String toName = userDAO.findById(toUid).getDisplayName();

                settlements.add(new Settlement(fromUid, fromName, toUid, toName, settleAmount));
            }

            if (creditAmount <= debitAmount) {
                ci++;
            } else {
                creditors.get(ci).setValue(creditAmount - settleAmount);
            }
            if (debitAmount <= creditAmount) {
                di++;
            } else {
                debtors.get(di).setValue(-(debitAmount - settleAmount));
            }
        }

        return settlements;
    }

    /** 获取用户在某个群组中的总消费 */
    public double getUserTotalShare(int groupId, int userId) {
        String sql = "SELECT COALESCE(SUM(es.share_amount), 0) FROM expense_shares es " +
                     "JOIN expenses e ON es.expense_id = e.id WHERE e.group_id = ? AND es.user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /** 用户在某个群组中的净收支（含已确认转账调整） */
    public double getUserBalance(int groupId, int userId) {
        String sqlPaid = "SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE group_id = ? AND payer_id = ?";
        String sqlShare = "SELECT COALESCE(SUM(es.share_amount), 0) FROM expense_shares es " +
                          "JOIN expenses e ON es.expense_id = e.id WHERE e.group_id = ? AND es.user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps1 = conn.prepareStatement(sqlPaid);
             PreparedStatement ps2 = conn.prepareStatement(sqlShare)) {
            ps1.setInt(1, groupId);
            ps1.setInt(2, userId);
            ps2.setInt(1, groupId);
            ps2.setInt(2, userId);

            double paid = 0, share = 0;
            try (ResultSet rs = ps1.executeQuery()) { if (rs.next()) paid = rs.getDouble(1); }
            try (ResultSet rs = ps2.executeQuery()) { if (rs.next()) share = rs.getDouble(1); }
            double base = Math.round((paid - share) * 100.0) / 100.0;

            // 转账调整：付款方债务减少(+)，收款方债权减少(-)
            double transferAdj = 0;
            String sqlAdj = "SELECT " +
                "  COALESCE(SUM(CASE WHEN from_user_id = ? THEN amount ELSE 0 END), 0) " +
                "- COALESCE(SUM(CASE WHEN to_user_id = ? THEN amount ELSE 0 END), 0) " +
                "FROM transfers WHERE group_id = ?";
            try (PreparedStatement ps3 = conn.prepareStatement(sqlAdj)) {
                ps3.setInt(1, userId);
                ps3.setInt(2, userId);
                ps3.setInt(3, groupId);
                try (ResultSet rs = ps3.executeQuery()) {
                    if (rs.next()) transferAdj = rs.getDouble(1);
                }
            }

            return Math.round((base + transferAdj) * 100.0) / 100.0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
