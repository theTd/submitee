CREATE TABLE templates (
    `id` INT NOT NULL AUTO_INCREMENT,
    `uuid` CHAR(128) NOT NULL,
    `grouping` VARCHAR(16) NOT NULL,
    `template_id` VARCHAR(128) NULL,
    `version` INT NOT NULL DEFAULT 0,
    INDEX(`template_id`),
    INDEX(`grouping`,`version`),
    INDEX(`uuid`),
    PRIMARY KEY(`id`)
) CHARSET utf8 COLLATE utf8_unicode_ci;

DELIMITER $$
CREATE TRIGGER object_map_update
    AFTER INSERT
    ON templates
    FOR EACH ROW
BEGIN
    INSERT INTO object_map(uuid, type) VALUES (NEW.uuid, 'template');
END$$
DELIMITER ;
