-- Official Indian scholarship schemes listed as open for 2026-27 when checked
-- on 2026-11-13. The deadline is time-sensitive and must be rechecked each cycle.
-- Income rules are automated; MANUAL_REVIEW deliberately prevents a definitive
-- ELIGIBLE result while other official criteria are not represented in verified facts.

INSERT INTO scholarships (
    name,
    provider,
    description,
    amount,
    deadline,
    education_level,
    state,
    category,
    status
)
SELECT
    'AICTE Pragati Scholarship Scheme for Girl Students (Degree)',
    'All India Council for Technical Education (AICTE)',
    '2026-27 cycle. For eligible girl students in AICTE-approved technical degree programs. The official scheme also has family, admission-year, and institute conditions that this application must verify separately. Maximum assistance: INR 50,000 per year. Official application and current notice: https://scholarships.gov.in/. Source checked 2026-11-13.',
    50000.00,
    '2026-11-30',
    'Undergraduate',
    NULL,
    NULL,
    'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1
    FROM scholarships
    WHERE name = 'AICTE Pragati Scholarship Scheme for Girl Students (Degree)'
      AND provider = 'All India Council for Technical Education (AICTE)'
);

INSERT INTO scholarships (
    name,
    provider,
    description,
    amount,
    deadline,
    education_level,
    state,
    category,
    status
)
SELECT
    'AICTE Saksham Scholarship Scheme (Degree)',
    'All India Council for Technical Education (AICTE)',
    '2026-27 cycle. For eligible students with benchmark disability studying in an AICTE-approved technical degree program. Disability percentage, institute, and admission-year conditions require separate verification. Maximum assistance: INR 50,000 per year. Official application and current notice: https://scholarships.gov.in/. Source checked 2026-11-13.',
    50000.00,
    '2026-11-30',
    'Undergraduate',
    NULL,
    NULL,
    'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1
    FROM scholarships
    WHERE name = 'AICTE Saksham Scholarship Scheme (Degree)'
      AND provider = 'All India Council for Technical Education (AICTE)'
);

INSERT INTO scholarships (
    name,
    provider,
    description,
    amount,
    deadline,
    education_level,
    state,
    category,
    status
)
SELECT
    'AICTE Swanath Scholarship Scheme (Degree and Diploma)',
    'All India Council for Technical Education (AICTE)',
    '2026-27 cycle. For students meeting one of the scheme-defined vulnerable-family categories and enrolled in an AICTE-approved regular degree or diploma program. Qualifying category, institute, course-year, and other-scholarship conditions require separate verification. Assistance: INR 50,000 per year. Official application and current notice: https://scholarships.gov.in/. Source checked 2026-11-13.',
    50000.00,
    '2026-11-30',
    NULL,
    NULL,
    NULL,
    'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1
    FROM scholarships
    WHERE name = 'AICTE Swanath Scholarship Scheme (Degree and Diploma)'
      AND provider = 'All India Council for Technical Education (AICTE)'
);

INSERT INTO scholarships (
    name,
    provider,
    description,
    amount,
    deadline,
    education_level,
    state,
    category,
    status
)
SELECT
    'PM YASASVI Top Class Education in College for OBC, EBC and DNT Students',
    'Ministry of Social Justice and Empowerment, Government of India',
    '2026-27 cycle. For eligible OBC, EBC, and DNT students in notified institutions. Family-income, notified-institute, course, merit, and slot conditions apply; the award varies with eligible fees and allowances, so no single total amount is shown. Confirm the live notice and institution list before applying. Official application: https://scholarships.gov.in/. Scheme guidance: https://scholarships.gov.in/public/schemeGuidelines/ApprovedmodifieddraftofTopClassCollege.pdf. Source checked 2026-11-13.',
    NULL,
    '2026-11-30',
    'Undergraduate',
    NULL,
    NULL,
    'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1
    FROM scholarships
    WHERE name = 'PM YASASVI Top Class Education in College for OBC, EBC and DNT Students'
      AND provider = 'Ministry of Social Justice and Empowerment, Government of India'
);

-- Add the facts the current engine can evaluate, plus a non-evaluable check that
-- deliberately leaves each result POSSIBLY_ELIGIBLE until all scheme conditions
-- can be assessed from reviewer-confirmed facts.
INSERT INTO scholarship_rules (
    scholarship_id,
    rule_type,
    operator,
    rule_value,
    description
)
SELECT s.id, r.rule_type, r.operator, r.rule_value, r.description
FROM scholarships s
JOIN (
    SELECT
        'AICTE Pragati Scholarship Scheme for Girl Students (Degree)' AS scholarship_name,
        'All India Council for Technical Education (AICTE)' AS provider,
        'ANNUAL_FAMILY_INCOME' AS rule_type,
        '<=' AS operator,
        '800000' AS rule_value,
        'Annual family income must not exceed INR 8 lakh.' AS description
    UNION ALL
    SELECT
        'AICTE Pragati Scholarship Scheme for Girl Students (Degree)',
        'All India Council for Technical Education (AICTE)',
        'MANUAL_REVIEW',
        '=',
        'REQUIRED',
        'Confirm the current official gender, family, admission-year, and institute conditions.'
    UNION ALL
    SELECT
        'AICTE Saksham Scholarship Scheme (Degree)',
        'All India Council for Technical Education (AICTE)',
        'ANNUAL_FAMILY_INCOME',
        '<=',
        '800000',
        'Annual family income must not exceed INR 8 lakh.'
    UNION ALL
    SELECT
        'AICTE Saksham Scholarship Scheme (Degree)',
        'All India Council for Technical Education (AICTE)',
        'MANUAL_REVIEW',
        '=',
        'REQUIRED',
        'Confirm disability threshold, current admission-year, and AICTE institute conditions.'
    UNION ALL
    SELECT
        'AICTE Swanath Scholarship Scheme (Degree and Diploma)',
        'All India Council for Technical Education (AICTE)',
        'ANNUAL_FAMILY_INCOME',
        '<=',
        '800000',
        'Annual family income must not exceed INR 8 lakh.'
    UNION ALL
    SELECT
        'AICTE Swanath Scholarship Scheme (Degree and Diploma)',
        'All India Council for Technical Education (AICTE)',
        'MANUAL_REVIEW',
        '=',
        'REQUIRED',
        'Confirm the qualifying family category, current course-year, institute, and other-scholarship conditions.'
    UNION ALL
    SELECT
        'PM YASASVI Top Class Education in College for OBC, EBC and DNT Students',
        'Ministry of Social Justice and Empowerment, Government of India',
        'ANNUAL_FAMILY_INCOME',
        '<=',
        '250000',
        'Annual family income must not exceed INR 2.5 lakh.'
    UNION ALL
    SELECT
        'PM YASASVI Top Class Education in College for OBC, EBC and DNT Students',
        'Ministry of Social Justice and Empowerment, Government of India',
        'MANUAL_REVIEW',
        '=',
        'REQUIRED',
        'Confirm category, notified institution, course, merit, and available-slot conditions.'
) r
    ON r.scholarship_name = s.name
   AND r.provider = s.provider
WHERE NOT EXISTS (
    SELECT 1
    FROM scholarship_rules existing_rule
    WHERE existing_rule.scholarship_id = s.id
      AND existing_rule.rule_type = r.rule_type
);
