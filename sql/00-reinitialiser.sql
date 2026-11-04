-- Remet le jeu de donnees dans son etat de reference avant chaque campagne.
-- TRUNCATE conserve la structure (donc la colonne description) ; il ne reste
-- qu'a recharger les deux CSV puis a reconstruire les descriptions.
USE benchmark_rest;
SET FOREIGN_KEY_CHECKS=0;
TRUNCATE TABLE item;
TRUNCATE TABLE category;
SET FOREIGN_KEY_CHECKS=1;
LOAD DATA LOCAL INFILE 'C:/Users/dell/Desktop/tp204_benchmark_rest/donnees/category.csv' INTO TABLE category FIELDS TERMINATED BY ',' LINES TERMINATED BY '\n' (id, code, name, updated_at);
LOAD DATA LOCAL INFILE 'C:/Users/dell/Desktop/tp204_benchmark_rest/donnees/item.csv' INTO TABLE item FIELDS TERMINATED BY ',' LINES TERMINATED BY '\n' (id, sku, name, price, stock, category_id, updated_at);
UPDATE item SET description = CONCAT('Reference ', sku, ' - article du catalogue Maroc Distribution, controle qualite le ', DATE_FORMAT(updated_at, '%d/%m/%Y'), '.');
ANALYZE TABLE item;
ANALYZE TABLE category;
