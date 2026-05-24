package com.library.controller;

import com.library.command.*;
import com.library.dto.LoanDto;
import com.library.factory.ServiceFactory;
import com.library.model.Loan;
import com.library.model.LoanStatus;
import com.library.model.LoanType;
import com.library.model.User;
import com.library.util.Json;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * REST endpoints (role-protected):
 *   READER:
 *     GET  /api/loans                         - my loans
 *     POST /api/loans              {bookId}   - place an order      (Command: OrderBook)
 *     POST /api/loans/{id}/cancel             - cancel my order     (Command: CancelOrder)
 *
 *   LIBRARIAN:
 *     GET  /api/loans?status=ORDERED          - all pending orders
 *     POST /api/loans/{id}/issue   {type}     - issue book          (Command: IssueBook)
 *     POST /api/loans/{id}/return             - accept return       (Command: ReturnBook)
 */
@WebServlet(urlPatterns = {"/api/loans", "/api/loans/*"})
public class LoanServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = (User) req.getAttribute("currentUser");
        String statusParam = req.getParameter("status");

        List<Loan> loans;
        if (statusParam != null) {
            requireLibrarian(u, resp);
            loans = ServiceFactory.getInstance().loans().listByStatus(LoanStatus.valueOf(statusParam));
        } else if ("LIBRARIAN".equals(u.getRole())) {
            loans = ServiceFactory.getInstance().loans().listAll();
        } else {
            loans = ServiceFactory.getInstance().loans().listForUser(u.getId());
        }
        Json.write(resp, 200, loans);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = (User) req.getAttribute("currentUser");
        String path = req.getPathInfo();
        try {
            Loan result;
            if (path != null && path.matches("/\\d+/cancel")) {
                long id = Long.parseLong(path.split("/")[1]);
                result = new CancelOrderCommand(svc(), id, u.getId()).execute();
            } else if (path != null && path.matches("/\\d+/issue")) {
                requireLibrarian(u, resp);
                long id = Long.parseLong(path.split("/")[1]);
                LoanDto body = Json.read(req.getInputStream(), LoanDto.class);
                LoanType type = body.getType() == null ? LoanType.SUBSCRIPTION : body.getType();
                result = new IssueBookCommand(svc(), id, u.getId(), type).execute();
            } else if (path != null && path.matches("/\\d+/return")) {
                requireLibrarian(u, resp);
                long id = Long.parseLong(path.split("/")[1]);
                result = new ReturnBookCommand(svc(), id).execute();
            } else {
                LoanDto body = Json.read(req.getInputStream(), LoanDto.class);
                result = new OrderBookCommand(svc(), u.getId(), body.getBookId()).execute();
            }
            Json.write(resp, 200, result);
        } catch (IllegalStateException e) {
            Json.write(resp, 409, Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            Json.write(resp, 403, Map.of("error", e.getMessage()));
        }
    }

    private static com.library.service.LoanService svc() {
        return ServiceFactory.getInstance().loans();
    }

    private static void requireLibrarian(User u, HttpServletResponse resp) {
        if (!"LIBRARIAN".equals(u.getRole()))
            throw new SecurityException("Тільки для бібліотекаря");
    }
}
