CREATE TABLE inntekt
(
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    løpenummer          BIGSERIAL,
    opprettet           TIMESTAMPTZ NOT NULL DEFAULT now(),
    personident TEXT NOT NULL,
    kilde               TEXT NOT NULL,
    daglig_beløp_ører   INT NOT NULL,
    fom                 DATE NOT NULL,
    tom                 DATE
);

CREATE INDEX inntekt_pid_idx ON inntekt(personident);