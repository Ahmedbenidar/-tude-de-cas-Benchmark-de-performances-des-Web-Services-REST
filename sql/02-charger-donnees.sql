USE benchmark_rest;
SET FOREIGN_KEY_CHECKS=0;
LOAD DATA LOCAL INFILE 'C:/Users/dell/Desktop/tp204_benchmark_rest/donnees/category.csv' INTO TABLE category FIELDS TERMINATED BY ',' LINES TERMINATED BY '\n' (id, code, name, updated_at);
LOAD DATA LOCAL INFILE 'C:/Users/dell/Desktop/tp204_benchmark_rest/donnees/item.csv' INTO TABLE item FIELDS TERMINATED BY ',' LINES TERMINATED BY '\n' (id, sku, name, price, stock, category_id, updated_at);
SET FOREIGN_KEY_CHECKS=1;