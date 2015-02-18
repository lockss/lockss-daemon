#!/usr/bin/perl -w
# $Id$
#
# Read in a list of AUs defined with the Project Muse plugin.
# Propose new AUs, either before or after the range provided
# in the list.

use strict;
use Getopt::Long;

my $opt_pre = 0;
my $opt_post = 1;
my $clockss = 0;
my $plugin = "";
my $base_url = "";
my %au_volume = ();

my @Usage = ("$0 [-h] [--pre=<num1>] [--post=<num2>] auid_file\n",
    "--pre=<num1>  Print <num1> earlier AUs (default $opt_pre)\n",
    "--post=<num2> Print <num2> newer AUs (default $opt_post)\n",
#    "-c            Use ClockssHighWirePressH20Plugin (default HighWirePressH20Plugin)\n",
    "-h            Print this help message.");
sub usage {
    print '$Revision$' . "\n";
    print "Usage:\n @Usage\n";
    exit(1);
}

my $opt_help = 0;
my $opt_debug = 0;
my $ret = GetOptions('help|h' => \$opt_help,
    'pre=i' => \$opt_pre,
    'post=i' => \$opt_post,
#    'c' => \$clockss,
    'debug' => \$opt_debug);
#if ($clockss) {
#   $plugin = "ClockssProjectMusePlugin";
#   } else {
   $plugin = "ProjectMusePlugin";
   $base_url = "http://muse.jhu.edu/";
#   }
if ($ret != 1 || $opt_help || (int(@ARGV) < 1)) {
    &usage;
}

while (my $line = <>) {
    chomp($line);
    # Check only for ProjectMuse plugin.
    if ($line =~ m/(ProjectMusePlugin)/i) {
        if ($line =~ m/\&base_url~(\S+)\&journal_dir~(\S+)\&volume~(\d+)/) {
            # my $base_url = $1;
            my $journal_dir = $2;
            my $vol_num  = $3;
            if (! exists($au_volume{$journal_dir})) {
                $au_volume{$journal_dir}{min} = $vol_num;
                $au_volume{$journal_dir}{max} = $vol_num;
            } else {
                if ($vol_num < $au_volume{$journal_dir}{min}) {
                    $au_volume{$journal_dir}{min} = $vol_num;
                }
                if ($vol_num > $au_volume{$journal_dir}{max}) {
                    $au_volume{$journal_dir}{max} = $vol_num;
                }
            }
        }
    }
}

foreach my $journal_dir (sort(keys(%au_volume))) {
    for (my $x = $au_volume{$journal_dir}{min} - $opt_pre; $x < $au_volume{$journal_dir}{min}; ++$x) {
        &print_au($plugin, $base_url, $journal_dir, $x) if ($x > 0);
    }
    for (my $x = $au_volume{$journal_dir}{max} + 1; $x <= $au_volume{$journal_dir}{max} + $opt_post; ++$x) {
        &print_au($plugin, $base_url, $journal_dir, $x) if ($x > 0);
    }
}

exit(0);

sub print_au {
    my ($plugin, $base_url, $journal_dir, $vol_num) = @_;
    printf("%s|%s|%s|%s|%s\n", "org", "lockss", "plugin", "projmuse",
    "${plugin}\&base_url~${base_url}\&journal_dir~${journal_dir}\&volume~${vol_num}");
    return(1);
}

