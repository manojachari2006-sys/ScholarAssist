-- Pratiksha's published condition is passing Kerala Plus Two, not current
-- residence in Kerala. Ask about the qualifying school result explicitly.
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

