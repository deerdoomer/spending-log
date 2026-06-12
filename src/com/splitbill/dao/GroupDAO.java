package com.splitbill.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.splitbill.model.BillGroup;
import com.splitbill.model.GroupMember;

public class GroupDAO {

    /** 创建群组 */
    public int createGroup(String name, String description, int createdBy) {
        String sql = "INSERT INTO bill_groups (name, description, created_by) VALUES (?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setInt(3, createdBy);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /** 加入群组（添加成员） */
    public boolean addMember(int groupId, int userId) {
        String sql = "INSERT IGNORE INTO group_members (group_id, user_id) VALUES (?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** 获取用户加入的所有群组 */
    public List<BillGroup> getGroupsByUser(int userId) {
        List<BillGroup> list = new ArrayList<>();
        String sql = "SELECT g.*, u.display_name AS creator_name, " +
                     "(SELECT COUNT(*) FROM group_members WHERE group_id = g.id) AS member_count " +
                     "FROM bill_groups g " +
                     "JOIN users u ON g.created_by = u.id " +
                     "WHERE g.id IN (SELECT group_id FROM group_members WHERE user_id = ?) " +
                     "ORDER BY g.created_at DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BillGroup g = new BillGroup();
                    g.setId(rs.getInt("id"));
                    g.setName(rs.getString("name"));
                    g.setDescription(rs.getString("description"));
                    g.setCreatedBy(rs.getInt("created_by"));
                    g.setCreatedByDisplayName(rs.getString("creator_name"));
                    g.setCreatedAt(rs.getString("created_at"));
                    g.setMemberCount(rs.getInt("member_count"));
                    list.add(g);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** 获取群组信息 */
    public BillGroup getGroupById(int groupId) {
        String sql = "SELECT g.*, u.display_name AS creator_name, " +
                     "(SELECT COUNT(*) FROM group_members WHERE group_id = g.id) AS member_count " +
                     "FROM bill_groups g " +
                     "JOIN users u ON g.created_by = u.id " +
                     "WHERE g.id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BillGroup g = new BillGroup();
                    g.setId(rs.getInt("id"));
                    g.setName(rs.getString("name"));
                    g.setDescription(rs.getString("description"));
                    g.setCreatedBy(rs.getInt("created_by"));
                    g.setCreatedByDisplayName(rs.getString("creator_name"));
                    g.setCreatedAt(rs.getString("created_at"));
                    g.setMemberCount(rs.getInt("member_count"));
                    return g;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** 获取群组的所有成员 */
    public List<GroupMember> getMembers(int groupId) {
        List<GroupMember> list = new ArrayList<>();
        String sql = "SELECT gm.*, u.display_name AS user_display_name " +
                     "FROM group_members gm JOIN users u ON gm.user_id = u.id " +
                     "WHERE gm.group_id = ? ORDER BY u.display_name";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    GroupMember m = new GroupMember();
                    m.setId(rs.getInt("id"));
                    m.setGroupId(rs.getInt("group_id"));
                    m.setUserId(rs.getInt("user_id"));
                    m.setUserDisplayName(rs.getString("user_display_name"));
                    m.setJoinedAt(rs.getString("joined_at"));
                    list.add(m);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** 根据用户名搜索用户所在的群组（用于加入群组时的查找功能） */
    public List<BillGroup> getGroupsByUserUsername(String username) {
        List<BillGroup> list = new ArrayList<>();
        String sql = "SELECT DISTINCT g.*, u.display_name AS creator_name, " +
                     "(SELECT COUNT(*) FROM group_members WHERE group_id = g.id) AS member_count " +
                     "FROM bill_groups g " +
                     "JOIN users u ON g.created_by = u.id " +
                     "JOIN group_members gm ON gm.group_id = g.id " +
                     "JOIN users mu ON mu.id = gm.user_id " +
                     "WHERE mu.username LIKE ? " +
                     "ORDER BY g.created_at DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + username + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BillGroup g = new BillGroup();
                    g.setId(rs.getInt("id"));
                    g.setName(rs.getString("name"));
                    g.setDescription(rs.getString("description"));
                    g.setCreatedBy(rs.getInt("created_by"));
                    g.setCreatedByDisplayName(rs.getString("creator_name"));
                    g.setCreatedAt(rs.getString("created_at"));
                    g.setMemberCount(rs.getInt("member_count"));
                    list.add(g);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** 解散群组（仅创建者可操作，数据库级联删除所有账单和成员） */
    public boolean deleteGroup(int groupId, int userId) {
        String sql = "DELETE FROM bill_groups WHERE id = ? AND created_by = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** 检查用户是否是群组成员 */
    public boolean isMember(int groupId, int userId) {
        String sql = "SELECT COUNT(*) FROM group_members WHERE group_id = ? AND user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
