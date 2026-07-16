UPDATE vr_services
SET title = 'VR-вечірка 60 хв',
    duration_minutes = 60,
    price = 400.00,
    active = TRUE
WHERE slug = 'vr-party-60';

UPDATE vr_services
SET slug = 'vr-sprint-120',
    title = 'VR-спрінт 120 хв',
    duration_minutes = 120,
    price = 800.00,
    active = TRUE
WHERE slug = 'vr-arena-120'
  AND NOT EXISTS (
      SELECT 1
      FROM vr_services
      WHERE slug = 'vr-sprint-120'
  );

INSERT INTO vr_services (slug, title, duration_minutes, price, active)
SELECT 'vr-sprint-120', 'VR-спрінт 120 хв', 120, 800.00, TRUE
WHERE NOT EXISTS (
    SELECT 1
    FROM vr_services
    WHERE slug = 'vr-sprint-120'
);

UPDATE vr_services
SET title = 'VR-спрінт 120 хв',
    duration_minutes = 120,
    price = 800.00,
    active = TRUE
WHERE slug = 'vr-sprint-120';

UPDATE vr_services
SET active = FALSE
WHERE slug IN ('vr-arena-120', 'vr-quest-90', 'vr-kids-45');
