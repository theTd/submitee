CREATE TABLE internal_users (
  `uid` INT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(128) NOT NULL,
  `password` CHAR() NOT NULL,
  PRIMARY KEY (`uid`)
)