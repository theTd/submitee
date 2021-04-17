CREATE TABLE events
(
    `id` INT NOT NULL AUTO_INCREMENT,
    `level` ENUM('INFO','ERROR','WARN', 'OTHER') NOT NULL DEFAULT 'OTHER',
    `entity` VARCHAR(128) NULL,
    `activity` VARCHAR(128) NULL,
    `detail` TEXT NOT NULL,
    `crc32` BIGINT NOT NULL,
    PRIMARY KEY (`id`),
    INDEX (`entity`),
    INDEX (`entity`, `activity`),
    INDEX (`crc32`)
);

CREATE TABLE event_occurs
(
    `id` INT NOT NULL AUTO_INCREMENT,
    `eid` INT NOT NULL,
    `time` TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY(`id`),
    FOREIGN KEY(eid) REFERENCES events(id) ON DELETE CASCADE
);
