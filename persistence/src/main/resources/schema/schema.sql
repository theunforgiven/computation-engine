-- You may need to change the TEXT datatype to accommodate
-- the SQL dialect of your platform.

CREATE TABLE computation_nodes (
    id BIGINT NOT NULL,
    library VARCHAR(256) NOT NULL,
    version VARCHAR(256) NOT NULL,
    attribute_name VARCHAR(256) NOT NULL,
    attribute_value TEXT NOT NULL -- Non-portable data type; might be CLOB, etc.
);

CREATE TABLE computation_edges (
    library VARCHAR(256) NOT NULL,
    version VARCHAR(256) NOT NULL,
    origin_id BIGINT NOT NULL,
    target_id BIGINT NOT NULL,
    sequence INTEGER
);

