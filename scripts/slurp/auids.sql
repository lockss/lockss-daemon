/*
   Parameters:
   (0) The name of the table (string).
   (1) The maximum length of AUIDs (integer).
   (2) The name of the sessions table (string).
   References the sessions table.
*/

/* Create the AUIDs table */
CREATE TABLE IF NOT EXISTS %s (

    /* A unique row ID */
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,

    /* A reference to a row ID in the sessions table */
    sid INT UNSIGNED NOT NULL,

    /* An AUID */    
    auid VARCHAR(%d),
    
    /* id is the primary key */
    PRIMARY KEY (id),
    
    /* sid is a foreign key */
    FOREIGN KEY (sid) REFERENCES %s(id) ON DELETE CASCADE ON UPDATE CASCADE

) ENGINE=INNODB;
