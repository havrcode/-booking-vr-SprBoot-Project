INSERT INTO vr_services (slug, title, duration_minutes, price, active)
SELECT 'vr-arena-120', 'VR-арена 120 хв', 120, 2200.00, TRUE
WHERE NOT EXISTS (
    SELECT 1
    FROM vr_services
    WHERE slug = 'vr-arena-120'
);
