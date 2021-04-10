CREATE TABLE blobs (
    `blob_id` INT NOT NULL AUTO_INCREMENT,
    `storage_id` INT NOT NULL,
    `blob_key` VARCHAR(128) NOT NULL,
    `file_name` VARCHAR(128) NULL,
    `create_time` TIMESTAMP NOT NULL DEFAULT now(),
    `content_type` VARCHAR(128) NULL,
    `uploader` VARCHAR(128) NOT NULL,
    PRIMARY KEY(`blob_id`),
    FOREIGN KEY(`storage_id`) REFERENCES `blob_storages`(`id`),
    UNIQUE KEY(`blob_key`)
) CHARSET UTF8 COLLATE utf8_unicode_ci;
