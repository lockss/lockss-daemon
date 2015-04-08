#!/usr/bin/perl -w
# $Id$
#
# Read in a list of AUs defined with the OJS plugin.
# Propose new AUs, either before or after the range provided
# in the list.

use strict;
use Getopt::Long;

my $opt_pre = 0;
my $opt_post = 1;
my $clockss = 0;
my $plugin = "";
my %au_volume = ();

my @Usage = ("$0 [-h] [--pre=<num1>] [--post=<num2>] auid_file\n",
    "--pre=<num1>  Print <num1> earlier AUs (default $opt_pre)\n",
    "--post=<num2> Print <num2> newer AUs (default $opt_post)\n",
    "-c            Use ClockssOJS2Plugin (default OJS2Plugin)\n",
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
    'c' => \$clockss,
    'debug' => \$opt_debug);
if ($clockss) {
   $plugin = "ClockssOJS2Plugin";
   } else {
   $plugin = "OJS2Plugin";
   }
if ($ret != 1 || $opt_help || (int(@ARGV) < 1)) {
    &usage;
}

while (my $line = <>) {
    chomp($line);
    # Check only for HighWire plugins.
    if ($line =~ m/(OJS2Plugin)/i) {
      # org|lockss|plugin|ojs2|OJS2Plugin&base_url~http%3A%2F%2Fwww%2Efaccamp%2Ebr%2Fojs%2F&journal_id~RMPE&year~2007
      if ($line =~ m/org\|lockss\|plugin\|ojs2\|(\S+)\&base_url~(\S+)\&journal_id~(\S+)\&year~(\d+)/) {
        my $au_plugin = $1;
        my $base_url = $2;
        my $journal_id = $3;
        my $year  = $4;
        if (! exists($au_volume{$base_url}{$journal_id})) {
          $au_volume{$base_url}{$journal_id}{min} = $year;
          $au_volume{$base_url}{$journal_id}{max} = $year;
          $au_volume{$base_url}{$journal_id}{start_plugin} = $au_plugin
        } else {
        if ($year < $au_volume{$base_url}{$journal_id}{min}) {
          $au_volume{$base_url}{$journal_id}{min} = $year;
        }
        if ($year > $au_volume{$base_url}{$journal_id}{max}) {
          $au_volume{$base_url}{$journal_id}{max} = $year;
          $au_volume{$base_url}{$journal_id}{start_plugin} = $au_plugin
        }
      }
    }
  }
}

foreach my $base_url (sort(keys(%au_volume))) {
    foreach my $journal_id (keys(%{$au_volume{$base_url}})) {
      for (my $x = $au_volume{$base_url}{$journal_id}{min} - $opt_pre; $x < $au_volume{$base_url}{$journal_id}{min}; ++$x) {
        &print_au($au_volume{$base_url}{$journal_id}{start_plugin}, $base_url, $journal_id, $x) if ($x > 0);
      }
      for (my $x = $au_volume{$base_url}{$journal_id}{max} + 1; $x <= $au_volume{$base_url}{$journal_id}{max} + $opt_post; ++$x) {
        &print_au($au_volume{$base_url}{$journal_id}{start_plugin}, $base_url, $journal_id, $x) if ($x > 0);
      }
    }
}

exit(0);

sub print_au {
    my ($plugin, $base_url, $journal_id, $year) = @_;
    printf("%s|%s|%s|%s|%s\n", "org", "lockss", "plugin", "ojs2",
    "${plugin}\&base_url~${base_url}\&journal_id~${journal_id}\&year~${year}");
    return(1);
}

