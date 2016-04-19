create database download;

\c download

create table request_info(
	id serial primary key,
	request_id text unique,
	request_time timestamp,
	download jsonb,
	job_id text,
	user_name text,
	user_emailaddress text,
	user_format text
);

create table result_info(
	request_info_id integer references request_info(id) primary key,
	response_time timestamp,
	response_code text
);
