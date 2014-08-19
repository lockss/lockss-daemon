#!/usr/bin/perl
#
# Filter a table and perform text substitutions on the 2nd field of the line.
# Used for replacing the plugin with plugin+status in test reports.

my %new_text = ();

my $sub_fname = shift(@ARGV);
open(IFILE, "<$sub_fname") ||
    die("Could not open substitution file '$sub_fname'!\n");
while (my $line = <IFILE>) {
    if ($line =~ m/([^\t]+)\t([^\t\n]+)/) {
        $new_text{$1} = $2;
    }
}

while (my $line = <>) {
    chomp($line);
    my @F = split(/\t+/, $line);
    my @G = ();
    for (my $x = 0; $x < @F; ++$x) {
        push(@G, $F[$x]);
        if ($x == 1) {
            my $new_field = "needs.plugin";
            if (exists($new_text{$F[1]})) {
                $new_field = $new_text{$F[$x]};
            }
            push(@G, $new_field);
        }
    };
    print join("\t", @G) . "\n";
}

exit(0);
