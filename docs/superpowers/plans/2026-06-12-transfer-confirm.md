# 结算确认功能 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use subagent-driven-development (recommended) or executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在群组算账的结算方案中，收款人可以点击确认已收款，并生成转账记录显示在账单中。

**Architecture:** 新增 `transfers` 表 + `Transfer` Model + `TransferDAO`，不改动现有结算算法。ExpenseServlet 增加 confirmTransfer 动作处理。JSP 端根据确认状态显示不同 UI。

**Tech Stack:** Java 8+, JSP/Servlet, MySQL 8, JDBC

---

## 文件结构总览

| 文件 | 操作 | 职责 |
|------|------|------|
| `splitbill.sql` | 追加 DDL | 新增 transfers 表 |
| `src/.../model/Transfer.java` | 新建 | 转账记录实体 |
| `src/.../model/Settlement.java` | 修改 | 加 confirmed / confirmedAt 字段 |
| `src/.../dao/TransferDAO.java` | 新建 | 确认/查询转账的数据访问 |
| `src/.../servlet/ExpenseServlet.java` | 修改 | doGet 注入确认状态，doPost 处理确认动作 |
| `WebContent/css/style.css` | 追加 | 确认按钮/标签/转账记录样式 |
| `WebContent/WEB-INF/page/settlement.jsp` | 修改 | 结算卡片加确认 UI，账单区加转账记录 |
| `WebContent/WEB-INF/page/groupDetail.jsp` | 修改 | 结算预览加确认标记 |

---

### Task 1: 数据库 — transfers 表 DDL

**Files:**
- Modify: `splitbill.sql` (末尾追加)

- [ ] **Step 1: 追加建表语句**

打开 `splitbill.sql`，在文件末尾追加以下内容：

```sql
-- 转账确认表
CREATE TABLE IF NOT EXISTS transfers (
    id INT PRIMARY KEY AUTO_INCREMENT,
    group_id INT NOT NULL,
    from_user_id INT NOT NULL,
    to_user_id INT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    confirmed_by INT NOT NULL,
    confirmed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (group_id) REFERENCES bill_groups(id) ON DELETE CASCADE,
    FOREIGN KEY (from_user_id) REFERENCES users(id),
    FOREIGN KEY (to_user_id) REFERENCES users(id),
    FOREIGN KEY (confirmed_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

在 MySQL 中执行此 SQL（或整个脚本重跑）。

---

### Task 2: 新建 Transfer.java Model

**Files:**
- Create: `src/com/splitbill/model/Transfer.java`

- [ ] **Step 1: 新建 Transfer 实体类**

```java
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
```

---

### Task 3: 修改 Settlement.java — 加确认状态字段

**Files:**
- Modify: `src/com/splitbill/model/Settlement.java`

- [ ] **Step 1: 新增 confirmed / confirmedAt 字段及 getter/setter**

修改后的完整文件：

```java
package com.splitbill.model;

/**
 * 结算实体 - 用于表示某人欠某人多少钱
 */
public class Settlement {
    private int fromUserId;
    private String fromUserName;
    private int toUserId;
    private String toUserName;
    private double amount;
    private boolean confirmed;    // 新增：是否已确认收款
    private String confirmedAt;   // 新增：确认时间

    public Settlement() {}

    public Settlement(int fromUserId, String fromUserName, int toUserId, String toUserName, double amount) {
        this.fromUserId = fromUserId;
        this.fromUserName = fromUserName;
        this.toUserId = toUserId;
        this.toUserName = toUserName;
        this.amount = amount;
    }

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

    public boolean isConfirmed() { return confirmed; }
    public void setConfirmed(boolean confirmed) { this.confirmed = confirmed; }

    public String getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(String confirmedAt) { this.confirmedAt = confirmedAt; }
}
```

---

### Task 4: 新建 TransferDAO.java

**Files:**
- Create: `src/com/splitbill/dao/TransferDAO.java`

- [ ] **Step 1: 确认转账方法 confirmTransfer**

```java
package com.splitbill.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.splitbill.model.Transfer;

public class TransferDAO {

    /**
     * 确认一笔结算转账
     * @return true 表示插入成功
     */
    public boolean confirmTransfer(int groupId, int fromUserId, int toUserId, double amount, int confirmedBy) {
        // 避免重复确认
        if (isTransferConfirmed(groupId, fromUserId, toUserId, amount)) {
            return false;
        }
        String sql = "INSERT INTO transfers (group_id, from_user_id, to_user_id, amount, confirmed_by) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setInt(2, fromUserId);
            ps.setInt(3, toUserId);
            ps.setDouble(4, amount);
            ps.setInt(5, confirmedBy);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 判断某笔结算方案是否已被确认
     * 匹配条件：同群组 + 同付款人 + 同收款人 + 同金额
     */
    public boolean isTransferConfirmed(int groupId, int fromUserId, int toUserId, double amount) {
        String sql = "SELECT COUNT(*) FROM transfers WHERE group_id = ? AND from_user_id = ? AND to_user_id = ? AND amount = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setInt(2, fromUserId);
            ps.setInt(3, toUserId);
            ps.setDouble(4, amount);
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

    /**
     * 获取群组内所有已确认的转账记录（按确认时间倒序）
     */
    public List<Transfer> getTransfersByGroup(int groupId) {
        List<Transfer> list = new ArrayList<>();
        String sql = "SELECT t.*, " +
                     "fu.display_name AS from_user_name, " +
                     "tu.display_name AS to_user_name " +
                     "FROM transfers t " +
                     "JOIN users fu ON t.from_user_id = fu.id " +
                     "JOIN users tu ON t.to_user_id = tu.id " +
                     "WHERE t.group_id = ? ORDER BY t.confirmed_at DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
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
                    list.add(t);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * 根据条件查询单条确认记录（用于获取确认时间）
     */
    public Transfer getTransfer(int groupId, int fromUserId, int toUserId, double amount) {
        String sql = "SELECT t.*, " +
                     "fu.display_name AS from_user_name, " +
                     "tu.display_name AS to_user_name " +
                     "FROM transfers t " +
                     "JOIN users fu ON t.from_user_id = fu.id " +
                     "JOIN users tu ON t.to_user_id = tu.id " +
                     "WHERE t.group_id = ? AND t.from_user_id = ? AND t.to_user_id = ? AND t.amount = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setInt(2, fromUserId);
            ps.setInt(3, toUserId);
            ps.setDouble(4, amount);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
```

---

### Task 5: 修改 ExpenseServlet.java — doGet 注入确认状态 / doPost 处理确认动作

**Files:**
- Modify: `src/com/splitbill/servlet/ExpenseServlet.java`

- [ ] **Step 1: 在类中声明 TransferDAO 实例**

在 `expenseDAO` 声明后面追加：

```java
private TransferDAO transferDAO = new TransferDAO();
```

需要同时添加 import：

```java
import com.splitbill.dao.TransferDAO;
import com.splitbill.model.Transfer;
```

- [ ] **Step 2: 修改 doGet 中 settlement 分支，注入确认状态**

找到 `settlement` 分支（约第 87-90 行），原代码：

```java
if ("settlement".equals(action)) {
    req.getRequestDispatcher("/WEB-INF/page/settlement.jsp").forward(req, resp);
    return;
}
```

修改为：

```java
if ("settlement".equals(action)) {
    // 获取已确认的转账记录列表
    List<Transfer> transfers = transferDAO.getTransfersByGroup(groupId);
    // 遍历结算方案，标记已确认项
    for (Settlement s : settlements) {
        if (transferDAO.isTransferConfirmed(groupId, s.getFromUserId(), s.getToUserId(), s.getAmount())) {
            s.setConfirmed(true);
            // 查询确认时间
            Transfer t = transferDAO.getTransfer(groupId, s.getFromUserId(), s.getToUserId(), s.getAmount());
            if (t != null) {
                s.setConfirmedAt(t.getConfirmedAt());
            }
        }
    }
    req.setAttribute("transfers", transfers);
    req.getRequestDispatcher("/WEB-INF/page/settlement.jsp").forward(req, resp);
    return;
}
```

- [ ] **Step 3: 在 doPost 中新增 confirmTransfer 动作处理**

在 `doPost` 方法中，在 `"dissolve".equals(action)` 分支之前插入以下代码：

```java
if ("confirmTransfer".equals(action)) {
    int groupId = Integer.parseInt(req.getParameter("groupId"));
    int fromUserId = Integer.parseInt(req.getParameter("fromUserId"));
    int toUserId = Integer.parseInt(req.getParameter("toUserId"));
    double amount = Double.parseDouble(req.getParameter("amount"));

    // 权限校验：只有收款人才能确认
    if (user.getId() != toUserId) {
        resp.sendRedirect("group?id=" + groupId + "&action=settlement&error=只有收款人才能确认收款");
        return;
    }

    boolean ok = transferDAO.confirmTransfer(groupId, fromUserId, toUserId, amount, user.getId());
    if (ok) {
        resp.sendRedirect("group?id=" + groupId + "&action=settlement&msg=确认收款成功");
    } else {
        resp.sendRedirect("group?id=" + groupId + "&action=settlement&error=确认失败（可能已确认过）");
    }
    return;
}
```

同时也需添加 import（如果前面 Step 1 已加则无需重复）：

```java
import com.splitbill.dao.TransferDAO;
import com.splitbill.model.Transfer;
```

---

### Task 6: 修改 CSS — 追加确认功能相关样式

**Files:**
- Modify: `WebContent/css/style.css`

- [ ] **Step 1: 在文件末尾追加样式**

```css
/* ========== 结算确认功能 ========== */
.btn-confirm {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 6px 16px;
    border-radius: 6px;
    font-size: 13px;
    font-weight: 600;
    border: none;
    cursor: pointer;
    transition: all 0.2s;
    background: #16a34a;
    color: #fff;
}
.btn-confirm:hover { background: #15803d; }

.confirmed-badge {
    color: #16a34a;
    font-weight: 600;
    font-size: 13px;
    display: inline-flex;
    align-items: center;
    gap: 4px;
}
.pending-badge {
    color: #d97706;
    font-weight: 600;
    font-size: 13px;
    display: inline-flex;
    align-items: center;
    gap: 4px;
}

/* 转账记录条目 */
.transfer-item {
    padding: 14px 20px;
    border-bottom: 1px solid #f0f2f5;
    background: #fafcf8;
    border-left: 3px solid #16a34a;
}
.transfer-item:last-child { border-bottom: none; }
.transfer-item .transfer-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 4px;
}
.transfer-item .transfer-desc { font-size: 14px; }
.transfer-item .transfer-amount { font-size: 16px; font-weight: 700; color: #16a34a; }
.transfer-item .transfer-meta { font-size: 12px; color: #999; }

/* 结算状态容器 */
.settlement-status { margin-left: auto; display: flex; align-items: center; }
```

---

### Task 7: 修改 settlement.jsp — 结算卡片确认 UI 及账单区转账记录

**Files:**
- Modify: `WebContent/WEB-INF/page/settlement.jsp`

- [ ] **Step 1: 修改结算方案卡片区域 — 每张卡片增加确认状态**

找到结算方案卡片循环的代码（约第 83-94 行）：

```jsp
<c:forEach items="${settlements}" var="s">
    <div class="settlement-card">
        <div class="settlement-arrow">
            <span class="from-user">${s.fromUserName}</span>
            <span class="arrow">→ 付给 →</span>
            <span class="to-user">${s.toUserName}</span>
        </div>
        <div class="settlement-money">
            💵 ¥<fmt:formatNumber value="${s.amount}" pattern="#,##0.00"/>
        </div>
    </div>
</c:forEach>
```

替换为：

```jsp
<c:forEach items="${settlements}" var="s">
    <div class="settlement-card">
        <div class="settlement-arrow">
            <span class="from-user">${s.fromUserName}</span>
            <span class="arrow">→ 付给 →</span>
            <span class="to-user">${s.toUserName}</span>
        </div>
        <div class="settlement-money">
            💵 ¥<fmt:formatNumber value="${s.amount}" pattern="#,##0.00"/>
        </div>
        <div class="settlement-status">
            <c:choose>
                <c:when test="${s.confirmed}">
                    <span class="confirmed-badge">✅ 已确认收款</span>
                    <span class="text-muted" style="font-size:11px; margin-left:6px;">${s.confirmedAt}</span>
                </c:when>
                <c:when test="${sessionScope.user.id == s.toUserId}">
                    <form action="group" method="post" style="display:inline" onsubmit="return confirm('确定已收到 ${s.fromUserName} 的 ¥<fmt:formatNumber value="${s.amount}" pattern="#,##0.00"/> 吗？')">
                        <input type="hidden" name="action" value="confirmTransfer">
                        <input type="hidden" name="groupId" value="${group.id}">
                        <input type="hidden" name="fromUserId" value="${s.fromUserId}">
                        <input type="hidden" name="toUserId" value="${s.toUserId}">
                        <input type="hidden" name="amount" value="${s.amount}">
                        <button type="submit" class="btn-confirm">✅ 确认收款</button>
                    </form>
                </c:when>
                <c:otherwise>
                    <span class="pending-badge">⏳ 等待对方确认</span>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</c:forEach>
```

- [ ] **Step 2: 在账单明细区域底部追加转账记录区块**

找到 `全部账单明细` 卡片（约第 103-134 行），在 `</c:choose>` 之后（即账单列表结束处）追加以下代码：

```jsp
<%-- 转账记录 --%>
<c:if test="${not empty transfers}">
    <div style="margin-top:12px; padding:0; border-top:2px dashed #e8ecf1;">
        <div style="padding:12px 20px; font-size:13px; color:#666; font-weight:600;">📤 转账记录</div>
        <c:forEach items="${transfers}" var="t">
            <div class="transfer-item">
                <div class="transfer-header">
                    <span class="transfer-desc">${t.fromUserName} → ${t.toUserName}</span>
                    <span class="transfer-amount">¥<fmt:formatNumber value="${t.amount}" pattern="#,##0.00"/></span>
                </div>
                <div class="transfer-meta">✅ 已确认 &nbsp;·&nbsp; ${t.confirmedAt}</div>
            </div>
        </c:forEach>
    </div>
</c:if>
```

放在 `</div>`（card 的结束标签）之前即可。

- [ ] **Step 3: 添加消息提示区域（在页面顶部）**

在结算页面 `page-header` 下方（约第 24 行 `<div class="container">` 之后），添加消息显示：

```jsp
<c:if test="${not empty param.msg}">
    <div class="msg success">${param.msg}</div>
</c:if>
<c:if test="${not empty param.error}">
    <div class="msg error">${param.error}</div>
</c:if>
```

---

### Task 8: 修改 groupDetail.jsp — 结算预览增加确认标记

**Files:**
- Modify: `WebContent/WEB-INF/page/groupDetail.jsp`

- [ ] **Step 1: 在结算预览项中增加确认标记**

找到结算预览循环（约第 131-138 行）：

```jsp
<c:forEach items="${settlements}" var="s">
    <div class="settlement-item">
        <span class="text-red">${s.fromUserName}</span>
        <span>  → 给 →  </span>
        <span class="text-green">${s.toUserName}</span>
        <span class="settlement-amount">¥<fmt:formatNumber value="${s.amount}" pattern="#,##0.00"/></span>
    </div>
</c:forEach>
```

替换为：

```jsp
<c:forEach items="${settlements}" var="s">
    <div class="settlement-item">
        <span class="text-red">${s.fromUserName}</span>
        <span> → 给 → </span>
        <span class="text-green">${s.toUserName}</span>
        <span class="settlement-amount">¥<fmt:formatNumber value="${s.amount}" pattern="#,##0.00"/></span>
        <c:if test="${s.confirmed}">
            <span class="confirmed-badge" style="margin-left:8px;">✅</span>
        </c:if>
    </div>
</c:forEach>
```

---

## 实施顺序（按依赖关系）

1. **Task 1**（SQL）— 先建表，不依赖其他代码
2. **Task 2**（Transfer.java）— 独立新文件
3. **Task 3**（Settlement.java）— 改 Model，独立修改
4. **Task 4**（TransferDAO.java）— 依赖 Task 1（表存在）和 Task 2（Transfer 类）
5. **Task 5**（ExpenseServlet.java）— 依赖 Task 3 和 Task 4
6. **Task 6**（CSS）— 独立追加，可在前端修改前后随时做
7. **Task 7**（settlement.jsp）— 依赖 Task 5（数据能正确传入）
8. **Task 8**（groupDetail.jsp）— 依赖 Task 5

建议顺序：Task 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8

---

## 检验方法

1. 运行应用，进入一个群组，添加几笔账单后进入"算账"页面
2. 确认结算方案卡片正常显示，每个卡片有对应状态（等待/可确认/已确认）
3. 以收款人身份登录，点击"确认收款"，确认弹窗正常
4. 确认后页面刷新，卡片显示"✅ 已确认收款"
5. 查看账单明细区，底部显示已确认的转账记录
6. 退回群组详情页，结算预览中已确认项显示 ✅ 标记
7. 以非收款人身份登录，看到的是"⏳ 等待对方确认"
