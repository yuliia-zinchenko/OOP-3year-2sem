package com.library.factory;

import com.library.dao.AuthorDao;
import com.library.dao.BookDao;
import com.library.dao.LoanDao;
import com.library.dao.UserDao;
import com.library.service.BookService;
import com.library.service.LoanService;
import com.library.service.UserService;

/**
 * GoF: Factory + Singleton. Single point that wires DAOs into services.
 */
public final class ServiceFactory {
    private static final ServiceFactory INSTANCE = new ServiceFactory();

    private final BookService bookService;
    private final UserService userService;
    private final LoanService loanService;
    private final AuthorDao authorDao;

    private ServiceFactory() {
        BookDao bookDao = new BookDao();
        UserDao userDao = new UserDao();
        LoanDao loanDao = new LoanDao();
        this.authorDao = new AuthorDao();
        this.bookService = new BookService(bookDao);
        this.userService = new UserService(userDao);
        this.loanService = new LoanService(loanDao, bookDao);
    }

    public static ServiceFactory getInstance() { return INSTANCE; }

    public BookService books() { return bookService; }
    public UserService users() { return userService; }
    public LoanService loans() { return loanService; }
    public AuthorDao authors() { return authorDao; }
}
