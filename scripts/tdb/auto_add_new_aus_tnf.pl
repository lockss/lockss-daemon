#!/usr/bin/perl -w

#GLN
my $last_stat1 = "";
#my $last_stat2 = "";
my $last_year  = "";
my $last_title = "";
my $last_volnum = "";
my $last_line_was_au = 0;
while (my $line = <>) {
    my $this_line_is_au = 0;
    if ($line =~ m/^(\s*)au\s+<\s+(\S+)\s+;\s+(\S+)\s+;\s+(\S.*)\s+;\s+(\S+)\s+>\s*$/) {
    $last_hdr    = $1;
    $last_stat1  = $2;
    $last_year   = $3;
    $last_title  = $4;
    $last_volnum = $5;
    $this_line_is_au = 1;
    } elsif (($line =~ m/^\s*$/) && $last_line_was_au) {
    # Found blank line. May need to add a new AU for the previous title.
    if (($last_year eq "2015") || ($last_year eq "2014-2015")) {
        # Status fields remain the same.
        my $this_stat1 = "expected";
#       my $this_stat2 = "exists";
        # Update year to be either 2016 or 2015-2016.
        my $this_year = "2016";
        $this_year = "2015-2016" if ($last_year eq "2014-2015");
        $this_year = "2016-2017" if ($last_year eq "2015-2016");
        # Volume number goes up by one.
        my $this_volnum = $last_volnum + 1;
        # Substitute new volume number into title.
        my $this_title = $last_title;
        $this_title =~ s/Volume $last_volnum/Volume $this_volnum/;
        # Craft new AU and print it.
        my $new_line = sprintf("%sau < %s ; %s ; %s ; %s >\n", $last_hdr,
        $this_stat1,
        $this_year,
        $this_title,
        $this_volnum);
        print $new_line;
    }
    }
    print $line;
    $last_line_was_au = $this_line_is_au;
}

exit(0);

# Clockss
#my $last_stat1 = "";
#my $last_stat2 = "";
#my $last_year  = "";
#my $last_title = "";
#my $last_volnum = "";
#my $last_proxy = "";
#my $last_line_was_au = 0;
#while (my $line = <>) {
#    my $this_line_is_au = 0;
#    # au < finished ; crawling ; 2014 ; International Journal of Ad Hoc and Ubiquitous Computing Volume 16 ; 16 ; reingest4.clockss.org:8082 >
#    if ($line =~ m/^(\s*)au\s+<\s+(\S+)\s+;\s+(\S+)\s+;\s+(\S+)\s+;\s+(\S.*)\s+;\s+(\S+)\s+;\s+(\S*)\s*>\s*$/) {
#    $last_hdr    = $1;
#    $last_stat1  = $2;
#    $last_stat2  = $3;
#    $last_year   = $4;
#    $last_title  = $5;
#    $last_volnum = $6;
#    $last_proxy = $7;
#    $this_line_is_au = 1;
#    } elsif (($line =~ m/^\s*$/) && $last_line_was_au) {
#    # Found blank line. May need to add a new AU for the previous title.
#    if (($last_year eq "2015") || ($last_year eq "2014-2015")) {
#        # Status fields remain the same.
#        my $this_stat1 = "expected";
#        my $this_stat2 = "exists";
#        # Update year to be either 2014 or 2013-2014.
#        my $this_year = "2016";
#        $this_year = "2015-2016" if ($last_year eq "2014-2015");
#        $this_year = "2016-2017" if ($last_year eq "2015-2016");
#        # Volume number goes up by one.
#        my $this_volnum = $last_volnum + 1;
#        # Substitute new volume number into title.
#        my $this_title = $last_title;
#        $this_title =~ s/Volume $last_volnum/Volume $this_volnum/;
#        # Craft new AU and print it.
#        my $new_line = sprintf("%sau < %s ; %s ; %s ; %s ; %s ; >\n", $last_hdr,
#        $this_stat1, $this_stat2,
#        $this_year,
#        $this_title,
#        $this_volnum);
#        print $new_line;
#    }
#    }
#    print $line;
#    $last_line_was_au = $this_line_is_au;
#}
#
#exit(0);

