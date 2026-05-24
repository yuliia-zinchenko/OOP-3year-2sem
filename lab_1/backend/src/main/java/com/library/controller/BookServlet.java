package com.library.controller;

import com.library.dto.BookDto;
import com.library.factory.ServiceFactory;
import com.library.mapper.BookMapper;
import com.library.model.Book;
import com.library.util.Json;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mapstruct.factory.Mappers;

import java.io.IOException;
import java.util.List;

@WebServlet(urlPatterns = {"/api/books", "/api/books/*"})
public class BookServlet extends HttpServlet {
    private static final Logger log = LogManager.getLogger(BookServlet.class);
    private final BookMapper mapper = Mappers.getMapper(BookMapper.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long id = parseId(req);
        if (id == null) {
            String q = req.getParameter("q");
            List<Book> books = (q == null || q.isBlank())
                    ? ServiceFactory.getInstance().books().list()
                    : ServiceFactory.getInstance().books().search(q);
            Json.write(resp, 200, mapper.toDto(books));
        } else {
            ServiceFactory.getInstance().books().get(id).ifPresentOrElse(
                    b -> {
                        try { Json.write(resp, 200, mapper.toDto(b)); } catch (IOException e) { throw new RuntimeException(e); }
                    },
                    () -> resp.setStatus(404));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        BookDto dto = Json.read(req.getInputStream(), BookDto.class);
        Book saved = ServiceFactory.getInstance().books().create(Book.builder()
                .title(dto.getTitle())
                .isbn(dto.getIsbn())
                .year(dto.getYear())
                .totalCopies(dto.getTotalCopies() == null ? 1 : dto.getTotalCopies())
                .availableCopies(dto.getAvailableCopies() == null ? 1 : dto.getAvailableCopies())
                .build());
        log.info("Created book id={} title={}", saved.getId(), saved.getTitle());
        Json.write(resp, 201, mapper.toDto(saved));
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        Long id = parseId(req);
        if (id == null) { resp.setStatus(400); return; }
        boolean ok = ServiceFactory.getInstance().books().delete(id);
        resp.setStatus(ok ? 204 : 404);
    }

    private static Long parseId(HttpServletRequest req) {
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) return null;
        try { return Long.parseLong(path.substring(1)); }
        catch (NumberFormatException e) { return null; }
    }
}
