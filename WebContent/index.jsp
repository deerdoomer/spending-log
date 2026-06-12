<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<c:if test="${not empty sessionScope.user}">
    <c:redirect url="/home"/>
</c:if>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>拼单记账本 - 登录</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body class="login-body">
<div class="login-container">
    <div class="login-box">
        <h1>📒 拼单记账本</h1>
        <p class="subtitle">和室友/朋友一起记账，自动算平</p>

        <c:if test="${not empty param.error}">
            <div class="msg error">${param.error}</div>
        </c:if>
        <c:if test="${not empty param.msg}">
            <div class="msg success">${param.msg}</div>
        </c:if>

        <c:choose>
            <c:when test="${not empty param.register}">
                <form action="${pageContext.request.contextPath}/login" method="post">
                    <input type="hidden" name="action" value="register">
                    <div class="form-group">
                        <label>用户名</label>
                        <input type="text" name="username" required placeholder="登录用">
                    </div>
                    <div class="form-group">
                        <label>密码</label>
                        <input type="password" name="password" required placeholder="至少6位">
                    </div>
                    <div class="form-group">
                        <label>显示名称</label>
                        <input type="text" name="displayName" required placeholder="群组中显示的名字">
                    </div>
                    <button type="submit" class="btn btn-primary">注 册</button>
                </form>
                <p class="switch-link">已有账号？<a href="${pageContext.request.contextPath}/">去登录</a></p>
            </c:when>
            <c:otherwise>
                <form action="${pageContext.request.contextPath}/login" method="post">
                    <div class="form-group">
                        <label>用户名</label>
                        <input type="text" name="username" required placeholder="输入用户名">
                    </div>
                    <div class="form-group">
                        <label>密码</label>
                        <input type="password" name="password" required placeholder="输入密码">
                    </div>
                    <button type="submit" class="btn btn-primary">登 录</button>
                </form>
                <p class="switch-link">没有账号？<a href="${pageContext.request.contextPath}/?register=true">注册一个新账号</a></p>
            </c:otherwise>
        </c:choose>
    </div>
</div>
</body>
</html>
