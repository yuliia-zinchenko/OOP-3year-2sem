package com.library.service;

import com.library.domain.Book;
import com.library.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository books;

    @Transactional(readOnly = true)
    public List<Book> search(String q) {
        return (q == null || q.isBlank()) ? books.findAll() : books.search(q.trim());
    }
}
