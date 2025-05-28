DO
$$BEGIN
    IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'spesidaler-opprydding-dev') THEN
        GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO "spesidaler-opprydding-dev";
        GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO "spesidaler-opprydding-dev";
    END IF;
END$$;
