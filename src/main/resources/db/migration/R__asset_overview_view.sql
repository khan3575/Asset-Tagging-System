-- =============================================================================
-- R__asset_overview_view
--
-- REPEATABLE migration (note the single underscore after R, no version number).
-- Flyway runs every versioned migration (V1, V2, ...) first, in order, exactly
-- once each. Repeatable migrations run AFTER all of those, and re-run whenever
-- their file content changes -- Flyway tracks them by checksum, not by version.
--
-- A VIEW definition is the textbook use case: it holds no data of its own, so
-- redefining it carries no migration risk, and CREATE OR REPLACE VIEW is
-- naturally idempotent. Editing this file and restarting the app is enough --
-- unlike a V-file, there is no "never edit this" rule here.
--
-- display_status recreates the old single-column AVAILABLE / ASSIGNED reading
-- from the two-axis model, so existing UI code that expects a status string
-- keeps working unchanged.
-- =============================================================================

CREATE OR REPLACE VIEW asset_overview AS
SELECT
    a.id,
    a.asset_tag,
    a.name,
    a.category_id,
    cat.name                                 AS category_name,
    a.purchase_date,
    a.purchase_value,
    a.condition_status,
    ac.custodian_id                          AS current_custodian_id,
    CONCAT(cu.first_name, ' ', cu.last_name) AS current_custodian_name,
    ac.custody_start                         AS current_custody_start,
    CASE
        WHEN a.condition_status <> 'IN_SERVICE' THEN a.condition_status
        WHEN ac.custodian_id IS NOT NULL        THEN 'ASSIGNED'
        ELSE 'AVAILABLE'
    END                                       AS display_status
FROM assets a
JOIN      asset_categories cat ON cat.id = a.category_id
-- Joining on active_asset_id, not asset_id + status = 'ACTIVE': active_asset_id
-- carries the unique constraint, so this join can never multiply a row even if
-- a bug elsewhere ever let two custody rows exist for one asset.
LEFT JOIN asset_custody   ac  ON ac.active_asset_id = a.id
LEFT JOIN users           cu  ON cu.id = ac.custodian_id;
