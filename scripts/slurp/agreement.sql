/* Version 1 */
/* Depends on the 'auids' table */
CREATE TABLE IF NOT EXISTS agreement (

    /* A unique row ID */
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,

    /* A reference to a row ID in the auids table */
    aid INT UNSIGNED NOT NULL,
    
    /* A peer ID */
    peer VARCHAR(31),
    
    /* The highest agreement value */
    highest_agreement FLOAT,
    
    /* The last agreement value */
    last_agreement FLOAT,
    
    /* The highest agreement hint */
    highest_agreement_hint FLOAT,
    
    /* The last agreement hint */
    last_agreement_hint FLOAT,
    
    /* Whether there is consensus with this peer */
    consensus BOOLEAN,

    /* Last consensus with this peer */
    last_consensus DATETIME,

    /* id is the primary key */
    PRIMARY KEY (id),
    
    /* aid is a foreign key */
    FOREIGN KEY (aid) REFERENCES auids(id) ON DELETE CASCADE ON UPDATE CASCADE

) ENGINE=INNODB;
