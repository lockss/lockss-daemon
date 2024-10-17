#!/usr/bin/perl -w
# This script scrapes the ams website to create a complete list of all the available AUs

use URI::Escape;
use Getopt::Long;
use LWP::UserAgent;
use HTTP::Request;
use HTTP::Cookies;
use HTML::Entities;
use LWP::Protocol::https;
use utf8;
#use encoding 'utf8';
use Encode qw(decode encode);

my $url = "cant_create_url";

# Don't save these cookies from run to run.
my $cjar = HTTP::Cookies->new();

# Create user agent.
my $ua = LWP::UserAgent->new( cookie_jar => $cjar, agent => "LOCKSS cache", ssl_opts => { verify_hostname => 0 } );
$ua->proxy('http', 'http://proxy.lockss.org:3128/');
$ua->no_proxy('localhost', '127.0.0.1');

#tdbout -EXMZFCMTYLc param[collection_id] tdb/clockssingest/american_mathematical_society.books.tdb | sort -u | ./scripts/tdb/check_ams_manifest_pages.pl | sort -u > ../SageEdits/ams_site_list
#tdbout -EXMZFCMTYLBc param[collection_id],param[year_string] tdb/clockssingest/american_mathematical_society.books.tdb | sort -u | sed s'/^/https:\/\/www.ams.org\//' | sed s'/,/\/year\//' > ../SageEdits/ams_tdb_list
#comm -23 ../SageEdits/ams_site_list ../SageEdits/ams_tdb_list
#*************************************

my @collection_list=();

while (my $line = <>) {
  chomp $line;
  push(@collection_list,$line);
}
#Find all Volumes
foreach my $collection (@collection_list) {
  #fetch the associated sitemap which has all the volumes.
  #collect the list of all the urls, and collect all the years.
  #output collection_id,year
  $url2 = sprintf("https://www.ams.org/clockssdata?p=%s", $collection);
  #printf("$url2\n"); #debug
  my $req2 = HTTP::Request->new(GET, $url2);
  my $resp2 = $ua->request($req2);
  if ($resp2->is_success) {
    #printf("Success2\n"); #debug
    my $man_contents2 = $resp2->content;
    if ($req2->url ne $resp2->request->uri) {
      printf("Redirected from %s\n", $url2);
    #} elsif (defined($man_contents2) && ($man_contents2 =~ m#href=\"/${collection}/[^>]*(\d+)\">#)){
    } elsif (defined($man_contents2) && ($man_contents2 =~ m%/${collection}/[^>]*(\d+)%)){
    #} elsif (defined($man_contents2) && ($man_contents2 =~ m/a/)){
      #printf("Not redirected and has a match.\n");
      #printf("%s\n", $man_contents2); #debug
      #https://www.govinfo.gov/sitemap/CRPT_2001_sitemap.xml
      foreach my $line2 (split(/\n/m, $man_contents2)) {
        if ($line2 =~ m%/(${collection}/[^>]*(\d+))%) {
          printf("https://www.ams.org/$1\n");
        }
      }
    }
  } else {
      printf("--REQ_FAIL--" . $url2 . " " . $resp2->code() . " " . $resp2->message() . "\n");
  }
} 

exit(0);
