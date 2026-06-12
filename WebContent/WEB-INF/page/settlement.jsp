<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>算账 - ${group.name}</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="nav">
    <div class="nav-inner">
        <span class="nav-title">📒 拼单记账本</span>
        <span class="nav-user">${sessionScope.user.displayName} <a href="${pageContext.request.contextPath}/logout" class="btn-small">退出</a></span>
    </div>
</div>

<div class="container">
    <div class="page-header">
        <h2>🧮 算账 — ${group.name}</h2>
        <a href="group?id=${group.id}" class="btn btn-secondary">← 返回</a>
    </div>

    <c:if test="${not empty param.msg}">
        <div class="msg success">${param.msg}</div>
    </c:if>
    <c:if test="${not empty param.error}">
        <div class="msg error">${param.error}</div>
    </c:if>

    <%-- 收支汇总 --%>
    <div class="card">
        <div class="card-header">
            <h3>📊 收支汇总</h3>
        </div>
        <table class="table">
            <thead>
                <tr>
                    <th>成员</th>
                    <th>总消费（平摊）</th>
                    <th>已支付</th>
                    <th>净收支</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach items="${balances}" var="b">
                    <tr>
                        <td><strong>${b.displayName}</strong></td>
                        <td>¥ <fmt:formatNumber value="${b.totalShare}" pattern="#,##0.00"/></td>
                        <c:set var="paidAmount" value="0"/>
                        <c:forEach items="${expenses}" var="e">
                            <c:if test="${e.payerId == b.userId}">
                                <c:set var="paidAmount" value="${paidAmount + e.amount}"/>
                            </c:if>
                        </c:forEach>
                        <td>¥ <fmt:formatNumber value="${paidAmount}" pattern="#,##0.00"/></td>
                        <td>
                            <c:choose>
                                <c:when test="${b.balance > 0}">
                                    <span class="text-green">应收 ¥<fmt:formatNumber value="${b.balance}" pattern="#,##0.00"/></span>
                                </c:when>
                                <c:when test="${b.balance < 0}">
                                    <span class="text-red">应付 ¥<fmt:formatNumber value="${-b.balance}" pattern="#,##0.00"/></span>
                                </c:when>
                                <c:otherwise>
                                    <span class="text-muted">已平 ✓</span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </div>

    <%-- 结算方案 --%>
    <div class="card" style="margin-top: 20px;">
        <div class="card-header">
            <h3>💰 结算方案</h3>
        </div>
        <c:choose>
            <c:when test="${empty settlements}">
                <div class="empty-state">
                    <p>🎉 所有人账已平，无需结算！</p>
                </div>
            </c:when>
            <c:otherwise>
                <table class="table settlement-table">
                    <thead>
                        <tr>
                            <th>付款人</th>
                            <th style="width:60px;"></th>
                            <th>收款人</th>
                            <th style="width:120px;">金额</th>
                            <th style="width:200px;">状态</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach items="${settlements}" var="s">
                            <tr>
                                <td><span class="from-user">${s.fromUserName}</span></td>
                                <td style="text-align:center; color:#999; font-size:13px;">→</td>
                                <td><span class="to-user">${s.toUserName}</span></td>
                                <td><span class="settlement-money">¥<fmt:formatNumber value="${s.amount}" pattern="#,##0.00"/></span></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${s.confirmed}">
                                            <span class="confirmed-badge">✅ 已确认收款</span>
                                            <span class="text-muted" style="font-size:11px; margin-left:4px;">${s.confirmedAt}</span>
                                        </c:when>
                                        <c:when test="${sessionScope.user.id == s.toUserId}">
                                            <form action="${pageContext.request.contextPath}/group" method="post" style="display:inline" onsubmit="return confirmConfirm(this, '${s.fromUserName}', ${s.amount})">
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
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
                <div class="settlement-note">
                    <p>💡 按照上面的方案转账后，所有人的账就平了！</p>
                </div>
            </c:otherwise>
        </c:choose>
    </div>

    <%-- 全部账单明细 --%>
    <div class="card" style="margin-top: 20px;">
        <div class="card-header">
            <h3>📋 全部账单明细</h3>
        </div>
        <c:choose>
            <c:when test="${empty expenses}">
                <div class="empty-state"><p>暂无账单</p></div>
            </c:when>
            <c:otherwise>
                <c:forEach items="${expenses}" var="e">
                    <div class="expense-item">
                        <div class="expense-header">
                            <strong>${e.description}</strong>
                            <span class="expense-amount">¥<fmt:formatNumber value="${e.amount}" pattern="#,##0.00"/></span>
                        </div>
                        <div class="expense-meta">
                            <span>👤 ${e.payerDisplayName} 付</span>
                            <span>📅 ${e.expenseDate}</span>
                        </div>
                        <div class="expense-shares">
                            <span class="share-label">分摊：</span>
                            <c:forEach items="${e.shares}" var="s">
                                <span class="share-tag">${s.userDisplayName} ¥<fmt:formatNumber value="${s.shareAmount}" pattern="#,##0.00"/></span>
                            </c:forEach>
                        </div>
                    </div>
                </c:forEach>
            </c:otherwise>
        </c:choose>

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
    </div>
</div>
<script>
/** 确认收款：弹窗询问 → 禁用按钮防重复提交 */
function confirmConfirm(form, fromUserName, amount) {
    if (!confirm('确定已收到 ' + fromUserName + ' 的 ¥' + amount.toFixed(2) + ' 吗？')) {
        return false;
    }
    var btn = form.querySelector('.btn-confirm');
    btn.disabled = true;
    btn.textContent = '⏳ 确认中...';
    return true;
}
</script>
</body>
</html>
