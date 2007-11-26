DROP TABLE IF EXISTS simple_values;
CREATE TABLE simple_values (
  int_val int,
  str_val varchar(255)
);

INSERT INTO simple_values (int_val, str_val) values (null, null);
INSERT INTO simple_values (int_val, str_val) values (0, 'Zero');
INSERT INTO simple_values (int_val, str_val) values (1, 'One');
INSERT INTO simple_values (int_val, str_val) values (2, 'Two');
INSERT INTO simple_values (int_val, str_val) values (3, 'Three');
INSERT INTO simple_values (int_val, str_val) values (4, 'Four');

DROP TABLE IF EXISTS updates;
CREATE TABLE updates (id int);

DROP TABLE IF EXISTS locks;
CREATE TABLE locks (name varchar(255) primary key not null);
INSERT INTO locks(name) VALUES ('lock');

DROP TABLE IF EXISTS adbcj_types;
CREATE TABLE adbcj_types (
  type_bigint bigint,
  type_boolean boolean,
  type_char char(10),
  type_date date,
  type_double double precision,
  type_integer integer,
  type_real real,
  type_smallint smallint
);