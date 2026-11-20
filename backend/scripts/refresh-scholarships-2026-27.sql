-- Repeatable, additive catalog correction for the existing 2026-27 catalog.
-- Run explicitly in MySQL Workbench only after reviewing the matching rows.
-- This script updates only exact catalog name/provider pairs and adds missing
-- SDEF rules. It never deletes scholarship, application, student, or review data.

START TRANSACTION;

UPDATE scholarships
SET application_url = 'https://form.jotform.com/swamid/sdef-india-scholarship-2026-27',
    description = 'For first-year, full-time professional undergraduate students in 2026-27. Eligible programs and entrance requirements vary by course. Requires at least 80% in Class 12, no gap year after Class 12, and annual family income at most INR 12 lakh. Award is up to INR 50,000 per year and renewable subject to the Foundation rules. Apply through the official SDEF application form.'
WHERE name = 'SDEF India Scholarship Program 2026-27'
  AND provider = 'Swami Dayanand Education Foundation (SDEF)';

UPDATE scholarships
SET application_url = 'https://www.pratiksha.org/'
WHERE name = 'Pratiksha Trust Scholarship 2026-27'
  AND provider = 'Pratiksha Trust';

UPDATE scholarships
SET application_url = 'https://vikas.org.in/'
WHERE name = 'Vikash Educational Charitable Trust Scholarship 2026-27'
  AND provider = 'Vikash Educational Charitable Trust (VECT)';

UPDATE scholarships
SET amount = NULL,
    description = 'For students from Class 1 through postgraduate study in India. Published criteria include annual family income below INR 6 lakh and at least 60% in the previous academic year. The needs-based award is determined after review/interview; INR 6,000 is a published minimum, not a fixed award amount.'
WHERE name = 'Kind Circle Scholarship for Meritorious Students 2026-27'
  AND provider = 'Kind Circle / Buddy4Study';

UPDATE scholarships
SET amount = NULL
WHERE name IN (
    'SDEF India Scholarship Program 2026-27',
    'Nirankari Rajmata Scholarship Scheme 2026-27'
)
  AND provider IN (
      'Swami Dayanand Education Foundation (SDEF)',
      'Sant Nirankari Charitable Foundation'
  );

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
    SELECT 'SDEF India Scholarship Program 2026-27' AS scholarship_name,
           'PUC_MARKS_PERCENTAGE' AS rule_type,
           '>=' AS operator,
           '80' AS rule_value,
           'Class 12 marks must be at least 80 percent or equivalent.' AS description
    UNION ALL SELECT 'SDEF India Scholarship Program 2026-27',
           'YEAR_OF_STUDY', '=', '1',
           'Applicant must be entering the first year of the eligible full-time undergraduate program.'
    UNION ALL SELECT 'SDEF India Scholarship Program 2026-27',
           'COURSE', 'IN',
           'BTech,BE,MBBS,BDS,BSc Nursing,BNursing,BPharm,PharmD,BArch,BVSc and AH',
           'Course must be one of the professional undergraduate programs listed by SDEF for 2026-27.'
) requested
JOIN scholarships scholarship
  ON scholarship.name = requested.scholarship_name
WHERE NOT EXISTS (
    SELECT 1
    FROM scholarship_rules existing_rule
    WHERE existing_rule.scholarship_id = scholarship.id
      AND existing_rule.rule_type = requested.rule_type
      AND existing_rule.operator = requested.operator
      AND existing_rule.rule_value = requested.rule_value
);

UPDATE scholarship_rules rule_row
JOIN scholarships scholarship
  ON scholarship.id = rule_row.scholarship_id
SET rule_row.description = 'Confirm no gap year after Class 12, the course-specific entrance score/rank and any required declarations in the official SDEF criteria.'
WHERE scholarship.name = 'SDEF India Scholarship Program 2026-27'
  AND rule_row.rule_type = 'MANUAL_REVIEW';

UPDATE scholarship_rules rule_row
JOIN scholarships scholarship
  ON scholarship.id = rule_row.scholarship_id
SET rule_row.rule_value = 'B.E.,BE,B.Tech,BTech,MBBS,BDS,B.Sc. Nursing,BSc Nursing,BNursing,B.Pharm.,BPharm,Pharm.D,PharmD,B.Arch.,BArch,BVSc & AH,BVSc and AH'
WHERE scholarship.name = 'SDEF India Scholarship Program 2026-27'
  AND rule_row.rule_type = 'COURSE'
  AND rule_row.operator = 'IN';

UPDATE scholarships
SET state = NULL
WHERE name = 'Pratiksha Trust Scholarship 2026-27'
  AND provider = 'Pratiksha Trust';

UPDATE scholarship_rules rule_row
JOIN scholarships scholarship
  ON scholarship.id = rule_row.scholarship_id
SET rule_row.rule_type = 'MANUAL_REVIEW',
    rule_row.operator = '=',
    rule_row.rule_value = 'REQUIRED',
    rule_row.description = 'Did you pass your qualifying Plus Two examination in Kerala?'
WHERE scholarship.name = 'Pratiksha Trust Scholarship 2026-27'
  AND rule_row.rule_type = 'STATE';

UPDATE scholarship_rules rule_row
JOIN scholarships scholarship
  ON scholarship.id = rule_row.scholarship_id
SET rule_row.description = 'Confirm admission to eligible Engineering or MBBS study, required entrance result and any current interview/form conditions.'
WHERE scholarship.name = 'Pratiksha Trust Scholarship 2026-27'
  AND rule_row.rule_type = 'MANUAL_REVIEW'
  AND rule_row.description LIKE 'Confirm course admission%';

COMMIT;

