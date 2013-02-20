#!/usr/bin/perl -w
# $Id: propose_new_aus.pl,v 1.3 2013-02-20 18:32:51 mellen22 Exp $
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
    print '$Revision: 1.3 $' . "\n";
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
    if ($line =~ m/(HighWireStrVolPlugin|HighWirePressPlugin|HighWirePressH20Plugin)/i) {
	if ($line =~ m/\&base_url~(\S+)\&volume_name~(\d+)/) {
	    my $base_url = $1;
	    my $vol_num  = $2;
	    if (! exists($au_volume{$base_url})) {
		$au_volume{$base_url}{min} = $vol_num;
		$au_volume{$base_url}{max} = $vol_num;
	    } else {
		if ($vol_num < $au_volume{$base_url}{min}) {
		    $au_volume{$base_url}{min} = $vol_num;
		}
		if ($vol_num > $au_volume{$base_url}{max}) {
		    $au_volume{$base_url}{max} = $vol_num;
		}
	    }
	}
    }
}

foreach my $base_url (sort(keys(%au_volume))) {
    for (my $x = $au_volume{$base_url}{min} - $opt_pre; $x < $au_volume{$base_url}{min}; ++$x) {
	&print_au($plugin, $base_url, $x) if ($x > 0);
    }
    for (my $x = $au_volume{$base_url}{max} + 1; $x <= $au_volume{$base_url}{max} + $opt_post; ++$x) {
	&print_au($plugin, $base_url, $x) if ($x > 0);
    }
}

exit(0);

sub print_au {
    my ($plugin, $base_url, $vol_num) = @_;
    printf("%s|%s|%s|%s|%s\n", "org", "lockss", "plugin", "highwire",
	"${plugin}\&base_url~${base_url}\&volume_name~${vol_num}");
    return(1);
}

