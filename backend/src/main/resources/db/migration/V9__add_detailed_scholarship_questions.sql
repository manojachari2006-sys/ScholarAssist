-- Add scholarship-specific facts to the quick eligibility check.
-- Values are taken from the criteria already described in the catalog.

UPDATE scholarships
SET category = 'OBC/EBC/DNT'
WHERE name = 'PM YASASVI Top Class Education in College for OBC, EBC and DNT Students 2026-27';

INSERT INTO scholarship_rules (
    scholarship_id,
    rule_type,
    operator,
    rule_value,
    description
)
SELECT scholarship.id,
       requested.rule_type,
       requested.operator,
       requested.rule_value,
       requested.description
FROM (
    SELECT 'AICTE Pragati Scholarship Scheme for Girl Students (Degree) 2026-27' AS scholarship_name,
           'GENDER' AS rule_type,
           '=' AS operator,
           'Female' AS rule_value,
           'This scheme is for girl students.' AS description
    UNION ALL SELECT 'AICTE Saksham Scholarship Scheme for Specially Abled Students (Degree) 2026-27',
           'DISABILITY_STATUS', '=', 'Yes',
           'The student must meet the scheme disability criteria.'
    UNION ALL SELECT 'AICTE Saksham Scholarship Scheme for Specially Abled Students (Degree) 2026-27',
           'DISABILITY_PERCENTAGE', '>=', '40',
           'Benchmark disability must be at least 40 percent.'
    UNION ALL SELECT 'ONGC Merit Scholarship 2026-27',
           'CATEGORY', 'IN', 'SC,ST,OBC,General EWS',
           'Select the category under which you are applying; income limits differ by category.'
    UNION ALL SELECT 'Kind Circle Scholarship for Meritorious Students 2026-27',
           'PREVIOUS_MARKS_PERCENTAGE', '>=', '60',
           'At least 60 percent is required in the previous academic year.'
    UNION ALL SELECT 'Kind Circle Scholarship for Meritorious Students 2026-27',
           'EDUCATION_LEVEL', 'IN', 'School,Higher Secondary,Diploma,Undergraduate,Postgraduate',
           'This scholarship supports students from Class 1 through postgraduate study.'
    UNION ALL SELECT 'Pratiksha Trust Scholarship 2026-27',
           'PUC_MARKS_PERCENTAGE', '>', '80',
           'Plus Two / PUC marks must be above 80 percent.'
    UNION ALL SELECT 'ONGC Merit Scholarship 2026-27',
           'EDUCATION_LEVEL', 'IN', 'Undergraduate,Postgraduate',
           'Select undergraduate or postgraduate study, as allowed courses vary by level.'
    UNION ALL SELECT 'Nirankari Rajmata Scholarship Scheme 2026-27',
           'EDUCATION_LEVEL', 'IN', 'Undergraduate,Postgraduate',
           'The listed professional and technical courses are at undergraduate or postgraduate level.'
    UNION ALL SELECT 'Vikash Educational Charitable Trust Scholarship 2026-27',
           'EDUCATION_LEVEL', 'IN', 'Diploma,Undergraduate,Postgraduate',
           'The listed courses include diploma, undergraduate, and postgraduate study.'
    UNION ALL SELECT 'HDFC Bank Parivartan ECSS - Higher Education 2026-27',
           'EDUCATION_LEVEL', 'IN', 'Undergraduate,Postgraduate',
           'This catalog entry is for recognized undergraduate or postgraduate study.'
    UNION ALL SELECT 'Vylor Agritech Scholarship - Postgraduate and PhD 2026-27',
           'EDUCATION_LEVEL', 'IN', 'Postgraduate,Doctoral',
           'This program is for postgraduate or PhD-level agriculture and allied courses.'
    UNION ALL SELECT 'Karnataka SSP Vidyasiri Scheme for Minorities 2026-27',
           'EDUCATION_LEVEL', 'IN', 'Higher Secondary,Diploma,Undergraduate,Postgraduate',
           'Select your level of post-matric study for the Minority Welfare scheme.'
    UNION ALL SELECT 'Karnataka SSP DTE Post-Matric Scholarship for ST Technical Students 2026-27',
           'EDUCATION_LEVEL', 'IN', 'Diploma,Undergraduate,Postgraduate',
           'Select your level of eligible technical post-matric study.'
    UNION ALL SELECT 'Karnataka SSP SC Post-Matric Scholarship (Social Welfare) 2026-27',
           'EDUCATION_LEVEL', 'IN', 'Higher Secondary,Diploma,Undergraduate,Postgraduate',
           'Select your level of post-matric study for the Social Welfare scheme.'
    UNION ALL SELECT 'Karnataka SSP ST Post-Matric Scholarship (Tribal Welfare) 2026-27',
           'EDUCATION_LEVEL', 'IN', 'Higher Secondary,Diploma,Undergraduate,Postgraduate',
           'Select your level of post-matric study for the Tribal Welfare scheme.'
    UNION ALL SELECT 'Karnataka SSP Scholarship for Students with Disabilities 2026-27',
           'EDUCATION_LEVEL', 'IN', 'Higher Secondary,Diploma,Undergraduate,Postgraduate,Doctoral',
           'Select your education level; this scheme lists support across courses.'
    UNION ALL SELECT 'DCE C. V. Raman Scholarship, Karnataka 2026-27',
           'CATEGORY', 'IN', 'General,SC,ST,OBC,EWS,Minority,Brahmin',
           'Select your category because the qualifying PUC marks threshold varies by category.'
    UNION ALL SELECT 'Karnataka SSP Sandeepani Shishya Vetana 2026-27',
           'SEAT_TYPE', '!=', 'Management',
           'Management-quota seats are excluded.'
    UNION ALL SELECT 'Karnataka SSP Sandeepani Shishya Vetana 2026-27',
           'SEAT_TYPE', '!=', 'COMEDK',
           'COMEDK admissions are excluded.'
    UNION ALL SELECT 'Karnataka SSP DTE Post-Matric Scholarship for ST Technical Students 2026-27',
           'SEAT_TYPE', '!=', 'Management',
           'Management-quota admissions are excluded.'
    UNION ALL SELECT 'Karnataka SSP Vidyasiri Scheme for Minorities 2026-27',
           'HOSTEL_STATUS', '=', 'Not allotted',
           'The student must have applied for a minority hostel and not received a seat.'
) requested
JOIN scholarships scholarship ON scholarship.name = requested.scholarship_name
WHERE NOT EXISTS (
    SELECT 1
    FROM scholarship_rules existing_rule
    WHERE existing_rule.scholarship_id = scholarship.id
      AND existing_rule.rule_type = requested.rule_type
      AND existing_rule.operator = requested.operator
      AND existing_rule.rule_value = requested.rule_value
);

-- Keep the SSP seat question focused on the remaining document/admission checks,
-- since seat type is now captured with a structured dropdown.
UPDATE scholarship_rules rule_row
JOIN scholarships scholarship ON scholarship.id = rule_row.scholarship_id
SET rule_row.description = 'Do your admission documents meet the remaining SSP requirements?'
WHERE scholarship.name = 'Karnataka SSP Sandeepani Shishya Vetana 2026-27'
  AND rule_row.rule_type = 'MANUAL_REVIEW'
  AND rule_row.description LIKE 'Are you admitted through an eligible seat%';
