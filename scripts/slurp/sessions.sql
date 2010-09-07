/*
   Parameters:
   (0) The name of the table (string).
   (1) The maximum length of the UI host name and port string (integer).
*/

/* Create the sessions table */
CREATE TABLE IF NOT EXISTS %s (

    /* A unique row ID */
    id INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    
    /* A required LOCKSS UI host name and port */
    ui VARCHAR(%d) NOT NULL,
    
    /* A required timestamp, default NOW() */
    begin DATETIME NOT NULL,
    
    /* A timestamp */
    end DATETIME

);
