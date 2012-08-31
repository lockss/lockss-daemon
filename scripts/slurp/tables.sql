/* Version 1 */
CREATE TABLE IF NOT EXISTS tables (

    /* A unique row ID */
    id INTEGER UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    
    /* A required table name */
    name VARCHAR(63) NOT NULL,

    /* A required version */
    version INTEGER UNSIGNED NOT NULL
    
) ENGINE=INNODB;
