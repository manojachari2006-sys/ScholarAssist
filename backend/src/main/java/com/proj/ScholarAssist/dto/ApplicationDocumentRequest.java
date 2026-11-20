package com.proj.ScholarAssist.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Request data for linking an existing document to an application. */
@Getter
@Setter
@NoArgsConstructor
public class ApplicationDocumentRequest {
    @NotNull
    private Long applicationId;

    @NotNull
    private Long documentId;

    private Boolean required = true;
}
