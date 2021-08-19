#!/usr/bin/perl
# $Id$
# ------------------------------------------------------------------------------
# Report Available Titles
# ------------------------------------------------------------------------------
# Produce a list of titles that are available in LOCKSS based on the best
# information available. This version takes a reference file with the 
# definitive list of committed titles, and produces a record for each
# by cross-referencing with the TDB files. The output is sorted by publisher
# then publication title.
# 
# To do this we need to look at the full list of titles in the TDB (which 
# should represent titles committed through publisher agreements) and 
# cross reference with KBART reports to get the coverage ranges.
# 
# The final list should consist of titles each of which is in one of the 
# following states:
# 
#  - IN PROCESS Unreleased AUs in TDB testing, with coverage info
#  - PRESERVED  Released AUs, including status 'down', with coverage info
# 
# This should cover everything which has been committed for preservation.
# The coverage info, where available, will come from the KBART reports and 
# so will inherit any deficiencies in that approach.
# 
# The following input files are required, and will be generated automatically
# unless specified:
# 
# (1) CSV of all Committed Titles (with fields publisher, title, issn, eissn).
#     This must be comma-separated plain text and contain no Microsoft-only
#     quotes or dashes.
# (2) KBART Production report, showing metadata for AUs released into production
#     (including those that have subsequently been marked as down).
# (3) KBART Unreleased report, showing metadata for AUs not released into production
#     (including those marked as "exists", which are not included in the unreleased 
#     report by default).
# 
# The union of the KBART reports should be the full set of TDB records, modulo
# whatever AU types have been omitted.
# 
# ------------------------------------------------------------------------------
# Generating the input files
# ------------------------------------------------------------------------------
# The committed titles report is generated using tdbout with the -j option. 
# The KBART reports are generated internally by running tdbout on all TDB 
# files (which is time-consuming) and then feeding that output into 
# org.lockss.devtools.RunKbartReport. The tdbout files can be reused if
# available; otherwise they must be generated explicitly by specifying 
# the option --generate-tdb-metadata.
# 
# The queries specified for tbout match all the statuses required for the
# given output state. They also specify that the type of each AU should be 
# "journal" - omitting blogs and books.
# ------------------------------------------------------------------------------
use strict;
use warnings;
use KBART;
use TDB;
use Getopt::Std;
use Getopt::Long;
use Text::CSV;
use Class::Struct;

# Check CSV can be used
# Set to allow binary input, diagnose errors automatically, and allow whitespace around quotes
my $csv = Text::CSV->new ( { binary => 1, auto_diag => 1, allow_whitespace => 1 } ) 
    or die "Cannot use CSV: ".Text::CSV->error_diag ();

# ------------------------------------------------------------------------------
# Title record and field structs
# ------------------------------------------------------------------------------
# A struct to contain info for a title record with a particular status
struct( TitleRecord => {
    publisher => '$',
    title     => '$',
    issn      => '$',
    eissn     => '$',
    vols      => '@', # array of vol ranges
    yrs       => '@',  # array of year ranges
    issues    => '@'  # array of issue ranges
        });

struct( TitleField => {
    name     => '$',
    index => '$', # Index used in the main "committed" file
    kbindex  => '$', # Index used in KBART file
        });

# Fields to read from input, and where to find them
my $FLD_PUBL  = TitleField->new(name => "Publisher",    index => 0,  kbindex => $KBART::PUBLISHER_NAME->index);
my $FLD_TITLE = TitleField->new(name => "Title",        index => 1,  kbindex => $KBART::PUBLICATION_TITLE->index);
my $FLD_ISSN  = TitleField->new(name => "ISSN",         index => 2,  kbindex => $KBART::PRINT_IDENTIFIER->index);
my $FLD_EISSN = TitleField->new(name => "eISSN",        index => 3,  kbindex => $KBART::ONLINE_IDENTIFIER->index);
# These fields have no presence in the committed title file, so no index
my $FLD_VOL_S = TitleField->new(name => "Volume start", index => -1, kbindex => $KBART::NUM_FIRST_VOL_ONLINE->index);
my $FLD_VOL_E = TitleField->new(name => "Volume end",   index => -1, kbindex => $KBART::NUM_LAST_VOL_ONLINE->index);
my $FLD_YR_S  = TitleField->new(name => "Year start",   index => -1, kbindex => $KBART::DATE_FIRST_ISSUE_ONLINE->index);
my $FLD_YR_E  = TitleField->new(name => "Year end",     index => -1, kbindex => $KBART::DATE_LAST_ISSUE_ONLINE->index);
my $FLD_ISS_S = TitleField->new(name => "Issue start",  index => -1, kbindex => $KBART::NUM_FIRST_ISSUE_ONLINE->index);
my $FLD_ISS_E = TitleField->new(name => "Issue end",    index => -1, kbindex => $KBART::NUM_LAST_ISSUE_ONLINE->index);


# ------------------------------------------------------------------------------
# Queries for tdbout
# ------------------------------------------------------------------------------
# Types of AU to output # TODO Read in from command line args
my @types = qw/journal/;

# Production TDB (released, down)
my $productionQuery = '--query \''.
    &combineTypes()." and (".
    'status is "released"'.
    'or status is "down"'.
    #'or status is "doesNotExist"'.
    ')\'';

# Unreleased TDB (crawling, manifest, testing, notReady, ready, exists)
my $unreleasedQuery = '--query \''.
    &combineTypes()." and (".
    'status is "crawling"'.
    'or status is "manifest"'.
    'or status is "testing"'.
    'or status is "notReady"'.
    'or status is "ready"'.
    'or status is "exists"'.
    ')\'';

# Combine the elements of the types array into an OR-ed query subexpression
sub combineTypes {
    # Return journal by default if there are no types
    return 'type is "journal"' if ($#types < 0);
    # Return simple query phrase if one type
    return "type is \"$types[0]\"" if ($#types == 0);
    # Otherwise combine the types in a bracketed subexpression
    my $str = "type is \"$types[0]\"";
    for my $t (@types[1..$#types]) {
        $str .= " or type is \"$t\"";
    }
    return "($str)";
}

# ------------------------------------------------------------------------------
# Output Strings
# ------------------------------------------------------------------------------
# Separator for multiple coverage ranges
my $rngSep = '; ';
# Separator for coverage range start and end strings
my $rngLink = 'to';
# Separator for packed id strings based on report fields
my $SEP = '##';
# String representing "now" as an endpoint of a volume range
my $now = 'present';

# Statuses for output
my $IN_PROGRESS = "In Progress";
my $PRESERVED = "Preserved";

# Column headings for output
my $PUBL   = "Publisher";
my $TITLE  = "Title";
my $ISSN   = "ISSN";
my $EISSN  = "eISSN";
my $STAT   = "Status";
my $VOLS   = "Volumes";
my $YEARS  = "Years";
my $ISSUES = "Issues";

# ------------------------------------------------------------------------------
# TDB Field definitions
# ------------------------------------------------------------------------------
# Map tdb field names to field names recognised by runKbartReport (KBART and other)
my %tdbFieldMapping = (
    $TDB::TITLE => $KBART::PUBLICATION_TITLE->label,
#    $TDB::NAME => $KBART::PUBLICATION_TITLE->label,
    $TDB::ISSN => $KBART::PRINT_IDENTIFIER->label,
    $TDB::EISSN => $KBART::ONLINE_IDENTIFIER->label,
    $TDB::PUBL => $KBART::PUBLISHER_NAME->label,
    # Map coverage-related strings to non-KBART field names recognised by runKbartReport
    $TDB::VOL => "volume",
    $TDB::YEAR => "year",
);

# Fields we want to output from the TDB files
my @tdbFields = ($TDB::TITLE, $TDB::NAME, $TDB::ISSN, $TDB::EISSN, $TDB::PUBL, $TDB::VOL, $TDB::YEAR, 
                 @TDB::VOL_ALTS, @TDB::YR_ALTS);
# The year and volume could each be in one of several fields so we need to 
# get them all including alts; processing is performed in runKbartReport to 
# decide which value should be used.


# ------------------------------------------------------------------------------
# Settings
# ------------------------------------------------------------------------------
# LOCKSS daemon home directory
my $daemonHome = "~/workspace/lockss-daemon";

# Name of TDB report with production AUs
my $tdbCsvProduction = "tdbout_production.csv";
# Name of TDB report with all unreleased AUs
my $tdbCsvUnreleased = "tdbout_unreleased.csv";
# Name of TDB report with all active AUs
my $tdbCsvActive = "tdbout_active.csv"

# Name of KBART report with production titles created in kreports.sh to be used
# here.
my $kbartProduction = "kbart_production_for_keepers.csv";
# Name of KBART report with all unreleased titles created in kreports.sh to be
# used here.
my $kbartUnreleased = "kbart_unreleased_for_keepers.csv";
# Name of KBART report with all active titles created in kreports.sh to be
# used here.
my $kbartUnreleased = "kbart_active_for_keepers.csv"

# Name of the report listing all committed titles; consists of "Publisher","Title","ISSN","eISSN"
my $committedTitles = "committed_titles.csv";

# KBART output is ordered KBART field by default
my $kborder = "KBART"; #$kborder = "PUBLISHER_PUBLICATION"

# Generate fresh CSV metadata (time-consuming)
my $generateCsvMetadata = 0; # Don't generate by default
# Show the warning messages from the KBART converter
my $showKbartWarnings = 0; # Hide them by default
# Generate data for clockss files
my $clockss = 0; # Do not generate CLOCKSS by default
# Generate data for gln files
my $gln = 0; # Generate GLN by default
my $networkname = ""; 
# Show help
my $help = 0;

# ------------------------------------------------------------------------------
# Option processing and setup
# ------------------------------------------------------------------------------

# Long option processing
my $ret = GetOptions ("daemon-home=s"     => \$daemonHome, # LOCKSS Daemon home dir
#            "production=s"      => \$kbartProduction, # KBART Production report filename
#            "unreleased=s"      => \$kbartUnreleased, # KBART Unreleased report filename
            "committed=s"       => \$committedTitles, # Committed titles report
            "generate-tdb-metadata" => \$generateCsvMetadata, # Generate fresh CSV metadata from TDB
            "show-kbart-warnings" => \$showKbartWarnings, # Show the warning messages from the KBART converter
            "clockss" => \$clockss,
            "gln" => \$gln,
            "network=s" => \$networkname, # name of network
            "help|?"            => \$help
    );

if ($networkname eq "") {
  if ($clockss == 1) {
    $networkname = "clockssingest";
  } elsif ($gln == 1) {
    $networkname = "prod"
  }
} elsif ($gln == 1 || $clockss == 1) {
  usage();
} elsif ($networkname eq "clockss") {
  # force to 'clockssingest' for compatibility with --clockss switch
  $networkname = "clockssingest"
} elsif ($networkname eq "gln") {
  # force to "gln for compatibibility with --gln switch
  $networkname = "prod"
}

if ($networkname ne "") {
    $networkname = "prod"
}
    
# Show help if needed
#usage() if $help;
if (($ret != 1) || $help) {
      usage();
}

# Now set up the paths based on lockss-daemon-home
# Location of the tdbout script
my $tdbout = "$daemonHome/scripts/tdb/tdbout";
# List of all the TDB files
my $tdbs_clockss = "$daemonHome/tdb/clockssingest/*.tdb";
my $tdbs_gln = "$daemonHome/tdb/prod*/*.tdb"; #including UK
my $tdbs = "$daemonHome/tdb/" . $networkname . "/*.tdb";
if ($networkname eq "clockssingest") {
    print STDERR "****CLOCKSS Report****\n";
} elsif ($networkname eq "prod") {
    print STDERR "****GLN Report****\n";
} else {
    print STDERR "****";
    print STDERR uc $networkname;
    print STDERR " Report****\n";
}


# ------------------------------------------------------------------------------
# Do the conversions
# ------------------------------------------------------------------------------

# 1. Generate CSV files of TDB content
# If the files exist and a new generate was not requested, no generation needed
if (-e $committedTitles and -e $tdbCsvProduction and -e $tdbCsvUnreleased and !$generateCsvMetadata) {
    print STDERR "Reusing generated CSV metadata.\n";
} else {
    # Print a message about missing files unless the generate was requested
    print STDERR "Previous CSV files not found.\n" unless ($generateCsvMetadata);

    # Generate new metadata from TDB
    &generateCsvMetadata();

    # Delete KBART files created from old TDB metadata 
    unlink ($kbartUnreleased);
    unlink ($kbartProduction);
}

# 2. Convert TDB CSV files to KBART with ranges
# if the files exist, no conversion needed
if (-e $kbartUnreleased and -e $kbartProduction) {
  print STDERR "Reusing converted KBART files.\n"
} else {
  &convertCsvIntoKbart();
}

# 3. Read in the CSVs of identifying fields for (un)released titles
my (%committedTuples, %kbartProductionTuples, %kbartUnreleasedTuples);
readCsvIntoHash($committedTitles, \%committedTuples, \&defaultPack);
readCsvIntoHash($kbartProduction, \%kbartProductionTuples, \&kbartPack);
readCsvIntoHash($kbartUnreleased, \%kbartUnreleasedTuples, \&kbartPack);

# 4. Print a header row
print STDERR "Printing report\n";
#print join(',', $PUBL, $TITLE, $ISSN, $EISSN, $STAT, $VOLS, $YEARS, $ISSUES)."\n";
print join(',', $PUBL, $TITLE, $ISSN, $EISSN, 
           $PRESERVED." ".$VOLS, $PRESERVED." ".$YEARS, #$PRESERVED." ".$ISSUES,
           $IN_PROGRESS." ".$VOLS, $IN_PROGRESS." ".$YEARS, #$IN_PROGRESS." ".$ISSUES
    )."\n";

# 5. Match identifying tuples to the KBART coverage records - sort the keys first 
for my $id (sort keys %committedTuples) {
    # Find title in kbart reports; print coverage with status Preserved/In Progress
    my $pres  = $kbartProductionTuples{$id};
    my $unrel = $kbartUnreleasedTuples{$id};
    print &combineStatusRecords($committedTuples{$id}, $pres, $unrel)."\n";
}

# The end
exit;


# ------------------------------------------------------------------------------
# Methods
# ------------------------------------------------------------------------------

# 
# Combine the fields of the supplied TitleRecords into a CSV string
# 
sub combineStatusRecords {
    # Record metadata, preserved list, unreleased list
    my ($rec, $pres, $unrel) = @_;
    my @tuple = (
        $rec->publisher,
        $rec->title,
        $rec->issn,
        $rec->eissn,
        
        # Preserved ranges
        # volume ranges
        defined $pres ? join ($rngSep, @{$pres->vols}) : "",
        # year ranges
        defined $pres ? join ($rngSep, @{$pres->yrs}) : "",
        # issue ranges
        #defined $pres ? join ($rngSep, @{$pres->issues}) : "",
        
        # In progress ranges
        # volume ranges
        defined $unrel ? join ($rngSep, @{$unrel->vols}) : "",
        # year ranges
        defined $unrel ? join ($rngSep, @{$unrel->yrs}) : "",
        # issue ranges
        #defined $unrel ? join ($rngSep, @{$unrel->issues}) : "",
        
        );
    $csv->combine(@tuple);
    $csv->string();
}


# 
# Combine the fields of the supplied TitleRecord into a CSV string
# 
sub combineRecord {
    my ($rec, $stat) = @_;
    my @tuple = (
        $rec->publisher,
        $rec->title,
        $rec->issn,
        $rec->eissn,
        $stat,
        join ($rngSep, @{$rec->vols}),
        join ($rngSep, @{$rec->yrs}),
        join ($rngSep, @{$rec->issues}),
        );
    $csv->combine(@tuple);
    $csv->string();
}

# 
# Pack a default CSV row into a TitleRecord and map it from an id string.
# The packed id will be unique in the default CSV format.
# 
sub defaultPack {
    # Array reference and csv row
    my ($hashRef, $r) = @_;
    my $publ = $r->[$FLD_PUBL->index];
    my $title = $r->[$FLD_TITLE->index];
    my $issn = $r->[$FLD_ISSN->index];
    my $eissn = $r->[$FLD_EISSN->index];
    my $id = makeId($publ, $title);

    # Pack to match committed report: publisher,title,issn,eissn
    $hashRef->{$id} = TitleRecord->new(
        publisher => $publ,
        title => $title,
        issn  => $issn,
        eissn => $eissn,
        );
}

# 
# Pack a KBART CSV row into a TitleRecord and map it from an id string. There 
# will be multiple volume/year entries per title/id, so if a TitleRecord already
# exists for the id, its volume/year arrays are updated. 
# 
sub kbartPack {
    # Array reference and csv row
    my ($hashRef, $r) = @_;
    my $publ = $r->[$FLD_PUBL->kbindex];
    my $title = $r->[$FLD_TITLE->kbindex];
    my $issn = $r->[$FLD_ISSN->kbindex];
    my $eissn = $r->[$FLD_EISSN->kbindex];

    # Pack to match committed report
    my $id = makeId($publ, $title);
    my $vrng = kbartRange($r->[$FLD_VOL_S->kbindex],$r->[$FLD_VOL_E->kbindex]);
    my $yrng = kbartRange($r->[$FLD_YR_S->kbindex],$r->[$FLD_YR_E->kbindex]);
    my $irng = kbartRange($r->[$FLD_ISS_S->kbindex],$r->[$FLD_ISS_E->kbindex]);
    unless (defined $hashRef->{$id}) {
        $hashRef->{$id} = TitleRecord->new(
            publisher => $publ,
            title => $title,
            issn  => $issn,
            eissn => $eissn,
            vols  => [],
            yrs   => [],
            issues=> []
            )
    };
    push @{$hashRef->{$id}->vols},   $vrng unless $vrng eq '';
    push @{$hashRef->{$id}->yrs},    $yrng unless $yrng eq '';
    push @{$hashRef->{$id}->issues}, $irng unless $irng eq '';
}

# 
# Make a string representing a range of titles from a kbart csv line.
# Return a blank string if there is no start value; return start to now 
# if there is a start but no end value. Otherwise return a range if the 
# start and end are different, or a single value.
# 
sub kbartRange {
    my ($s, $e) = @_;
    return '' if ($s eq '');
    return $e if ($e eq "$s($now)");
    return "$s $rngLink $now" if ($e eq '');
    return "$s $rngLink $e" if ($s ne $e);
    return $s;
}

# 
# Construct an id out of the supplied field values. Any number of
# values can be supplied; they are just concatenated with the 
# separator defined in $SEP.
# 
sub makeId {
    join($SEP, @_);
}

# Read the specified CSV file into the hash indicated by the supplied reference,
# using the given packing method reference.
# Note: Discards the first line as a header row.
sub readCsvIntoHash {
    my ($csvFile, $hashRef, $packRef) = @_;
    print STDERR "Reading CSV file $csvFile";
    #open my $fh, "<:encoding(utf8)", $csvFile or die "$!";
    open my $fh, $csvFile or die "$! ";
    # Ignore header
    my $header = $csv->getline( $fh );
    while ( my $row = $csv->getline( $fh ) ) {
        &$packRef($hashRef, $row);
    }
    $csv->eof or $csv->error_diag();
    close $fh;
    # Print total for debugging
    print STDERR " ... ".(scalar keys %{$hashRef})." titles\n"
}

# 
# Generate CSV files of TDB metadata, using tdbout. 
# Each line contains an AU.
# Use tdbout -n option to print field names.
# 
sub generateCsvMetadata {
    print STDERR "Generating new CSV metadata from TDB (3 files):\n";

    # Delete old files
    unlink ($tdbCsvProduction);
    unlink ($tdbCsvUnreleased);
    unlink ($committedTitles);

    # Create field list for tdbout
    my $tdbFieldList = join(',', @tdbFields);
    # Create header for the CSV output from TDB
    my $header = join(',', map { $tdbFieldMapping{$_} or $_ } @tdbFields);
    # Create header for the committed titles output (the fields are the default output from tdbout -j)
    my $cheader = join(',', ($FLD_PUBL->name, $FLD_TITLE->name, $FLD_ISSN->name, $FLD_EISSN->name));
    # Dump stderr to /dev/null
    my $dumpErr = " 2>/dev/null ";

    # (0) Make the participating publishers report
    print STDERR " 1. Participating publishers and titles ($committedTitles)\n";
    # Don't forget the header row (though it is not used)
    run("echo '$cheader' > $committedTitles");
    run("$tdbout -j $tdbs $dumpErr | sort -u >> $committedTitles");

    # (1) TDB metadata for AUs released into production
    #     (including those that have subsequently been marked as down)
    print STDERR " 2. Production AUs  ($tdbCsvProduction)\n";
    run("echo '$header' > $tdbCsvProduction");
    run("$tdbout $productionQuery -c '$tdbFieldList' $tdbs $dumpErr | sort -u >> $tdbCsvProduction");
    # TDBOUT does not give error return value, so check the size of the output file
    die "tdbout failed: $?\n" unless &fileHasMoreThanAHeader($tdbCsvProduction);

    # (2) TDB metadata for AUs not released into production
    print STDERR " 3. Unreleased AUs  ($tdbCsvUnreleased)\n";
    run("echo '$header' > $tdbCsvUnreleased");
    run("$tdbout $unreleasedQuery -c '$tdbFieldList' $tdbs $dumpErr| sort -u >> $tdbCsvUnreleased");
    # TDBOUT does not give error return value, so check the size of the output file
    die "tdbout failed: $?\n" unless &fileHasMoreThanAHeader($tdbCsvUnreleased);
} 

# Run a command (and set of args) using system, and stop with message if it is cancelled
sub run {
    system(@_) == 0 or die "-- Cancelled --";
}

# 
# Convert CSV files of TDB metadata, using KBART exporter.
# 
sub convertCsvIntoKbart {
    print STDERR "Converting TDB CSV metadata into KBART:\n";
    # Delete old files
    unlink ($kbartUnreleased);
    unlink ($kbartProduction);

    # Create header
    my $tdbFieldList = join(',', @tdbFields);
    # Field headings to use in the CSV output from tdb
    my $header = join(',', map { $tdbFieldMapping{$_} or $_ } @tdbFields);

    # Command to run RunKbartReport, including all daemon libs on classpath
    my $runKbartReport = 
        "java -cp .:$daemonHome:`ls $daemonHome/lib/*.jar | tr '\n' ':'` ".
        "org.lockss.devtools.RunKbartReport";
    # Redirect stderr to /dev/null unless requested
    my $errRedir = $showKbartWarnings ? "" : " 2>/dev/null";

    # (1) KBART metadata for titles released into production
    print STDERR " 1. Production AUs  ($kbartProduction)\n";
    system("$runKbartReport -d $kborder -i $tdbCsvProduction > $kbartProduction $errRedir") 
        == 0 or die  "RunKbartReport failed: $?\n";

    # (2) KBART metadata for titles not released into production
    print STDERR " 2. Unreleased AUs  ($kbartUnreleased)\n";
    system("$runKbartReport -d $kborder -i $tdbCsvUnreleased > $kbartUnreleased $errRedir") 
        == 0 or die  "RunKbartReport failed: $?\n";
} 

sub fileHasMoreThanAHeader() {
    my $f = shift;
    my $n = `wc -l $f`;
    $n =~ s/^\s+//g;
    $n =~ s/\s+$//g;
    my @res = split(/\s+/, $n);
    $res[0] > 1;
}

sub getFieldLabel() {
    my $f = shift;
    return $tdbFieldMapping{$f} or $f;
}

# Show usage message
sub usage {
    print STDERR <<END;
        
    Description: 
      Report all available titles in LOCKSS, along with statuses, 
      and coverage ranges where available. Reads the default or 
      supplied CSV files and combines them into a report listing
      which ranges are "$IN_PROGRESS" and which are "$PRESERVED".
      NOTE: Only journals are included.

    Usage: 
      $0 [options]
      
    Arguments:
      --daemon-home <path>       LOCKSS Daemon home dir
                                  (default $daemonHome)
      --production <filename>    Filename of KBART Production report
                                  (default $kbartProduction)
      --unreleased <filename>    Filename of KBART Unreleased report
                                  (default $kbartUnreleased)
      --committed <filename>     Filename of Committed Titles report
                                  (default $committedTitles)

    Options:
      --gln                      Generate data from the GLN, including the UK 
                                  (default, do not use with --clockss)
      --clockss                  Generate data from the CLOCKSS Ingest Network,
                                  (not default, do not use with --gln)
      --network <networkname>    Generate data from the network specified by networkname;
                                  gln->prod, clockss->clockssingest
                                  (not default, do not use with --gln or --clockss)
      --generate-tdb-metadata    Generate fresh CSV metadata from TDBs 
                                  (time-consuming but necessary on first run)
      --show-kbart-warnings      Show the warning messages from the KBART converter
      --help|--?                 Help message
           
END
;
    exit;
}


# ------------------------------------------------------------------------------
# KBART RunKbartReport usage, for reference
# ------------------------------------------------------------------------------
# usage: RunKbartReport -i <arg> [-h] [-e] [-d <arg>]
# 
#  -i,--input-file <arg>    Path to the input file
#  -h,--help                Show help
#  -e,--hide-empty-cols     Hide output columns that are empty.
#  -d,--data-format <arg>   Format of the output data records.
# 
# Note that the -i option is required
# 
# Data format argument must be one of the following identifiers (default KBART):
# 
#   KBART (Full KBART format in default ordering; one line per coverage range)
#   PUBLISHER_PUBLICATION (Standard KBART fields with publisher name at the start)
#   TITLE_COVERAGE_RANGES (List coverage ranges for each title; one per line, publisher first)
#   TITLES_BASIC (Publisher, Publication, ISSN, eISSN)
#   TITLE_ISSN (List ISSNs and Title only)
#   ISSN_ONLY (Produce a list of ISSNs)
# 
# Input file should be UTF-8 encoded.
