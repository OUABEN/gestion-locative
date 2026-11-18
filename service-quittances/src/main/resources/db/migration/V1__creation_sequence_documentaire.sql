CREATE SEQUENCE numero_document_seq
    AS BIGINT
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    NO MAXVALUE
    CACHE 1
    NO CYCLE;

COMMENT ON SEQUENCE numero_document_seq IS
    'Compteur global pour la numerotation des documents du service Quittances.';