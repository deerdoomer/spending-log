# 结算确认功能设计文档

## 概述

在群组算账功能的结算方案中，当系统计算出"X 付给 Y ¥XX.XX"时，收款人 Y 可以点击确认按钮，表示已收到 X 的转账。确认后，该笔转账会记录到群组账单中。

## 动机

结算方案只是理论计算结果，实际转账发生在 App 之外。本功能让团队成员可以标记哪些结算已经实际完成，避免重复催收和混淆。

## 数据库变更

新建 `transfers` 表：

```sql
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
);
```

每条记录表示一笔已确认的结算转账。`confirmed_by` 是确认人的用户 ID（即收款人 Y）。

## Model 层

### Transfer.java（新建）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | int | 主键 |
| groupId | int | 群组 ID |
| fromUserId | int | 付款人 ID |
| fromUserName | String | 付款人显示名 |
| toUserId | int | 收款人 ID |
| toUserName | String | 收款人显示名 |
| amount | double | 金额 |
| confirmedBy | int | 确认人 ID |
| confirmedAt | String | 确认时间 |

### Settlement.java（修改）

新增两个字段，用于视图层判断确认状态：

| 字段 | 类型 | 说明 |
|------|------|------|
| confirmed | boolean | 是否已被收款人确认 |
| confirmedAt | String | 确认时间（confirmed=true 时有值） |

## DAO 层

### TransferDAO.java（新建）

| 方法 | 功能 |
|------|------|
| `confirmTransfer(groupId, fromUserId, toUserId, amount, confirmedBy)` | 插入一条确认记录 |
| `getTransfersByGroup(groupId)` | 获取群组所有已确认转账 |
| `isTransferConfirmed(groupId, fromUserId, toUserId, amount)` | 判断某笔结算方案是否已被确认 |

`isTransferConfirmed` 使用 `from_user_id`、`to_user_id`、`amount` 三字段匹配，因为结算金额是精确到分的浮点数，同组内不可能有两条完全相同的"X 付给 Y 金额 Z"的记录。

## Servlet 层

### ExpenseServlet.java 修改

**doGet** — 加载 settlement 页面时：
1. 调用 `TransferDAO.getTransfersByGroup(groupId)` 获取已确认转账列表
2. 遍历 `settlements`，对每一条用 `isTransferConfirmed` 匹配已确认记录
3. 若匹配到，设置 `settlement.confirmed = true` 和 `settlement.confirmedAt`
4. 将 `transfers`（已确认列表）也传入 request

**doPost** — 新增动作 `confirmTransfer`：
1. 参数：`groupId`, `fromUserId`, `toUserId`, `amount`, `confirmedBy`（当前会话用户 ID）
2. 权限校验：`confirmedBy` 必须等于 `toUserId`（只有收款人才能确认）
3. 避免重复确认：先查是否已存在
4. 插入 `transfers` 表
5. 重定向回结算页面 `group?id=${groupId}&action=settlement`

## 视图层

### settlement.jsp 修改

#### 结算方案卡片区域

每个结算项根据状态显示不同的 UI：

| 状态 | 显示内容 |
|------|----------|
| 已确认 | 卡片右侧显示 `✅ 已确认收款` 绿色标签 + 确认时间 |
| 未确认，当前用户是收款人 | 卡片右侧显示 `<button>✅ 确认收款</button>` 绿色按钮 |
| 未确认，当前用户不是收款人 | 卡片右侧显示 `⏳ 等待对方确认` 橙色标签 |

交互流程：
1. 收款人 Y 点击"确认收款"
2. 弹出确认提醒"确定已收到来自 X 的 ¥XX.XX？"
3. 确认后 POST 到 `/group` 的 `confirmTransfer` 动作
4. 刷新页面，显示已确认状态

#### 账单明细区域

在原有消费记录列表下方新增"转账记录"区块：

```
📋 全部账单明细

--- 消费记录（原有） ---
...（不变）...

--- 转账记录（新增） ---
📤 Alice → Bob    ¥30.00    2026-06-12 14:30  已确认
```

已确认转账按时间倒序排列，与消费记录分开展示，避免混淆。

### groupDetail.jsp 修改

在"结算建议"卡片中，已确认的结算项增加 `✅` 标记，其他内容不变。

## CSS 样式补充

```css
/* 确认按钮 */
.btn-confirm { background: #16a34a; color: #fff; padding: 6px 16px; border-radius: 6px; border: none; cursor: pointer; font-size: 13px; font-weight: 600; }
.btn-confirm:hover { background: #15803d; }

/* 已确认标签 */
.confirmed-badge { color: #16a34a; font-weight: 600; font-size: 13px; display: flex; align-items: center; gap: 4px; }

/* 待确认标签 */
.pending-badge { color: #d97706; font-weight: 600; font-size: 13px; display: flex; align-items: center; gap: 4px; }

/* 转账记录条目 */
.transfer-item { padding: 14px 20px; border-bottom: 1px solid #f0f2f5; background: #fafcf8; border-left: 3px solid #16a34a; margin-bottom: 2px; }
.transfer-item:last-child { border-bottom: none; }
.transfer-header { display: flex; justify-content: space-between; align-items: center; }
.transfer-desc { font-size: 14px; }
.transfer-amount { font-size: 16px; font-weight: 700; color: #16a34a; }
.transfer-meta { font-size: 12px; color: #999; margin-top: 4px; }
```

## 结算算法的影响

**不改变**结算算法。确认功能仅跟踪结算是否完成，不影响 `calculateSettlements()` 的结果。这样做的好处是：
- 如果某人确认后又想反悔，逻辑清晰
- 算账结果始终反映"理想结算方案"
- 确认只是标记"这个方案我已经执行了"

## 涉及文件清单

| 文件 | 操作类型 |
|------|----------|
| `splitbill.sql` | 追加 transfers 建表语句 |
| `src/com/splitbill/model/Transfer.java` | 新建 |
| `src/com/splitbill/model/Settlement.java` | 修改（加 2 字段） |
| `src/com/splitbill/dao/TransferDAO.java` | 新建 |
| `src/com/splitbill/servlet/ExpenseServlet.java` | 修改 |
| `WebContent/WEB-INF/page/settlement.jsp` | 修改 |
| `WebContent/WEB-INF/page/groupDetail.jsp` | 小修改 |
| `WebContent/css/style.css` | 追加样式 |

## 安全考虑

- 只有收款人（settlement 中的 `toUserId`）才能点击确认按钮
- 服务端必须校验当前用户 ID 等于 `toUserId`
- 禁止重复确认同一笔结算（幂等性检查）
