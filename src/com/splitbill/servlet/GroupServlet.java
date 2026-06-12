package com.splitbill.servlet;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.splitbill.dao.GroupDAO;
import com.splitbill.model.BillGroup;
import com.splitbill.model.User;

@WebServlet("/home")
public class GroupServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private GroupDAO groupDAO = new GroupDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.sendRedirect("login");
            return;
        }

        User user = (User) session.getAttribute("user");

        // 处理群组操作参数
        String action = req.getParameter("action");
        if ("create".equals(action)) {
            req.getRequestDispatcher("/WEB-INF/page/createGroup.jsp").forward(req, resp);
            return;
        }
        if ("join".equals(action)) {
            req.getRequestDispatcher("/WEB-INF/page/joinGroup.jsp").forward(req, resp);
            return;
        }

        // 获取用户的群组列表
        List<BillGroup> groups = groupDAO.getGroupsByUser(user.getId());
        req.setAttribute("groups", groups);
        req.getRequestDispatcher("/WEB-INF/page/home.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.sendRedirect("login");
            return;
        }
        User user = (User) session.getAttribute("user");

        String action = req.getParameter("action");

        if ("create".equals(action)) {
            String name = req.getParameter("name");
            String desc = req.getParameter("description");
            if (name == null || name.trim().isEmpty()) {
                req.setAttribute("error", "请输入群组名称");
                req.getRequestDispatcher("/WEB-INF/page/createGroup.jsp").forward(req, resp);
                return;
            }

            int groupId = groupDAO.createGroup(name.trim(), desc != null ? desc.trim() : "", user.getId());
            if (groupId > 0) {
                // 创建者自动成为成员
                groupDAO.addMember(groupId, user.getId());
                resp.sendRedirect("group?id=" + groupId);
            } else {
                req.setAttribute("error", "创建失败");
                req.getRequestDispatcher("/WEB-INF/page/createGroup.jsp").forward(req, resp);
            }
            return;
        }

        if ("join".equals(action)) {
            String groupIdStr = req.getParameter("groupId");
            String usernameToAdd = req.getParameter("username");
            if (groupIdStr != null && !groupIdStr.trim().isEmpty()) {
                // 通过群组ID加入
                int groupId = Integer.parseInt(groupIdStr.trim());
                BillGroup g = groupDAO.getGroupById(groupId);
                if (g == null) {
                    req.setAttribute("error", "群组不存在");
                    req.getRequestDispatcher("/WEB-INF/page/joinGroup.jsp").forward(req, resp);
                    return;
                }
                groupDAO.addMember(groupId, user.getId());
                resp.sendRedirect("group?id=" + groupId);
                return;
            }
            if (usernameToAdd != null && !usernameToAdd.trim().isEmpty()) {
                // 通过成员用户名搜索群组
                List<BillGroup> groups = groupDAO.getGroupsByUserUsername(usernameToAdd.trim());
                req.setAttribute("foundGroups", groups);
                req.getRequestDispatcher("/WEB-INF/page/joinGroup.jsp").forward(req, resp);
                return;
            }
            req.setAttribute("error", "请输入群组ID或成员用户名");
            req.getRequestDispatcher("/WEB-INF/page/joinGroup.jsp").forward(req, resp);
        }
    }
}
