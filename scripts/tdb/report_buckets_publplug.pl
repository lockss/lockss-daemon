#!/usr/bin/perl

#This script is designed to evaluate a set of AUs and deliver a chart showing
#a count of AUs from a particular publisher/plugin pair that have been added to the ingest machines.

#GLN: To create report, comparing two points in time.
#git checkout master
#git checkout `git rev-list -n 1 --before="2020-04-01 00:00" master`
#ant jar-lockss
#/scripts/tdb/tdbout -t auid,status tdb/prod/{,_retired/}*.tdb | sort -u > ../tmp/file1.txt
#git checkout master
#git checkout `git rev-list -n 1 --before="2021-04-01 00:00" master`
#ant jar-lockss
#./scripts/tdb/tdbout -t auid,status,publisher,plugin tdb/prod/{,_retired/}*.tdb | sort -u > ../tmp/file2.txt
#./scripts/tdb/report_buckets.pl ../tmp/file1.txt ../tmp/file2.txt
#git checkout master
#git pull origin master
#ant jar-lockss

#CLOCKSS: To create a report, comparing two points in time.
#git checkout master
#git checkout `git rev-list -n 1 --before="2021-04-01 00:00" master`
#ant jar-lockss
#./scripts/tdb/tdbout -t auid,status tdb/clockssingest/{,_retired/}a*.tdb | sort -u > ../SageEdits/file1.txt
#git checkout master
#git checkout `git rev-list -n 1 --before="2021-05-01 00:00" master`
#ant jar-lockss
#./scripts/tdb/tdbout -t auid,status,publisher,plugin tdb/clockssingest/{,_retired/}a*.tdb | sort -u > ../SageEdits/file2.txt
#./scripts/tdb/report_buckets_publplug.pl ../SageEdits/file1.txt ../SageEdits/file2.txt > ../SageEdits/buckets_today.tsv
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
    "finished" => 11,
    "ingNotReady" => 12,
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
    $line =~s/\n//g;
    my ($auid, $status) = split(/\t/, $line);
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
#exit(0);

# Read in second file. (Should be the report from the later
# date.
open(IFILE, "<$file2_name");
while (my $line = <IFILE>) {
    $line =~s/\n//g;
    my ($auid, $status, $publisher, $plugin) = split(/\t/, $line);
    my $status_code = $code{"other"};
    if (exists($code{$status})) {
        $status_code = $code{$status};
    }
    $auid_status{$auid}{end} = $status_code;
    $auid_status{$auid}{publisher} = $publisher;
    $auid_status{$auid}{plugin} = $plugin;
}
close(IFILE);

# Debug report
#foreach my $auid (keys(%auid_status)) {
#    printf("%s %d %d %s %s\n", $auid, $auid_status{$auid}{start}, $auid_status{$auid}{end}, $auid_status{$auid}{plugin}, $auid_status{$auid}{publisher});
#}

#exit(0);

# Clean up data structure by adding "notPresent" codes where
# status is missing.
foreach my $auid (keys(%auid_status)) {
    if (! exists($auid_status{$auid}{start})) {
        $auid_status{$auid}{start} = $code{"notPresent"};
    }
    if (! exists($auid_status{$auid}{end})) {
        $auid_status{$auid}{end} = $code{"deleted"};
    }
    #printf("%s %s %s %s\n", $auid, &code_name($auid_status{$auid}{start}), &code_name($auid_status{$auid}{end}), $auid_status{$auid}{publisher});

    $start_code = $auid_status{$auid}{start};
    $end_code = $auid_status{$auid}{end};
    $publisher = $auid_status{$auid}{publisher};
    $plugin = $auid_status{$auid}{plugin};
    #if (!exists($publ_plug{$publisher}{$plugin})) {
    #    $publ_plug{$publisher}{$plugin} = 0;
    #}
    if ($start_code >= $code{"notPresent"} && 
        $start_code <= $code{"ready"} &&
        $end_code >= $code{"crawling"} &&
        $end_code <= $code{"finished"}) {
            if (!exists($publ_plug{$publisher}{$plugin})) {
                $publ_plug{$publisher}{$plugin} = 0;
            }
            $publ_plug{$publisher}{$plugin} += 1;
        }
}

#For each AUid, check start status and end status.
#For CLOCKSS
#If the start status is: notPresent, expected, exists, manifest, wanted, testing, notReady, or ready.
#and the end status is: crawling, deepCrawl, frozen, or finished.
#For GLN
#If the start status is: notPresent, expected, exists, manifest, wanted, testing, notReady, or ready.
#and the end status is: released
#then add 1 to an array of the publisher+plugin

#foreach my $auid (keys(%auid_status)) {
#    $start_code = $auid_status{$auid}{start};
#    $end_code = $auid_status{$auid}{end};
#    $publisher = $auid_status{$auid}{publisher};
#    $plugin = $auid_status{$auid}{plugin};
#    if ($start_code >= $code{"notPresent"} && 
#        $start_code <= $code{"ready"} &&
#        $end_code >= $code{"crawling"} &&
#        $end_code <= $code{"finished"}) {
#            $publ_plug[$publisher][$plugin] += 1;
#
##		if (($start_code == $code{"released"} || $start_code == $code{"down"} || $start_code == $code{"superseded"}) &&
#		     !($end_code == $code{"released"} || $end_code == $code{"down"} || $end_code == $code{"superseded"})) {
#			printf("Previously %s, now %s: %s\n",&code_name($start_code),&code_name($end_code),$auid);
#		}
#    }
#}

# Print out report

#For each publisher, looping on each plugin for each publisher,
#Print: $publisher \t $plugin \t $publ_plug{$publisher}{$plugin}

printf("-D- Found %d publishers\n", scalar(keys(%publ_plug)));
foreach my $x (sort keys(%publ_plug)) {
    foreach my $y (sort keys(%{ $publ_plug{$x} })) {
        printf("%s\t%s\t%d\n", $x,$y,$publ_plug{$x}{$y});
    }
}

exit(0);


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
