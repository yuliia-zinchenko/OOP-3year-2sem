package com.library.service;

import com.library.dao.BookDao;
import com.library.model.Book;

import java.util.List;
import java.util.Optional;

public class BookService {
    private final BookDao bookDao;

    public BookService(BookDao bookDao) { this.bookDao = bookDao; }

    public List<Book> list() { return bookDao.findAll(); }
    public List<Book> search(String query) { return bookDao.search(query); }
    public Optional<Book> get(Long id) { return bookDao.findById(id); }
    public Book create(Book b) { return bookDao.save(b); }
    public boolean delete(Long id) { return bookDao.deleteById(id); }
}
