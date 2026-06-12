<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>创建群组 - 拼单记账本</title>
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
        <h2>创建群组</h2>
        <a href="home" class="btn btn-secondary">← 返回</a>
    </div>

    <c:if test="${not empty error}">
        <div class="msg error">${error}</div>
    </c:if>

    <div class="form-card">
        <form action="home" method="post">
            <input type="hidden" name="action" value="create">
            <div class="form-group">
                <label>群组名称 *</label>
                <input type="text" name="name" required placeholder="例如：301宿舍、项目组聚餐">
            </div>
            <div class="form-group">
                <label>群组描述</label>
                <textarea name="description" rows="3" placeholder="（可选）简单描述一下这个群组"></textarea>
            </div>
            <button type="submit" class="btn btn-primary">创建群组</button>
        </form>
    </div>
</div>
</body>
</html>
