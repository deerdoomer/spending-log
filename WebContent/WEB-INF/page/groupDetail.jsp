<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>${group.name} - 拼单记账本</title>
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
        <div>
            <h2>${group.name}</h2>
            <p class="text-muted">${group.description}</p>
            <p class="text-muted" style="font-size:0.85em; margin-top:4px;">🆔 群组 ID：<strong>${group.id}</strong>（分享给朋友，他们可凭此 ID 加入）</p>
        </div>
        <div style="display:flex; gap:8px; align-items:center;">
            <a href="home" class="btn btn-secondary">← 返回</a>
        </div>
    </div>

    <c:if test="${not empty param.error}">
        <div class="msg error">${param.error}</div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="msg error">${error}</div>
    </c:if>

    <div class="grid-2col">
        <%-- 左侧：成员信息与收支 --%>
        <div class="card">
            <div class="card-header">
                <h3>👥 成员 (${members.size()})</h3>
            </div>
            <table class="table">
                <thead>
                    <tr>
                        <th>成员</th>
                        <th>总消费</th>
                        <th>收支</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach items="${balances}" var="b">
                        <tr>
                            <td>${b.displayName}</td>
                            <td>¥ <fmt:formatNumber value="${b.totalShare}" pattern="#,##0.00"/></td>
                            <td>
                                <c:choose>
                                    <c:when test="${b.balance > 0}">
                                        <span class="text-green">+¥<fmt:formatNumber value="${b.balance}" pattern="#,##0.00"/></span>
                                    </c:when>
                                    <c:when test="${b.balance < 0}">
                                        <span class="text-red">-¥<fmt:formatNumber value="${-b.balance}" pattern="#,##0.00"/></span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="text-muted">¥0.00</span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>

            <div style="margin-top: 15px; display: flex; gap: 10px; flex-wrap: wrap;">
                <a href="group?id=${group.id}&action=addExpense" class="btn btn-primary">➕ 记一笔</a>
                <a href="group?id=${group.id}&action=settlement" class="btn btn-secondary">🧮 算账</a>
                <c:if test="${sessionScope.user.id == group.createdBy}">
                    <form action="group" method="post" style="display:inline" onsubmit="return confirm('确定要解散群组「${group.name}」吗？\n所有账单数据将被永久删除！')">
                        <input type="hidden" name="action" value="dissolve">
                        <input type="hidden" name="groupId" value="${group.id}">
                        <button type="submit" class="btn btn-danger">🗑 解散群组</button>
                    </form>
                </c:if>
            </div>
        </div>

        <%-- 右侧：最近账单 --%>
        <div class="card">
            <div class="card-header">
                <h3>📋 账单记录</h3>
            </div>
            <c:choose>
                <c:when test="${empty expenses}">
                    <div class="empty-state">
                        <p>还没有账单记录</p>
                        <p>点击"记一笔"开始记账吧！</p>
                    </div>
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
                                <c:forEach items="${e.shares}" var="s" varStatus="st">
                                    <span class="share-tag">${s.userDisplayName} ¥<fmt:formatNumber value="${s.shareAmount}" pattern="#,##0.00"/></span>
                                </c:forEach>
                            </div>
                        </div>
                    </c:forEach>
                </c:otherwise>
            </c:choose>
        </div>
    </div>

    <%-- 快速结算预览 --%>
    <c:if test="${not empty settlements}">
        <div class="card" style="margin-top: 20px;">
            <div class="card-header">
                <h3>💰 结算建议</h3>
            </div>
            <div class="settlement-preview">
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
            </div>
            <p class="text-muted" style="margin-top:10px;">💡 查看完整结算方案请点击"算账"</p>
        </div>
    </c:if>
</div>
</body>
</html>
