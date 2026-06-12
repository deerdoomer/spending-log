<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>记一笔 - ${group.name}</title>
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
        <h2>📝 记一笔 — ${group.name}</h2>
        <a href="group?id=${group.id}" class="btn btn-secondary">← 返回</a>
    </div>

    <c:if test="${not empty error}">
        <div class="msg error">${error}</div>
    </c:if>

    <div class="form-card">
        <form action="group" method="post" onsubmit="return validateForm()">
            <input type="hidden" name="action" value="addExpense">
            <input type="hidden" name="groupId" value="${group.id}">

            <div class="form-group">
                <label>金额 *</label>
                <input type="number" name="amount" step="0.01" min="0.01" required placeholder="0.00">
            </div>

            <div class="form-group">
                <label>描述</label>
                <input type="text" name="description" placeholder="例如：买水果、聚餐、水电费">
            </div>

            <div class="form-group">
                <label>平摊成员 *（选择参与分摊的人）</label>
                <div class="checkbox-group">
                    <c:forEach items="${members}" var="m">
                        <label class="checkbox-label">
                            <input type="checkbox" name="shareUsers" value="${m.userId}"
                                ${m.userId == sessionScope.user.id ? 'checked' : ''}>
                            ${m.userDisplayName}
                        </label>
                    </c:forEach>
                </div>
                <div class="text-muted" style="font-size: 13px; margin-top: 5px;">
                    💡 默认只有你被选中，请选择所有需要平摊的人
                </div>
            </div>

            <button type="submit" class="btn btn-primary">✅ 确认记账</button>
        </form>
    </div>
</div>

<script>
function validateForm() {
    var checkboxes = document.querySelectorAll('input[name="shareUsers"]:checked');
    if (checkboxes.length === 0) {
        alert('请至少选择一位平摊成员！');
        return false;
    }
    return true;
}
</script>
</body>
</html>
