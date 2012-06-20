# ------------------------------------------------------------------------------
# TDB constants for Perl
# 
# 
# ------------------------------------------------------------------------------
package TDB;

use strict;
use warnings;
use Class::Struct;
use Exporter;
use vars qw($VERSION @ISA @EXPORT @EXPORT_OK %EXPORT_TAGS);

$VERSION     = 1.00;
@ISA         = qw(Exporter);
@EXPORT      = ();
@EXPORT_OK   = qw(
$TITLE
$NAME
$ISSN
$EISSN
$ISSNL
$PUBL
$YEAR
$VOL
@YR_ALTS
@VOL_ALTS
);

# TDB field names
our $TITLE = "title";
our $NAME = "name";
our $ISSN = "issn";
our $EISSN = "eissn";
our $ISSNL = "issnl";
our $PUBL = "publisher";
# The year and volume could each be in one of several fields.
our $YEAR = "year";
our $VOL = "volume";
# Alternative field names
our @YR_ALTS  = ("param[year]");
our @VOL_ALTS = ("param[volume]", 
                 "param[volume_name]",
                 "param[volume_str]"
    );

1;
