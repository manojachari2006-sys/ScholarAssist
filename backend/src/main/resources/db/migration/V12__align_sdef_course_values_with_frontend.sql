-- The quick-check UI uses these labels for equivalent SDEF-eligible programs.
-- Keep the database rule values aligned with the exact selectable labels.
UPDATE scholarship_rules rule_row
JOIN scholarships scholarship
  ON scholarship.id = rule_row.scholarship_id
SET rule_row.rule_value = 'B.E.,BE,B.Tech,BTech,MBBS,BDS,B.Sc. Nursing,BSc Nursing,BNursing,B.Pharm.,BPharm,Pharm.D,PharmD,B.Arch.,BArch,BVSc & AH,BVSc and AH'
WHERE scholarship.name = 'SDEF India Scholarship Program 2026-27'
  AND rule_row.rule_type = 'COURSE'
  AND rule_row.operator = 'IN';

