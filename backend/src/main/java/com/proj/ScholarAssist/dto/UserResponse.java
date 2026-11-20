package com.proj.ScholarAssist.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

/** Safe user response that never includes a password or password hash. */
@Getter
@Setter
@NoArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private String role;
    private LocalDateTime createdAt;
}
