/* Version 1 */
/* Depends on the sessions table */
CREATE TABLE IF NOT EXISTS commdata (

    /* A unique row ID */
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,

    /* A reference to a row ID in the sessions table */
    sid INT UNSIGNED NOT NULL,

    /* A peer ID */
    peer VARCHAR(31),

    /* The origin count */
    origin INT UNSIGNED,

    /* The fail count */
    fail INT UNSIGNED,

    /* The accept count */
    accept INT UNSIGNED,

    /* The number of messages sent */
    sent INT UNSIGNED,
    
    /* The number of messages received */
    received INT UNSIGNED,
    
    /* The channel column */
    channel VARCHAR(15),

    /* The send queue column */
    send_queue INT UNSIGNED,

    /* The date/time of the last attempt */
    last_attempt DATETIME,

    /* The date/time of the next retry */
    next_retry DATETIME,

    /* id is the primary key */
    PRIMARY KEY (id),
    
    /* sid is a foreign key */
    FOREIGN KEY (sid) REFERENCES sessions(id) ON DELETE CASCADE ON UPDATE CASCADE

) ENGINE=INNODB;
