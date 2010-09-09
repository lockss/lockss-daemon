/*
   Parameters:
   (0) The name of the table (string).
   (1) The maximum length of article identifiers (integer).
   (2) The name of the AUIDs table.
   References the AUIDs table.
*/

/* Create the articles table */
CREATE TABLE IF NOT EXISTS %s (

    /* A reference to a row ID in the AUIDs table */
    aid INT UNSIGNED NOT NULL,

    /* An article identifier */
    article VARCHAR(%d),
    
    /* aid is a foreign key */
    FOREIGN KEY (aid) REFERENCES %s(id) ON DELETE CASCADE ON UPDATE CASCADE
    
) ENGINE=INNODB;
