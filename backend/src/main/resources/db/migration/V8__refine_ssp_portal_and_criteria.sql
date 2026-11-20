-- SSP is one shared application portal for multiple department schemes.
-- Keep scheme records separate so each one can retain its own eligibility criteria.

-- The Minority Welfare SSP scheme requires a recognized minority category.
UPDATE scholarships
SET category = 'Minority'
WHERE name = 'Karnataka SSP Vidyasiri Scheme for Minorities 2026-27';

UPDATE scholarships
SET application_url = 'https://ssp.postmatric.karnataka.gov.in/post_sa_2627/login'
WHERE name IN (
    'Karnataka SSP Sandeepani Shishya Vetana 2026-27',
    'Karnataka SSP Vidyasiri Scheme for Minorities 2026-27',
    'DCE C. V. Raman Scholarship, Karnataka 2026-27',
    'Karnataka SSP DTE Post-Matric Scholarship for ST Technical Students 2026-27',
    'Karnataka SSP SC Post-Matric Scholarship (Social Welfare) 2026-27',
    'Karnataka SSP ST Post-Matric Scholarship (Tribal Welfare) 2026-27',
    'Karnataka SSP Scholarship for Students with Disabilities 2026-27'
);

-- Make the quick-check prompts specific and answerable instead of bundling
-- several unrelated conditions into one Yes/No question.
UPDATE scholarship_rules rule_row
JOIN scholarships scholarship ON scholarship.id = rule_row.scholarship_id
SET rule_row.description = 'Do you have a valid EWS certificate required for this scheme?'
WHERE scholarship.name = 'Karnataka SSP Sandeepani Shishya Vetana 2026-27'
  AND rule_row.rule_type = 'MANUAL_REVIEW';

UPDATE scholarship_rules rule_row
JOIN scholarships scholarship ON scholarship.id = rule_row.scholarship_id
SET rule_row.description = 'Have you applied for a minority hostel seat and not been allotted one?'
WHERE scholarship.name = 'Karnataka SSP Vidyasiri Scheme for Minorities 2026-27'
  AND rule_row.rule_type = 'MANUAL_REVIEW';

UPDATE scholarship_rules rule_row
JOIN scholarships scholarship ON scholarship.id = rule_row.scholarship_id
SET rule_row.description = 'Are you enrolled in BSc at an eligible Karnataka government college?'
WHERE scholarship.name = 'DCE C. V. Raman Scholarship, Karnataka 2026-27'
  AND rule_row.rule_type = 'MANUAL_REVIEW';

UPDATE scholarship_rules rule_row
JOIN scholarships scholarship ON scholarship.id = rule_row.scholarship_id
SET rule_row.description = 'Are you enrolled in an eligible technical course and college under this SSP scheme?'
WHERE scholarship.name = 'Karnataka SSP DTE Post-Matric Scholarship for ST Technical Students 2026-27'
  AND rule_row.rule_type = 'MANUAL_REVIEW';

UPDATE scholarship_rules rule_row
JOIN scholarships scholarship ON scholarship.id = rule_row.scholarship_id
SET rule_row.description = 'Does your course group and institution meet the SSP Social Welfare rules for this benefit tier?'
WHERE scholarship.name = 'Karnataka SSP SC Post-Matric Scholarship (Social Welfare) 2026-27'
  AND rule_row.rule_type = 'MANUAL_REVIEW';

UPDATE scholarship_rules rule_row
JOIN scholarships scholarship ON scholarship.id = rule_row.scholarship_id
SET rule_row.description = 'Does your course group and institution meet the SSP Tribal Welfare rules for this benefit tier?'
WHERE scholarship.name = 'Karnataka SSP ST Post-Matric Scholarship (Tribal Welfare) 2026-27'
  AND rule_row.rule_type = 'MANUAL_REVIEW';

UPDATE scholarship_rules rule_row
JOIN scholarships scholarship ON scholarship.id = rule_row.scholarship_id
SET rule_row.description = 'Do you have a valid UDID or other disability certificate required by this scheme?'
WHERE scholarship.name = 'Karnataka SSP Scholarship for Students with Disabilities 2026-27'
  AND rule_row.rule_type = 'MANUAL_REVIEW';

-- Add the other distinct student checks for the SSP schemes. These checks remain
-- preliminary self-reports; the SSP portal determines the authoritative result.
INSERT INTO scholarship_rules (scholarship_id, rule_type, operator, rule_value, description)
SELECT scholarship.id, requested.rule_type, requested.operator,
       requested.rule_value, requested.description
FROM (
    SELECT 'Karnataka SSP Sandeepani Shishya Vetana 2026-27' AS scholarship_name,
           'MANUAL_REVIEW' AS rule_type,
           '=' AS operator,
           'REQUIRED' AS rule_value,
           'Are you admitted through an eligible seat rather than management quota or COMEDK?' AS description
    UNION ALL SELECT 'Karnataka SSP Sandeepani Shishya Vetana 2026-27', 'MANUAL_REVIEW', '=', 'REQUIRED',
           'Have you been promoted to the current year of study?'
    UNION ALL SELECT 'Karnataka SSP Vidyasiri Scheme for Minorities 2026-27', 'MANUAL_REVIEW', '=', 'REQUIRED',
           'Are your course and college eligible under the Minority Welfare post-matric scheme?'
    UNION ALL SELECT 'Karnataka SSP Vidyasiri Scheme for Minorities 2026-27', 'MANUAL_REVIEW', '=', 'REQUIRED',
           'Have you been promoted to the current academic year?'
    UNION ALL SELECT 'DCE C. V. Raman Scholarship, Karnataka 2026-27', 'COURSE', '=', 'BSc',
           'Applicant must be enrolled in BSc.'
    UNION ALL SELECT 'DCE C. V. Raman Scholarship, Karnataka 2026-27', 'MANUAL_REVIEW', '=', 'REQUIRED',
           'Are you studying two eligible subjects from Physics, Chemistry, Mathematics, Botany, Zoology, or Geology?'
    UNION ALL SELECT 'DCE C. V. Raman Scholarship, Karnataka 2026-27', 'MANUAL_REVIEW', '=', 'REQUIRED',
           'Do your qualifying PUC marks meet the threshold for your category, and have you been promoted?'
    UNION ALL SELECT 'Karnataka SSP DTE Post-Matric Scholarship for ST Technical Students 2026-27', 'MANUAL_REVIEW', '=', 'REQUIRED',
           'Have you been promoted to the current academic year?'
    UNION ALL SELECT 'Karnataka SSP SC Post-Matric Scholarship (Social Welfare) 2026-27', 'MANUAL_REVIEW', '=', 'REQUIRED',
           'Does your income qualify for the benefit tier you are seeking?'
    UNION ALL SELECT 'Karnataka SSP ST Post-Matric Scholarship (Tribal Welfare) 2026-27', 'MANUAL_REVIEW', '=', 'REQUIRED',
           'Does your income qualify for the benefit tier you are seeking?'
    UNION ALL SELECT 'Karnataka SSP Scholarship for Students with Disabilities 2026-27', 'MANUAL_REVIEW', '=', 'REQUIRED',
           'Are your course, college, and promotion status eligible under this scheme?'
) requested
JOIN scholarships scholarship ON scholarship.name = requested.scholarship_name
WHERE NOT EXISTS (
    SELECT 1
    FROM scholarship_rules existing_rule
    WHERE existing_rule.scholarship_id = scholarship.id
      AND existing_rule.rule_type = requested.rule_type
      AND existing_rule.description = requested.description
);
