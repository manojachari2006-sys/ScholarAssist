package com.proj.ScholarAssist.repository;

import com.proj.ScholarAssist.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/** Database access for uploaded document metadata. */
public interface DocumentRepository extends JpaRepository<Document, Long> {
    /** Lists documents owned by a student profile. */
    List<Document> findByStudent_Id(Long studentId);

    /** Lists documents that have not yet received a reviewer decision. */
    List<Document> findByVerificationStatusOrderByUploadedAtAsc(String verificationStatus);

    /** Lists documents whose contents a reviewer has already approved. */
    List<Document> findByVerificationStatusOrderByReviewedAtDesc(String verificationStatus);
}
