package com.proj.ScholarAssist.repository;

import com.proj.ScholarAssist.entity.ApplicationDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/** Database access for application-to-document links. */
public interface ApplicationDocumentRepository extends JpaRepository<ApplicationDocument, Long> {
    /** Finds a link using the unique application/document pair. */
    Optional<ApplicationDocument> findByApplication_IdAndDocument_Id(
            Long applicationId,
            Long documentId);

    /** Lists linked documents for one application. */
    List<ApplicationDocument> findByApplication_Id(Long applicationId);

    /** Checks whether a document is referenced before allowing deletion. */
    boolean existsByDocument_Id(Long documentId);
}
