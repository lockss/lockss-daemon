/* Version 2 */
/* Depends on the 'auids' table */
CREATE TABLE IF NOT EXISTS aus (

    /* A unique row ID */
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,

    /* A reference to a row ID in the auids table */
    aid INT UNSIGNED NOT NULL,
    
    /* The AU's name */
    name VARCHAR(511) CHARACTER SET utf8,
    
    /* The AU's publisher */
    publisher VARCHAR(255) CHARACTER SET utf8,
    
    /* The AU's year; "YYYY" or "YYYY-ZZZZ" */
    year VARCHAR(9),
    
    /* The AU's repository identifier */
    repository VARCHAR(255),
    
    /* The AU's creation date */
    creation_date DATETIME,
    
    /* The AU's status */
    status VARCHAR(127),
    
    /* The AU's available flag */
    available BOOLEAN,

    /* The AU's last crawl */
    last_crawl DATETIME,
    
    /* The AU's last crawl result */
    last_crawl_result VARCHAR(127),
    
    /* The AU's last completed crawl */
    last_completed_crawl DATETIME,
    
    /* The AU's last poll */
    last_poll DATETIME,
    
    /* The AU's last poll result */
    last_poll_result VARCHAR(127),
    
    /* The AU's last completed poll */
    last_completed_poll DATETIME,
    
    /* The AU's content size */
    content_size BIGINT UNSIGNED,
    
    /* The AU's disk usage in MB */
    disk_usage FLOAT UNSIGNED,

    /* The AU's title */
    title VARCHAR(511) CHARACTER SET utf8,
    
    /* id is the primary key */
    PRIMARY KEY (id),
    
    /* aid is a foreign key */
    FOREIGN KEY (aid) REFERENCES auids(id) ON DELETE CASCADE ON UPDATE CASCADE

) ENGINE=INNODB;
