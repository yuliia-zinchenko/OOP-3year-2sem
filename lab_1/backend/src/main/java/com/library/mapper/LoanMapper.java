package com.library.mapper;

import com.library.dto.LoanDto;
import com.library.model.Loan;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface LoanMapper {
    @Mapping(target = "bookTitle", ignore = true)
    LoanDto toDto(Loan loan);
}
