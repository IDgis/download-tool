create database download;

\c download

create table result_info(
	id serial,
	request_id text,
	response_time timestamp,
	response_code text
);

create table request_info(
	id serial,
	request_id text,
	request_time timestamp,
	download text,
	job_id text,
	user_name text,
	user_emailaddress text,
	user_format text
);
