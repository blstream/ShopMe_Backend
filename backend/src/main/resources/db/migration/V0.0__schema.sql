create table VOIVODESHIP
(
	ID UUID not null primary key,
	NAME VARCHAR(255) not null
)
;

create unique index IDX_VOIVODESHIP_NAME
	on VOIVODESHIP (NAME)
;

create table ADDRESS
(
	ID UUID not null primary key,
	CITY VARCHAR(255),
	NUMBER VARCHAR(255),
	STREET VARCHAR(255),
	ZIP_CODE VARCHAR(255)
)
;

create table CATEGORY
(
	ID UUID not null primary key,
	NAME VARCHAR(255),
)
;

create unique index IDX_CATEGORY_NAME
on CATEGORY (NAME)
;

create table INVOICE
(
	ID UUID not null primary key,
	COMPANY_NAME VARCHAR(255),
	NIP VARCHAR(255),
	INVOICE_ADDRESS_ID UUID
		constraint FK_INVOICE_ADDRESS
			references ADDRESS
)
;

create table OWNER
(
	ID UUID not null primary key,
	ADDITIONAL_INFO VARCHAR(800),
	EMAIL VARCHAR(255),
	NAME VARCHAR(20) not null,
	PHONE_NUMBER VARCHAR(10)
)
;

create table OFFER
(
	ID UUID not null primary key,
	BASE_DESCRIPTION VARCHAR(500) not null,
	BASE_PRICE NUMERIC not null,
	EXTENDED_DESCRIPTION VARCHAR(500),
	EXTENDED_PRICE NUMERIC,
	EXTRA_DESCRIPTION VARCHAR(500),
	EXTRA_PRICE NUMERIC,
	TITLE VARCHAR(30) not null,
	DATE TIMESTAMP not null,
	OWNER_ID UUID not null
		constraint FK_OFFER_OWNER
			references OWNER
)
;

create table OFFER_CATEGORY
(
	CATEGORY_ID UUID not null
		constraint FK_OFFER_CATEGORY_CATEGORY
			references CATEGORY,
	OFFER_ID UUID not null
		constraint FK_OFFER_CATEGORY_OFFER
			references OFFER
)
;

create table USERS
(
	ID UUID not null primary key,
	BANK_ACCOUNT VARCHAR(255),
	EMAIL VARCHAR(255),
	INVOICE_REQUEST BOOLEAN,
	NAME VARCHAR(255),
	PASSWORD VARCHAR(255),
	PHONE_NUMBER VARCHAR(255),
	SURNAME VARCHAR(255),
	ADDRESS_ID UUID
		constraint FK_USERS_ADDRESS
			references ADDRESS,
	INVOICE_ID UUID
		constraint FK_USERS_INVOICE
			references INVOICE
)
;
