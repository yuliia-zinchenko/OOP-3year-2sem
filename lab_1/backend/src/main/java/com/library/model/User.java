package com.library.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String sub;
    private String email;
    private String fullName;
    private String role;          // READER | LIBRARIAN
    private LocalDateTime createdAt;
}
