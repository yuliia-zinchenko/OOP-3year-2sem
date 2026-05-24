package com.library.mapper;

import com.library.dto.BookDto;
import com.library.model.Author;
import com.library.model.Book;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "default")
public interface BookMapper {

    @org.mapstruct.Mapping(source = "authors", target = "authorNames", qualifiedByName = "authorsToNames")
    BookDto toDto(Book book);

    List<BookDto> toDto(List<Book> books);

    @Named("authorsToNames")
    static List<String> authorsToNames(List<Author> authors) {
        if (authors == null) return List.of();
        return authors.stream().map(Author::getFullName).collect(Collectors.toList());
    }
}
