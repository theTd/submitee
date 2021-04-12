CREATE TABLE internal_users (
  `uid` INT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(128) NULL,
  `email` VARCHAR(128) NOT NULL,
  `password` CHAR(128) NOT NULL,
  PRIMARY KEY (uid),
  UNIQUE KEY (`username`),
  INDEX(`email`)
) CHARSET UTF8 COLLATE utf8_unicode_ci;
