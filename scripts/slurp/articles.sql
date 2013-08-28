/* Version 1 */
/* Depends on the 'auids' table */
CREATE TABLE IF NOT EXISTS articles (

    /* A unique row ID */
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,

    /* A reference to a row ID in the auids table */
    aid INT UNSIGNED NOT NULL,
    
    /* An article URL */
    article VARCHAR(511),
    
    /* id is the primary key */
    PRIMARY KEY (id),
    
    /* aid is a foreign key */
    FOREIGN KEY (aid) REFERENCES auids(id) ON DELETE CASCADE ON UPDATE CASCADE

) ENGINE=INNODB;
