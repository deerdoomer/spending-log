<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>我的群组 - 拼单记账本</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="nav">
    <div class="nav-inner">
        <span class="nav-title">📒 拼单记账本</span>
        <span class="nav-user">
            ${sessionScope.user.displayName}
            <a href="${pageContext.request.contextPath}/logout" class="btn-small">退出</a>
        </span>
    </div>
</div>

<div class="container">
    <div class="page-header">
        <h2>我的群组</h2>
        <div class="header-actions">
            <a href="home?action=create" class="btn btn-primary">➕ 创建群组</a>
            <a href="home?action=join" class="btn btn-secondary">🔗 加入群组</a>
        </div>
    </div>

    <c:if test="${not empty param.msg}">
        <div class="msg success">${param.msg}</div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="msg error">${error}</div>
    </c:if>

    <c:choose>
        <c:when test="${empty groups}">
            <div class="empty-state">
                <p>你还没有加入任何群组</p>
                <p>创建一个群组，开始和室友/朋友一起记账吧！</p>
            </div>
        </c:when>
        <c:otherwise>
            <div class="group-list">
                <c:forEach items="${groups}" var="g">
                    <a href="group?id=${g.id}" class="group-card">
                        <div class="group-card-body">
                            <h3>${g.name}</h3>
                            <p class="group-desc">${g.description}</p>
                            <div class="group-meta">
                                <span>👤 ${g.memberCount} 人</span>
                                <span>📅 ${g.createdAt}</span>
                                <span>创建者：${g.createdByDisplayName}</span>
                            </div>
                        </div>
                    </a>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>
</div>
</body>
</html>
