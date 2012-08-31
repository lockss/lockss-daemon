/* Version 1 */
CREATE TABLE IF NOT EXISTS sessions (

    /* A unique row ID */
    id INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    
    /* A required LOCKSS UI host name and port string */
    ui VARCHAR(127) NOT NULL,
    
    /* A required timestamp */
    begin DATETIME NOT NULL,
    
    /* A timestamp */
    end DATETIME,

    /* A required bitfield */
    flags INT UNSIGNED NOT NULL

) ENGINE=INNODB;
