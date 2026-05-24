package com.library.controller;

import com.library.dto.BookDto;
import com.library.mapper.BookMapper;
import com.library.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService books;
    private final BookMapper mapper;

    @GetMapping
    public List<BookDto> search(@RequestParam(value = "q", required = false) String q) {
        return mapper.toDto(books.search(q));
    }
}
