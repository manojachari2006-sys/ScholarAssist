package com.proj.ScholarAssist.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Validated public registration payload; the server always assigns the STUDENT role. */
@Getter
@Setter
public class RegisterRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid Email address")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "Password is Required")
    @Size(min = 8, message = "Password must contain at least 8 characters")
    private String password;
}
