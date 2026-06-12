package com.splitbill.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.splitbill.dao.UserDAO;
import com.splitbill.model.User;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // 已登录则跳主页
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            resp.sendRedirect("home");
            return;
        }
        // 处理 ?register=true 参数，转发到注册页
        if ("true".equals(req.getParameter("register"))) {
            req.setAttribute("registerMode", true);
        }
        req.getRequestDispatcher("/WEB-INF/page/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = req.getParameter("action");

        if ("register".equals(action)) {
            doRegister(req, resp);
        } else {
            doLogin(req, resp);
        }
    }

    private void doLogin(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null || username.trim().isEmpty()) {
            req.setAttribute("error", "请输入用户名和密码");
            req.getRequestDispatcher("/WEB-INF/page/login.jsp").forward(req, resp);
            return;
        }

        User user = userDAO.login(username.trim(), password);
        if (user != null) {
            HttpSession session = req.getSession();
            session.setAttribute("user", user);
            resp.sendRedirect("home");
        } else {
            req.setAttribute("error", "用户名或密码错误");
            req.getRequestDispatcher("/WEB-INF/page/login.jsp").forward(req, resp);
        }
    }

    private void doRegister(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        String displayName = req.getParameter("displayName");

        if (username == null || password == null || displayName == null ||
            username.trim().isEmpty() || password.trim().isEmpty() || displayName.trim().isEmpty()) {
            req.setAttribute("error", "请填写所有字段");
            req.setAttribute("registerMode", true);
            req.getRequestDispatcher("/WEB-INF/page/login.jsp").forward(req, resp);
            return;
        }

        if (userDAO.isUsernameTaken(username.trim())) {
            req.setAttribute("error", "用户名已被使用");
            req.setAttribute("registerMode", true);
            req.getRequestDispatcher("/WEB-INF/page/login.jsp").forward(req, resp);
            return;
        }

        boolean ok = userDAO.register(username.trim(), password.trim(), displayName.trim());
        if (ok) {
            req.setAttribute("message", "注册成功，请登录");
            req.getRequestDispatcher("/WEB-INF/page/login.jsp").forward(req, resp);
        } else {
            req.setAttribute("error", "注册失败，请重试");
            req.setAttribute("registerMode", true);
            req.getRequestDispatcher("/WEB-INF/page/login.jsp").forward(req, resp);
        }
    }
}
