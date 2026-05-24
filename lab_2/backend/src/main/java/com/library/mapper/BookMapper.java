package com.library.mapper;

import com.library.domain.Author;
import com.library.domain.Book;
import com.library.dto.BookDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface BookMapper {

    @Mapping(target = "authorNames", source = "authors", qualifiedByName = "authorNames")
    BookDto toDto(Book book);

    List<BookDto> toDto(List<Book> books);

    @Named("authorNames")
    static List<String> authorNames(Set<Author> authors) {
        return authors == null ? List.of() : authors.stream().map(Author::getFullName).toList();
    }
}
