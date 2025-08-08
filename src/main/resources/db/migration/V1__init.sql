-- ----------  Domain / enum helpers ----------

CREATE TYPE sighting_group AS ENUM ('whale','dolphin','shark','fish','other');
CREATE TYPE report_status  AS ENUM ('ok','bad_weather');

-- ----------  Core lookup table ----------

CREATE TABLE species (
                         id              text        PRIMARY KEY,          -- slug e.g. 'humpback-whale'
                         "group"         sighting_group NOT NULL,
                         common_name     text        NOT NULL,
                         binomial_name   text,
                         aliases         text[]      DEFAULT '{}',         -- ARRAY for fast “search LIKE ANY”
                         first_seen      date,
                         last_seen       date,
                         created_at      timestamptz NOT NULL DEFAULT now(),
                         updated_at      timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX ix_species_group      ON species("group");
CREATE INDEX ix_species_aliases_gin ON species USING gin(aliases);

-- ----------  Day-level facts ----------

CREATE TABLE daily_report (
                              report_date     date        PRIMARY KEY,          -- natural key; 1 row / calendar day
                              tours           int         NOT NULL CHECK (tours >= 0),
                              status          report_status NOT NULL,
                              fetched_at      timestamptz NOT NULL,             -- when crawler grabbed that HTML
                              source_url      text        NOT NULL,
                              checksum        uuid        NOT NULL,             -- MD5/xxhash of the raw HTML for change-detection
                              version         int         NOT NULL DEFAULT 1,   -- simple optimistic versioning
                              created_at      timestamptz NOT NULL DEFAULT now(),
                              updated_at      timestamptz NOT NULL DEFAULT now()
);

-- Cover most API filters very cheaply
CREATE INDEX ix_daily_report_date           ON daily_report(report_date DESC);
CREATE INDEX ix_daily_report_status         ON daily_report(status);

-- ----------  Per-species tallies ----------

CREATE TABLE observation (
                             report_date     date        REFERENCES daily_report(report_date) ON DELETE CASCADE,
                             species_id      text        REFERENCES species(id)              ON DELETE RESTRICT,
                             individuals     int         NOT NULL CHECK (individuals >= 0),

                             PRIMARY KEY (report_date,species_id)     -- guarantees max one row per species per day
);

CREATE INDEX ix_observation_species     ON observation(species_id);
CREATE INDEX ix_observation_species_date ON observation(species_id,report_date);