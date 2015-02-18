#!/usr/bin/perl -w
# $Id$
#
# Read in a list of AUs defined with the HighWire plugins.
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
    "-c            Use ClockssHighWirePressH20Plugin (default HighWirePressH20Plugin)\n",
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
   $plugin = "ClockssHighWirePressH20Plugin";
   } else {
   $plugin = "HighWirePressH20Plugin";
   }
if ($ret != 1 || $opt_help || (int(@ARGV) < 1)) {
    &usage;
}

while (my $line = <>) {
    chomp($line);
    # Check only for HighWire plugins.
    if ($line =~ m/(HighWireStrVolPlugin|HighWirePressPlugin|HighWirePressH20Plugin|DrupalPlugin)/i) {
      if ($line =~ m/org\|lockss\|plugin\|highwire\|(\S+)\&base_url~(\S+)\&volume_name~(\d+)/) {
        my $au_plugin = $1;
        my $base_url = $2;
        my $vol_num  = $3;
        if (! exists($au_volume{$base_url})) {
          $au_volume{$base_url}{min} = $vol_num;
          $au_volume{$base_url}{max} = $vol_num;
          $au_volume{$base_url}{start_plugin} = $au_plugin
        } else {
        if ($vol_num < $au_volume{$base_url}{min}) {
          $au_volume{$base_url}{min} = $vol_num;
        }
        if ($vol_num > $au_volume{$base_url}{max}) {
          $au_volume{$base_url}{max} = $vol_num;
          $au_volume{$base_url}{start_plugin} = $au_plugin
        }
      }
    }
  }
}

foreach my $base_url (sort(keys(%au_volume))) {
    for (my $x = $au_volume{$base_url}{min} - $opt_pre; $x < $au_volume{$base_url}{min}; ++$x) {
      &print_au($au_volume{$base_url}{start_plugin}, $base_url, $x) if ($x > 0);
    }
    for (my $x = $au_volume{$base_url}{max} + 1; $x <= $au_volume{$base_url}{max} + $opt_post; ++$x) {
      &print_au($au_volume{$base_url}{start_plugin}, $base_url, $x) if ($x > 0);
    }
}

exit(0);

sub print_au {
    my ($plugin, $base_url, $vol_num) = @_;
    printf("%s|%s|%s|%s|%s\n", "org", "lockss", "plugin", "highwire",
    "${plugin}\&base_url~${base_url}\&volume_name~${vol_num}");
    return(1);
}

