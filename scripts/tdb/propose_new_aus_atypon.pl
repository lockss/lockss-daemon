#!/usr/bin/perl -w
# $Id: propose_new_aus.pl 39864 2015-02-18 09:10:24Z thib_gc $
#
# Read in a list of AUs defined with the Atypon plugins.
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
    "-c            Use Clockss Atypon Plugin (default GLN Atypon Plugin)\n",
    "-h            Print this help message.");
sub usage {
    print '$Revision: 39864 $' . "\n";
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
   $plugin = "ClockssAtyponPlugin";
   } else {
   $plugin = "AtyponPlugin";
   }
if ($ret != 1 || $opt_help || (int(@ARGV) < 1)) {
    &usage;
}

while (my $line = <>) {
    chomp($line);
    # Check only for Atypon plugins.
    if ($line =~ m/(atypon|TaylorAndFrancisPlugin)/i) {
      if ($line =~ m/org\|lockss\|plugin\|taylorandfrancis\|(\S+)\&base_url~(\S+)\&journal_id~(\S+)\&volume_name~(\d+)/) {
        my $au_plugin = $1;
        my $base_url = $2;
        my $journal_id = $3;
        my $vol_num  = $4;
        if (! exists($au_volume{$journal_id})) {
          $au_volume{$journal_id}{min} = $vol_num;
          $au_volume{$journal_id}{max} = $vol_num;
          $au_volume{$journal_id}{d_base_url} = $base_url;
          $au_volume{$journal_id}{d_plugin} = $au_plugin;
        } else {
        if ($vol_num < $au_volume{$journal_id}{min}) {
          $au_volume{$journal_id}{min} = $vol_num;
        }
        if ($vol_num > $au_volume{$journal_id}{max}) {
          $au_volume{$journal_id}{max} = $vol_num;
        }
      }
    }
  }
}

foreach my $journal_id (sort(keys(%au_volume))) {
    for (my $x = $au_volume{$journal_id}{min} - $opt_pre; $x < $au_volume{$journal_id}{min}; ++$x) {
      &print_au($au_volume{$journal_id}{d_plugin}, $au_volume{$journal_id}{d_base_url}, $journal_id, $x) if ($x > 0);
    }
    for (my $x = $au_volume{$journal_id}{max} + 1; $x <= $au_volume{$journal_id}{max} + $opt_post; ++$x) {
      &print_au($au_volume{$journal_id}{d_plugin}, $au_volume{$journal_id}{d_base_url}, $journal_id, $x) if ($x > 0);
    }
}

exit(0);

sub print_au {
    my ($plugin, $base_url, $journal_id, $vol_num) = @_;
    printf("%s|%s|%s|%s|%s\n", "org", "lockss", "plugin", "taylorandfrancis",
    "${plugin}\&base_url~${base_url}\&journal_id~${journal_id}\&volume_name~${vol_num}");
    return(1); 
}

