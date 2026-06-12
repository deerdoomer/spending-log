package com.splitbill.servlet;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.splitbill.dao.ExpenseDAO;
import com.splitbill.dao.GroupDAO;
import com.splitbill.dao.TransferDAO;
import com.splitbill.dao.UserDAO;
import com.splitbill.model.BillGroup;
import com.splitbill.model.Expense;
import com.splitbill.model.GroupMember;
import com.splitbill.model.Settlement;
import com.splitbill.model.Transfer;
import com.splitbill.model.User;

@WebServlet("/group")
public class ExpenseServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private GroupDAO groupDAO = new GroupDAO();
    private ExpenseDAO expenseDAO = new ExpenseDAO();
    private TransferDAO transferDAO = new TransferDAO();
    private UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.sendRedirect("login");
            return;
        }
        User user = (User) session.getAttribute("user");

        String idStr = req.getParameter("id");
        if (idStr == null || idStr.trim().isEmpty()) {
            resp.sendRedirect("home");
            return;
        }

        int groupId = Integer.parseInt(idStr.trim());

        // 检查权限
        if (!groupDAO.isMember(groupId, user.getId())) {
            resp.sendRedirect("home");
            return;
        }

        BillGroup group = groupDAO.getGroupById(groupId);
        List<GroupMember> members = groupDAO.getMembers(groupId);
        List<Expense> expenses = expenseDAO.getExpensesByGroup(groupId);

        // 计算每个成员的总消费和净收支
        List<com.splitbill.model.UserBalance> balances = new ArrayList<>();
        for (GroupMember m : members) {
            double totalShare = expenseDAO.getUserTotalShare(groupId, m.getUserId());
            double netBalance = expenseDAO.getUserBalance(groupId, m.getUserId());
            com.splitbill.model.UserBalance ub = new com.splitbill.model.UserBalance();
            ub.setUserId(m.getUserId());
            ub.setDisplayName(m.getUserDisplayName());
            ub.setTotalShare(totalShare);
            ub.setBalance(netBalance);
            balances.add(ub);
        }

        // 结算方案
        List<Settlement> settlements = expenseDAO.calculateSettlements(groupId);

        // 获取已确认的转账记录，标记结算方案的确认状态（所有视图均需要）
        List<Transfer> transfers = transferDAO.getTransfersByGroup(groupId);
        for (Settlement s : settlements) {
            Transfer t = transferDAO.getTransfer(groupId, s.getFromUserId(), s.getToUserId(), s.getAmount());
            if (t != null) {
                s.setConfirmed(true);
                s.setConfirmedAt(t.getConfirmedAt());
            }
        }

        req.setAttribute("group", group);
        req.setAttribute("members", members);
        req.setAttribute("expenses", expenses);
        req.setAttribute("balances", balances);
        req.setAttribute("settlements", settlements);
        req.setAttribute("transfers", transfers);

        // 如果请求添加账单，跳转
        String action = req.getParameter("action");
        if ("addExpense".equals(action)) {
            req.getRequestDispatcher("/WEB-INF/page/addExpense.jsp").forward(req, resp);
            return;
        }
        if ("settlement".equals(action)) {
            req.getRequestDispatcher("/WEB-INF/page/settlement.jsp").forward(req, resp);
            return;
        }

        req.getRequestDispatcher("/WEB-INF/page/groupDetail.jsp").forward(req, resp);
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

        if ("addExpense".equals(action)) {
            int groupId = Integer.parseInt(req.getParameter("groupId"));
            double amount = Double.parseDouble(req.getParameter("amount"));
            String description = req.getParameter("description");
            String[] shareUserIds = req.getParameterValues("shareUsers");

            if (shareUserIds == null || shareUserIds.length == 0) {
                req.setAttribute("error", "请选择平摊成员");
                BillGroup group = groupDAO.getGroupById(groupId);
                req.setAttribute("group", group);
                req.setAttribute("members", groupDAO.getMembers(groupId));
                req.getRequestDispatcher("/WEB-INF/page/addExpense.jsp").forward(req, resp);
                return;
            }

            List<Integer> userIds = new ArrayList<>();
            for (String s : shareUserIds) {
                userIds.add(Integer.parseInt(s));
            }

            boolean ok = expenseDAO.addExpense(groupId, user.getId(), amount, description, userIds);
            if (ok) {
                resp.sendRedirect("group?id=" + groupId);
            } else {
                req.setAttribute("error", "添加账单失败");
                BillGroup group = groupDAO.getGroupById(groupId);
                req.setAttribute("group", group);
                req.setAttribute("members", groupDAO.getMembers(groupId));
                req.getRequestDispatcher("/WEB-INF/page/addExpense.jsp").forward(req, resp);
            }
        }

        if ("confirmTransfer".equals(action)) {
            int groupId = Integer.parseInt(req.getParameter("groupId"));
            int fromUserId = Integer.parseInt(req.getParameter("fromUserId"));
            int toUserId = Integer.parseInt(req.getParameter("toUserId"));
            double amount = Double.parseDouble(req.getParameter("amount"));

            // 校验：当前用户必须是群组成员
            if (!groupDAO.isMember(groupId, user.getId())) {
                resp.sendRedirect("home");
                return;
            }

            // 权限校验：只有收款人才能确认
            if (user.getId() != toUserId) {
                resp.sendRedirect("group?id=" + groupId + "&action=settlement&error=" + URLEncoder.encode("只有收款人才能确认收款", "UTF-8"));
                return;
            }

            boolean ok = transferDAO.confirmTransfer(groupId, fromUserId, toUserId, amount, user.getId());
            if (ok) {
                resp.sendRedirect("group?id=" + groupId + "&action=settlement&msg=" + URLEncoder.encode("确认收款成功", "UTF-8"));
            } else {
                resp.sendRedirect("group?id=" + groupId + "&action=settlement&error=" + URLEncoder.encode("确认失败，请查看服务器日志", "UTF-8"));
            }
            return;
        }

        if ("dissolve".equals(action)) {
            int groupId = Integer.parseInt(req.getParameter("groupId"));
            BillGroup group = groupDAO.getGroupById(groupId);
            if (group == null) {
                resp.sendRedirect("home");
                return;
            }
            // 仅创建者可解散
            if (group.getCreatedBy() != user.getId()) {
                resp.sendRedirect("group?id=" + groupId + "&error=只有群组创建者才能解散群组");
                return;
            }
            boolean ok = groupDAO.deleteGroup(groupId, user.getId());
            if (ok) {
                resp.sendRedirect("home?msg=群组已解散");
            } else {
                resp.sendRedirect("group?id=" + groupId + "&error=解散失败");
            }
        }
    }
}
