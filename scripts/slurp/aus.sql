/*
   Parameters:
   (0) The name of the table (string).
   (1) The maximum length of the AU name (integer).
   (2) The maximum length of the publisher name (integer).
   (3) The maximum length of the repository identifier (integer).
   (4) The maximum length of the status string (integer).
   (5) The maximum length of the last crawl result (integer).
   (6) The maximum length of the last poll result (integer).
*/

/* Create the AUs table */
CREATE TABLE IF NOT EXISTS %s (

    /* A reference to a row ID in the sessions table */
    sid INT UNSIGNED NOT NULL,

    /* A reference to a row ID in the auids table */
    aid INT UNSIGNED NOT NULL,
    
    /* The AU's name */
    name VARCHAR(%d),
    
    /* The AU's publisher */
    publisher VARCHAR(%d),
    
    /* The AU's year; "YYYY" or "YYYY-ZZZZ" */
    year VARCHAR(9),
    
    /* The AU's repository identifier */
    repository VARCHAR(%d),
    
    /* The AU's creation date */
    creation_date DATETIME,
    
    /* The AU's status */
    status VARCHAR(%d),
    
    /* The AU's available flag */
    available BOOLEAN,

    /* The AU's last crawl */
    last_crawl DATETIME,
    
    /* The AU's last crawl result */
    last_crawl_result VARCHAR(%d),
    
    /* The AU's last completed crawl */
    last_completed_crawl DATETIME,
    
    /* The AU's last poll */
    last_poll DATETIME,
    
    /* The AU's last poll result */
    last_poll_result VARCHAR(%d),
    
    /* The AU's last completed poll */
    last_completed_poll DATETIME,
    
    /* The AU's content size */
    content_size BIGINT UNSIGNED,
    
    /* The AU's disk usage in MB */
    disk_usage FLOAT UNSIGNED

);
