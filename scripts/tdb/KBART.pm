# ------------------------------------------------------------------------------
# KBART constants for Perl
# 
# 
# ------------------------------------------------------------------------------
package KBART;

use strict;
use warnings;
use Class::Struct;
use Exporter;
use vars qw($VERSION @ISA @EXPORT @EXPORT_OK %EXPORT_TAGS);

$VERSION     = 1.00;
@ISA         = qw(Exporter);
@EXPORT      = ();
@EXPORT_OK   = qw(
$PUBLICATION_TITLE
$PRINT_IDENTIFIER
$ONLINE_IDENTIFIER
$DATE_FIRST_ISSUE_ONLINE
$NUM_FIRST_VOL_ONLINE
$NUM_FIRST_ISSUE_ONLINE
$DATE_LAST_ISSUE_ONLINE
$NUM_LAST_VOL_ONLINE
$NUM_LAST_ISSUE_ONLINE
$TITLE_URL
$FIRST_AUTHOR
$TITLE_ID
$EMBARGO_INFO
$COVERAGE_DEPTH
$COVERAGE_NOTES
$PUBLISHER_NAME
);
#%EXPORT_TAGS = ( DEFAULT => [qw(&func1)],
#                 Both    => [qw(&func1 &func2)]);

struct( Field => {
    index => '$', # Default 0-based index
    label => '$', # Recommended label
    name  => '$', # A name
        });


# KBART fields in default order
our $PUBLICATION_TITLE       = Field->new(index => 0, 
                                          label => "publication_title",       
                                          name  => "Publication Title");
our $PRINT_IDENTIFIER        = Field->new(index => 1, 
                                          label => "print_identifier",        
                                          name  => "Publication Title");
our $ONLINE_IDENTIFIER       = Field->new(index => 2, 
                                          label => "online_identifier",       
                                          name  => "Online Identifier");
our $DATE_FIRST_ISSUE_ONLINE = Field->new(index => 3, 
                                          label => "date_first_issue_online", 
                                          name  => "Date First Issue Online");
our $NUM_FIRST_VOL_ONLINE    = Field->new(index => 4, 
                                          label => "num_first_vol_online",    
                                          name  => "Num First Vol Online");
our $NUM_FIRST_ISSUE_ONLINE  = Field->new(index => 5, 
                                          label => "num_first_issue_online",  
                                          name  => "Num First Issue Online");
our $DATE_LAST_ISSUE_ONLINE  = Field->new(index => 6, 
                                          label => "date_last_vol_online",    
                                          name  => "Date Last Vol Online");
our $NUM_LAST_VOL_ONLINE     = Field->new(index => 7, 
                                          label => "num_last_vol_online",     
                                          name  => "Num Lat Vol Online");
our $NUM_LAST_ISSUE_ONLINE   = Field->new(index => 8, 
                                          label => "num_last_issue_online",   
                                          name  => "Num Last Issue Online");
our $TITLE_URL               = Field->new(index => 9, 
                                          label => "title_url",               
                                          name  => "Title URL");
our $FIRST_AUTHOR            = Field->new(index => 10, 
                                          label => "first_author",           
                                          name  => "First Author");
our $TITLE_ID                = Field->new(index => 11, 
                                          label => "title_id",               
                                          name  => "Title ID");
our $EMBARGO_INFO            = Field->new(index => 12, 
                                          label => "embargo_info",           
                                          name  =>  "Embargo Info");
our $COVERAGE_DEPTH          = Field->new(index => 13, 
                                          label => "coverage_depth",         
                                          name  => "Coverage Depth");
our $COVERAGE_NOTES          = Field->new(index => 14, 
                                          label => "coverage_notes",         
                                          name  => "Coverage Notes");
our $PUBLISHER_NAME          = Field->new(index => 15, 
                                          label => "publisher_name",         
                                          name  => "Publisher Name");

1;
