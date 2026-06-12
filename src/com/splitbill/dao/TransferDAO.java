package com.splitbill.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.splitbill.model.Transfer;

public class TransferDAO {

    /** 确认转账 —— 插入一条转账记录（幂等：如果已确认仍返回成功） */
    public boolean confirmTransfer(int groupId, int fromUserId, int toUserId, double amount, int confirmedBy) {
        // 幂等：如果已确认，直接返回成功
        if (isTransferConfirmed(groupId, fromUserId, toUserId, amount)) {
            return true;
        }

        String sql = "INSERT INTO transfers (group_id, from_user_id, to_user_id, amount, confirmed_by) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setInt(2, fromUserId);
            ps.setInt(3, toUserId);
            ps.setBigDecimal(4, BigDecimal.valueOf(amount));
            ps.setInt(5, confirmedBy);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            // 检查是否刚刚被另一个请求插入成功（竞态条件）
            if (isTransferConfirmed(groupId, fromUserId, toUserId, amount)) {
                return true;
            }
            // 打印完整错误信息到服务器日志
            System.err.println("[TransferDAO] 确认收款失败: groupId=" + groupId
                + " fromUserId=" + fromUserId + " toUserId=" + toUserId
                + " amount=" + amount + " confirmedBy=" + confirmedBy);
            System.err.println("[TransferDAO] SQL错误: code=" + e.getErrorCode()
                + " state=" + e.getSQLState() + " msg=" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /** 检查转账是否已被确认 */
    public boolean isTransferConfirmed(int groupId, int fromUserId, int toUserId, double amount) {
        String sql = "SELECT COUNT(*) FROM transfers WHERE group_id=? AND from_user_id=? AND to_user_id=? AND amount=?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setInt(2, fromUserId);
            ps.setInt(3, toUserId);
            ps.setBigDecimal(4, BigDecimal.valueOf(amount));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** 获取群组的所有转账记录 */
    public List<Transfer> getTransfersByGroup(int groupId) {
        List<Transfer> list = new ArrayList<>();
        String sql = "SELECT t.*, fu.display_name AS from_user_name, tu.display_name AS to_user_name " +
                     "FROM transfers t " +
                     "JOIN users fu ON t.from_user_id = fu.id " +
                     "JOIN users tu ON t.to_user_id = tu.id " +
                     "WHERE t.group_id=? ORDER BY t.confirmed_at DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapTransfer(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** 获取单条转账记录（精确匹配四个条件） */
    public Transfer getTransfer(int groupId, int fromUserId, int toUserId, double amount) {
        String sql = "SELECT t.*, fu.display_name AS from_user_name, tu.display_name AS to_user_name " +
                     "FROM transfers t " +
                     "JOIN users fu ON t.from_user_id = fu.id " +
                     "JOIN users tu ON t.to_user_id = tu.id " +
                     "WHERE t.group_id=? AND t.from_user_id=? AND t.to_user_id=? AND t.amount=?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setInt(2, fromUserId);
            ps.setInt(3, toUserId);
            ps.setBigDecimal(4, BigDecimal.valueOf(amount));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapTransfer(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** 将当前行映射为 Transfer 对象 */
    private Transfer mapTransfer(ResultSet rs) throws SQLException {
        Transfer t = new Transfer();
        t.setId(rs.getInt("id"));
        t.setGroupId(rs.getInt("group_id"));
        t.setFromUserId(rs.getInt("from_user_id"));
        t.setFromUserName(rs.getString("from_user_name"));
        t.setToUserId(rs.getInt("to_user_id"));
        t.setToUserName(rs.getString("to_user_name"));
        t.setAmount(rs.getDouble("amount"));
        t.setConfirmedBy(rs.getInt("confirmed_by"));
        t.setConfirmedAt(rs.getString("confirmed_at"));
        return t;
    }
}
