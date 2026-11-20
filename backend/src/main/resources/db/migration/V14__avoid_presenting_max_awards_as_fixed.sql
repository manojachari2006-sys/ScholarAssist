-- A single amount column cannot express a maximum/range without misleading
-- the card UI into presenting the ceiling as a guaranteed award.
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

