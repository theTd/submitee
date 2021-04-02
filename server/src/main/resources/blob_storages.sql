CREATE TABLE blob_storages (
    `id` INT NOT NULL AUTO_INCREMENT,
    `type_id` VARCHAR(128) NOT NULL,
    `name` VARCHAR(128) NOT NULL,
    PRIMARY KEY (`id`),
    INDEX(`type_id`,`name`)
);
