CREATE TABLE internal_users (
  `uid` INT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(128) NOT NULL,
  `password` CHAR(128) NOT NULL,
  PRIMARY KEY (uid)
) CHARSET UTF8 COLLATE utf8_unicode_ci;
