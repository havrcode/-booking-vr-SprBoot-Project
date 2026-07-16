UPDATE vr_services
SET slug = 'vr-marathon-120',
    title = 'VR-марафон 120 хв',
    duration_minutes = 120,
    price = 800.00,
    active = TRUE
WHERE slug = 'vr-sprint-120'
  AND NOT EXISTS (
      SELECT 1
      FROM vr_services
      WHERE slug = 'vr-marathon-120'
  );

INSERT INTO vr_services (slug, title, duration_minutes, price, active)
SELECT 'vr-marathon-120', 'VR-марафон 120 хв', 120, 800.00, TRUE
WHERE NOT EXISTS (
    SELECT 1
    FROM vr_services
    WHERE slug = 'vr-marathon-120'
);

UPDATE vr_services
SET title = 'VR-марафон 120 хв',
    duration_minutes = 120,
    price = 800.00,
    active = TRUE
WHERE slug = 'vr-marathon-120';

UPDATE vr_services
SET active = FALSE
WHERE slug = 'vr-sprint-120';
