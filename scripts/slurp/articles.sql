/*
   Parameters:
   (0) The name of the table (string).
   (1) The maximum length of article identifiers (integer).
*/

/* Create the articles table */
CREATE TABLE IF NOT EXISTS %s (

    /* A reference to a row ID in the sessions table */
    sid INT UNSIGNED NOT NULL,

    /* A reference to a row ID in the AUIDs table */
    aid INT UNSIGNED NOT NULL,

    /* An article identifier */
    article VARCHAR(%d)
    
);
