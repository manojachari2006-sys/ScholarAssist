package com.proj.ScholarAssist.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Client-editable profile fields; optionality follows the nullable V1 columns. */
@Getter
@Setter
@NoArgsConstructor
public class StudentProfileRequest {
    @NotBlank(message = "Full name is required")
    @Size(max = 150)
    private String fullName;

    @Size(max = 20)
    private String phone;

    @Past(message = "Date of Birth must be in the past")
    private LocalDate dateOfBirth;

    @Size(max = 30)
    private String gender;

    @Size(max = 100)
    private String state;

    @Size(max = 100)
    private String district;

    @Size(max = 200)
    private String collegeName;

    @Size(max = 150)
    private String course;

    @Min(1)
    private Integer yearOfStudy;

    @DecimalMin(value = "0.0", message = "CGPA cannot be less than 0")
    @DecimalMax(value = "10.0", message = "CGPA cannot exceed 10")
    @Digits(integer = 2, fraction = 2)
    private BigDecimal cgpa;

    @DecimalMin("0.0")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal annualFamilyIncome;

    @Size(max = 50)
    private String category;
}
