#!/usr/bin/perl

#GLN: To create report, comparing two points in time.
#git checkout master
#git checkout `git rev-list -n 1 --before="2020-04-01 00:00" master`
#ant jar-lockss
#/scripts/tdb/tdbout -t auid,status tdb/prod/{,*/}*.tdb | sort -u > ../tmp/file1.txt
#git checkout master
#git checkout `git rev-list -n 1 --before="2021-04-01 00:00" master`
#ant jar-lockss
#./scripts/tdb/tdbout -t auid,status tdb/prod/{,*/}*.tdb | sort -u > ../tmp/file2.txt
#./scripts/tdb/report_buckets.pl ../tmp/file1.txt ../tmp/file2.txt
#git checkout master
#git pull origin master
#ant jar-lockss

#CLOCKSS: To create a report, comparing clockss status1 and status2.
#git checkout master
#git checkout `git rev-list -n 1 --before="2021-05-01 00:00" master`
#ant jar-lockss
#./scripts/tdb/tdbout -t auid,status tdb/clockssingest/{,*/}*.tdb | sort -u > ../SageEdits/file1.txt
#./scripts/tdb/tdbout -t auid,status2 tdb/clockssingest/{,*/}*.tdb | sort -u > ../SageEdits/file2.txt
#./scripts/tdb/report_buckets.pl ../SageEdits/file1.txt ../SageEdits/file2.txt > ../SageEdits/buckets_today.tsv
#git checkout master
#git pull
#ant jar-lockss


my %code = ("notPresent" => 0,
    "expected" => 1,
    "exists" => 2,
    "manifest" => 3,
    "wanted" => 4,
    "testing" => 5,
    "notReady" => 6,
    "ready" => 7,
    "crawling" => 8,
    "deepCrawl" => 9,
    "frozen" => 10,
    "ingNotReady" => 11,
    "finished" => 12,
    "released" => 13,
    "down" => 14,
    "superseded" => 15,
    "zapped" => 16,
    "doNotProcess" => 17,
    "doesNotExist" => 18,
    "other" => 19,
    "deleted" => 20);

my %auid_status = ();

my $file1_name = shift(@ARGV);
my $file2_name = shift(@ARGV);

# Read in first file. (Should be the report from the earlier
# date.
open(IFILE, "<$file1_name");
while (my $line = <IFILE>) {
    my ($auid, $status) = split(/\s+/, $line);
    my $status_code = $code{"other"};
    if (exists($code{$status})) {
        $status_code = $code{$status};
    }
    $auid_status{$auid}{start} = $status_code;
}
close(IFILE);

# Debug report
#foreach my $auid (keys(%auid_status)) {
#    printf("%s %d\n", $auid, $auid_status{$auid}{start});
#}

# Read in second file. (Should be the report from the later
# date.
open(IFILE, "<$file2_name");
while (my $line = <IFILE>) {
    my ($auid, $status) = split(/\s+/, $line);
    my $status_code = $code{"other"};
    if (exists($code{$status})) {
        $status_code = $code{$status};
    }
    $auid_status{$auid}{end} = $status_code;
}
close(IFILE);

# Clean up data structure by adding "notPresent" codes where
# things are missing.
foreach my $auid (keys(%auid_status)) {
    if (! exists($auid_status{$auid}{start})) {
        $auid_status{$auid}{start} = $code{"notPresent"};
    }
    if (! exists($auid_status{$auid}{end})) {
        $auid_status{$auid}{end} = $code{"deleted"};
    }
}

# Fill 2D array of buckets for each combination
# of start and end status
my @start_end = ();
# Initialize all to 0
foreach my $x (values(%code)) {
	foreach my $y (values(%code)) {
		$start_end[$x][$y] = 0;
	}
}
foreach my $auid (keys(%auid_status)) {
    $start_code = $auid_status{$auid}{start};
    $end_code = $auid_status{$auid}{end};
		$start_end[$start_code][$end_code] += 1;
		if ($end_code == $code{"other"}) {
			printf("Unexpected status:%s\n",$auid);
		}
#		if ($start_code == $code{"released"} && $end_code == $code{"deleted"}) {
#			printf("Previously released, now deleted:%s\n",$auid);
#		}
#		if ($start_code == $code{"down"} && $end_code == $code{"deleted"}) {
#			printf("Previously down, now deleted:%s\n",$auid);
#		}
#		if ($start_code == $code{"superseded"} && $end_code == $code{"deleted"}) {
#			printf("Previously superseded, now deleted:%s\n",$auid);
#		}
#		if ($start_code == $code{"released"} && $end_code == $code{"exists"}) {
#			printf("Previously released, now exists:%s\n",$auid);
#		}
#		if ($start_code == $code{"released"} && $end_code == $code{"manifest"}) {
#			printf("Previously released, now manifest:%s\n",$auid);
#		}
		if (($start_code == $code{"released"} || $start_code == $code{"down"} || $start_code == $code{"superseded"}) &&
		     !($end_code == $code{"released"} || $end_code == $code{"down"} || $end_code == $code{"superseded"})) {
			printf("Previously %s, now %s: %s\n",&code_name($start_code),&code_name($end_code),$auid);
		}
	}

# Print out report
# Header
printf("Status");
foreach my $y (sort by_value values(%code)) {
	printf("\t%s", &code_name($y));
}
print("\n");
foreach my $x (sort by_value values(%code)) {
	printf("%s", &code_name($x));
	foreach my $y (sort by_value values(%code)) {
		printf("\t%d", $start_end[$x][$y]);
	}
	print("\n");
}
print("\n");

exit(0);

# Fill buckets based on info for each AU ID.
###my @start_bucket = ();
###my @end_bucket = ();
###my @no_change_bucket = ();
###my @change_bucket = ();
###foreach my $c (keys(%code)) {
###    my $code_num = $code{$c};
###    $start_bucket[$code_num] = 0;
###    $end_bucket[$code_num] = 0;
###    $no_change_bucket[$code_num] = 0;
###    $change_bucket[$code_num] = 0;
###}
###foreach my $auid (keys(%auid_status)) {
###    $start_code = $auid_status{$auid}{start};
###    $end_code = $auid_status{$auid}{end};
###    $start_bucket[$start_code] += 1;
###    $end_bucket[$end_code] += 1;
###    if ($start_code == $end_code) {
###        $no_change_bucket[$start_code] += 1;
###    } else {
###        if ($end_code == $code{"deleted"}) {
###            $change_bucket[$end_code] += 1;
###        } elsif (($start_code == $code{"other"}) ||
###	 	($end_code == $code{"other"})) {
###	    $change_bucket[$code{"other"}] += 1;
###        } elsif (($start_code == $code{"doNotProcess"}) ||
###	 	($end_code == $code{"doNotProcess"})) {
###	    $change_bucket[$code{"doNotProcess"}] += 1;
###        } elsif (($start_code == $code{"doesNotExist"}) ||
###	 	($end_code == $code{"doesNotExist"})) {
###	    $change_bucket[$code{"doesNotExist"}] += 1;
###	} else {
###	    # Use a for loop to check all possible buckets.
###	    # Don't check buckets beyond superseded because
###	    # the "if" clauses above have already checked
###	    # for them.
###	    for (my $c = $code{"notPresent"};
###		    $c <= $code{"superseded"};
###		    ++$c) {
###		if (($start_code <= $c) && ($end_code > $c)) {
###		    $change_bucket[$c] += 1;
###		}
###	    }
###        }
###    }
###}
###
#### Write out report.
###for (my $c = $code{"notPresent"}; $c <= $code{"deleted"}; ++$c) {
###    printf("%s\t%d\t%d\t%d\t%d\n",
###	&code_name($c),
###	$start_bucket[$c], $end_bucket[$c],
###	$no_change_bucket[$c], $change_bucket[$c]);
###}
###
###exit(0);
###

sub code_name {
    my ($code_num) = @_;
    my $rval = "unknown";
    foreach my $cn (keys(%code)) {
	if ($code_num == $code{$cn}) {
	    $rval = $cn;
	    last;
	}
    }
    return($rval);
}

sub by_value {
  return($a <=> $b);
}
