-- Refresh the discovery catalog using the reviewed 2026-27 scholarship catalog.
-- Older non-catalog sample rows are retained for referential history but hidden
-- from student discovery by marking them INACTIVE.
ALTER TABLE scholarships
    ADD COLUMN application_url VARCHAR(1000) NULL;

-- Normalize the four official programs already installed by V6 and add their
-- individual official application destinations.
UPDATE scholarships
SET name = 'AICTE Pragati Scholarship Scheme for Girl Students (Degree) 2026-27',
    description = 'For eligible girl students in AICTE-approved technical degree programs. Conditions include first-year entry or second-year lateral entry and family criteria. Income at most INR 8 lakh; maximum assistance INR 50,000 per year. NSP listed applications through 30 November 2026.',
    application_url = 'https://scholarships.gov.in/All-Scholarships'
WHERE provider = 'All India Council for Technical Education (AICTE)'
  AND name LIKE 'AICTE Pragati Scholarship Scheme for Girl Students (Degree)%';

UPDATE scholarships
SET name = 'AICTE Saksham Scholarship Scheme for Specially Abled Students (Degree) 2026-27',
    description = 'For eligible students with benchmark disability in AICTE-approved technical degree programs. Published guidance includes disability of at least 40%, entry-year conditions and family income at most INR 8 lakh. Maximum assistance INR 50,000 per year. NSP listed applications through 30 November 2026.',
    application_url = 'https://scholarships.gov.in/All-Scholarships'
WHERE provider = 'All India Council for Technical Education (AICTE)'
  AND name LIKE 'AICTE Saksham Scholarship Scheme%';

UPDATE scholarships
SET name = 'AICTE Swanath Scholarship Scheme (Degree and Diploma) 2026-27',
    description = 'For applicants meeting a scheme-defined vulnerable-family category and enrolled in an AICTE-approved regular technical degree or diploma. Income at most INR 8 lakh; other course-year and other-scholarship conditions apply. Assistance INR 50,000 per year. NSP listed applications through 30 November 2026.',
    application_url = 'https://scholarships.gov.in/All-Scholarships'
WHERE provider = 'All India Council for Technical Education (AICTE)'
  AND name LIKE 'AICTE Swanath Scholarship Scheme%';

UPDATE scholarships
SET name = 'PM YASASVI Top Class Education in College for OBC, EBC and DNT Students 2026-27',
    description = 'For eligible OBC/EBC/DNT students studying at notified institutions; annual family income at most INR 2.5 lakh. NSP listed the 2026-27 student application deadline as 30 November 2026. Tuition and allowance support varies by institution and course.',
    application_url = 'https://scholarships.gov.in/All-Scholarships'
WHERE provider = 'Ministry of Social Justice and Empowerment, Government of India'
  AND name LIKE 'PM YASASVI Top Class Education in College%';

-- Add the remaining records from the reviewed catalog. URLs point to the
-- application page; description text summarizes key conditions and limitations.
INSERT INTO scholarships (
    name, provider, description, amount, deadline, education_level,
    state, category, status, application_url
)
SELECT catalog.name, catalog.provider, catalog.description, catalog.amount,
       catalog.deadline, catalog.education_level, catalog.state,
       catalog.category, 'ACTIVE', catalog.application_url
FROM (
    SELECT 'SDEF India Scholarship Program 2026-27' AS name,
           'Swami Dayanand Education Foundation (SDEF)' AS provider,
           'First-year full-time professional UG applicants in 2026-27; eligible programs include BTech/BE, MBBS, BDS and BArch. Class 12 marks and course/category-specific entrance ranks apply. Up to INR 50,000 per academic year, renewable subject to rules.' AS description,
           50000.00 AS amount, '2026-11-30' AS deadline, 'Undergraduate' AS education_level,
           NULL AS state, NULL AS category,
           'https://www.buddy4study.com/page/sdef-india-scholarship-program-2026-27' AS application_url
    UNION ALL SELECT 'ONGC Merit Scholarship 2026-27', 'ONGC Foundation',
           'First-year Engineering/MBBS, MBA, and Master''s in Geology/Geophysics for SC/ST/OBC/economically weaker General applicants. INR 48,000 per year. Income limits vary by category; full-time study, marks, age and no-other-scholarship conditions apply.',
           48000.00, '2026-11-30', NULL, NULL, NULL, 'https://ongcscholar.org/'
    UNION ALL SELECT 'Nirankari Rajmata Scholarship Scheme 2026-27', 'Sant Nirankari Charitable Foundation',
           'Merit-cum-means support for selected professional/technical UG and PG courses in India. Tuition aid up to INR 75,000 per year. Published requirements include Class 12 merit, income below INR 5 lakh, competitive admission and restrictions on other aid/quota. Application is sent by speed post.',
           75000.00, '2026-11-30', NULL, NULL, NULL, 'https://nirankarifoundation.org/scholarship/'
    UNION ALL SELECT 'Vylor Agritech Scholarship - Undergraduate 2026-27', 'Vylor',
           'Any-year undergraduate Agriculture/allied-course students at eligible government institutions, including ICAR-recognized colleges and State Agricultural Universities. INR 25,000 per year; income up to INR 6 lakh. Female applicants receive preference.',
           25000.00, '2026-11-30', 'Undergraduate', NULL, NULL,
           'https://www.buddy4study.com/page/vylor-agritech-scholarship-program-for-undergraduate-students-fy-26-27'
    UNION ALL SELECT 'Vylor Agritech Scholarship - Postgraduate and PhD 2026-27', 'Vylor',
           'Any-year postgraduate/PhD Agriculture or allied-course students at eligible government institutions, including ICAR-recognized colleges and State Agricultural Universities. INR 35,000 per year; income up to INR 6 lakh. Female applicants receive preference.',
           35000.00, '2026-11-30', 'Postgraduate', NULL, NULL,
           'https://www.buddy4study.com/page/vylor-agritech-scholarship-program'
    UNION ALL SELECT 'Kind Circle Scholarship for Meritorious Students 2026-27', 'Kind Circle / Buddy4Study',
           'For students from Class 1 through postgraduate study in India. Family income below INR 6 lakh and at least 60 percent in the previous academic year. Needs-based award after interview; INR 6,000 is the published minimum, not a guaranteed fixed amount.',
           6000.00, '2027-03-31', NULL, NULL, NULL,
           'https://www.buddy4study.com/page/kind-circle-scholarship-for-meritorious-students'
    UNION ALL SELECT 'Pratiksha Trust Scholarship 2026-27', 'Pratiksha Trust',
           'Students who passed Kerala Plus Two and entered Engineering or MBBS in 2026-27, scored above 80 percent and have family income below INR 3 lakh. Award ceilings vary: Engineering INR 54,000/year; MBBS INR 60,000/year. Interview may apply.',
           NULL, '2026-12-10', 'Undergraduate', 'Kerala', NULL,
           'https://www.buddy4study.com/scholarship/pratiksha-trust-scholarship'
    UNION ALL SELECT 'Vikash Educational Charitable Trust Scholarship 2026-27', 'Vikash Educational Charitable Trust (VECT)',
           'For meritorious, financially disadvantaged students from Odisha pursuing eligible diploma, UG or PG courses in Humanities, Science, Commerce, Engineering, Medicine, Nursing, Agriculture and other listed fields. Amount varies. Current instructions require a printed form and supporting documents sent by post.',
           NULL, '2026-11-30', NULL, 'Odisha', NULL, 'https://www.vikas.org.in/'
    UNION ALL SELECT 'HDFC Bank Parivartan ECSS - Higher Education 2026-27', 'HDFC Bank (Parivartan CSR Programme)',
           'For Indian students in recognized UG/PG programs; at least 55 percent in the latest qualifying exam and family income up to INR 2.5 lakh. Awards vary by course and level from INR 30,000 to INR 75,000. Recent personal/family crisis is a preference.',
           NULL, '2026-12-15', NULL, NULL, NULL, 'https://www.parivartanecss.com/'
    UNION ALL SELECT 'Karnataka SSP Sandeepani Shishya Vetana 2026-27', 'Karnataka State Brahmin Development Board, Government of Karnataka',
           'For eligible Karnataka Brahmin students studying any course, with EWS certificate, income below INR 8 lakh and promotion to the current year. Management-quota and COMEDK admissions are excluded. Current SSP portal lists 31 December 2026 for this department.',
           NULL, '2026-12-31', NULL, 'Karnataka', 'Brahmin',
           'https://ssp.postmatric.karnataka.gov.in/post_sa_2627/login'
    UNION ALL SELECT 'Karnataka SSP Vidyasiri Scheme for Minorities 2026-27', 'Department of Minority Welfare, Government of Karnataka',
           'For eligible Karnataka minority-community students in higher education with family income at most INR 2.5 lakh, promoted to the current year, and not allotted a minority-hostel seat after applying. Award varies. SSP lists 30 November 2026 for Minority Welfare post-matric applications.',
           NULL, '2026-11-30', NULL, 'Karnataka', NULL,
           'https://ssp.postmatric.karnataka.gov.in/post_sa_2627/login'
    UNION ALL SELECT 'DCE C. V. Raman Scholarship, Karnataka 2026-27', 'Department of Collegiate Education, Government of Karnataka',
           'For BSc students at eligible Karnataka government colleges studying two subjects from Physics, Chemistry, Mathematics, Botany, Zoology and Geology, with promotion to the current year. Reported PUC thresholds are 60 percent for SC/ST and 65 percent for others. No family-income limit; award varies.',
           NULL, '2027-01-31', 'Undergraduate', 'Karnataka', NULL,
           'https://ssp.postmatric.karnataka.gov.in/post_sa_2627/login'
    UNION ALL SELECT 'Karnataka SSP DTE Post-Matric Scholarship for ST Technical Students 2026-27', 'Department of Technical Education, Government of Karnataka',
           'For ST students in specified technical programs at eligible Karnataka colleges, promoted to the current year and not admitted through management quota. Published annual income band INR 2.5 lakh to INR 10 lakh. Award varies by fees and course.',
           NULL, '2026-11-30', NULL, 'Karnataka', 'ST',
           'https://ssp.postmatric.karnataka.gov.in/post_sa_2627/login'
    UNION ALL SELECT 'Karnataka SSP SC Post-Matric Scholarship (Social Welfare) 2026-27', 'Social Welfare Department, Government of Karnataka',
           'For Karnataka SC students in post-matric study. Full-benefit income band is reported up to INR 2.5 lakh; some engineering/medical cases above that up to INR 10 lakh may receive partial fee support. Benefit depends on course/group; amount is variable.',
           NULL, '2027-01-31', NULL, 'Karnataka', 'SC',
           'https://ssp.postmatric.karnataka.gov.in/post_sa_2627/login'
    UNION ALL SELECT 'Karnataka SSP ST Post-Matric Scholarship (Tribal Welfare) 2026-27', 'Tribal Welfare Department, Government of Karnataka',
           'For Karnataka ST students in post-matric study. Full-benefit income band is reported up to INR 2.5 lakh; some engineering/medical cases above that up to INR 10 lakh may receive partial support. Award depends on course/group.',
           NULL, '2026-12-31', NULL, 'Karnataka', 'ST',
           'https://ssp.postmatric.karnataka.gov.in/post_sa_2627/login'
    UNION ALL SELECT 'Karnataka SSP Scholarship for Students with Disabilities 2026-27', 'Department of Disability Welfare, Government of Karnataka',
           'For eligible students with disabilities in Karnataka, any course/college, with promotion to the present year and valid UDID. The cited scheme says there is no caste or income limit; assistance varies. SSP lists 30 November 2026 for post-matric Disability Welfare applications.',
           NULL, '2026-11-30', NULL, 'Karnataka', NULL,
           'https://ssp.postmatric.karnataka.gov.in/post_sa_2627/login'
) catalog
WHERE NOT EXISTS (
    SELECT 1 FROM scholarships existing
    WHERE existing.name = catalog.name AND existing.provider = catalog.provider
);

-- Refresh URLs and reactivate matching catalog rows that may already have
-- existed as manually entered records before this migration.
UPDATE scholarships
SET status = 'ACTIVE',
    application_url = CASE name
        WHEN 'SDEF India Scholarship Program 2026-27' THEN 'https://www.buddy4study.com/page/sdef-india-scholarship-program-2026-27'
        WHEN 'ONGC Merit Scholarship 2026-27' THEN 'https://ongcscholar.org/'
        WHEN 'Nirankari Rajmata Scholarship Scheme 2026-27' THEN 'https://nirankarifoundation.org/scholarship/'
        WHEN 'Vylor Agritech Scholarship - Undergraduate 2026-27' THEN 'https://www.buddy4study.com/page/vylor-agritech-scholarship-program-for-undergraduate-students-fy-26-27'
        WHEN 'Vylor Agritech Scholarship - Postgraduate and PhD 2026-27' THEN 'https://www.buddy4study.com/page/vylor-agritech-scholarship-program'
        WHEN 'Kind Circle Scholarship for Meritorious Students 2026-27' THEN 'https://www.buddy4study.com/page/kind-circle-scholarship-for-meritorious-students'
        WHEN 'Pratiksha Trust Scholarship 2026-27' THEN 'https://www.buddy4study.com/scholarship/pratiksha-trust-scholarship'
        WHEN 'Vikash Educational Charitable Trust Scholarship 2026-27' THEN 'https://www.vikas.org.in/'
        WHEN 'HDFC Bank Parivartan ECSS - Higher Education 2026-27' THEN 'https://www.parivartanecss.com/'
        WHEN 'Karnataka SSP Sandeepani Shishya Vetana 2026-27' THEN 'https://ssp.postmatric.karnataka.gov.in/post_sa_2627/login'
        WHEN 'Karnataka SSP Vidyasiri Scheme for Minorities 2026-27' THEN 'https://ssp.postmatric.karnataka.gov.in/post_sa_2627/login'
        WHEN 'DCE C. V. Raman Scholarship, Karnataka 2026-27' THEN 'https://ssp.postmatric.karnataka.gov.in/post_sa_2627/login'
        WHEN 'Karnataka SSP DTE Post-Matric Scholarship for ST Technical Students 2026-27' THEN 'https://ssp.postmatric.karnataka.gov.in/post_sa_2627/login'
        WHEN 'Karnataka SSP SC Post-Matric Scholarship (Social Welfare) 2026-27' THEN 'https://ssp.postmatric.karnataka.gov.in/post_sa_2627/login'
        WHEN 'Karnataka SSP ST Post-Matric Scholarship (Tribal Welfare) 2026-27' THEN 'https://ssp.postmatric.karnataka.gov.in/post_sa_2627/login'
        WHEN 'Karnataka SSP Scholarship for Students with Disabilities 2026-27' THEN 'https://ssp.postmatric.karnataka.gov.in/post_sa_2627/login'
        WHEN 'PM YASASVI Top Class Education in College for OBC, EBC and DNT Students 2026-27' THEN 'https://scholarships.gov.in/All-Scholarships'
        WHEN 'AICTE Pragati Scholarship Scheme for Girl Students (Degree) 2026-27' THEN 'https://scholarships.gov.in/All-Scholarships'
        WHEN 'AICTE Saksham Scholarship Scheme for Specially Abled Students (Degree) 2026-27' THEN 'https://scholarships.gov.in/All-Scholarships'
        WHEN 'AICTE Swanath Scholarship Scheme (Degree and Diploma) 2026-27' THEN 'https://scholarships.gov.in/All-Scholarships'
    END
WHERE name IN (
    'SDEF India Scholarship Program 2026-27',
    'ONGC Merit Scholarship 2026-27',
    'Nirankari Rajmata Scholarship Scheme 2026-27',
    'Vylor Agritech Scholarship - Undergraduate 2026-27',
    'Vylor Agritech Scholarship - Postgraduate and PhD 2026-27',
    'Kind Circle Scholarship for Meritorious Students 2026-27',
    'Pratiksha Trust Scholarship 2026-27',
    'Vikash Educational Charitable Trust Scholarship 2026-27',
    'HDFC Bank Parivartan ECSS - Higher Education 2026-27',
    'Karnataka SSP Sandeepani Shishya Vetana 2026-27',
    'Karnataka SSP Vidyasiri Scheme for Minorities 2026-27',
    'DCE C. V. Raman Scholarship, Karnataka 2026-27',
    'Karnataka SSP DTE Post-Matric Scholarship for ST Technical Students 2026-27',
    'Karnataka SSP SC Post-Matric Scholarship (Social Welfare) 2026-27',
    'Karnataka SSP ST Post-Matric Scholarship (Tribal Welfare) 2026-27',
    'Karnataka SSP Scholarship for Students with Disabilities 2026-27',
    'PM YASASVI Top Class Education in College for OBC, EBC and DNT Students 2026-27',
    'AICTE Pragati Scholarship Scheme for Girl Students (Degree) 2026-27',
    'AICTE Saksham Scholarship Scheme for Specially Abled Students (Degree) 2026-27',
    'AICTE Swanath Scholarship Scheme (Degree and Diploma) 2026-27'
);

-- Retain older sample rows and their application history without offering them
-- to students. Only the 20 reviewed catalog records remain active for discovery.
UPDATE scholarships
SET status = 'INACTIVE'
WHERE name NOT IN (
    'SDEF India Scholarship Program 2026-27',
    'ONGC Merit Scholarship 2026-27',
    'Nirankari Rajmata Scholarship Scheme 2026-27',
    'Vylor Agritech Scholarship - Undergraduate 2026-27',
    'Vylor Agritech Scholarship - Postgraduate and PhD 2026-27',
    'Kind Circle Scholarship for Meritorious Students 2026-27',
    'Pratiksha Trust Scholarship 2026-27',
    'Vikash Educational Charitable Trust Scholarship 2026-27',
    'HDFC Bank Parivartan ECSS - Higher Education 2026-27',
    'Karnataka SSP Sandeepani Shishya Vetana 2026-27',
    'Karnataka SSP Vidyasiri Scheme for Minorities 2026-27',
    'DCE C. V. Raman Scholarship, Karnataka 2026-27',
    'Karnataka SSP DTE Post-Matric Scholarship for ST Technical Students 2026-27',
    'Karnataka SSP SC Post-Matric Scholarship (Social Welfare) 2026-27',
    'Karnataka SSP ST Post-Matric Scholarship (Tribal Welfare) 2026-27',
    'Karnataka SSP Scholarship for Students with Disabilities 2026-27',
    'PM YASASVI Top Class Education in College for OBC, EBC and DNT Students 2026-27',
    'AICTE Pragati Scholarship Scheme for Girl Students (Degree) 2026-27',
    'AICTE Saksham Scholarship Scheme for Specially Abled Students (Degree) 2026-27',
    'AICTE Swanath Scholarship Scheme (Degree and Diploma) 2026-27'
);

-- Attach the rule rows from the reviewed PDF. MANUAL_REVIEW keeps the current
-- engine conservative where course, marks, disability, gender, institute,
-- current-year enrollment, or other scheme-specific facts are not verified.
INSERT INTO scholarship_rules (scholarship_id, rule_type, operator, rule_value, description)
SELECT s.id, r.rule_type, r.operator, r.rule_value, r.description
FROM scholarships s
JOIN (
    SELECT 'SDEF India Scholarship Program 2026-27' AS scholarship_name, 'ANNUAL_FAMILY_INCOME' AS rule_type, '<=' AS operator, '1200000' AS rule_value, 'Annual family income must not exceed INR 12 lakh.' AS description
    UNION ALL SELECT 'SDEF India Scholarship Program 2026-27', 'MANUAL_REVIEW', '=', 'REQUIRED', 'Confirm first-year enrollment, eligible course, Class 12 marks, entrance rank/category threshold and declarations.'
    UNION ALL SELECT 'ONGC Merit Scholarship 2026-27', 'MANUAL_REVIEW', '=', 'REQUIRED', 'Confirm eligible course/year, category-specific income cap, marks, age, full-time status and other-scholarship restriction.'
    UNION ALL SELECT 'Nirankari Rajmata Scholarship Scheme 2026-27', 'ANNUAL_FAMILY_INCOME', '<', '500000', 'Annual family income must be below INR 5 lakh.'
    UNION ALL SELECT 'Nirankari Rajmata Scholarship Scheme 2026-27', 'MANUAL_REVIEW', '=', 'REQUIRED', 'Confirm eligible course, marks, competitive admission, no-other-aid/quota certificate and postal submission.'
    UNION ALL SELECT 'Vylor Agritech Scholarship - Undergraduate 2026-27', 'ANNUAL_FAMILY_INCOME', '<=', '600000', 'Annual family income must not exceed INR 6 lakh.'
    UNION ALL SELECT 'Vylor Agritech Scholarship - Undergraduate 2026-27', 'MANUAL_REVIEW', '=', 'REQUIRED', 'Confirm Agriculture/allied course, eligible government institution, enrollment year and employee-family exclusion.'
    UNION ALL SELECT 'Vylor Agritech Scholarship - Postgraduate and PhD 2026-27', 'ANNUAL_FAMILY_INCOME', '<=', '600000', 'Annual family income must not exceed INR 6 lakh.'
    UNION ALL SELECT 'Vylor Agritech Scholarship - Postgraduate and PhD 2026-27', 'MANUAL_REVIEW', '=', 'REQUIRED', 'Confirm course, eligible institution, enrollment level/year and employee-family exclusion.'
    UNION ALL SELECT 'Kind Circle Scholarship for Meritorious Students 2026-27', 'ANNUAL_FAMILY_INCOME', '<', '600000', 'Annual family income must be below INR 6 lakh.'
    UNION ALL SELECT 'Kind Circle Scholarship for Meritorious Students 2026-27', 'MANUAL_REVIEW', '=', 'REQUIRED', 'Confirm previous-year marks, enrollment, financial need and interview/award decision.'
    UNION ALL SELECT 'Pratiksha Trust Scholarship 2026-27', 'ANNUAL_FAMILY_INCOME', '<', '300000', 'Annual family income must be below INR 3 lakh.'
    UNION ALL SELECT 'Pratiksha Trust Scholarship 2026-27', 'STATE', '=', 'Kerala', 'Must have passed qualifying Plus Two in Kerala.'
    UNION ALL SELECT 'Pratiksha Trust Scholarship 2026-27', 'MANUAL_REVIEW', '=', 'REQUIRED', 'Confirm course admission, Plus Two score, entrance rank and interview requirements.'
    UNION ALL SELECT 'Vikash Educational Charitable Trust Scholarship 2026-27', 'STATE', '=', 'Odisha', 'Applicant must be from Odisha.'
    UNION ALL SELECT 'Vikash Educational Charitable Trust Scholarship 2026-27', 'MANUAL_REVIEW', '=', 'REQUIRED', 'Confirm eligible course, merit, financial need, current form and hard-copy submission instructions.'
    UNION ALL SELECT 'HDFC Bank Parivartan ECSS - Higher Education 2026-27', 'ANNUAL_FAMILY_INCOME', '<=', '250000', 'Annual family income must not exceed INR 2.5 lakh.'
    UNION ALL SELECT 'HDFC Bank Parivartan ECSS - Higher Education 2026-27', 'MANUAL_REVIEW', '=', 'REQUIRED', 'Confirm nationality, full-time recognized UG/PG program, 55 percent marks and current call documents.'
    UNION ALL SELECT 'Karnataka SSP Sandeepani Shishya Vetana 2026-27', 'ANNUAL_FAMILY_INCOME', '<', '800000', 'Annual family income must be below INR 8 lakh.'
    UNION ALL SELECT 'Karnataka SSP Sandeepani Shishya Vetana 2026-27', 'MANUAL_REVIEW', '=', 'REQUIRED', 'Confirm valid EWS certificate, eligible seat/admission, promotion and SSP documents.'
    UNION ALL SELECT 'Karnataka SSP Vidyasiri Scheme for Minorities 2026-27', 'ANNUAL_FAMILY_INCOME', '<=', '250000', 'Annual family income must not exceed INR 2.5 lakh.'
    UNION ALL SELECT 'Karnataka SSP Vidyasiri Scheme for Minorities 2026-27', 'MANUAL_REVIEW', '=', 'REQUIRED', 'Confirm recognized minority community, eligible course/college, promotion and hostel application/no-seat condition.'
    UNION ALL SELECT 'DCE C. V. Raman Scholarship, Karnataka 2026-27', 'STATE', '=', 'Karnataka', 'Must study at an eligible Karnataka government college.'
    UNION ALL SELECT 'DCE C. V. Raman Scholarship, Karnataka 2026-27', 'MANUAL_REVIEW', '=', 'REQUIRED', 'Confirm BSc course, subject combination, excluded colleges, PUC mark threshold and promotion.'
    UNION ALL SELECT 'Karnataka SSP DTE Post-Matric Scholarship for ST Technical Students 2026-27', 'ANNUAL_FAMILY_INCOME', '>=', '250000', 'Published annual-income band starts at INR 2.5 lakh.'
    UNION ALL SELECT 'Karnataka SSP DTE Post-Matric Scholarship for ST Technical Students 2026-27', 'ANNUAL_FAMILY_INCOME', '<=', '1000000', 'Published annual-income band ends at INR 10 lakh.'
    UNION ALL SELECT 'Karnataka SSP DTE Post-Matric Scholarship for ST Technical Students 2026-27', 'MANUAL_REVIEW', '=', 'REQUIRED', 'Confirm ST status, listed technical course, eligible college/seat and promotion.'
    UNION ALL SELECT 'Karnataka SSP SC Post-Matric Scholarship (Social Welfare) 2026-27', 'ANNUAL_FAMILY_INCOME', '<=', '1000000', 'Scheme information includes income bands up to INR 10 lakh; course and benefit tier must be confirmed.'
    UNION ALL SELECT 'Karnataka SSP SC Post-Matric Scholarship (Social Welfare) 2026-27', 'MANUAL_REVIEW', '=', 'REQUIRED', 'Confirm SC category, course group, institution/seat, income tier and fee reimbursement rules.'
    UNION ALL SELECT 'Karnataka SSP ST Post-Matric Scholarship (Tribal Welfare) 2026-27', 'ANNUAL_FAMILY_INCOME', '<=', '1000000', 'Scheme information includes income bands up to INR 10 lakh; course and benefit tier must be confirmed.'
    UNION ALL SELECT 'Karnataka SSP ST Post-Matric Scholarship (Tribal Welfare) 2026-27', 'MANUAL_REVIEW', '=', 'REQUIRED', 'Confirm ST category, course group, institution/seat, income tier and fee reimbursement rules.'
    UNION ALL SELECT 'Karnataka SSP Scholarship for Students with Disabilities 2026-27', 'STATE', '=', 'Karnataka', 'Must meet Karnataka residence and SSP scheme requirements.'
    UNION ALL SELECT 'Karnataka SSP Scholarship for Students with Disabilities 2026-27', 'MANUAL_REVIEW', '=', 'REQUIRED', 'Confirm disability/UDID, course, college, promotion and correct post-matric application.'
) r ON r.scholarship_name = s.name
WHERE NOT EXISTS (
    SELECT 1 FROM scholarship_rules existing_rule
    WHERE existing_rule.scholarship_id = s.id
      AND existing_rule.rule_type = r.rule_type
);

