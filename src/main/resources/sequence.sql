
CREATE SEQUENCE IF NOT EXISTS items_id_seq START 1 INCREMENT 1;
SELECT setval('items_id_seq', COALESCE((SELECT MAX(id) FROM items), 0) + 1, false);
