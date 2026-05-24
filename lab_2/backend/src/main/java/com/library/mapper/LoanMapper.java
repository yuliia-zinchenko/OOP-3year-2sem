package com.library.mapper;

import com.library.domain.Loan;
import com.library.dto.LoanDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LoanMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "bookId", source = "book.id")
    @Mapping(target = "bookTitle", source = "book.title")
    @Mapping(target = "readerEmail", source = "user.email")
    @Mapping(target = "librarianId", source = "librarian.id")
    LoanDto toDto(Loan loan);

    List<LoanDto> toDto(List<Loan> loans);
}
