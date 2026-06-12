<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>加入群组 - 拼单记账本</title>
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
        <h2>加入群组</h2>
        <a href="home" class="btn btn-secondary">← 返回</a>
    </div>

    <c:if test="${not empty error}">
        <div class="msg error">${error}</div>
    </c:if>

    <div class="form-card">
        <h3>方式一：输入群组 ID</h3>
        <form action="home" method="post">
            <input type="hidden" name="action" value="join">
            <div class="form-group">
                <label>群组 ID</label>
                <input type="number" name="groupId" required placeholder="创建群组后获得的ID">
            </div>
            <button type="submit" class="btn btn-primary">加入群组</button>
        </form>
    </div>

    <div class="form-card" style="margin-top: 20px;">
        <h3>方式二：通过成员用户名查找</h3>
        <form action="home" method="post">
            <input type="hidden" name="action" value="join">
            <div class="form-group">
                <label>用户名</label>
                <input type="text" name="username" placeholder="输入群组成员的用户名">
            </div>
            <button type="submit" class="btn btn-primary">查找群组</button>
        </form>

        <c:if test="${foundGroups != null}">
            <div class="search-results">
                <h4>搜索结果：</h4>
                <c:choose>
                    <c:when test="${foundGroups.size() > 0}">
                        <c:forEach items="${foundGroups}" var="g">
                            <div class="group-row">
                                <span class="group-name"><strong>${g.name}</strong></span>
                                <span class="group-meta">👤 ${g.memberCount} 人 · 创建者 ${g.createdByDisplayName}</span>
                                <span class="group-id">ID: ${g.id}</span>
                                <form action="home" method="post" style="display:inline">
                                    <input type="hidden" name="action" value="join">
                                    <input type="hidden" name="groupId" value="${g.id}">
                                    <button type="submit" class="btn-small btn-primary">加入</button>
                                </form>
                            </div>
                        </c:forEach>
                    </c:when>
                    <c:otherwise>
                        <div class="msg error">未找到该用户所在的群组</div>
                    </c:otherwise>
                </c:choose>
            </div>
        </c:if>
    </div>
</div>
</body>
</html>
