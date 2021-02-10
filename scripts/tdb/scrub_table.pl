#!/usr/bin/perl -w
#
# Scrub a list of pub, title, issn, eissn (tab separated) for various
# rules.

my %issn_eissn = ();

#creates an array of arrays; an array for each issn and eissn.
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

print "ISSN and EISSN issues:\n";
print "*** NOTE: In cases where the issn and eissn are identical, sets will be listed twice ***\n";
foreach my $issn (sort(keys(%issn_eissn))) {
    next if ($issn eq "");
    $issn = &trim($issn);
    if ($issn !~ m/^\d\d\d\d-\d\d\d[0-9X]$/) {
        print "$issn incorrect format\n\n";
    } elsif (! &verify_issn($issn)) {
        print "$issn bad check digit\n\n";
    }
    if (int(@{$issn_eissn{$issn}}) > 1) {
        my $print_this = 1;
#        if (int(@{$issn_eissn{$issn}}) == 2 && 
##            (@{$issn_eissn{$issn}}->[0]->[0] ne @{$issn_eissn{$issn}}->[1]->[0]) &&
##            (@{$issn_eissn{$issn}}->[0]->[1] eq @{$issn_eissn{$issn}}->[1]->[1]) &&
##            (@{$issn_eissn{$issn}}->[0]->[2] eq @{$issn_eissn{$issn}}->[1]->[2]) &&
##            (@{$issn_eissn{$issn}}->[0]->[3] eq @{$issn_eissn{$issn}}->[1]->[3])) {
##            $print_this = 0;
##        }
#            ($issn_eissn{$issn}[0]->[0] ne $issn_eissn{$issn}[1]->[0]) &&
#            ($issn_eissn{$issn}[0]->[1] eq $issn_eissn{$issn}[1]->[1]) &&
#            ($issn_eissn{$issn}[0]->[2] eq $issn_eissn{$issn}[1]->[2]) &&
#            ($issn_eissn{$issn}[0]->[3] eq $issn_eissn{$issn}[1]->[3])) {
#            #There are exactly two records, the publishers do not match, but the title, issn, and eissn do match
#            $print_this = 0;
#        }

    # Suppress printing if:
    #  Title is the same for all records, and
    #  ISSN is the same for all records, and
    #  EISSN is the same for all records.
    my $check_title = "";
    my $check_issn = "";
    my $check_eissn = "";
    my $suppress_this = 1;
    foreach my $rec_ref (@{$issn_eissn{$issn}}) {
        if ($check_title eq "") {
        $check_title = $rec_ref->[1];
        } elsif (lc($rec_ref->[1]) ne lc($check_title)) {
        $suppress_this = 0;
        last;
        }
        if ($check_issn eq "") {
        $check_issn = $rec_ref->[2];
        } elsif ($rec_ref->[2] ne $check_issn) {
        $suppress_this = 0;
        last;
        }
        if ($check_eissn eq "") {
        $check_eissn = $rec_ref->[3];
        } elsif ($rec_ref->[3] ne $check_eissn) {
        $suppress_this = 0;
        last;
        }
    }
    if ($suppress_this == 1) {
        $print_this = 0;
    }

    # Suppress printing if:
    #  Publisher is "Taylor & Francis" for all records, and
    #  ISSN is the same for all records, and
    #  EISSN is the same for all records.
    $check_issn = "";
    $check_eissn = "";
    $suppress_this = 1;
    foreach my $rec_ref (@{$issn_eissn{$issn}}) {
        if ($rec_ref->[0] ne "Taylor & Francis") {
        $suppress_this = 0;
        last;
        }
        if ($check_issn eq "") {
        $check_issn = $rec_ref->[2];
        } elsif ($rec_ref->[2] ne $check_issn) {
        $suppress_this = 0;
        last;
        }
        if ($check_eissn eq "") {
        $check_eissn = $rec_ref->[3];
        } elsif ($rec_ref->[3] ne $check_eissn) {
        $suppress_this = 0;
        last;
        }
    }
    if ($suppress_this == 1) {
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

sub verify_issn {
  my ($issn) = @_;
  
  # Remove the dash.
  $issn =~ s/-//;
  my $checksum = 0;
  for (my $x = 0; $x < 7; ++$x) {
      $checksum += substr($issn, $x, 1) * (8 - $x);
  }
  if (substr($issn, 7, 1) eq "X") {
      $checksum += 10;
  } else {
      $checksum += substr($issn, 7, 1);
  }
  if ($checksum % 11 == 0) {
      return(1);
  } else {
      return(0);
  }

# Perl trim function to remove whitespace from the start and end of the string
sub trim($)
{
    my $string = shift;
    $string =~ s/^\s+//;
    $string =~ s/\s+$//;
    return $string;
}

}
