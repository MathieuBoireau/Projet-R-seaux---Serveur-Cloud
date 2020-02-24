DROP TABLE login_projet CASCADE;

CREATE TABLE login_projet(
    login varchar NOT NULL PRIMARY KEY,
    passwd varchar NOT NULL,
    CONSTRAINT chk_login CHECK (login NOT LIKE '%/%')
);

CREATE OR REPLACE FUNCTION f_trig_hash() RETURNS trigger as $$
    DECLARE pass varchar;
    BEGIN
        select NEW.passwd INTO pass;
        select MD5('tuvasdanshelp' || pass) into NEW.passwd;
        RETURN NEW;
    END
$$ language plpgsql;

DROP TRIGGER IF EXISTS trig_hash on login_projet;

create trigger trig_hash
BEFORE UPDATE or INSERT on login_projet
for each ROW
execute procedure f_trig_hash();

CREATE OR REPLACE FUNCTION verifPass(log varchar, pass varchar) RETURNS boolean as $$
    DECLARE password varchar;
    BEGIN
        select passwd into password from login_projet where login LIKE log;
        IF NOT FOUND THEN
            RETURN FALSE;
        END IF;
        RETURN(MD5('tuvasdanshelp' || pass) LIKE password);
    END
$$ language plpgsql;

DROP TABLE share_projet CASCADE;

CREATE TABLE share_projet(
    login varchar NOT NULL REFERENCES login_projet(login),
    path varchar NOT NULL,
    PRIMARY KEY(login, path),
    type varchar NOT NULL,
    CONSTRAINT chk_type CHECK (type in('owner', 'shared'))
);

CREATE OR REPLACE FUNCTION f_trig_share() RETURNS trigger as $$
    BEGIN
        RAISE NOTICE 'Execution';
        IF (OLD.type LIKE 'owner') THEN
            RAISE NOTICE 'C est owner';
            DELETE FROM share_projet where path LIKE old.path and type LIKE 'shared';
        END IF;
        RETURN OLD;
    END
$$ language plpgsql;

DROP TRIGGER IF EXISTS t_share on share_projet;

CREATE trigger t_share
BEFORE DELETE on share_projet
for each row
execute procedure f_trig_share();

CREATE OR REPLACE FUNCTION verifShare(log varchar, file varchar) RETURNS boolean as $$
    BEGIN
        PERFORM path from share_projet where login LIKE log and path LIKE file;
        IF NOT FOUND THEN
            RETURN FALSE;
        END IF;
        RETURN TRUE;
    END
$$ language plpgsql;
