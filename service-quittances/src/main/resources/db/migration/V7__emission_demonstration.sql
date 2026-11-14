CREATE TABLE emission_demo (
    quittance_id UUID PRIMARY KEY REFERENCES quittance(id),
    pdf BYTEA NOT NULL CHECK (octet_length(pdf) BETWEEN 1 AND 10485760),
    signature BYTEA NOT NULL CHECK (octet_length(signature) BETWEEN 256 AND 1024),
    certificat BYTEA NOT NULL CHECK (octet_length(certificat) BETWEEN 1 AND 32768),
    sha256 VARCHAR(64) NOT NULL CHECK (sha256 ~ '^[0-9a-f]{64}$'),
    cree_le TIMESTAMPTZ NOT NULL,
    mode VARCHAR(20) NOT NULL DEFAULT 'DEMONSTRATION' CHECK (mode = 'DEMONSTRATION')
);
COMMENT ON TABLE emission_demo IS 'PDF immuable et preuve locale de test. Ne constitue pas une signature qualifiée.';
