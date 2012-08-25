/* Version 1 */
/* Depends on the sessions table */
CREATE TABLE IF NOT EXISTS auids (

    /* A unique row ID */
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,

    /* A reference to a row ID in the sessions table */
    sid INT UNSIGNED NOT NULL,

    /* An AUID */    
    auid VARCHAR(511),
    
    /* id is the primary key */
    PRIMARY KEY (id),
    
    /* sid is a foreign key */
    FOREIGN KEY (sid) REFERENCES sessions(id) ON DELETE CASCADE ON UPDATE CASCADE

) ENGINE=INNODB;
