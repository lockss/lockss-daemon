#!/usr/bin/perl -w
# This script scrapes the usdocs website to create a complete list of all the available AUs, not including the federal courts

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

# Create user agent.
my $ua = LWP::UserAgent->new;
#User Agent
    $ua->agent('Mozilla/8.0');
# Cookies. Don't save cookies from run to run
    $ua->cookie_jar(HTTP::Cookies->new());

#tdbout -Dc param[collection_id],year tdb/usdocspln/united_states_government_publishing_office.fdsys.tdb > tmp/list1a
#tdbout -MTYRc param[collection_id],year tdb/usdocspln/united_states_government_publishing_office.tdb >> tmp/list1a
#cat tmp/list1a | sort  > tmp/list1b
#cat tmp/list1b | uniq > tmp/list1c
#echo "Duplicate AUs in previous and new tdb files."
#comm -23 tmp/list1b tmp/list1c
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
  #printf("$url2\n");
  #printf("%s\n",$url2);
  my $req2 = HTTP::Request->new(GET, $url2);
  my $resp2 = $ua->request($req2);
  if ($resp2->is_success) {
    #printf("Success2\n");
    my $man_contents2 = $resp2->content;
    if ($req2->url ne $resp2->request->uri) {
      printf("Redirected from %s\n", $url2);
    } elsif (defined($man_contents2) && ($man_contents2 =~ m/a/)){
      #printf("Not redirected and has a match.\n");
      #https://www.govinfo.gov/sitemap/CRPT_2001_sitemap.xml
      foreach my $line2 (split(/\n/m, $man_contents2)) {
        if ($line2 =~ m#sitemap/${collection}_(\d+)_sitemap\.xml#) {
          printf("%s,$1\n", $collection);
        }
      }
    }
  } else {
      printf("--REQ_FAIL--" . $url2 . " " . $resp2->code() . " " . $resp2->message() . "\n");
  }
} 

#
#Then output the collection name (e.g. BILLS) and year (e.g. 2001), comma seperated
#sort, output list2
#*************************************
#Compare the two lists
#Output the items in list2 that are unique 
#
exit(0);
