#!/usr/bin/perl -w
#
# Scrub a list of pub, title, issn, eissn (tab separated) for various
# rules.

my %issn_eissn = ();

while (my $line = <>) {
    chomp($line);
    $line =~ s/^\t+//;
    my ($pub, $title, $issn, $eissn) = split(/\t/, $line);
    if (! exists($issn_eissn{$issn})) {
        $issn_eissn{$issn} = [ [ $pub, $title, $issn, $eissn ] ];
    } else {
	push(@{$issn_eissn{$issn}}, [ $pub, $title, $issn, $eissn ] );
    }
    if (! exists($issn_eissn{$eissn})) {
        $issn_eissn{$eissn} = [ [ $pub, $title, $issn, $eissn ] ];
    } else {
	push(@{$issn_eissn{$eissn}}, [ $pub, $title, $issn, $eissn ] );
    }
}

print "Duplicate ISSNs and EISSNs:\n";
print "*** NOTE: In cases where the issn and eissn are identical, sets will be listed twice ***\n";
foreach my $issn (sort(keys(%issn_eissn))) {
    next if ($issn eq "");
    if (int(@{$issn_eissn{$issn}}) > 1) {
        my $print_this = 1;
        if (int(@{$issn_eissn{$issn}}) == 2 && 
            (@{$issn_eissn{$issn}}->[0]->[0] ne @{$issn_eissn{$issn}}->[1]->[0]) &&
            (@{$issn_eissn{$issn}}->[0]->[1] eq @{$issn_eissn{$issn}}->[1]->[1]) &&
            (@{$issn_eissn{$issn}}->[0]->[2] eq @{$issn_eissn{$issn}}->[1]->[2]) &&
            (@{$issn_eissn{$issn}}->[0]->[3] eq @{$issn_eissn{$issn}}->[1]->[3])) {
            $print_this = 0;
        }
        if ($print_this) {
            foreach my $rec (@{$issn_eissn{$issn}}) {
	        print "$rec->[0]\t$rec->[1]\t$rec->[2]\t$rec->[3]\n";
  	    }
	    print "\n";
        }
    }
}

exit(0);

