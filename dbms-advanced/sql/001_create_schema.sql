create schema olist;
set search_path to olist, public;

-- customers 테이블
create table olist.customers (
	customer_id VARCHAR(50) primary key,
	customer_unique_id VARCHAR(50) not null,
	customer_zip_code_prefix VARCHAR(10),
	customer_city VARCHAR(100),
	customer_state VARCHAR(2)
);

-- $ psql -d olist postgres
-- $ \copy olist.customers FROM 'D:/데이터셋/Brazilian E-Commerce Public Dataset/olist_customers_dataset.csv' WITH (FORMAT csv, HEADER true, ENCODING 'UTF8')


-- orders 테이블
create table olist.orders (
	order_id varchar(50) primary key,
	customer_id varchar(50) not null,
	order_status varchar(20) not null,
	order_purchase_timestamp timestamp not null,
	order_approved_at timestamp,
	order_delivered_carrier_date timestamp,
	order_delivered_customer_date timestamp,
	order_estimated_delivery_date timestamp
);

alter table olist.orders
	add constraint fk_orders_customer
	foreign key (customer_id) references customers(customer_id)
	on delete cascade;

-- $ psql -d olist postgres
-- $ \copy olist.orders FROM 'D:/데이터셋/Brazilian E-Commerce Public Dataset/olist_orders_dataset.csv' WITH (FORMAT csv, HEADER true, ENCODING 'UTF8')


-- product_category 테이블
create table olist.product_category (
	product_category_name varchar(50) primary key,
	product_category_name_english varchar(50)
);

-- $ psql -d olist postgres
-- $ \copy olist.product_category FROM 'D:/데이터셋/Brazilian E-Commerce Public Dataset/product_category_name_translation.csv' WITH (FORMAT csv, HEADER true, ENCODING 'UTF8')


-- product 테이블
create table if not exists olist.products (
	product_id varchar(50) primary key,
	product_category_name varchar(50),
	product_name_length integer,
	product_description_length integer,
	product_photos_qty integer,
	product_weight_g integer,
	product_length_cm integer,
	product_height_cm integer,
	product_width_cm integer,
	constraint fk_products_category
		foreign key (product_category_name) references product_category(product_category_name)
);

alter table olist.products
	drop constraint fk_products_category;

-- $ psql -d olist postgres
-- $ \copy olist.products FROM 'D:/데이터셋/Brazilian E-Commerce Public Dataset/olist_products_dataset.csv' WITH (FORMAT csv, HEADER true, ENCODING 'UTF8')


-- order_items 테이블
create table if not exists olist.order_items (
	order_id varchar(50),
	order_item_id integer,
	product_id varchar(50) not null,
	seller_id varchar(50),
	shipping_limit_date timestamp,
	price numeric(12, 4),
	freight_value numeric(12, 4),
	constraint pk_order_items primary key (order_id, order_item_id),
	constraint fk_order_items_item
		foreign key (product_id) references olist.products(product_id)
		on delete cascade,
	constraint fk_order_items_order
		foreign key (order_id) references olist.orders(order_id)
		on delete cascade
);

-- $ psql -d olist postgres
-- $ \copy olist.order_items FROM 'D:/데이터셋/Brazilian E-Commerce Public Dataset/olist_order_items_dataset.csv' WITH (FORMAT csv, HEADER true, ENCODING 'UTF8')


-- order_payments 테이블
create table if not exists olist.order_payments (
	order_id varchar(50),
	payment_sequential integer not null,
	payment_type varchar(20) not null,
	payment_installments integer not null,
	payment_value numeric(12, 4),
	constraint pk_order_payments primary key (order_id, payment_sequential),
	constraint fk_order_payments_order
		foreign key (order_id) references olist.orders (order_id)
		on delete cascade
);

-- $ psql -d olist postgres
-- $ \copy olist.order_payments FROM 'D:/데이터셋/Brazilian E-Commerce Public Dataset/olist_order_payments_dataset.csv' WITH (FORMAT csv, HEADER true, ENCODING 'UTF8')

