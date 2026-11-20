package com.proj.ScholarAssist.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

/** Public document metadata; intentionally excludes the local storage path. */
@Getter
@Setter
@NoArgsConstructor
public class DocumentResponse {
    private Long id;
    private String documentType;
    private String fileName;
    private String verificationStatus;
    private LocalDateTime uploadedAt;
    private LocalDateTime reviewedAt;
    private String reviewFeedback;
}
