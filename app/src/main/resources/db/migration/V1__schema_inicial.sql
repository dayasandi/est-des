CREATE TABLE sector (
    code         VARCHAR(8)     NOT NULL,
    base_price   DECIMAL(12, 2) NOT NULL,
    max_capacity INT            NOT NULL,
    PRIMARY KEY (code),
    CONSTRAINT ck_sector_capacity CHECK (max_capacity > 0),
    CONSTRAINT ck_sector_price CHECK (base_price >= 0)
) ENGINE = InnoDB;

CREATE TABLE spot (
    id          BIGINT        NOT NULL,
    sector_code VARCHAR(8)    NOT NULL,
    lat         DECIMAL(9, 6) NOT NULL,
    lng         DECIMAL(9, 6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_spot_sector FOREIGN KEY (sector_code) REFERENCES sector (code),
    CONSTRAINT uq_spot_location UNIQUE (lat, lng)
) ENGINE = InnoDB;

CREATE TABLE parking_session (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    plate         VARCHAR(16)   NOT NULL,
    status        VARCHAR(8)    NOT NULL,
    entry_time    DATETIME(3)   NOT NULL,
    spot_id       BIGINT        NULL,
    sector_code   VARCHAR(8)    NULL,
    hourly_price  DECIMAL(12, 2) NULL,
    price_factor  DECIMAL(4, 2) NULL,
    exit_time     DATETIME(3)   NULL,
    amount        DECIMAL(12, 2) NULL,
    active_plate  VARCHAR(16) GENERATED ALWAYS AS (IF(status <> 'CLOSED', plate, NULL)) STORED,
    occupied_spot BIGINT GENERATED ALWAYS AS (IF(status = 'PARKED', spot_id, NULL)) STORED,
    PRIMARY KEY (id),
    CONSTRAINT uq_session_active_plate UNIQUE (active_plate),
    CONSTRAINT uq_session_occupied_spot UNIQUE (occupied_spot),
    CONSTRAINT fk_session_spot FOREIGN KEY (spot_id) REFERENCES spot (id),
    CONSTRAINT fk_session_sector FOREIGN KEY (sector_code) REFERENCES sector (code),
    CONSTRAINT ck_session_status CHECK (status IN ('ENTERED', 'PARKED', 'CLOSED')),
    CONSTRAINT ck_session_amount CHECK (amount IS NULL OR amount >= 0)
) ENGINE = InnoDB;

CREATE INDEX ix_session_occupancy ON parking_session (status, sector_code);
CREATE INDEX ix_session_revenue ON parking_session (status, exit_time, sector_code);

CREATE TABLE capacity_counter (
    id       TINYINT NOT NULL,
    total    INT     NOT NULL,
    reserved INT     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_capacity_single_row CHECK (id = 1),
    CONSTRAINT ck_capacity_bounds CHECK (reserved >= 0)
) ENGINE = InnoDB;
