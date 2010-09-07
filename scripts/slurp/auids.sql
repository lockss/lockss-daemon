/*
   Parameters:
   (0) The name of the table (string).
   (1) The maximum length of AUIDs (integer).
*/

/* Create the AUIDs table */
CREATE TABLE IF NOT EXISTS %s (

    /* A unique row ID */
    id INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,

    /* A reference to a row ID in the sessions table */
    sid INT UNSIGNED NOT NULL,

    /* An AUID */    
    auid VARCHAR(%d)

);
