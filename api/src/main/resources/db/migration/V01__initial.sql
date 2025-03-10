CREATE TYPE oppløsning AS ENUM
(
    'Daglig', 'Månedlig', 'Årlig', 'Periodisert'
);

CREATE TABLE inntekt
(
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    løpenummer          BIGSERIAL,
    opprettet           TIMESTAMPTZ NOT NULL DEFAULT now(),
    personident         TEXT NOT NULL,
    kilde               TEXT NOT NULL,
    beløp_ører          INT,
    beløp_oppløsning    oppløsning NOT NULL,
    fom                 DATE NOT NULL,
    tom                 DATE,
    CONSTRAINT gyldig_periode CHECK (tom IS NULL OR tom >= fom),
    CONSTRAINT gyldig_oppløsning CHECK (tom IS NOT NULL OR beløp_oppløsning != 'Periodisert'::oppløsning)
);

CREATE INDEX inntekt_pid_idx ON inntekt(personident);