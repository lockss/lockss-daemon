#!/usr/bin/perl -w

# PERL script to check manifest pages for existence and formatting.
#
# usage: $./scripts/tdb/tdbout --exists --expected --auid tdb/prod/bioscientifica.tdb | scripts/tdb/read_auid_new.pl
#
# Inputs: a list of auids (often piped from tdbout)
# Outputs: a line for each auid
#   <RESULT>, <AU Name>, <auid>, <manifest url>
#   and totals statistics
#   <Time>
#   <Total proper manifest pages found>
#   <Total missing manifest pages>
#   <Total auids that were not found>  (likely need to add the 'if' block in this file)

use URI::Escape;
use Getopt::Long;
use LWP::UserAgent;
use LWP::Protocol::https;
use HTTP::Request;
use HTTP::Cookies;
use HTML::Entities;
use utf8;
use Encode qw(decode encode);
push(@LWP::Protocol::http::EXTRA_SOCK_OPTS, MaxLineLength => 16*1024); #For Hindawi header is too long

my $lockss_tag  = "LOCKSS system has permission to collect, preserve, and serve this Archival Unit";
my $oa_tag      = "LOCKSS system has permission to collect, preserve, and serve this open access Archival Unit";
my $clockss_tag = "CLOCKSS system has permission to ingest, preserve, and serve this Archival Unit";
my $cc_license_tag = "rel..license";
my $cc_license_url = "href=\"https?://creativecommons\.org/licenses/(by|by-sa|by-nc|by-nd|by-nc-sa|by-nc-nd)/(1\.0|2\.0|2\.5|3\.0|4\.0)/?.";
my $cc_by_tag = "href=\"https?://creativecommons.org/licenses/by";
my $bmc_tag = "<span>Archive</span>";
my $bmc2_tag = "<span>Issues</span>";
my $igi_tag = "/gateway/issue/";
my $igi_book_tag = "/gateway/chapter/full-text";
  my $au_type = "";
  my $au_issn = "";
  my $au_eissn = "";
  my $au_year = "";
  my $au_title = "";
my $total_manifests = 0;
my $total_missing = 0;
my $total_missing_plugin = 0;
my $datestring = localtime();
my $my_year = 1900 + (localtime)[5];

# Set up "cookie jar" to hold session cookies for Web access.
# Don't save these cookies from run to run.
my $cjar = HTTP::Cookies->new();

# Create user agent.
my $ua = LWP::UserAgent->new( cookie_jar => $cjar, agent => "LOCKSS cache", ssl_opts => { verify_hostname => 0 } );
$ua->proxy('http', 'http://proxy.lockss.org:3128/');
$ua->no_proxy('localhost', '127.0.0.1');

#Fetch IP address
my $ip_address = "NO_IP_ADDRESS";
my $ip_url = "https://ipecho.net/plain";
my $ip_req = HTTP::Request->new(GET, $ip_url);
my $ip_resp = $ua->request($ip_req);
if ($ip_resp->is_success) {
  $ip_address = $ip_resp->content;
}

while (my $line = <>) {
  chomp $line;
  my $auid_long = $line;
  my @input_strings = split(/\t/, $line); #either 1 element (auid) or 6 (auid,type,issn,eissn,year,title)
  my $num_values = int(@input_strings); #either 1 or 6
  if ($num_values == 6) {
    $auid_long = $input_strings[0];
    $au_type = $input_strings[1];
    $au_issn = $input_strings[2];
    $au_eissn = $input_strings[3];
    $au_year = $input_strings[4];
    $au_title = $input_strings[5];
    $au_title =~ s/ & / and /;
    $au_title = lc($au_title);
    $au_title =~ s/^the //;
    #printf("  au_title:%s   au_year:%s\n", $au_title, $au_year); #debug

#    printf("*Elements: %s %s %s %s %s\n", $au_type, $au_issn, $au_eissn, $au_year, $au_title); #debug
  }
  my @input_rec = split(/\|/, $input_strings[0]); #split the plugin path elements
  my $num_elements = int(@input_rec); #number of path elements plus 1 (the auid and all the parameters)
  my $auid = $input_rec[$num_elements-1]; #capure the auid without the path.
  my @auid_rec = split(/\&/, $auid); #split the parameters
  my $plugin = shift(@auid_rec); #capure the plugin name without the path.
  #Create an array for the parameters
  my %param = ();
  foreach my $param_entry (@auid_rec) {
    if ($param_entry =~ m/^([^\~]+)\~([^\~]+)$/) {
      $param{$1} = $2;
    }
  }
  my $url = "cant_create_url";
  my $url_p = "cant_create_url";
  my $url_s = "cant_create_url";
  my $url_d = "cant_create_url";
  my $url_e = "cant_create_url";
  my $url_de = "cant_create_url";
  my $man_url = "cant_create_url";
  my $man_url_p = "cant_create_url";
  my $man_url_s = "cant_create_url";
  my $man_url_d = "cant_create_url";
  my $man_url_e = "cant_create_url";
  my $man_url_de = "cant_create_url";
  my $vol_title = "NO TITLE FOUND";
  my $result = "Plugin Unknown";

#    printf("*Elements: %s %s %s %s %s\n", $au_type, $au_issn, $au_eissn, $au_year, $au_title); #debug

  #if ($plugin eq "HighWirePressH20Plugin" || $plugin eq "HighWirePressPlugin") {
  if ($plugin eq "HighWirePressH20Plugin") {
    $url = sprintf("%slockss-manifest/vol_%s_manifest.dtl",
      $param{base_url}, $param{volume_name});
    $url_d = sprintf("%slockss-manifest/vol_%s_manifest.html",
      $param{base_url}, $param{volume_name});
    $man_url = uri_unescape($url);
    $man_url_d = uri_unescape($url_d);
    $base_url_short = substr(uri_unescape($param{base_url}), 0, -1);
    #printf("%s\n", $base_url_short);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      #printf("Req:%s\nResp:%s\nurl_d:%s\n",$req->url,$resp->request->uri,$man_url_d);
      if ($req->url ne $resp->request->uri) {
        $vol_title = $resp->request->uri;
        if ($resp->request->uri eq $man_url_d) {
          #printf("Uses Drupal plugin\n");
          $result = "Moved_to_Drupal";
        } else {
          #printf("Doesn't use Drupal plugin\n");
          $result = "Redirected";
        }
        #$result = "Redirected";
    } elsif (($man_contents !~ m/body.*="$base_url_short[^"]*"/s) && ($man_contents !~ m/body.*="\/content\/.*"/s)) { #"
   #} elsif (($man_contents !~ m/body.*="$base_url_short[^"]*"/s) || ($man_contents !~ m/body.*="https?:\/\/.*"/s)) { #"
        #manifest page issue urls (after "body") must contain urls which start with the same characters as the manifest url
        #or must be a relative url, in which case, the link would not start with https://.
        if ($man_contents =~ m/<a\s*href="([^"]*)">/si) { #"
              $vol_title = $1;
        }
        $result = "--BAD_ISS_URL--";
      } elsif (defined($man_contents) && (($man_contents =~ m/\/cgi\/reprint\/$param{volume_name}\//) || ($man_contents =~ m/$base_url_short" lockss-probe/))) { #"
          $result = "CGI_probe_link";
          if ($man_contents =~ m/<title>\s*(.*)\s+C?LOCKSS\s+Manifest\s+Page.*<\/title>/si) {
            $vol_title = $1;
            $vol_title =~ s/\s*\n\s*/ /g;
            if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
              $vol_title = "\"" . $vol_title . "\"";
            }
          }
      } elsif (defined($man_contents) && (($man_contents =~ m/$lockss_tag/) || ($man_contents =~ m/$oa_tag/)) && (($man_contents =~ m/\/content\/$param{volume_name}\//) || ($man_contents =~ m/\/content\/vol$param{volume_name}\//))) {
        $result = "Manifest";
        if ($man_contents =~ m/<title>\s*(.*)\s+C?LOCKSS\s+Manifest\s+Page.*<\/title>/si) {
          $vol_title = $1;
          $vol_title =~ s/\s*\n\s*/ /g;
          if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
            $vol_title = "\"" . $vol_title . "\"";
          }
        }
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    }
    sleep(4);

  #} elsif ($plugin eq "ClockssHighWirePressH20Plugin" || $plugin eq "ClockssHighWirePlugin") {
  } elsif ($plugin eq "ClockssHighWirePressH20Plugin") {
        $url = sprintf("%sclockss-manifest/vol_%s_manifest.dtl",
            $param{base_url}, $param{volume_name});
        $url_d = sprintf("%sclockss-manifest/vol_%s_manifest.html",
            $param{base_url}, $param{volume_name});
        $man_url = uri_unescape($url);
        $man_url_d = uri_unescape($url_d);
        $base_url_short = substr(uri_unescape($param{base_url}), 0, -1);
        my $req = HTTP::Request->new(GET, $man_url);
        my $resp = $ua->request($req);
        if ($resp->is_success) {
            my $man_contents = $resp->content;
            if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              if ($resp->request->uri eq $man_url_d) {
                #printf("Uses Drupal plugin\n");
                $result = "Moved_to_Drupal";
              } else {
                #printf("Doesn't use Drupal plugin\n");
                $result = "Redirected";
              }
              #$result = "Redirected";
    } elsif (($man_contents !~ m/body.*="$base_url_short[^"]*"/s) && ($man_contents !~ m/body.*="\/content\/.*"/s)) { #"
        #manifest page issue urls (after "body") must contain urls which start with the same characters as the manifest url
        #or must be a relative url, in which case, the link would not start with https://.
        if ($man_contents =~ m/<a\s*href="([^"]*)">/si) { #"
              $vol_title = $1;
        }
        $result = "--BAD_ISS_URL--";
            } elsif (defined($man_contents) && (($man_contents =~ m/\/cgi\/reprint\/$param{volume_name}\//) || ($man_contents =~ m/$base_url_short" lockss-probe/))) { #"
                $result = "CGI_probe_link";
                if ($man_contents =~ m/<title>\s*(.*)\s+C?LOCKSS\s+Manifest\s+Page.*<\/title>/si) {
                    $vol_title = $1;
                    $vol_title =~ s/\s*\n\s*/ /g;
                    if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
                        $vol_title = "\"" . $vol_title . "\"";
                    }
                }
            }  elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/) && (($man_contents =~ m/\/content\/$param{volume_name}\//) || ($man_contents =~ m/\/content\/vol$param{volume_name}\//))) {
                $result = "Manifest";
                if ($man_contents =~ m/<title>\s*(.*)\s+C?LOCKSS\s+Manifest\s+Page.*<\/title>/si) {
                    $vol_title = $1;
                    $vol_title =~ s/\s*\n\s*/ /g;
                    if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
                        $vol_title = "\"" . $vol_title . "\"";
                    }
                }
            } else {
                $result = "--NO_TAG--"
            }
        } else {
            $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
        }
        sleep(4);

#GLN HighWire HW Drupal
  } elsif (($plugin =~ m/^(?!Clockss).+DrupalPlugin/) ||
           ($plugin eq AsnJournalsPlugin)) {
    $url = sprintf("%slockss-manifest/vol_%s_manifest.html",
        $param{base_url}, $param{volume_name});
        #printf("********************\n");  #debug
        #printf("url=%s\n",$url);  #debug
    $man_url = uri_unescape($url);
    #printf("man_url=%s\n",$man_url);  #debug
    $base_url_short = substr(uri_unescape($param{base_url}), 0, -1);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    #print($resp->headers_as_string); #debug
    my $man_contents = $resp->is_success ? $resp->content : "";
    if (! $resp->is_success) {
        $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    } elsif ($req->url ne $resp->request->uri) {
        $vol_title = $resp->request->uri;
        $result = "Redirected";
    } elsif (! defined($man_contents)) {
        $result = "--NOT_DEF--";
    } elsif ($man_contents !~ m/$lockss_tag/) {
        $result = "--NO_TAG--";
    } elsif (($man_contents !~ m/body.*="$base_url_short[^"]*"/s) || ($man_contents !~ m/body.*="https?:\/\/.*"/s)) { #"
        #manifest page issue urls (after "body") must contain urls which start with the same characters as the manifest url
        #or must be a relative url, in which case, the link would not start with https://.
        if ($man_contents =~ m/<a\s*href="([^"]*)">/si) { #"
              $vol_title = $1;
        }
        $result = "--BAD_ISS_URL--";
    } elsif (($man_contents =~ m/\/cgi\/reprint\/$param{volume_name}\//) || ($man_contents =~ m/$base_url_short" lockss-probe/)) { #"
        $result = "CGI_probe_link";
    } elsif (($man_contents =~ m/\/content\/$param{volume_name}\//) || ($man_contents =~ m/\/content\/vol$param{volume_name}\//)) {
        #Collect title
        if ($man_contents =~ m/<title>\s*(.*)\s+C?LOCKSS\s+Manifest\s+Page.*<\/title>/si) {
            $vol_title = $1;
            $vol_title =~ s/\s*\n\s*/ /g;
            if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
                $vol_title = "\"" . $vol_title . "\"";
            }
        }
        #Test probe link
        if ($man_contents !~ m/="([^"]*)" lockss-probe="true"/si) { #"
            $result = "MissingProbeLink";
        } else {
            my $pl_url = $1;
            my $req_pl = HTTP::Request->new(GET, $pl_url);
            my $resp_pl = $ua->request($req_pl);
            #print("Result:$result");
            if ($pl_url eq "") {
                $result = "EmptyProbeLink";
            } elsif (substr($pl_url,0,12) ne substr($man_url,0,12)) {
                #print substr($pl_url,0,11);
                #print substr($man_url,0,11);
                $result = "Base-Probe-Mismatch";
                $vol_title = $pl_url;
            } elsif ($pl_url !~ m/\/$param{volume_name}\//) {
                #print ("Probe_url:$pl_url");
                #print ("Volume:$param{volume_name}");
                $result = "ProbeLink-BadVol";
                $vol_title = $pl_url;
            } elsif (! $resp_pl->is_success) {
                $result = "BadProbeLink-" . $resp_pl->code() . " " . $resp_pl->message();
                $vol_title = $pl_url;
            } elsif ($req_pl->url ne $resp_pl->request->uri) {
                $result = "ProbeLinkRedirect";
                $vol_title = $pl_url;
            } else {
                $result = "Manifest";
            }
        }
    } else {
        if ($man_contents =~ m/<a HREF="">/) {
            $result = "--EMPTY_ISSUE_URL--";
        } else {
            $result = "--UNKNOWN--";
        }
    }
    sleep(4);

#CLOCKSS Highwire HW Drupal
  } elsif ($plugin =~ m/^Clockss.+DrupalPlugin/) {
    $url = sprintf("%sclockss-manifest/vol_%s_manifest.html",
        $param{base_url}, $param{volume_name});
        #printf("********************\n");  #debug
        #printf("url=%s\n",$url);  #debug
    $man_url = uri_unescape($url);
    #printf("man_url=%s\n",$man_url);  #debug
    $base_url_short = substr(uri_unescape($param{base_url}), 0, -1);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    #print($resp->headers_as_string); #debug
    my $man_contents = $resp->is_success ? $resp->content : "";
    if (! $resp->is_success) {
        $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    } elsif ($req->url ne $resp->request->uri) {
        $vol_title = $resp->request->uri;
        $result = "Redirected";
    } elsif (! defined($man_contents)) {
        $result = "--NOT_DEF--";
    } elsif ($man_contents !~ m/$clockss_tag/) {
        $result = "--NO_TAG--";
    } elsif (($man_contents !~ m/body.*="$base_url_short[^"]*"/s) || ($man_contents !~ m/body.*="https?:\/\/.*"/s)) { #"
        #manifest page issue urls (after "body") must contain urls which start with the same characters as the manifest url
        #or must be a relative url, in which case, the link would not start with https://.
        if ($man_contents =~ m/<a\s*href="([^"]*)">/si) { #"
              $vol_title = $1;
        }
        $result = "--BAD_ISS_URL--";
    } elsif (($man_contents =~ m/\/cgi\/reprint\/$param{volume_name}\//) || ($man_contents =~ m/$base_url_short" lockss-probe/)) { #"
        $result = "CGI_probe_link";
    } elsif (($man_contents =~ m/\/content\/$param{volume_name}\//) || ($man_contents =~ m/\/content\/vol$param{volume_name}\//)) {
        #Collect title
        if ($man_contents =~ m/<title>\s*(.*)\s+C?LOCKSS\s+Manifest\s+Page.*<\/title>/si) {
            $vol_title = $1;
            $vol_title =~ s/\s*\n\s*/ /g;
            if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
                $vol_title = "\"" . $vol_title . "\"";
            }
        }
        #Test probe link
        if ($man_contents !~ m/="([^"]*)" lockss-probe="true"/si) { #"
            $result = "MissingProbeLink";
        } else {
            my $pl_url = $1;
            my $req_pl = HTTP::Request->new(GET, $pl_url);
            my $resp_pl = $ua->request($req_pl);
            #print("Result:$result");
            if ($pl_url eq "") {
                $result = "EmptyProbeLink";
            } elsif (substr($pl_url,0,12) ne substr($man_url,0,12)) {
                #print substr($pl_url,0,11);
                #print substr($man_url,0,11);
                $result = "Base-Probe-Mismatch";
                $vol_title = $pl_url;
            } elsif ($pl_url !~ m/\/$param{volume_name}\//) {
                #print ("Probe_url:$pl_url");
                #print ("Volume:$param{volume_name}");
                $result = "ProbeLink-BadVol";
                $vol_title = $pl_url;
            } elsif (! $resp_pl->is_success) {
                $result = "BadProbeLink-" . $resp_pl->code() . " " . $resp_pl->message();
                $vol_title = $pl_url;
            } elsif ($req_pl->url ne $resp_pl->request->uri) {
                $result = "ProbeLinkRedirect";
                $vol_title = $pl_url;
            } else {
                $result = "Manifest";
            }
        }
    } else {
        if ($man_contents =~ m/<a HREF="">/) {
            $result = "--EMPTY_ISSUE_URL--";
        } else {
            $result = "--UNKNOWN--";
        }
    }
    sleep(4);

#GLN Highwire HW Origins & Scolaris
  } elsif (($plugin =~ m/^(?!Clockss).+OriginsPlugin/) || 
           ($plugin =~ m/^(?!Clockss).+ScolarisPlugin/)) {
    $url = sprintf("%scontent/%s/lockss-manifest/vol_%s_manifest.html",
    #$url = sprintf("%slockss-manifest/%s_vol_%s_manifest.html",
        $param{base_url}, $param{journal_id}, $param{volume_name});
        #printf("********************\n");  #debug
        #printf("url=%s\n",$url);  #debug
    $man_url = uri_unescape($url);
    #printf("man_url=%s\n",$man_url);  #debug
    $base_url_short = substr(uri_unescape($param{base_url}), 0, -1);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    #print($resp->headers_as_string); #debug
    my $man_contents = $resp->is_success ? $resp->content : "";
    if (! $resp->is_success) {
        $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    } elsif ($req->url ne $resp->request->uri) {
        $vol_title = $resp->request->uri;
        $result = "Redirected";
    } elsif (! defined($man_contents)) {
        $result = "--NOT_DEF--";
    } elsif ($man_contents !~ m/$lockss_tag/) {
        $result = "--NO_TAG--";
    } elsif (($man_contents !~ m/body.*="$base_url_short[^"]*"/s) || ($man_contents !~ m/body.*="https?:\/\/.*"/s)) { #"
    #} elsif (($man_contents ~ m/main.*="$base_url_short[^"]*".*main>/s) || ($man_contents !~ m/<main.*="https?:\/\/.*".*main>/s)) { #"
        #manifest page issue urls (after "body") must contain urls which start with the same characters as the manifest url
        #or must be a relative url, in which case, the link would not start with https://.
        $result = "--BAD_ISS_URL--";
    } elsif (($man_contents =~ m/\/cgi\/reprint\/$param{volume_name}\//) || ($man_contents =~ m/$base_url_short" lockss-probe/)) { #"
        $result = "CGI_probe_link";
    } elsif ($man_contents =~ m/\/content\/$param{journal_id}\/$param{volume_name}\//) {
        #Collect title
        if ($man_contents =~ m/<title>\s*(.*)\s+C?LOCKSS\s+Manifest\s+Page.*<\/title>/si) {
            $vol_title = $1;
            $vol_title =~ s/\s*\n\s*/ /g;
            if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
                $vol_title = "\"" . $vol_title . "\"";
            }
        }
        #Test probe link
        if ($man_contents !~ m/="([^"]*)" lockss-probe="true"/si) { #"
            $result = "MissingProbeLink";
        } else {
            my $pl_url = $1;
            my $req_pl = HTTP::Request->new(GET, $pl_url);
            my $resp_pl = $ua->request($req_pl);
            #print("Result:$result");
            if ($pl_url eq "") {
                $result = "EmptyProbeLink";
            } elsif (substr($pl_url,0,12) ne substr($man_url,0,12)) {
                #print substr($pl_url,0,11);
                #print substr($man_url,0,11);
                $result = "Base-Probe-Mismatch";
                $vol_title = $pl_url;
            } elsif ($pl_url !~ m/\/$param{volume_name}\//) {
                #print ("Probe_url:$pl_url");
                #print ("Volume:$param{volume_name}");
                $result = "ProbeLink-BadVol";
                $vol_title = $pl_url;
            } elsif (! $resp_pl->is_success) {
                $result = "BadProbeLink-" . $resp_pl->code() . " " . $resp_pl->message();
                $vol_title = $pl_url;
            } elsif ($req_pl->url ne $resp_pl->request->uri) {
                #print ("req:$req_pl->url\n");
                #print ("resp:$resp_pl->request->uri\n");
                $result = "ProbeLinkRedirect";
                $vol_title = $resp_pl->request->uri;
            } else {
                $result = "Manifest";
            }
        }
    } else {
        if ($man_contents =~ m/<a HREF="">/) {
            $result = "--EMPTY_ISSUE_URL--";
        } else {
            $result = "--UNKNOWN--";
        }
    }
    sleep(4);

#CLOCKSS HighWire HW Origins & Scolaris
  } elsif (($plugin =~ m/^Clockss.+OriginsPlugin/) || ($plugin =~ m/^Clockss.+ScolarisPlugin/)) {
    $url = sprintf("%scontent/%s/clockss-manifest/vol_%s_manifest.html",
    #$url = sprintf("%sclockss-manifest/%s_vol_%s_manifest.html",
        $param{base_url}, $param{journal_id}, $param{volume_name});
        #printf("********************\n");  #debug
        #printf("url=%s\n",$url);  #debug
    $man_url = uri_unescape($url);
    #printf("man_url=%s\n",$man_url);  #debug
    $base_url_short = substr(uri_unescape($param{base_url}), 0, -1);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    #print($resp->headers_as_string); #debug
    my $man_contents = $resp->is_success ? $resp->content : "";
    if (! $resp->is_success) {
        $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    } elsif ($req->url ne $resp->request->uri) {
        $vol_title = $resp->request->uri;
        $result = "Redirected";
    } elsif (! defined($man_contents)) {
        $result = "--NOT_DEF--";
    } elsif ($man_contents !~ m/$clockss_tag/) {
        $result = "--NO_TAG--";
    } elsif (($man_contents !~ m/body.*="$base_url_short[^"]*"/s) || ($man_contents !~ m/body.*="https?:\/\/.*"/s)) { #"
    #} elsif (($man_contents !~ m/main.*="$base_url_short[^"]*"/s) || ($man_contents !~ m/main.*="https?:\/\/.*"/s)) { #"
    #} elsif (($man_contents !~ m/<main.*="$base_url_short[^"]*".*main>/s) || ($man_contents !~ m/<main.*="https?:\/\/.*".*main>/s)) { #"
    #} elsif (($man_contents !~ m/<main.*="\/content\/$param{journal_id}\/$param{volume_name}\/.*main>/s)) { #"
    #} elsif (($man_contents ~ m/main.*="$base_url_short[^"]*".*main>/s) || ($man_contents !~ m/<main.*="https?:\/\/.*".*main>/s)) { #"
        #manifest page issue urls (after "body") must contain urls which start with the same characters as the manifest url
        #or must be a relative url, in which case, the link would not start with https://.
        $result = "--BAD_ISS_URL--";
    } elsif (($man_contents =~ m/\/cgi\/reprint\/$param{volume_name}\//) || ($man_contents =~ m/$base_url_short" lockss-probe/)) { #"
        $result = "CGI_probe_link";
    } elsif ($man_contents =~ m/\/content\/$param{journal_id}\/$param{volume_name}\//) {
        #Collect title
        if ($man_contents =~ m/<title>\s*(.*)\s+C?LOCKSS\s+Manifest\s+Page.*<\/title>/si) {
            $vol_title = $1;
            $vol_title =~ s/\s*\n\s*/ /g;
            if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
                $vol_title = "\"" . $vol_title . "\"";
            }
        }
        #Test probe link
        if ($man_contents !~ m/="([^"]*)" lockss-probe="true"/si) { #"
            $result = "MissingProbeLink";
        } else {
            my $pl_url = $1;
            my $req_pl = HTTP::Request->new(GET, $pl_url);
            my $resp_pl = $ua->request($req_pl);
            #print("Result:$result");
            if ($pl_url eq "") {
                $result = "EmptyProbeLink";
            } elsif (substr($pl_url,0,12) ne substr($man_url,0,12)) {
                #print substr($pl_url,0,11);
                #print substr($man_url,0,11);
                $result = "Base-Probe-Mismatch";
                $vol_title = $pl_url;
            } elsif ($pl_url !~ m/\/$param{volume_name}\//) {
                #print ("Probe_url:$pl_url");
                #print ("Volume:$param{volume_name}");
                $result = "ProbeLink-BadVol";
                $vol_title = $pl_url;
            } elsif (! $resp_pl->is_success) {
                $result = "BadProbeLink-" . $resp_pl->code() . " " . $resp_pl->message();
                $vol_title = $pl_url;
            } elsif ($req_pl->url ne $resp_pl->request->uri) {
                print ("req_pl->url:$req_pl->url");
                print ("resp_pl->request->uri:$resp_pl->request->uri");
                $result = "ProbeLinkRedirect";
                #$vol_title = $pl_url;
                $vol_title = $resp_pl->request->uri;
            } else {
                $result = "Manifest";
            }
        }
    } else {
        if ($man_contents =~ m/<a HREF="">/) {
            $result = "--EMPTY_ISSUE_URL--";
        } else {
            $result = "--UNKNOWN--";
        }
    }
    sleep(4);

  } elsif ($plugin eq "ProjectMuse2017Plugin") {
    $url = sprintf("%slockss?vid=%s",
      $param{base_url}, $param{resource_id});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if (defined($man_contents) && (($man_contents =~ m/$lockss_tag/) || ($man_contents =~ m/$oa_tag/)) && ($man_contents =~ m/href=\"\/issue\//) && ($man_contents !~ "No issues available.")) {
        if ($man_contents =~ m/<h1>(.*)<\/h1>/si) {
          $vol_title = $1;
          if ($man_contents =~ m/<h2>(.*)<\/h2>/si) {
            $vol_title = $vol_title . " " . $1;
          }
          $vol_title =~ s/\s*\n\s*/ /g;
          $vol_title =~ s/,//;
        }
        $result = "Manifest"
      } elsif ($man_contents !~ m/$lockss_tag/) {
        $result = "--NO_TAG--"
      } elsif ($man_contents =~ m/No issues available/ || $man_contents !~ m/href=\"\/issue\//){
        $result = "--NO_ISSUES--"
      } else {
        $result = "--OTHER_ERROR--"
      }
    
  } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
  }
      sleep(4);

  } elsif ($plugin eq "ClockssLSUIAJournalPlugin") {
    $url = sprintf("%sen/archive/%d",
      $param{base_url}, $param{year});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);

    $url = sprintf("%sen", 
      $param{base_url});
    $per_url = uri_unescape($url);
    my $per_req = HTTP::Request->new(GET, $per_url);
    my $per_resp = $ua->request($per_req);

    if ($resp->is_success) {
      my $man_contents = $resp->content;
      my $per_contents = $per_resp->content;
      #<h3> <a href="/en/journals/tom-6-4-2023"> Vol. 6, No. 4, 2023 </a> </h3>
      if (defined($man_contents) && defined($man_contents) && ($per_contents =~ m/$clockss_tag/) && ($man_contents =~ m/href=\"\/en\/journals\/tom-$param{volume_name}-\d*-$param{year}\"/)) {
        #if ($man_contents =~ m/<h1>(.*)<\/h1>/si) {
        if ($man_contents =~ m/<h1>([^<]*)<\/h1>/si) {
          $vol_title = $1;
          if ($man_contents =~ m/<h2>([^<]*)<\/h2>/si) {
            $vol_title = $vol_title . " " . $1;
          }
          $vol_title =~ s/\s*\n\s*/ /g;
          $vol_title =~ s/,//;
        }
        $result = "Manifest"
      } elsif ($man_contents !~ m/$clockss_tag/) {
        $result = "--NO_TAG--"
      } elsif ($man_contents !~ m/href=\"\/en\/journals\/tom-$param{volume_name}-\d*-$param{year}\"/){
        $result = "--NO_ISSUES--"
      } else {
        $result = "--OTHER_ERROR--"
      }
    
  } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
  }
      sleep(4);

  } elsif ($plugin eq "GovInfoSitemapsPlugin") {
      #https://www.govinfo.gov/sitemap/USCODE_2001_sitemap.xml
      $url = sprintf("%ssitemap/%s_%d_sitemap.xml",
      $param{base_url}, $param{collection_id}, $param{year});
      $man_url = uri_unescape($url);
      my $req = HTTP::Request->new(GET, $man_url);
      my $resp = $ua->request($req);
      if ($resp->is_success) {
          my $man_contents = $resp->content;
          if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
          } elsif (defined($man_contents) && (($man_contents =~ m/$lockss_tag/) || ($man_contents =~ m/$oa_tag/)) && ($man_contents =~ m#https://www\.govinfo\.gov/#)) {
              $vol_title = $param{collection_id};
              $result = "Manifest"
          } else {
              $result = "--NO_TAG--"
          }
      } else {
          $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
      }
      sleep(5);

  } elsif ($plugin eq "BioOne2020Plugin") {
      $url = sprintf("%sjournals/%s/issues/%d",
          $param{base_url}, $param{journal_id}, $param{year});
      $man_url = uri_unescape($url);
      my $req = HTTP::Request->new(GET, $man_url);
      my $resp = $ua->request($req);
      if ($resp->is_success) {
          my $man_contents = $resp->content;
          if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
          } elsif ($man_contents !~ m/\"JournalsBrowseSmallHeader\">$param{year}/) {
              $vol_title = "Non-existant-year?";
              $result = "Redirected";
          } elsif (defined($man_contents) && ($man_contents =~ m/$lockss_tag/)) {
              if ($man_contents =~ m/<title>\s*(.*)\s*<\/title>/si) {
                  $vol_title = $1;
              }
              #if ($man_contents =~ m/\/articles\//) {
              #if ($man_contents =~ m/\/volume\/.+\/issue\//) {
              if ($man_contents =~ m/journals\/$param{journal_id}\/volume-/) {
                  $result = "Manifest";
              } else {
                  $result = "--NO_CONT--";
              }
          } else {
              $result = "--NO_TAG--";
          }
      } else {
          $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
      }
      sleep(5);

  } elsif ($plugin eq "UbiquityPartnerNetworkPlugin") {
      ####start url & permission url in one
      $url = sprintf("%slockss/year/%d/",
          $param{base_url}, $param{year});
      $man_url = uri_unescape($url);
      my $req = HTTP::Request->new(GET, $man_url);
      my $resp = $ua->request($req);
      if ($resp->is_success) {
          my $man_contents = $resp->content;
          if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
          } elsif (defined($man_contents) && ($man_contents =~ m/$lockss_tag/)) {
              if ($man_contents =~ m/<title>\s*(.*)\s*<\/title>/si) {
                  $vol_title = $1;
              }
              #if ($man_contents =~ m/\/articles\//) {
              #if ($man_contents =~ m/\/volume\/.+\/issue\//) {
              if ($man_contents =~ m/\/volume\//) {
                  $result = "Manifest";
              } else {
                  $result = "--NO_CONT--";
              }
          } else {
              $result = "--NO_TAG--";
          }
      } else {
          $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
      }
      sleep(5);

# See instead the OJS3 MPC
#  } elsif ($plugin eq "ClockssUbiquityPartnerNetworkPlugin") {
#      ####start url only
#      $url = sprintf("%slockss/year/%d/",
#          $param{base_url2}, $param{year});
#      $man_url = uri_unescape($url);
#      my $req = HTTP::Request->new(GET, $man_url);
#      my $resp = $ua->request($req);
#      ####permission url only
#      $url_p = sprintf("%sabout",
#          $param{base_url});
#      $man_url_p = uri_unescape($url_p);
#      my $req_p = HTTP::Request->new(GET, $man_url_p);
#      my $resp_p = $ua->request($req_p);
#      if ($resp->is_success) {
#          my $man_contents_p = $resp_p->content;
#          if ($req->url ne $resp->request->uri) {
#              $vol_title = $resp->request->uri;
#              $result = "Redirected";
#          } elsif (defined($man_contents_p) && ($man_contents_p =~ m/$clockss_tag/)) {
#              if ($man_contents_p =~ m/<title>\s*(.*)\s*<\/title>/si) {
#                  $vol_title = $1;
#              }
#              my $man_contents = $resp->content;
#              #printf("$man_contents");
#              #if (defined($man_contents) && ($man_contents =~ m/\/volume\//) && ($man_contents =~ m/\($param{year}\)/)) {
#              if (defined($man_contents) && (($man_contents =~ m/\/volume\//) || ($man_contents =~ m/\/articles\//)) && ($man_contents =~ m/$param{year}/)) {
#              #if (defined($man_contents)) {
#                  $result = "Manifest";
#              } else {
#                  $result = "--NO_CONT--";
#              }
#          } else {
#              $result = "--NO_TAG--";
#          }
#      } else {
#          $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
#      }
#      sleep(5);
#
  } elsif ($plugin eq "UbiquityPartnerNetworkBooksPlugin" ||
           $plugin eq "ClockssUbiquityPartnerNetworkBooksPlugin") {
    $url = sprintf("%ssite/books/%s/",
      $param{base_url}, $param{book_doi});
    $book_doi_short = uri_unescape($param{book_doi});
    #$book_doi_short =~ s/^..//; only use this if the book_doi starts with m/ or e/
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    my $man_contents = $resp->is_success ? $resp->content : "";
    if (! $resp->is_success) {
        $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    #start_url redirects to a url with an m or an e. So only check first 20 characters. substr($string,0,19)
    } elsif (substr($req->url,0,19) ne substr($resp->request->uri,0,19)) {
        $vol_title = $resp->request->uri;
        $result = "Redirected";
    } elsif (! defined($man_contents)) {
        $result = "--NOT_DEF--";
    } elsif ($man_contents !~ m/$cc_license_tag/ || $man_contents !~ m/$cc_license_url/) {
        $result = "--NO_TAG--";
    } elsif (($man_contents =~ m/<h1>\s*(\S.*\S)\s*<\/h1>/si) || ($man_contents =~ m/<title>\s*(\S.*\S)\s*<\/title>/si)) {
        #Collect title
        if ($man_contents =~ m/<h1>\s*(\S.*\S)\s*<\/h1>/si) {
            $vol_title = $1;
        }
        if ($man_contents =~ m/<title>\s*(\S.*\S)\s*<\/title>/si) {
            $vol_title = $1 . ". " . $vol_title;
        }
        #Test for link to book
        if ($man_contents =~ m/\/site\/books\/$book_doi_short\/read\// or $man_contents =~ m/\/site\/books\/$book_doi_short\/download\//) {
            #for example /site/books/10.21435/sff.10/read/ some books use /site/books/10.21525/kriterium.2/download/1252/
            $result = "Manifest";
        } else {
            $result = "--NO_CONT--";
        }
    }
    sleep(5);

  } elsif (($plugin eq "RocksBackpagesPlugin")) {
    $url = sprintf("%sLibrary/Publication/%s?year=%d", 
      $param{base_url}, $param{journal_id}, $param{year});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    my $man_contents = $resp->is_success ? $resp->content : "";
    if (! $resp->is_success) {
        $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    } elsif ($req->url ne $resp->request->uri) {
        $vol_title = $resp->request->uri;
        $result = "Redirected";
    } elsif (! defined($man_contents)) {
        $result = "--NOT_DEF--";
    } elsif ($man_contents !~ m/$oa_tag/si && $man_contents !~ m/$lockss_tag/si) {
        $result = "--NO_TAG--";
    #Test for link to chapter
    } elsif ($man_contents =~ m/$param{year}/) {
        #Collect title
        $result = "Manifest";
    } else {
        $result = "--NO_CONT--";
    }
  
  sleep(5);

  } elsif (($plugin eq "ClockssPLoSPlugin")) {
    $url = sprintf("%s%s/lockss-manifest",
      $param{base_url}, $param{journal_id});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    my $man_contents = $resp->is_success ? $resp->content : "";
    if (! $resp->is_success) {
        $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    } elsif ($req->url ne $resp->request->uri) {
        $vol_title = $resp->request->uri;
        $result = "Redirected";
    } elsif (! defined($man_contents)) {
        $result = "--NOT_DEF--";
    } elsif ($man_contents !~ m/$oa_tag/si && $man_contents !~ m/$clockss_tag/si) {
        $result = "--NO_TAG--";
    #Test for link to chapter
    } elsif ($man_contents =~ m/$param{year}/) {
        #Collect title
        $vol_title = $param{journal_id} . " " . $param{year};
        $result = "Manifest";
    } else {
        $result = "--NO_CONT--";
    }
  
  sleep(5);

  } elsif (($plugin eq "ClockssPLoSMonthlyPlugin")) {
    $url = sprintf("%s%s/lockss-manifest/vol_%s",
      $param{base_url}, $param{journal_id}, $param{year});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    my $man_contents = $resp->is_success ? $resp->content : "";
    if (! $resp->is_success) {
        $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    } elsif ($req->url ne $resp->request->uri) {
        $vol_title = $resp->request->uri;
        $result = "Redirected";
    } elsif (! defined($man_contents)) {
        $result = "--NOT_DEF--";
    } elsif ($man_contents !~ m/$oa_tag/si && $man_contents !~ m/$clockss_tag/si) {
        $result = "--NO_TAG--";
    #Test for link to chapter
    } elsif ($man_contents =~ m/$param{month}/) {
        #Collect title
        $vol_title = $param{journal_id} . " " . $param{year} . " " . $param{month};
        $result = "Manifest";
    } else {
        $result = "--NO_CONT--";
    }
  
  sleep(5);

  } elsif (($plugin eq "Emerald2020BooksPlugin") || 
          ($plugin eq "ClockssEmerald2020BooksPlugin")) {
    $url = sprintf("%sinsight/publication/doi/%s",
      $param{base_url}, $param{book_uri});
    $book_doi_short = uri_unescape($param{book_uri});
    $book_doi_short =~ s/^..//;
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    my $man_contents = $resp->is_success ? $resp->content : "";
    if (! $resp->is_success) {
        $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    } elsif ($req->url ne $resp->request->uri) {
        $vol_title = $resp->request->uri;
        $result = "Redirected";
    } elsif (! defined($man_contents)) {
        $result = "--NOT_DEF--";
    } elsif ($man_contents !~ m/$lockss_tag/si && $man_contents !~ m/$clockss_tag/si) {
        $result = "--NO_TAG--";
    #Test for link to chapter
    } elsif ($man_contents =~ m/\/insight\/content\/doi\/10.1/) {
        #Collect title
        if ($man_contents =~ m/<title>\s*(\S.*\S)\s*<\/title>/si) {
            $vol_title = $1;
        }
        $result = "Manifest";
    } else {
        $result = "--NO_CONT--";
    }
  
  sleep(5);

  } elsif ($plugin eq "OAPENBooksPlugin") {
    $url = sprintf("%s%s",
      $param{base_url}, $param{resource_id});
    #Permission page is same as the start page for GLN
    $book_handle_short = uri_unescape($param{resource_id});
    #printf("book_handle_short: %s\n", $book_handle_short); #debug
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    my $man_contents = $resp->is_success ? $resp->content : "";
    if (! $resp->is_success) {
        $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    } elsif ($req->url ne $resp->request->uri) {
        $vol_title = $resp->request->uri;
        $result = "Redirected";
    } elsif (! defined($man_contents)) {
        $result = "--NOT_DEF--";
    } elsif ($man_contents !~ m/$lockss_tag/si && $man_contents !~ m/$cc_license_url/si) {
        $result = "--NO_TAG--";
    #Test for link to chapter
    #/bitstream/handle/20.500.12657/41485/9783835391154.pdf?  Seeking
    #https://library.oapen.org/handle/20.500.12657/41485  handle
    #} elsif ($man_contents =~ m/\/bitstream\/$book_handle_short\/\d*[^>]*.pdf\?/ || $man_contents =~ m/\/bitstream\/$book_handle_short\/external_content.pdf.jpg\?/ || $man_contents =~ m/\/bitstream\/$book_handle_short\/PUB[^>]*.pdf\?/) {
    } elsif ($man_contents =~ m/\/bitstream\/$book_handle_short\/[^>]*.pdf\?/ || $man_contents =~ m/\/bitstream\/$book_handle_short\/external_content.pdf.jpg\?/) {
        #Collect title
        if ($man_contents =~ m/<title>\s*(\S.*\S)\s*<\/title>/si) {
            $vol_title = $1;
        }
        $result = "Manifest";
    } else {
        $result = "--NO_CONT--";
    }
  
  sleep(5);

  } elsif ($plugin eq "ClockssOAPENBooksPlugin") {
    $url = sprintf("%s%s",
      $param{base_url}, $param{resource_id});
    #Permission page is https://library.oapen.org/robots.txt for CLOCKSS
    $book_handle_short = uri_unescape($param{resource_id});
    #printf("book_handle_short: %s\n", $book_handle_short); #debug
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    my $man_contents = $resp->is_success ? $resp->content : "";
    if (! $resp->is_success) {
        $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    } elsif ($req->url ne $resp->request->uri) {
        $vol_title = $resp->request->uri;
        $result = "Redirected";
    } elsif (! defined($man_contents)) {
        $result = "--NOT_DEF--";
    #Test for link to chapter
    #/bitstream/handle/20.500.12657/41485/9783835391154.pdf?  Seeking
    #https://library.oapen.org/handle/20.500.12657/41485  handle
    #} elsif ($man_contents =~ m/\/bitstream\/$book_handle_short\/\d*[^>]*.pdf\?/ || $man_contents =~ m/\/bitstream\/$book_handle_short\/external_content.pdf.jpg\?/ || $man_contents =~ m/\/bitstream\/$book_handle_short\/PUB[^>]*.pdf\?/) {
    } elsif ($man_contents =~ m/\/bitstream\/$book_handle_short\/[^>]*.pdf\?/ || $man_contents =~ m/\/bitstream\/$book_handle_short\/external_content.pdf.jpg\?/) {
        #Collect title
        if ($man_contents =~ m/<title>\s*(\S.*\S)\s*<\/title>/si) {
            $vol_title = $1;
        }
        $result = "Manifest";
    } else {
        $result = "--NO_CONT--";
    }
  
  sleep(5);

  } elsif ($plugin eq "ClockssIUMJ2018Plugin") {
      ####start url
      #https://www.iumj.indiana.edu/IUMJ/toc.php?writeyear=2017
      $url = sprintf("%sIUMJ/toc.php?writeyear=%d",
          $param{base_url}, $param{year});
      $man_url = uri_unescape($url);
      my $req = HTTP::Request->new(GET, $man_url);
      my $resp = $ua->request($req);
      ####permission url
      $url_p = sprintf("%slockss.txt",
          $param{base_url});
      $man_url_p = uri_unescape($url_p);
      my $req_p = HTTP::Request->new(GET, $man_url_p);
      my $resp_p = $ua->request($req_p);
      if ($resp->is_success) {
          my $man_contents_p = $resp_p->content;
          if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
          } elsif (defined($man_contents_p) && ($man_contents_p =~ m/$clockss_tag/)) {
              my $man_contents = $resp->content;
              if (defined($man_contents) && ($man_contents =~ m/\($param{year}\)/)) {
                  $result = "Manifest";
              } else {
                  $result = "--NO_CONT--";
              }
          } else {
              $result = "--NO_TAG--";
          }
      } else {
          $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
      }
      sleep(5);

  } elsif ($plugin eq "IUMJ2018Plugin") {
      ####start url
      #https://www.iumj.indiana.edu/IUMJ/toc.php?writeyear=2017
      $url = sprintf("%sIUMJ/toc.php?writeyear=%d",
          $param{base_url}, $param{year});
      $man_url = uri_unescape($url);
      my $req = HTTP::Request->new(GET, $man_url);
      my $resp = $ua->request($req);
      ####permission url
      $url_p = sprintf("%slockss.txt",
          $param{base_url});
      $man_url_p = uri_unescape($url_p);
      my $req_p = HTTP::Request->new(GET, $man_url_p);
      my $resp_p = $ua->request($req_p);
      if ($resp->is_success) {
          my $man_contents_p = $resp_p->content;
          if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
          } elsif (defined($man_contents_p) && ($man_contents_p =~ m/$lockss_tag/)) {
              my $man_contents = $resp->content;
              if (defined($man_contents) && ($man_contents =~ m/\($param{year}\)/)) {
                  $result = "Manifest";
              } else {
                  $result = "--NO_CONT--";
              }
          } else {
              $result = "--NO_TAG--";
          }
      } else {
          $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
      }
      sleep(5);

  } elsif ($plugin eq "BePressPlugin") {
      $url = sprintf("%s%s/lockss-volume%d.html",
          $param{base_url}, $param{journal_abbr}, $param{volume});
      $man_url = uri_unescape($url);
      my $req = HTTP::Request->new(GET, $man_url);
      my $resp = $ua->request($req);
      if ($resp->is_success) {
        my $man_contents = $resp->content;
        if ($req->url ne $resp->request->uri) {
          $vol_title = $resp->request->uri;
          $result = "Redirected";
        } elsif (defined($man_contents) && 
             (($man_contents =~ m/$lockss_tag/) || ($man_contents =~ m/$oa_tag/)) &&
              ($man_contents =~ m/$param{journal_abbr}\/vol$param{volume}/)) {
          if ($man_contents =~ m/<title>(.*) --.*<\/title>/si) {
            $vol_title = $1;
            $vol_title =~ s/\s*\n\s*/ /g;
            if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
              $vol_title = "\"" . $vol_title . "\"";
            }
          }
          $result = "Manifest"
        } else {
          $result = "--NO_TAG--"
        }
      } else {
        $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
      }
      sleep(4);

  } elsif ($plugin eq "ClockssBerkeleyElectronicPressPlugin") {
        $url = sprintf("%s%s/lockss-volume%d.html",
            $param{base_url}, $param{journal_abbr}, $param{volume});
        $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && (($man_contents =~ m/$clockss_tag/)) && ($man_contents =~ m/$param{journal_abbr}\/vol$param{volume}/)) {
    if ($man_contents =~ m/<title>(.*) --.*<\/title>/si) {
        $vol_title = $1;
        $vol_title =~ s/\s*\n\s*/ /g;
        if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
      $vol_title = "\"" . $vol_title . "\"";
        }
    }
    $result = "Manifest"
      } else {
    $result = "--NO_TAG--"
      }
  } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
  }
        sleep(4);

#  } elsif ($plugin eq "OJS2Plugin" || $plugin eq "CoActionPublishingPlugin") {
  } elsif ($plugin eq "OJS2Plugin") {
        $url = sprintf("%sindex.php/%s/gateway/lockss?year=%d",
            $param{base_url}, $param{journal_id}, $param{year});
        $man_url = uri_unescape($url);
        # default url if no manifest pages set up.
        $url_d = sprintf("%sindex.php/%s/index",
            $param{base_url}, $param{journal_id});
        $man_url_d = uri_unescape($url_d);
        # default url w/o index.php if no manifest pages set up.
        $url_de = sprintf("%s%s/index",
            $param{base_url}, $param{journal_id});
        $man_url_de = uri_unescape($url_de);
        # no index.php.
        $url_e = sprintf("%s%s/gateway/lockss?year=%d",
            $param{base_url}, $param{journal_id}, $param{year});
        $man_url_e = uri_unescape($url_e);
        #printf("man_url_e: %s\n", $man_url_e);

        my $req = HTTP::Request->new(GET, $man_url);
        my $resp = $ua->request($req);
        if ($resp->is_success) {
            my $man_contents = $resp->content;
            #printf("resp-request-uri: %s\n", $resp->request->uri);
            if (($req->url ne $resp->request->uri && 
               ($resp->request->uri ne $man_url_d && $resp->request->uri ne $man_url_e && $resp->request->uri ne $man_url_de)) &&
                    (($man_contents =~ m/$lockss_tag/) || ($man_contents =~ m/$oa_tag/)) && 
                    (($man_contents =~ m/\($param{year}\)/) || ($man_contents =~ m/: $param{year}/))
               ) {
                $vol_title = $resp->request->uri;
                #$vol_title = $req_m->url . " NOT " . $resp_m->request->uri . " OR " . $req_s->url . " NOT " . $resp_s->request->uri;
                $result = "Redirected";
            } elsif (defined($man_contents) && ($man_contents =~ m/content=\"Open Journal Systems 3\./)) {
                #$vol_title = $resp->request->uri;
                $result = "MOVED_TO_OJS3";
            } elsif (defined($man_contents) && 
                    (($man_contents =~ m/$lockss_tag/) || ($man_contents =~ m/$oa_tag/)) && 
                    (($man_contents =~ m/\($param{year}\)/) || ($man_contents =~ m/: $param{year}/))
                ) {
                if ($man_contents =~ m/<title>([^<>]*)<\/title>/si) {
                    $vol_title = $1;
                    $vol_title =~ s/\s*\n\s*/ /g;
                    if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
                        $vol_title = "\"" . $vol_title . "\"";
                    }
                }
                $result = "Manifest"
            } else {
                #$result = "--NO_TAG--";
                if (!defined($man_contents)) {
                    $result = "--NO_CONT--";
                } elsif (($man_contents !~ m/$lockss_tag/) && ($man_contents !~ m/$oa_tag/)) {
                    $result = "--NO_TAG--";
                } elsif (($man_contents !~ m/\($param{year}\)/) && ($man_contents !~ m/: $param{year}/)) {
                    $result = "--NO_YEAR--";
                } else {
                    $result = "--MYST--";
                }
            }
        } else {
            $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
        }
        sleep(4);

  } elsif ($plugin eq "ClockssOJS2Plugin") {
       #Url with permission statement
        $url_m = sprintf("%sindex.php/%s/about/editorialPolicies",
            $param{base_url}, $param{journal_id});
        $man_url_x = uri_unescape($url_m);
        my $req_m = HTTP::Request->new(GET, $man_url_x);
        my $resp_m = $ua->request($req_m);
       #Url with permission statement - Alternate. No index.php
        $url_m_alt = sprintf("%s%s/about/editorialPolicies",
            $param{base_url}, $param{journal_id});
        $man_url_alt = uri_unescape($url_m_alt);
        my $req_m_alt = HTTP::Request->new(GET, $man_url_alt);
        my $resp_m_alt = $ua->request($req_m_alt);
       #Url with list of urls for issues
        $url_s = sprintf("%sindex.php/%s/gateway/lockss?year=%d",
            $param{base_url}, $param{journal_id}, $param{year});
        $start_url = uri_unescape($url_s);
        my $req_s = HTTP::Request->new(GET, $start_url);
        my $resp_s = $ua->request($req_s);
       #Url with list of urls for issues - Alternate. No index.php
        $url_s_alt = sprintf("%s%s/gateway/lockss?year=%d",
            $param{base_url}, $param{journal_id}, $param{year});
        $start_url_alt = uri_unescape($url_s_alt);
        my $req_s_alt = HTTP::Request->new(GET, $start_url_alt);
        my $resp_s_alt = $ua->request($req_s_alt);
       #For reporting at the end
        #================================= with index.php
        $man_url = $start_url . " + " . $man_url_x ;
        if (($resp_s->is_success) && ($resp_m->is_success)) {
            my $man_contents = $resp_m->content;
            my $start_contents = $resp_s->content;
            if ((($req_m->url ne $resp_m->request->uri) && ($req_m_alt->url ne $resp_m->request->uri)) || ($req_s->url ne $resp_s->request->uri)) {
                $vol_title = $req_m->url . " NOT " . $resp_m->request->uri . " OR " . $req_s->url . " NOT " . $resp_s->request->uri;
                $result = "Redirected";
            } elsif (defined($man_contents) && ($man_contents =~ m/content=\"Open Journal Systems 3\./)) {
                #$vol_title = $resp->request->uri;
                $result = "MOVED_TO_OJS3";
            } elsif (defined($man_contents) && defined($start_contents) && (($man_contents =~ m/$clockss_tag/) || ($man_contents =~ m/$oa_tag/) || ($man_contents =~ m/$cc_license_tag/)) && (($start_contents =~ m/\($param{year}\)/) || ($start_contents =~ m/: $param{year}/))) {
                $result = "Manifest"
            } else {
                #$result = "--NO_TAG--"
                if (!defined($man_contents) || !defined($start_contents)) {
                    $result = "--NO_CONT--";
                } elsif (($man_contents !~ m/$lockss_tag/) && ($man_contents !~ m/$oa_tag/)) {
                    $result = "--NO_TAG--";
                } elsif (($man_contents !~ m/\($param{year}\)/) && ($man_contents !~ m/: $param{year}/)) {
                    $result = "--NO_YEAR--";
                } else {
                    $result = "--MYST--";
                }
            }
        } else {
            $result = "--REQ_FAIL--"
        }
        #======================= w/o index.php
        if ($result eq "--REQ_FAIL--") {
            $man_url = $start_url_alt . " + " . $man_url_alt ;
            if (($resp_s_alt->is_success) && ($resp_m_alt->is_success)) {
                my $man_contents_alt = $resp_m_alt->content;
                my $start_contents_alt = $resp_s_alt->content;
                if (defined($man_contents_alt) && defined($start_contents_alt) && (($man_contents_alt =~ m/$clockss_tag/) || ($man_contents_alt =~ m/$oa_tag/)) && (($start_contents_alt =~ m/\($param{year}\)/) || ($start_contents_alt =~ m/: $param{year}/))) {
                    $result = "Manifest"
                } else {
                    #$result = "--NO_TAG--"
                    if (!defined($man_contents_alt) || !defined($start_contents_alt)) {
                        $result = "--NO_CONT--";
                    } elsif (($man_contents_alt !~ m/$lockss_tag/) && ($man_contents_alt !~ m/$oa_tag/)) {
                        $result = "--NO_TAG--";
                    } elsif (($man_contents_alt !~ m/\($param{year}\)/) && ($man_contents_alt !~ m/: $param{year}/)) {
                        $result = "--NO_YEAR--";
                    } else {
                        $result = "--MYST--";
                    }
                }
            } else {
                $result = "--REQ_FAIL--"
            }
        }
        sleep(4); 

# thin child of ClockssOJS2 but with a different start_url and no permission_url
  } elsif (($plugin eq "ClockssJidcOJS2Plugin") || 
           ($plugin eq "ClockssOjs3Plugin" && $param{base_url} !~ "talenta") || 
           ($plugin eq "ClockssUbiquityPartnerNetworkPlugin")) {
    #OJS3 allows an attr to define variants for location of manifest
    #print $param{base_url};
        if ($param{base_url} =~ m/scholarworks/) {
            $url = sprintf("%sjournals/index.php/%s/gateway/clockss?year=%d",
            $param{base_url}, $param{journal_id}, $param{year});
        } elsif (uri_unescape($param{journal_id}) =~ m/tekaharoa/) {
            $url = sprintf("%sindex.php/%s/gateway/clockss?year=%d",
            $param{base_url}, $param{journal_id}, $param{year});
        } elsif (uri_unescape($param{base_url}) =~ m/aut\.ac\.nz/) {
            $url = sprintf("%s%s/gateway/clockss?year=%d",
            $param{base_url}, $param{journal_id}, $param{year});
        } elsif (uri_unescape($param{base_url}) =~ m/talenta\.usu\.ac\.id/) {  #https://talenta.usu.ac.id
            $url = sprintf("%s%s/gateway/clockss?year=%d",
            $param{base_url}, $param{journal_id}, $param{year});
        } elsif ($plugin eq "ClockssUbiquityPartnerNetworkPlugin") {
            $url = sprintf("%sindex.php/%s/gateway/clockss?year=%d",
            $param{base_url2}, $param{journal_id}, $param{year});
            $auid_long =~ s/&base_url2~http.*//;
        } else {
          #default behavior
          $url = sprintf("%sindex.php/%s/gateway/clockss?year=%d",
          $param{base_url}, $param{journal_id}, $param{year});
        }
        $man_url = uri_unescape($url);
        my $req = HTTP::Request->new(GET, $man_url);
        my $resp = $ua->request($req);
        if ($resp->is_success) {
            my $man_contents = $resp->content;
            if ($req->url ne $resp->request->uri) {
                $vol_title = $resp->request->uri;
                $result = "Redirected";
            } elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/) && ($man_contents !~ m/Open Journal Systems 3.1.1/) && (($man_contents =~ m/\($param{year}\)/) || ($man_contents =~ m/: $param{year}/))) {
                if ($man_contents =~ m/<title>([^<>]*)<\/title>/si) {
                    $vol_title = $1;
                    $vol_title =~ s/\s*\n\s*/ /g;
                    $vol_title =~ s/\s*\t\s*/ /g;
                    if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
                        $vol_title = "\"" . $vol_title . "\"";
                    }
                }
                $result = "Manifest"
            } else {
                #$result = "--NO_TAG--";
                if (!defined($man_contents)) {
                    $result = "--NO_CONT--";
                } elsif ($man_contents =~ m/Open Journal Systems 3.1.1/) {
                    $result = "--BAD_V_3X--";
                } elsif (($man_contents !~ m/$clockss_tag/) && ($man_contents !~ m/$oa_tag/)) {
                    $result = "--NO_TAG--";
                } elsif (($man_contents !~ m/\($param{year}\)/) && ($man_contents !~ m/: $param{year}/)) {
                    $result = "--NO_YEAR--";
                } else {
                    $result = "--MYST--";
                }
            }
        } else {
            $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
        }
        sleep(4);

  # Start ClockssOMPBooksPlugin.
  } elsif ($plugin eq "ClockssOMPBooksPlugin") {
    $url = sprintf("%s%s/catalog/book/ed-%s",
        $param{base_url}, $param{publisher_id}, $param{isbn});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    #printf("resp is %s\n",$resp->status_line);
    my $man_contents = $resp->is_success ? $resp->content : "";
    if (! $resp->is_success) {
        $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    } elsif ($req->url ne $resp->request->uri) {
        $vol_title = $resp->request->uri;
        $result = "Redirected";
    } elsif (! defined($man_contents)) {
        $result = "--NOT_DEF--";
    } elsif ($man_contents !~ m/$clockss_tag/) {
        $result = "--NO_TAG--";
    } elsif ($man_contents !~ m/\/issue\/$param{year}\/$param{volume_name}\/\d/) {
        $result = "--NO_VOL--";
    } else {
        $result = "Manifest";
        if ($man_contents =~ m/<title>(.*)<\/title>/si) {
            $vol_title = $1 . " Volume " . $param{volume_name} . " Year " . $param{year};
            $vol_title =~ s/\s*\n\s*/ /g;
        }
    }
  sleep(4);
  # End ClockssOMPBooksPlugin

# thin child of OJS2 but with a different start_url and no permission_url
  } elsif ($plugin eq "Ojs3Plugin") {
    #OJS3 allows an attr to define variants for location of manifest
        if ($param{base_url} =~ m/scholarworks/) {
            $url = sprintf("%sjournals/index.php/%s/gateway/lockss?year=%d",
            $param{base_url}, $param{journal_id}, $param{year});
        } else {
          #default behavior
          $url = sprintf("%sindex.php/%s/gateway/lockss?year=%d",
          $param{base_url}, $param{journal_id}, $param{year});
        }
        $man_url = uri_unescape($url);
        my $req = HTTP::Request->new(GET, $man_url);
        my $resp = $ua->request($req);
        if ($resp->is_success) {
            my $man_contents = $resp->content;
            if ($req->url ne $resp->request->uri) {
                $vol_title = $resp->request->uri;
                $result = "Redirected";
            } elsif (defined($man_contents) && ($man_contents =~ m/$lockss_tag/) && ($man_contents !~ m/Open Journal Systems 3.1.1/) && (($man_contents =~ m/\($param{year}\)/) || ($man_contents =~ m/: $param{year}/))) {
#            } elsif (defined($man_contents) && ($man_contents =~ m/$lockss_tag/) && (($man_contents =~ m/\($param{year}\)/) || ($man_contents =~ m/: $param{year}/))) {
                if ($man_contents =~ m/<title>([^<>]*)<\/title>/si) {
                    $vol_title = $1;
                    $vol_title =~ s/\s*\n\s*/ /g;
                    if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
                        $vol_title = "\"" . $vol_title . "\"";
                    }
                }
                $result = "Manifest"
            } else {
                #$result = "--NO_TAG--";
                if (!defined($man_contents)) {
                    $result = "--NO_CONT--";
                } elsif ($man_contents =~ m/Open Journal Systems 3.1.1/) {
                    $result = "--BAD_V_3X--";
                } elsif (($man_contents !~ m/$lockss_tag/) && ($man_contents !~ m/$oa_tag/)) {
                    $result = "--NO_TAG--";
                } elsif (($man_contents !~ m/\($param{year}\)/) && ($man_contents !~ m/: $param{year}/)) {
                    $result = "--NO_YEAR--";
                } else {
                    $result = "--MYST--";
                }
            }
        } else {
            $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
        }
        sleep(4);

  } elsif ($plugin eq "PensoftOaiPlugin" || $plugin eq "ClockssPensoftOaiPlugin") {
    #no lockss permission statement on start page. Permission statement is on the base_url
    $perm_url = uri_unescape($param{base_url});
    #start_url for all OAI queries https://bdj.pensoft.net/oai.php?verb=ListRecords&identifier=bdj&metadataPrefix=oai_dc
    $url = sprintf("%soai.php?verb=ListRecords&set=%s&metadataPrefix=oai_dc",
      $param{base_url}, $param{au_oai_set});
    if (defined($param{au_oai_date}) && $param{au_oai_date} =~ m/^[0-9]{4}$/) {
      $url = $url . "&from=" . $param{au_oai_date} . "-01-01" . "&until=" . $param{au_oai_date} . "-12-31";
    }
    $man_url = uri_unescape($url);
    my $req_s = HTTP::Request->new(GET, $man_url);
    my $resp_s = $ua->request($req_s);
    my $req_p = HTTP::Request->new(GET, $perm_url);
    my $resp_p = $ua->request($req_p);
    
    if ($resp_p->is_success) {
      my $perm_contents = $resp_p->content;
      my $lcl_tag = $cc_license_tag;
      
      #printf("%s\n", $req_p->url);
      #printf("%s\n", $resp_p->request->uri);
      #printf("%s\n", $req_s->url);
      #printf("%s\n", $resp_s->request->uri);
      if (($req_p->url ne $resp_p->request->uri) || ($req_s->url ne $resp_s->request->uri)) {
          $vol_title = $resp_p->request->uri . "+" . $resp_s->request->uri;
          $result = "Redirected";

      } elsif (defined($perm_contents) && ($perm_contents =~ m/$lcl_tag/s) ) {
        if ($resp_s->is_success) {
          if ($resp_s->content =~ m/results in an empty (set|list)/is) {
            $result = "--EMPTY_LIST--"
          } else {
            $result = "Manifest";
          }
        } else {
          #printf("URL: %s\n", $man_url);
          $result = "--REQ_FAIL--"
        }
      } else {
        #printf("URL: %s\n", $perm_url);
        $result = "--NO_LOCKSS--"
      }
    } else {
      #printf("URL: %s\n", $perm_url);
      $result = "--PERM_REQ_FAIL--"
    }
    sleep(4);

  } elsif ($plugin eq "ClockssPensoftBooksPlugin" || $plugin eq "PensoftBooksPlugin" ) {
    #no lockss permission statement on start page. Permission statement is on the base_url
    $perm_url = uri_unescape($param{base_url});
    $url = sprintf("%sarticle/%s",
      $param{base_url}, $param{book_id});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    my $req_p = HTTP::Request->new(GET, $perm_url);
    my $resp_p = $ua->request($req_p);

    if ($resp_p->is_success && $resp->is_success) {
      my $perm_contents = $resp_p->content;
      my $man_contents = $resp->content;
      if ($req_p->url ne $resp_p->request->uri || $req->url ne $resp->request->uri) {
        $vol_title = $resp->request->uri;
        $result = "Redirected";
      } elsif (defined($man_contents) && ($perm_contents =~ m/$clockss_tag/) && ($perm_contents =~ m/$lockss_tag/) && ($man_contents =~ m/$param{book_id}\/download\/pdf/)) {
        if ($man_contents =~ m/<title>\s*(.*)\s*<\/title>/si) {
          $vol_title = $1;
          $vol_title =~ s/\s*\n\s*/ /g;
        }
        $result = "Manifest"
      } else {
        $result = "--"
      }
        } else {
            $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
        }
        sleep(4);
        
  #Residencia Pediatrica.
  } elsif (($plugin eq "ClockssResPediatricaOaiPlugin")) {
    #permission is different from start
    $perm_url = uri_unescape($param{base_url}) . "sobre-rp";
      #printf("URL: %s\n", $perm_url); #debug
      $vol_title = $perm_url ;
    #start_url for all OAI queries https://www.comicsgrid.com/api/oai/?verb=ListRecords&metadataPrefix=oai_dc&from=2019-01-01&until=2019-12-31
    #From RP start_url: "%slockss?oai_granularity=%s&amp;au_oai_date=%d", base_url, oai_granularity, year
    #$url = sprintf("%slockss?oai_granularity=%s&amp;au_oai_date=%d",
      #$param{base_url}, $param{oai_granularity}, $param{year});
    $url = sprintf("%soai?verb=ListRecords&metadataPrefix=oai_dc&from=%d-01-01&until=%d-12-31",
      $param{base_url}, $param{year}, $param{year});
    $man_url = uri_unescape($url);
    #$man_url = $url;
    my $req_p = HTTP::Request->new(GET, $perm_url);
    my $resp_p = $ua->request($req_p);
    my $req_s = HTTP::Request->new(GET, $man_url);
    my $resp_s = $ua->request($req_s);
    
    if ($resp_p->is_success) {
        my $perm_contents = $resp_p->content;
        #my $lcl_tag = $clockss_tag;
        if (($req_p->url ne $resp_p->request->uri) || ($req_s->url ne $resp_s->request->uri)) {
            $vol_title = $resp_p->request->uri . "+" . $resp_s->request->uri;
            $result = "Redirected";
        } elsif (defined($perm_contents) && ($perm_contents =~ m/$cc_license_url/s)) {
#        if (defined($perm_contents) && ($perm_contents =~ m/$clockss_tag/s) && ($perm_contents =~ m/$lockss_tag/s)) {
            if ($resp_s->is_success) {
                if ($resp_s->content =~ m/results in an empty (set|list)/is) {
                    $result = "--EMPTY_LIST--"
                } elsif ($resp_s->content !~ m/<dc:date>$param{year}/ ) {
                    $result = "--MISSING-YEAR--"
                } else {
                    $result = "Manifest";
                }
            } else {
                #printf("URL: %s\n", $man_url);
                $result = "--REQ_FAIL--"
            }
        } else {
        #printf("URL: %s\n", $perm_url);
        $result = "--NO_LOCKSS--"
      }
    } else {
      #printf("URL: %s\n", $perm_url);
      $result = "--PERM_REQ_FAIL--"
    }
    sleep(4);

  #Janeway. No journal_id
  } elsif (($plugin eq "OLHPlugin") || 
           ($plugin eq "ClockssOLHPlugin") ||
           ($plugin eq "ClockssAperioJournalsPlugin") ||
           ($plugin eq "UniversityofMichiganPlugin") ||
           ($plugin eq "ClockssUniversityofMichiganPlugin")) {
           #($plugin eq "GhentUniversityLibraryDomainModeJournalsPlugin") ||
           #($plugin eq "ClockssGhentUniversityLibraryDomainModeJournalsPlugin") ||
    #permission is different from start
    $perm_url = uri_unescape($param{base_url}) . "plugins/clockss/";
    #$perm_url = uri_unescape($param{base_url}) . "clockss/";
      #printf("URL: %s\n", $perm_url); #debug
      $vol_title = $perm_url ;
    #start_url for all OAI queries https://www.comicsgrid.com/api/oai/?verb=ListRecords&metadataPrefix=oai_dc&from=2019-01-01&until=2019-12-31
    $url = sprintf("%sapi/oai/?verb=ListRecords&amp;metadataPrefix=oai_dc&amp;from=%d-01-01&amp;until=%d-12-31",
      $param{base_url}, $param{year}, $param{year});
    $man_url = uri_unescape($url);
    my $req_p = HTTP::Request->new(GET, $perm_url);
    my $resp_p = $ua->request($req_p);
    my $req_s = HTTP::Request->new(GET, $man_url);
    my $resp_s = $ua->request($req_s);
    
    if ($resp_p->is_success) {
        my $perm_contents = $resp_p->content;
        #my $lcl_tag = $clockss_tag;
        if (($req_p->url ne $resp_p->request->uri) || ($req_s->url ne $resp_s->request->uri)) {
            $vol_title = $resp_p->request->uri . "+" . $resp_s->request->uri;
            $result = "Redirected";
        } elsif (defined($perm_contents) && ($perm_contents =~ m/$clockss_tag/s) && ($perm_contents =~ m/$lockss_tag/s)) {
#        if (defined($perm_contents) && ($perm_contents =~ m/$clockss_tag/s) && ($perm_contents =~ m/$lockss_tag/s)) {
            if ($resp_s->is_success) {
                if ($resp_s->content =~ m/results in an empty (set|list)/is) {
                    $result = "--EMPTY_LIST--"
                } elsif ($resp_s->content !~ m/<dc:date>$param{year}/ ) {
                    $result = "--MISSING-YEAR--"
                } else {
                    $result = "Manifest";
                }
            } else {
                #printf("URL: %s\n", $man_url);
                $result = "--REQ_FAIL--"
            }
        } else {
        #printf("URL: %s\n", $perm_url);
        $result = "--NO_LOCKSS--"
      }
    } else {
      #printf("URL: %s\n", $perm_url);
      $result = "--PERM_REQ_FAIL--"
    }
    sleep(4);

  #Janeway. With journal_id
  } elsif (($plugin eq "ClockssIowaStateDPPlugin") ||
           ($plugin eq "IowaStateDPPlugin") ||
           ($plugin eq "ClockssUniversityofIowaPlugin") ||
           ($plugin eq "ClockssUofMichiganJournalsPlugin") ||
           ($plugin eq "UniversityofIowaPlugin") ||
           ($plugin eq "ClockssGhentUniversityLibraryPlugin") ||
           ($plugin eq "GhentUniversityLibraryPlugin")) {
    #permission is different from start
    $perm_url = uri_unescape($param{base_url}) . $param{journal_id} . "/plugins/clockss/";
    #printf("URL: %s\n", $perm_url); #debug
    $vol_title = $perm_url ;
    #start_url for all OAI queries https://www.comicsgrid.com/api/oai/?verb=ListRecords&metadataPrefix=oai_dc&from=2019-01-01&until=2019-12-31
    $url = sprintf("%s%s/api/oai/?verb=ListRecords&amp;metadataPrefix=oai_dc&amp;from=%d-01-01&amp;until=%d-12-31",
      $param{base_url}, $param{journal_id}, $param{year}, $param{year});

    #$url = sprintf("%slockss?year=%d&amp;optional_journal_id=%s",
    #  $param{base_url}, $param{year}, $param{journal_id});
    $man_url = uri_unescape($url);
    my $req_p = HTTP::Request->new(GET, $perm_url);
    my $resp_p = $ua->request($req_p);
    my $req_s = HTTP::Request->new(GET, $man_url);
    my $resp_s = $ua->request($req_s);
    
    if ($resp_p->is_success) {
      my $perm_contents = $resp_p->content;
      #my $lcl_tag = $clockss_tag;
        if (($req_p->url ne $resp_p->request->uri) || ($req_s->url ne $resp_s->request->uri)) {
            $vol_title = $resp_p->request->uri . "+" . $resp_s->request->uri;
            $result = "Redirected";
        } elsif (defined($perm_contents) && ($perm_contents =~ m/$clockss_tag/s) && ($perm_contents =~ m/$lockss_tag/s)) {
        if ($resp_s->is_success) {
          #if ($resp_s->content =~ m/results in an empty (set|list)/is) {
            #$result = "--EMPTY_LIST--"
          #} elsif ($resp_s->content !~ m/<dc:date>$param{year}/ ) {
            #$result = "--MISSING-YEAR--"
          #} else {
            $result = "Manifest";
          #}
        } else {
          #printf("URL: %s\n", $man_url);
          $result = "--REQ_FAIL--"
        }
      } else {
        #printf("URL: %s\n", $perm_url);
        $result = "--NO_LOCKSS--"
      }
    } else {
      #printf("URL: %s\n", $perm_url);
      $result = "--PERM_REQ_FAIL--"
    }
    sleep(4);

  } elsif ($plugin eq "GeorgThiemeVerlagPlugin") {
        #Url with list of urls for issues
        #printf("%s\n",decode_entities($tmp));
        $url = sprintf("%sproducts/ejournals/issues/%s/%s",
          $param{base_url}, $param{journal_id}, $param{volume_name});
        $man_url = uri_unescape($url);
        my $doi = uri_unescape($param{journal_id});
        my $req = HTTP::Request->new(GET, $man_url);
        my $resp = $ua->request($req);
        if ($resp->is_success) {
            my $man_contents = $resp->content;
            #no lockss permission statement on start page. Permission statement is here: https://www.thieme-connect.de/lockss.txt
            if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
            } elsif (defined($man_contents) && ($man_contents =~ m/Year $param{volume_name}/) && ($man_contents =~ m/DOI: $doi/)) {
                if ($man_contents =~ m/<h1>(.*)<\/h1>/si) {
                    $vol_title = $1
                }
                $result = "Manifest"
            } else {
                $result = "--"
            }
        } else {
            $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
        }
        sleep(4);
        
  } elsif ($plugin eq "ClockssGeorgThiemeVerlagPlugin") {
        #Url with list of urls for issues
        #printf("%s\n",decode_entities($tmp));
        $url = sprintf("%sproducts/ejournals/issues/%s/%s",
          $param{base_url}, $param{journal_id}, $param{volume_name});
        $man_url = uri_unescape($url);
        my $doi = uri_unescape($param{journal_id});
        my $req = HTTP::Request->new(GET, $man_url);
        my $resp = $ua->request($req);
        if ($resp->is_success) {
            my $man_contents = $resp->content;
            if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
            } elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/) && ($man_contents =~ m/Year $param{volume_name}/) && ($man_contents =~ m/DOI: $doi/)) {
                if ($man_contents =~ m/<h1>(.*)<\/h1>/si) {
                    $vol_title = $1
                }
                $result = "Manifest"
            } else {
                $result = "--NO_TAG--"
            }
        } else {
            $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
        }
        sleep(4);
        
  } elsif ($plugin eq "GeorgThiemeVerlagBooksPlugin") {
        $url = sprintf("%sproducts/ebooks/book/%s",
          $param{base_url}, $param{doi});
        $man_url = uri_unescape($url);
        my $req = HTTP::Request->new(GET, $man_url);
        my $resp = $ua->request($req);
        if ($resp->is_success) {
            my $man_contents = $resp->content;
            #no lockss permission statement on start page. Permission statement is here: https://www.thieme-connect.de/lockss.txt
            if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
            } elsif (defined($man_contents) && ($man_contents =~ m/products\/ebooks\/pdf/)) {
                 if ($man_contents =~ m/<h1 class="productTitle">(.*)\s*<\/h1>/si) {
                    $vol_title = $1;
                    $vol_title =~ s/\s*\n\s*/ /g;
                }
                $result = "Manifest"
            } else {
                $result = "--"
            }
        } else {
            $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
        }
        sleep(4);
        
  } elsif ($plugin eq "ClockssGeorgThiemeVerlagBooksPlugin") {
        $url = sprintf("%sproducts/ebooks/book/%s",
          $param{base_url}, $param{doi});
        $man_url = uri_unescape($url);
        my $req = HTTP::Request->new(GET, $man_url);
        my $resp = $ua->request($req);
        #printf("response: %s", $resp->status_line);
        if ($resp->is_success) {
            my $man_contents = $resp->content;
            if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
            } elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/) && ($man_contents =~ m/products\/ebooks\/pdf/)) {
                if ($man_contents =~ m/<h1 class="productTitle">(.*)\s*<\/h1>/si) {
                    $vol_title = $1;
                    $vol_title =~ s/\s*\n\s*/ /g;
                }
                $result = "Manifest"
            } else {
                $result = "--NO_TAG--"
            }
        } else {
            $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
        }
        sleep(4);

  } elsif ($plugin eq "ClockssDoveMedicalPressPlugin") {
        $url = sprintf("%slockss.php?t=clockss&pa=issue&j_id=%s&year=%d",
          $param{base_url}, $param{journal_id}, $param{year});
        $man_url = uri_unescape($url);
        my $req = HTTP::Request->new(GET, $man_url);
        my $resp = $ua->request($req);
        if ($resp->is_success) { 
            my $man_contents = $resp->content;
            if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
            } elsif (defined($man_contents) && ($man_contents =~ m/<h1>CLOCKSS - Published Issues: (.*) $param{year}/) && ($man_contents =~ m/href=\"[^\"]*\">$1/)) { 
                #<h1>CLOCKSS - Published Issues: Biosimilars 2015</h1>
                #if ($man_contents =~ m/<h1>CLOCKSS - Published Issues: (.*) $param{year}<\/h1>/si) {
                if ($man_contents =~ m/<h1>CLOCKSS - Published Issues: (.*) $param{year}<\/h1>/si) {
                    $vol_title = $1
                }
                $result = "Manifest"
            } else {
                $result = "--"
            }
        } else {
            $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
        }
        sleep(4);

  } elsif ($plugin eq "ClockssTaarPlugin") {
        $url = sprintf("%sarticles/%s",
          $param{base_url}, $param{volume_name});
        $man_url = uri_unescape($url);
        my $req = HTTP::Request->new(GET, $man_url);
        my $resp = $ua->request($req);
        if ($resp->is_success) { 
            my $man_contents = $resp->content;
            if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
            } elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/) && ($man_contents =~ m/<title>\d\d\d\d - Volume $param{volume_name} - Aerosol and Air Quality Research<\/title>/)) { 
                #<title>2001 - Volume 1 - Aerosol and Air Quality Research</title>
                #if ($man_contents =~ m/<h1>CLOCKSS - Published Issues: (.*) $param{volume_name}<\/h1>/si) {
                #    $vol_title = $1
                #}
                $vol_title = "Aerosol and Air Quality Research";
                $result = "Manifest"
            } else {
                $result = "--"
            }
        } else {
            $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
        }
        sleep(4);

#PubFactory Journals GLN
  } elsif (($plugin eq "BerghahnJournalsPlugin")   ||
           ($plugin eq "PubFactoryJournalsPlugin") ||
           ($plugin eq "AjtmhPlugin")              ||
           ($plugin eq "AMetSoc2021Plugin")        ||
           ($plugin eq "BioscientificaPlugin")     ||
           ($plugin eq "ManchesterUniversityPressPlugin")) {
      $url = sprintf("%slockss-manifest/journal/%s/%s",
      $param{base_url}, $param{journal_id}, $param{volume_name});
      $man_url = uri_unescape($url);
      my $req = HTTP::Request->new(GET, $man_url);
      my $resp = $ua->request($req);
      if ($resp->is_success) {
          my $man_contents = $resp->content;
          if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
          } elsif (defined($man_contents) && (($man_contents =~ m/$lockss_tag/) &&
                  ($man_contents =~ m/\/journals\/$param{journal_id}\/$param{volume_name}\//))) {
              if ($man_contents =~ m/<h1>\s*(.*) LOCKSS Manifest Page\s*<\/h1>/si) {
                  $vol_title = $1;
                  $vol_title =~ s/\s*\n\s*/ /g;
                  $vol_title =~ s/ &amp\; / & /;
                  if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
                      $vol_title = "\"" . $vol_title . "\"";
                  }
              }
              $result = "Manifest";
          } else {
              $result = "--NO_TAG--"
          }
      } else {
          $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
      }
        sleep(4);

#PubFactory Journals CLOCKSS
  } elsif (($plugin eq "ClockssBerghahnJournalsPlugin") ||
           ($plugin eq "ClockssAjtmhPlugin")            ||
           ($plugin eq "ClockssAVMAJournalsPlugin")     ||
           ($plugin eq "ClockssAPMAJournalsPlugin")     ||
           ($plugin eq "ClockssAMetSoc2021Plugin")      ||
           ($plugin eq "ClockssManchesterUniversityPressPlugin")) {
      $url = sprintf("%slockss-manifest/journal/%s/%s",
      $param{base_url}, $param{journal_id}, $param{volume_name});
      $man_url = uri_unescape($url);
      my $req = HTTP::Request->new(GET, $man_url);
      my $resp = $ua->request($req);
      if ($resp->is_success) {
          my $man_contents = $resp->content;
          if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
          } elsif (defined($man_contents) && (($man_contents =~ m/$clockss_tag/) &&
                  ($man_contents =~ m/\/journals\/$param{journal_id}\/$param{volume_name}\//))) {
              if ($man_contents =~ m/<h1>\s*(.*) LOCKSS Manifest Page\s*<\/h1>/si) {
                  $vol_title = $1;
                  $vol_title =~ s/\s*\n\s*/ /g;
                  $vol_title =~ s/ &amp\; / & /;
                  if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
                      $vol_title = "\"" . $vol_title . "\"";
                  }
              }
              $result = "Manifest";
          } else {
              $result = "--NO_TAG--"
          }
      } else {
          $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
      }
        sleep(4);
        
#PubFactory Books GLN
  } elsif ($plugin eq "ManchesterUniversityPressBooksPlugin") {
      $url = sprintf("%slockss-manifest/book/%s",
      $param{base_url}, $param{book_isbn});
      $man_url = uri_unescape($url);
      my $req = HTTP::Request->new(GET, $man_url);
      my $resp = $ua->request($req);
      if ($resp->is_success) {
          my $man_contents = $resp->content;
          if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
          } elsif (defined($man_contents) && (($man_contents =~ m/$lockss_tag/) &&
                  ($man_contents =~ m/\/view\/$param{book_isbn}\/$param{book_isbn}\./))) {
              if ($man_contents =~ m/<h1>\s*(.*) LOCKSS Manifest Page\s*<\/h1>/si) {
                  $vol_title = $1;
                  $vol_title =~ s/\s*\n\s*/ /g;
                  $vol_title =~ s/ &amp\; / & /;
                  if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
                      $vol_title = "\"" . $vol_title . "\"";
                  }
              }
              $result = "Manifest";
          } else {
              $result = "--NO_TAG--"
          }
      } else {
          $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
      }
        sleep(4);

#PubFactory Books CLOCKSS
  } elsif ($plugin eq "ClockssManchesterUniversityPressBooksPlugin") {
      $url = sprintf("%slockss-manifest/book/%s",
      $param{base_url}, $param{book_isbn});
      $man_url = uri_unescape($url);
      my $req = HTTP::Request->new(GET, $man_url);
      my $resp = $ua->request($req);
      if ($resp->is_success) {
          my $man_contents = $resp->content;
          if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
          } elsif (defined($man_contents) && (($man_contents =~ m/$clockss_tag/) &&
                  ($man_contents =~ m/\/view\/$param{book_isbn}\/$param{book_isbn}\./))) {
              if ($man_contents =~ m/<h1>\s*(.*) LOCKSS Manifest Page\s*<\/h1>/si) {
                  $vol_title = $1;
                  $vol_title =~ s/\s*\n\s*/ /g;
                  $vol_title =~ s/ &amp\; / & /;
                  if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
                      $vol_title = "\"" . $vol_title . "\"";
                  }
              }
              $result = "Manifest";
          } else {
              $result = "--NO_TAG--"
          }
      } else {
          $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
      }
        sleep(4);

  } elsif (($plugin eq "TaylorAndFrancisPlugin") ||
           ($plugin eq "GenericAtyponPlugin") ||
           ($plugin eq "ACCJournalsPlugin") ||
           ($plugin eq "AIAAPlugin") ||
           ($plugin eq "AllenPressJournalsPlugin") ||
           ($plugin eq "AmericanSpeechLanguageHearingAssocAtyponPlugin") ||
           ($plugin eq "AmPublicHealthAssocPlugin") ||
           ($plugin eq "AMetSocPlugin") ||
           ($plugin eq "AmPhysSocAtyponPlugin") ||
           ($plugin eq "AmPsychPubAtyponPlugin") ||
           ($plugin eq "ARRSPlugin") ||
           ($plugin eq "AscoJournalsPlugin") ||
           ($plugin eq "AsmPlugin") ||
           ($plugin eq "ACPJournalsPlugin") ||
           ($plugin eq "AtsJournalsPlugin") ||
           ($plugin eq "BecarisPublishingPlugin") ||
           ($plugin eq "BESBJSPlugin") ||
           ($plugin eq "BIRAtyponPlugin") ||
           ($plugin eq "EdinburghUniversityPressPlugin") ||
           ($plugin eq "EHP2022AtyponJournalPlugin") ||
           ($plugin eq "EmeraldGroupPlugin") ||
           ($plugin eq "EndocrineSocietyPlugin") ||
           ($plugin eq "FasebAtyponPlugin") ||
           ($plugin eq "FutureSciencePlugin") ||
           ($plugin eq "GeologicalSocietyLondonPlugin") ||
           ($plugin eq "HealthAffairsPlugin") ||
           ($plugin eq "IndersciencePlugin") ||
           ($plugin eq "JstorPlugin") ||
           ($plugin eq "LiverpoolJournalsPlugin") ||
           ($plugin eq "ManeyAtyponPlugin") ||
           ($plugin eq "MarkAllenPlugin") ||
           ($plugin eq "MultiSciencePlugin") ||
           ($plugin eq "MassachusettsMedicalSocietyPlugin") ||
           ($plugin eq "NasPlugin") ||
           ($plugin eq "NRCResearchPressPlugin") ||
           ($plugin eq "RoyalSocietyPublishingAtyponPlugin") ||
           ($plugin eq "RsnaJournalsPlugin") ||
           ($plugin eq "SageAtyponJournalsPlugin") ||
           ($plugin eq "SiamPlugin") ||
           ($plugin eq "UChicagoPressPlugin") ||
           ($plugin eq "WageningenJournalsPlugin")) {
      $url = sprintf("%slockss/%s/%s/index.html",
      $param{base_url}, $param{journal_id}, $param{volume_name});
      $man_url = uri_unescape($url);
      $jid = uri_unescape($param{journal_id});  #some journal_id's have a dot.
      my $req = HTTP::Request->new(GET, $man_url);
      my $resp = $ua->request($req);
      if ($resp->is_success) {
          my $man_contents = $resp->content;
          #JSTOR plugin links are like ?journalCode=chaucerrev&amp;issue=2&amp;volume=44
          if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
          } elsif (defined($man_contents) && (($man_contents =~ m/$lockss_tag/) && 
                  (($man_contents =~ m/\/$jid\/$param{volume_name}\//) || 
                  ($man_contents =~ m/\/toc\/$jid\/*$param{volume_name}\"/) || 
                  ##Royal Society Publishing: "/toc/rsbl/2014/10/12"  or "/toc/rsbm/2018/64"  
                  ($man_contents =~ m/\"\/toc\/$jid\/[12][67890]\d\d\/$param{volume_name}(\/[-0-9]*)?\"/) || 
                  ##Liverpool Journals Back Content: such as "/toc/10.3828/labourhistory.2004.issue-87"
                  ($man_contents =~ m/\"\/toc\/10.3828\/$jid.[12][67890]\d\d.issue-$param{volume_name}\"/) || 

                  ($man_contents =~ m/\/$jid\S*volume=$param{volume_name}/)))) {
              if ($man_contents =~ m/<title>\s*(.*) LOCKSS Manifest Page\s*<\/title>/si) {
                  $vol_title = $1;
                  $vol_title =~ s/\s*\n\s*/ /g;
                  $vol_title =~ s/ &amp\; / & /;
                  if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
                      $vol_title = "\"" . $vol_title . "\"";
                  }
              }
              $result = "Manifest";
          } elsif ($man_contents !~ m/$lockss_tag/) {
              $result = "--NO_TAG--"
          } else {
            $vol_title = "";
            if ($man_contents =~ m/href=([^>]*)>/) {
              $vol_title = $1;
            }
            if ($vol_title =~ m/\/$jid\//) {
              $result = "--BAD_VOL--"
            } else {
              $result = "--BAD_JID--"
            }
          }
      } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
  }
        sleep(4);

  } elsif ($plugin eq "ClockssSageAtyponJournalsPlugin" && $num_values == 6) {
      $url = sprintf("%sclockss/%s/%s/index.html",
      $param{base_url}, $param{journal_id}, $param{volume_name});
      #printf("Six Values\n");
      my $site_title = "";
      my $site_year = "";
      $man_url = uri_unescape($url);
      $man_url =~ s/^http:/https:/; #swap http for https, because we don't care about this redirect.
      $jid = uri_unescape($param{journal_id});  #some journal_ids have a dot.
      my $req = HTTP::Request->new(GET, $man_url);
      my $resp = $ua->request($req);
      my $man_contents = $resp->is_success ? $resp->content : "";
      
      if ($man_contents =~ m/<title>\s*(.*) (\S*) CLOCKSS Manifest Page\s*<\/title>/si) {
          $site_title = $1;
          $site_year = $2;
          $site_title =~ s/\s*\n\s*/ /g;
          $site_title =~ s/ &amp\; / & /;
          $site_title =~ s/ & / and /;
          $site_title = lc($site_title);
          $site_title =~ s/^the //;
          #printf("site_title:%s site_year:%s\n", $site_title, $site_year); #debug
      }
      if (! $resp->is_success) {
          $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
      } elsif ($req->url ne $resp->request->uri) {
          $vol_title = $resp->request->uri;
          $result = "Redirected";
      } elsif (! defined($man_contents)) {
          $result = "--NOT_DEF--";
      } elsif ($man_contents !~ m/$clockss_tag/) {
          $result = "--NO_TAG--";
      } elsif ($man_contents !~ m/\/toc\/$jid\//) {
          $result = "--BAD_JID--";
      } elsif ($man_contents !~ m/\/toc\/$jid\/$param{volume_name}/) {
          if ($man_contents =~ m/<title>\s*(.*) (\d\d\d\d) CLOCKSS Manifest Page\s*<\/title>/si) {
              $vol_title = $1 . " " . $2 . " JID-" . $jid . " Vol-" . $param{volume_name};
              #$vol_title =~ s/\s*\n\s*/ /g;
              #$vol_title =~ s/ &amp\; / & /;
          }
          $result = "--BAD_VOL--";
      } elsif ($site_title ne $au_title) {
          if ($man_contents =~ m/<title>\s*(.*) (\d\d\d\d) CLOCKSS Manifest Page\s*<\/title>/si) {
              $vol_title = $site_title . " " . $site_year . "-tdb-" . $au_title . " " . $au_year;
          }
          $result = "--BAD_TITLE--";
      } elsif ($site_year ne $au_year) {
          if ($man_contents =~ m/<title>\s*(.*) (\d\d\d\d) CLOCKSS Manifest Page\s*<\/title>/si) {
              $vol_title = $site_title . " " . $site_year . "-tdb-" . $au_title . " " . $au_year;
          }
          $result = "--BAD_YEAR--";
          } else {
          if ($man_contents =~ m/<title>\s*(.*) (\d\d\d\d) CLOCKSS Manifest Page\s*<\/title>/si) {
              $vol_title = $1 . " " . $2;
              $vol_title =~ s/\s*\n\s*/ /g;
              $vol_title =~ s/ &amp\; / & /;
          }
              $result = "Manifest";
          }
        sleep(4);

  } elsif (($plugin eq "ClockssTaylorAndFrancisPlugin") ||
           ($plugin eq "ClockssGenericAtyponPlugin") ||
           ($plugin eq "ClockssARRSPlugin") ||
           ($plugin eq "ClockssAaasPlugin") ||
           ($plugin eq "ClockssAIAAPlugin") ||
           ($plugin eq "ClockssAllenPressJournalsPlugin") ||
           ($plugin eq "ClockssAmericanSpeechLanguageHearingAssocAtyponPlugin") ||
           ($plugin eq "ClockssAMetSocPlugin") ||
           ($plugin eq "ClockssAmmonsScientificPlugin") ||
           ($plugin eq "ClockssAmPhysSocAtyponPlugin") ||
           ($plugin eq "ClockssAmPsychPubAtyponPlugin") ||
           ($plugin eq "ClockssASCEPlugin") ||
           ($plugin eq "ClockssAscoJournalsPlugin") ||
           ($plugin eq "ClockssAsmPlugin") ||
           ($plugin eq "ClockssBecarisPublishingPlugin") ||
           ($plugin eq "ClockssBIRAtyponPlugin") ||
           ($plugin eq "ClockssEdinburghUniversityPressPlugin") ||
           ($plugin eq "ClockssEmeraldGroupPlugin") ||
           ($plugin eq "ClockssEndocrineSocietyPlugin") ||
           ($plugin eq "ClockssFasebAtyponPlugin") ||
           ($plugin eq "ClockssFutureSciencePlugin") ||
           ($plugin eq "ClockssGeologicalSocietyLondonPlugin") ||
           ($plugin eq "ClockssIndersciencePlugin") ||
           ($plugin eq "ClockssJstorPlugin") ||
           ($plugin eq "ClockssLiverpoolJournalsPlugin") ||
           ($plugin eq "ClockssManeyAtyponPlugin") ||
           ($plugin eq "ClockssMarkAllenPlugin") ||
           ($plugin eq "ClockssMultiSciencePlugin") ||
           ($plugin eq "ClockssNasPlugin") ||
           ($plugin eq "ClockssNRCResearchPressPlugin") ||
           ($plugin eq "ClockssPracticalActionJournalsPlugin") ||
           ($plugin eq "ClockssRoyalSocietyPublishingAtyponPlugin") ||
           ($plugin eq "ClockssSageAtyponJournalsPlugin") ||
           ($plugin eq "ClockssSEGPlugin") ||
           ($plugin eq "ClockssSiamPlugin") ||
           ($plugin eq "ClockssUChicagoPressPlugin") ||
           ($plugin eq "ClockssWageningenJournalsPlugin")) {
      $url = sprintf("%sclockss/%s/%s/index.html",
      $param{base_url}, $param{journal_id}, $param{volume_name});
      $man_url = uri_unescape($url);
      $jid = uri_unescape($param{journal_id});  #some journal_id's have a dot.
      my $req = HTTP::Request->new(GET, $man_url);
      my $resp = $ua->request($req);
      if ($resp->is_success) {
          my $man_contents = $resp->content;
          #JSTOR plugin links are like ?journalCode=chaucerrev&amp;issue=2&amp;volume=44
          if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
          } elsif (defined($man_contents) && (($man_contents =~ m/$clockss_tag/) && 
                  (($man_contents =~ m/\/$jid\/$param{volume_name}\//) || 
                  ($man_contents =~ m/\/toc\/$jid\/*$param{volume_name}\"/) || 
                  ##Royal Society Publishing: "/toc/rsbl/2014/10/12"  or "/toc/rsbm/2018/64"  
                  ($man_contents =~ m/\"\/toc\/$jid\/[12][67890]\d\d\/$param{volume_name}(\/[-0-9]*)?\"/) || 
                  ##Liverpool Journals Back Content: such as "/toc/10.3828/labourhistory.2004.issue-87"
                  ($man_contents =~ m/\"\/toc\/10.3828\/$jid.[12][67890]\d\d.issue-$param{volume_name}\"/) || 
                  ($man_contents =~ m/\/$jid\S*volume=$param{volume_name}/)))) {
              if ($man_contents =~ m/<title>\s*(.*) CLOCKSS Manifest Page\s*<\/title>/si) {
                  $vol_title = $1;
                  $vol_title =~ s/\s*\n\s*/ /g;
                  $vol_title =~ s/ &amp\; / & /;
                  if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
                      $vol_title = "\"" . $vol_title . "\"";
                  }
              }
              $result = "Manifest";
          } elsif ($man_contents !~ m/$clockss_tag/) {
              $result = "--NO_TAG--"
          } else {
            $vol_title = "";
            if ($man_contents =~ m/href=([^>]*)>/) {
              $vol_title = $1;
            }
            if ($vol_title =~ m/\/$jid\//) {
              $result = "--BAD_VOL--"
            } else {
              $result = "--BAD_JID--"
            }
          }
      } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
  }
        sleep(4);

  } elsif ($plugin eq "ClockssSageAtyponJournalsPlugin" && $num_values == 6) {
      $url = sprintf("%sclockss/%s/%s/index.html",
      $param{base_url}, $param{journal_id}, $param{volume_name});
      $man_url = uri_unescape($url);
      $jid = uri_unescape($param{journal_id});  #some journal_ids have a dot.
      my $req = HTTP::Request->new(GET, $man_url);
      my $resp = $ua->request($req);
      if ($resp->is_success) {
          my $man_contents = $resp->content;
          #JSTOR plugin links are like ?journalCode=chaucerrev&amp;issue=2&amp;volume=44
          if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
          } elsif (defined($man_contents) && (($man_contents =~ m/$clockss_tag/) && 
                  (($man_contents =~ m/\/$jid\/$param{volume_name}\//) || 
                  ($man_contents =~ m/\/toc\/$jid\/*$param{volume_name}\"/) || 
                  ##Royal Society Publishing: "/toc/rsbl/2014/10/12"  or "/toc/rsbm/2018/64"  
                  ($man_contents =~ m/\"\/toc\/$jid\/[12][67890]\d\d\/$param{volume_name}(\/[-0-9]*)?\"/) || 
                  ($man_contents =~ m/\/$jid\S*volume=$param{volume_name}/)))) {
              if ($man_contents =~ m/<title>\s*(.*) CLOCKSS Manifest Page\s*<\/title>/si) {
                  $vol_title = $1;
                  $vol_title =~ s/\s*\n\s*/ /g;
                  $vol_title =~ s/ &amp\; / & /;
                  if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
                      $vol_title = "\"" . $vol_title . "\"";
                  }
              }
              $result = "Manifest";
          } elsif ($man_contents !~ m/$clockss_tag/) {
              $result = "--NO_TAG--"
          } else {
            $vol_title = "";
            if ($man_contents =~ m/href=([^>]*)>/) {
              $vol_title = $1;
            }
            if ($vol_title =~ m/\/$jid\//) {
              $result = "--BAD_VOL--"
            } else {
              $result = "--BAD_JID--"
            }
          }
      } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
  }
        sleep(4);

#  # the non-Clockss Atypon Books plugins go here
#  # bug in this code. Using the old version of the code instead
#  } elsif (($plugin eq "GenericAtyponBooksPlugin") ||
#           ($plugin eq "AIAABooksPlugin") ||
#           ($plugin eq "EmeraldGroupBooksPlugin") ||
#           ($plugin eq "EndocrineSocietyBooksPlugin") ||
#           ($plugin eq "FutureScienceBooksPlugin") ||
#           ($plugin eq "LiverpoolBooksPlugin") ||
#           ($plugin eq "SiamBooksPlugin") ||
#           ($plugin eq "WageningenBooksPlugin")) {
#    $url = sprintf("%slockss/eisbn/%s",
#        $param{base_url}, $param{book_eisbn});
#    $man_url = uri_unescape($url);
#    my $req = HTTP::Request->new(GET, $man_url);
#    my $resp = $ua->request($req);
#    my $man_contents = $resp->is_success ? $resp->content : "";
#    if (! $resp->is_success) {
#        $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
#    } elsif ($req->url ne $resp->request->uri) {
#        $vol_title = $resp->request->uri;
#        $result = "Redirected";
#    } elsif (! defined($man_contents)) {
#        $result = "--NOT_DEF--";
#    } elsif ($man_contents !~ m/$lockss_tag/) {
#        $result = "--NO_TAG--";
#    } elsif ($man_contents !~ m/doi\/book\/([^\/]+)\/([^"']+)/) { #"
#        $result = "--BAD_DOI--";
#    } elsif ($man_contents =~ m/doi\/book\/([^\/]+)\/([^"']+)/) { #"
#        my $doi1 = $1;
#        my $doi2 = $2;
#        #get the title of the book if we found the manifest page
#        if ($man_contents =~ m/<title>(.*) Manifest Page<\/title>/si) {
#            $vol_title = $1;
#            $vol_title =~ s/\s*\n\s*/ /g;
#            $vol_title =~ s/ &amp\; / & /;
#            if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
#                $vol_title = "\"" . $vol_title . "\"";
#            }
#        }
#        # now make sure a PDF is actually available on the book landing page
#        # whole book pdf will use the same doi as the book landing page ex. https://epubs.siam.org/doi/book/10.1137/1.9781611972863
#        $url = sprintf("%sdoi/book/%s/%s",$param{base_url}, $doi1, $doi2);
#        my $book_url = uri_unescape($url);
#        my $breq = HTTP::Request->new(GET, $book_url);
#        my $bresp = $ua->request($breq);
#        if ($bresp->is_success) {
#            my $b_contents = $bresp->content;
#            # what we're looking for on the page is href="/doi/pdf/doi1/doi2" OR href="/doi/pdfplus/doi1/doi2" OR href="/doi/epub/doi1/doi2"
#            #printf("href=\"pdfplus/%s/%s\"",${doi1},${doi2});
#            #if (defined($b_contents) && ($b_contents =~ m/href=\"[^"]+pdf(plus)?\/${doi1}\/${doi2}/)) {
#            #if (defined($b_contents) && ($b_contents =~ m/href=\"[^"]+pdf(plus)?\/${doi1}\//)) {  #"
#            #if (defined($b_contents) && (($b_contents =~ m/href=\"[^"]+(epdf|pdf|epub|doi\/)(book)?(full)?(plus)?\/${doi1}\//) || ($b_contents =~ m/>Buy PDF</))) {  #"
#            if (defined($b_contents) && (($b_contents =~ m/href=\"[^"]+(epdf|pdf|epub\/)(full)?(plus)?\/${doi1}\//) || ($b_contents =~ m/>Buy PDF</) || ($b_contents =~ m/>Buy Online</) || ($b_contents =~ m/>Buy Digital</))) {  #"
#                $result = "Manifest";
#            }
#        }
#    } else {
#        $result = "--CODE_BUG--";
#    }
#    sleep(4);

  # the non-Clockss Atypon Books plugins go here
  } elsif (($plugin eq "GenericAtyponBooksPlugin") ||
           ($plugin eq "AIAABooksPlugin") ||
           ($plugin eq "EmeraldGroupBooksPlugin") ||
           ($plugin eq "EndocrineSocietyBooksPlugin") ||
           ($plugin eq "FutureScienceBooksPlugin") ||
           ($plugin eq "LiverpoolBooksPlugin") ||
           ($plugin eq "SiamBooksPlugin") ||
           ($plugin eq "WageningenBooksPlugin")) {
      $url = sprintf("%slockss/eisbn/%s",
          $param{base_url}, $param{book_eisbn});
      $man_url = uri_unescape($url);
      my $req = HTTP::Request->new(GET, $man_url);
      my $resp = $ua->request($req);
      if ($resp->is_success) {
          my $man_contents = $resp->content;
          if ($req->url ne $resp->request->uri) {
              $vol_title =  $resp->request->uri;
              $result = "Redirected";
          } elsif (defined($man_contents) && ($man_contents =~ m/$lockss_tag/)) {
              #prepare for the worst by presetting a not found result...
              $result = "--";
              if ($man_contents =~ m/doi\/book\/([^\/]+)\/([^"']+)/) {  #"
                  my $doi1 = $1;
                  my $doi2 = $2;
                  #get the title of the book if we found the manifest page
                  if ($man_contents =~ m/<title>(.*) Manifest Page<\/title>/si) {
                      $vol_title = $1;
                      $vol_title =~ s/\s*\n\s*/ /g;
                      $vol_title =~ s/ &amp\; / & /;
                      if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
                          $vol_title = "\"" . $vol_title . "\"";
                      }
                  }
                  # now make sure a PDF is actually available on the book landing page
                  # whole book pdf will use the same doi as the book landing page
                  $url = sprintf("%sdoi/book/%s/%s",$param{base_url}, $doi1, $doi2);
                  my $book_url = uri_unescape($url);
                  my $breq = HTTP::Request->new(GET, $book_url);
                  my $bresp = $ua->request($breq);
                  if ($bresp->is_success) {
                      my $b_contents = $bresp->content;
                      # what we're looking for on the page is href="/doi/pdf/doi1/doi2" OR href="/doi/pdfplus/doi1/doi2" OR href="/doi/epub/doi1/doi2"
                      #printf("href=\"pdfplus/%s/%s\"",${doi1},${doi2});
                      #if (defined($b_contents) && ($b_contents =~ m/href=\"[^"]+pdf(plus)?\/${doi1}\/${doi2}/)) {
                      #if (defined($b_contents) && ($b_contents =~ m/href=\"[^"]+(pdf|epub)(plus)?\/${doi1}\//)) {  #"
                      #if (defined($b_contents) && (($b_contents =~ m/href=\"[^"]+(epdf|pdf|epub|doi\/)(book)?(full)?(plus)?\/${doi1}\//) || ($b_contents =~ m/>Buy PDF</))) {  #"
                      if (defined($b_contents) && (($b_contents =~ m/href=\"[^"]+(epdf|pdf|epub\/)(full)?(plus)?\/${doi1}\//) || ($b_contents =~ m/>Buy PDF</) || ($b_contents =~ m/>Buy Online</) || ($b_contents =~ m/>Buy Digital</))) {  #"
                          $result = "Manifest";
                      }
                  }
              }
          } else {
              $result = "--NO_TAG--"
          }
      } else {
          $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
      }
        sleep(4);

  # the CLOCKSS Atypon Books plugins go here
  } elsif (($plugin eq "ClockssGenericAtyponBooksPlugin") ||
           ($plugin eq "ClockssAIAABooksPlugin") ||
           ($plugin eq "ClockssASCEBooksPlugin") ||
#           ($plugin eq "ClockssEmeraldGroupBooksPlugin") ||
           ($plugin eq "ClockssEndocrineSocietyBooksPlugin") ||
           ($plugin eq "ClockssFutureScienceBooksPlugin") ||
           ($plugin eq "ClockssLiverpoolBooksPlugin") ||
           ($plugin eq "ClockssNRCResearchPressBooksPlugin") ||
           ($plugin eq "ClockssPracticalActionBooksPlugin") ||
           ($plugin eq "ClockssSEGBooksPlugin") ||
           ($plugin eq "ClockssSiamBooksPlugin") ||
           ($plugin eq "ClockssWageningenBooksPlugin")) {
      $url = sprintf("%sclockss/eisbn/%s",
          $param{base_url}, $param{book_eisbn});
      $man_url = uri_unescape($url);
      my $req = HTTP::Request->new(GET, $man_url);
      my $resp = $ua->request($req);
      if ($resp->is_success) {
          my $man_contents = $resp->content;
          if ($req->url ne $resp->request->uri) {
              $vol_title =  $resp->request->uri;
              $result = "Redirected";
          } elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/)) {
              #prepare for the worst by presetting a not found result...
              $result = "--";
              if ($man_contents =~ m/doi\/book\/([^\/]+)\/([^"']+)/) {  #"
                  my $doi1 = $1;
                  my $doi2 = $2;
                  #get the title of the book if we found the manifest page
                  if ($man_contents =~ m/<title>(.*) Manifest Page<\/title>/si) {
                      $vol_title = $1;
                      $vol_title =~ s/\s*\n\s*/ /g;
                      $vol_title =~ s/ &amp\; / & /;
                      if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
                          $vol_title = "\"" . $vol_title . "\"";
                      }
                  }
                  # now make sure a PDF is actually available on the book landing page
                  # whole book pdf will use the same doi as the book landing page
                  $url = sprintf("%sdoi/book/%s/%s",$param{base_url}, $doi1, $doi2);
                  my $book_url = uri_unescape($url);
                  my $breq = HTTP::Request->new(GET, $book_url);
                  my $bresp = $ua->request($breq);
                  if ($bresp->is_success) {
                      my $b_contents = $bresp->content;
                      # what we're looking for on the page is href="/doi/pdf/doi1/doi2" OR href="/doi/pdfplus/doi1/doi2" OR href="/doi/epub/doi1/doi2"
                      #printf("href=\"pdfplus/%s/%s\"",${doi1},${doi2});
                      #if (defined($b_contents) && ($b_contents =~ m/href=\"[^"]+pdf(plus)?\/${doi1}\/${doi2}/)) {
                      #if (defined($b_contents) && ($b_contents =~ m/href=\"[^"]+(pdf|epub)(plus)?\/${doi1}\//)) {  #"
                      #if (defined($b_contents) && (($b_contents =~ m/href=\"[^"]+(epdf|pdf|epub|doi\/)(book)?(full)?(plus)?\/${doi1}\//) || ($b_contents =~ m/>Buy PDF</))) {  #"
                      if (defined($b_contents) && (($b_contents =~ m/href=\"[^"]+(epdf|pdf|epub\/)(full)?(plus)?\/${doi1}\//) || ($b_contents =~ m/>Buy PDF</) || ($b_contents =~ m/>Buy Online</) || ($b_contents =~ m/>Buy Digital</))) {  #"
                          $result = "Manifest";
                      }
                  }
              }
          } else {
              $result = "--NO_TAG--"
          }
      } else {
          $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
      }
        sleep(4);

  # the CLOCKSS Atypon Meeting Abstracts plugins
  } elsif ($plugin eq "ClockssSEGAbstractsPlugin") {
      $url = sprintf("%saction/showLockss?doi=%s",
          $param{base_url}, $param{item_doi});
      $man_url = uri_unescape($url);
      $man_doi = uri_unescape($param{item_doi});
      $bit_doi = substr($man_doi, 0, 2);
      #printf($man_url);
      my $req = HTTP::Request->new(GET, $man_url);
      my $resp = $ua->request($req);
      if ($resp->is_success) {
          my $man_contents = $resp->content;
          if ($req->url ne $resp->request->uri) {
              $vol_title =  $resp->request->uri;
              $result = "Redirected";
          } elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/)) {
              #prepare for the worst by presetting a not found result...
              $result = "--FOUND_TAG--";
              #if ($man_contents =~ m/doi\/book\/$param{item_doi}/) {
              if ($man_contents =~ m/doi\/book\/$man_doi/) {
                  $result = "--FOUND_URL--";
                  #get the title of the book if we found the manifest page
                  if ($man_contents =~ m/<title>(.*) Manifest Page<\/title>/si) {
                      $vol_title = $1;
                      $vol_title =~ s/\s*\n\s*/ /g;
                      $vol_title =~ s/ &amp\; / & /;
                      if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
                          $vol_title = "\"" . $vol_title . "\"";
                      }
                  }
                  # now make sure a PDF is actually available on the book landing page
                  # whole book pdf will use the same doi as the book landing page
                  $url = sprintf("%sdoi/book/%s",$param{base_url}, $param{item_doi});
                  my $book_url = uri_unescape($url);
                  my $breq = HTTP::Request->new(GET, $book_url);
                  my $bresp = $ua->request($breq);
                  if ($bresp->is_success) {
                      my $b_contents = $bresp->content;
                      $result = "--FOUND_START_PAGE--";
                      # what we're looking for on the page is href="/doi/reader/doi1/doi2"
                      #if (defined($b_contents) && ($b_contents =~ m/href=\"[^"]+doi\/reader\/substr($man_doi,0,5)/)) {  #"
                      if (defined($b_contents) && ($b_contents =~ m/href=\"[^"]+doi\/reader\/$bit_doi/)) {  #"
                          $result = "Manifest";
                      }
                  }
              }
          } else {
              $result = "--NO_TAG--"
          }
      } else {
          $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
      }
        sleep(4);

# use "\w+" at beginning of match to indicate something other than needs.SourcePlugin
#file transfer: old style (Warc|Source)Plugin (base+year), new style SourcePlugin (base+dir), DeliveredSourcePlugin (base + year + dir)
#the url can be built up from the available parameters
  } elsif (($plugin =~ m/\w+SourcePlugin/) || 
           ($plugin =~ m/\w+WarcPlugin/) || 
           ($plugin eq "ClockssCasaliniLibriBooksPlugin")) {
      $url = sprintf("%s", $param{base_url});
      # if there is a year parameter (delivered source and original source plugins) that comes next
      if (defined $param{year}) {
      $url .= "$param{year}/";
      } 
      # if there is a directory (delivered source in addition to year  and new directory based source plugins instead of year
      if (defined $param{directory}) {
      $url .= "$param{directory}/";
      }
      $man_url = uri_unescape($url);
      my $req = HTTP::Request->new(GET, $man_url);
      my $resp = $ua->request($req);
      if ($resp->is_success) {
          my $man_contents = $resp->content;
          #allow for newlines
          my $alt_clockss_tag = "CLOCKSS(\n)? (\n)?system(\n)? (\n)?has(\n)? (\n)?permission(\n)? (\n)?to(\n)? (\n)?ingest,(\n)? (\n)?preserve,(\n)? (\n)?and(\n)? (\n)?serve(\n)? (\n)?this(\n)? (\n)?Archival(\n)? (\n)?Unit";
          if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
              #does it have even one link on the page? then we have contents
          } elsif (defined($man_contents) && (($man_contents =~ m/$alt_clockss_tag/) && 
                   ($man_contents =~ m/a href=\"/))) {
              if ($man_contents =~ m/<title>\s*(.*)\s*<\/title>/si) {
                  $vol_title = $1;
                  $vol_title =~ s/\s*\n\s*/ /g;
                  $vol_title =~ s/ &amp\; / & /;
                  if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
                      $vol_title = "\"" . $vol_title . "\"";
                  }
              }
              $result = "Manifest";
          } else {
              $result = "--NO_TAG--"
          }
      } else {
          $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
      }
      sleep(1);

  } elsif ($plugin eq "ClockssAmericanMathematicalSocietyPlugin") {
    $url = sprintf("%sclockssdata/?p=%s&y=%d",
      $param{base_url}, $param{journal_id}, $param{year});
    $man_url = uri_unescape($url);
    #printf("\nUrl: %s\n", $man_url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/)) {
        $vol_title = $param{journal_id};
        if (($man_contents =~ m/\/$param{journal_id}\//) && ($man_contents =~ m/$param{year}/)) {
          $result = "Manifest";
        } else {
          $result = "--NO_URL--";
        }
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    }
    sleep(4);

  } elsif ($plugin eq "ClockssAmericanMathematicalSocietyBooksPlugin") {
    $url_p = sprintf("%sclockssdata?p=%s", $param{base_url}, $param{collection_id});
    $perm_url = uri_unescape($url_p);
      #printf("\nPermUrl: %s\n", $perm_url); #debug
    my $perm_req = HTTP::Request->new(GET, $perm_url);
    my $perm_resp = $ua->request($perm_req);

    $url_s = sprintf("%sbooks/%s/year/%s/", $param{base_url}, $param{collection_id}, $param{year_string});
    $start_url = uri_unescape($url_s);
      #printf("\nStartUrl: %s\n", $start_url); #debug
    my $start_req = HTTP::Request->new(GET, $start_url);
    my $start_resp = $ua->request($start_req);
    
    $man_url = $perm_url . "+" . $start_url;

    if ($perm_resp->is_success && $start_resp->is_success) {
      my $perm_contents = $perm_resp->content;
      my $start_contents = $start_resp->content;
      if ($perm_req->url ne $perm_resp->request->uri || $start_req->url ne $start_resp->request->uri) {
        $vol_title = $perm_resp->request->uri . "+" . $start_resp->request->uri;
        $result = "Redirected";
      } elsif (defined($perm_contents) && ($perm_contents =~ m/$clockss_tag/)) {
          if (defined($start_contents) && ($start_contents =~ m/\/books\/$param{collection_id}\//)) {
            $vol_title = $param{collection_id} . " Volume " . $param{year_string};
            $result = "Manifest";
         } else {
            $result = "--NO_CONTENT--";
         }
      } else {
        $result = "--NO_TAG--";
      }
  } else {
    $result = "--REQ_FAIL--" . $start_resp->code() . " " . $start_resp->message();
  }
  sleep(4);

  } elsif ($plugin eq "MathematicalSciencesPublishersPlugin") {
    $url = sprintf("%s%s/%d/manifest",
      $param{base_url}, $param{journal_id}, $param{year});
    $man_url = uri_unescape($url);
    #printf("\nUrl: %s\n", $man_url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$lockss_tag/) && ($man_contents =~ m/\/$param{journal_id}\/$param{year}/)) {
        $vol_title = $param{journal_id};
        $result = "Manifest";
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    }
    sleep(4);

  } elsif ($plugin eq "ClockssMathematicalSciencesPublishersPlugin") {
    $url = sprintf("%s%s/%d/manifest",
      $param{base_url}, $param{journal_id}, $param{year});
    $man_url = uri_unescape($url);
    #printf("\nUrl: %s\n", $man_url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/) && ($man_contents =~ m/\/$param{journal_id}\/$param{year}/)) {
        $vol_title = $param{journal_id};
        $result = "Manifest";
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    }
    sleep(4);

  } elsif ($plugin eq "IngentaJournalPlugin") {
    $url = sprintf("%scontent/%s?format=lockss&volume=%s",
      $param{base_url}, $param{journal_issn}, $param{volume_name});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$lockss_tag/) && ($man_contents =~ m/<a href="[^"]*$param{volume_name}/)) {
        if ($man_contents =~ m/<TITLE>\s*(.*)\s*<\/TITLE>/si) {
          $vol_title = $1;
          $vol_title =~ s/\s*\n\s*/ /g;
          if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
            $vol_title = "\"" . $vol_title . "\"";
          }
        }
        $result = "Manifest"
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    }
    sleep(4);
  } elsif ($plugin eq "ClockssIngentaJournalPlugin") {
    $url = sprintf("%scontent/%s?format=clockss&volume=%s",
      $param{base_url}, $param{journal_issn}, $param{volume_name});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/) && ($man_contents =~ m/<a href="[^"]*$param{volume_name}/)) {
        if ($man_contents =~ m/<TITLE>\s*(.*)\s*<\/TITLE>/si) {
          $vol_title = $1;
          $vol_title =~ s/\s*\n\s*/ /g;
          if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
            $vol_title = "\"" . $vol_title . "\"";
          }
        }
        $result = "Manifest"
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    }
    sleep(4);
  } elsif ($plugin eq "ClockssIngentaBooksPlugin") {
    $url = sprintf("%scontent/%s?format=clockss",
      $param{base_url}, $param{book_isbn});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      #manifest must have pub_id and art00001 - /content/bkpub/2ouatw/2016/00000001/00000001/art00001
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/) && ($man_contents =~ m/<a href="[^"]*$param{publisher_id}\/[^"]+\/art00001/)) {  #"
        if ($man_contents =~ m/<TITLE>Ingenta Connect\s*(.*)\s*CLOCKSS MANIFEST PAGE<\/TITLE>/si) {
          $vol_title = $1;
          $vol_title =~ s/\s*\n\s*/ /g;
          if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
            $vol_title = "\"" . $vol_title . "\"";
          }
        }
        $result = "Manifest"
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    }
    sleep(4);

  } elsif ($plugin eq "BloomsburyQatarPlugin") {
    $url = sprintf("%slockss/%s/%s/index.html",
      $param{base_url}, $param{journal_id}, $param{volume_name});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$lockss_tag/)) {
      if ($man_contents =~ m/<title>\s*(.*)\s*LOCKSS Manifest Page\s*<\/title>/si) {
          $vol_title = $1;
          $vol_title =~ s/\s*\n\s*/ /g;
          if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
            $vol_title = "\"" . $vol_title . "\"";
          }
        }
        $result = "Manifest"
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    }
    sleep(4);
  } elsif ($plugin eq "ClockssBloomsburyQatarPlugin") {
    $url = sprintf("%sclockss/%s/%s/index.html",
      $param{base_url}, $param{journal_id}, $param{volume_name});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($man_contents =~ m/<title>\s*(.*)\s*CLOCKSS Manifest Page\s*<\/title>/si) {
        if ($man_contents =~ m/<tr>\s*<td class=.labelName.>Journal<\/td><td class=.labelValue.><a href=\"\S*\">(.*)<\/a>\s*<\/td>\s*<\/tr>/si) {
          $vol_title = $1;
          $vol_title =~ s/\s*\n\s*/ /g;
          if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
            $vol_title = "\"" . $vol_title . "\"";
          }
        }
        $result = "Manifest"
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    }
    sleep(4);

#  } elsif ($plugin eq "ClockssIOPSciencePlugin") {
#    $url = sprintf("%svolume/%s/%s",
#      $param{base_url}, $param{journal_issn}, $param{volume_name});
#    $man_url = uri_unescape($url);
#    my $req = HTTP::Request->new(GET, $man_url);
#    my $resp = $ua->request($req);
#    if ($resp->is_success) {
#      my $man_contents = $resp->content;
#      if ($req->url ne $resp->request->uri) {
#              $vol_title = $resp->request->uri;
#              $result = "Redirected";
#      } elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/)) {
#        if ($man_contents =~ m/<TITLE>\s*(.*)\s*<\/TITLE>/si) {
#          $vol_title = $1;
#          $vol_title =~ s/\s*\n\s*/ /g;
#          if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
#            $vol_title = "\"" . $vol_title . "\"";
#          }
#        }
#        $result = "Manifest"
#      } else {
#        $result = "--NO_TAG--"
#      }
#    } else {
#      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
#    }
#    sleep(4);

  } elsif ($plugin eq "IgiGlobalPlugin") {
    $url = sprintf("%slockss/journal-issues.aspx?issn=%s&volume=%s",
      $param{base_url}, $param{journal_issn}, $param{volume});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$lockss_tag/) && ($man_contents =~ m/$igi_tag/)) {
        if ($man_contents =~ m/<TITLE>\s*(.*)\s*<\/TITLE>/si) {
          $vol_title = $1;
          $vol_title =~ s/\s*\n\s*/ /g;
          if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
            $vol_title = "\"" . $vol_title . "\"";
          }
        }
        $result = "Manifest"
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    }
    sleep(4);

  } elsif ($plugin eq "ClockssIgiGlobalPlugin") {
    $url = sprintf("%slockss/journal-issues.aspx?issn=%s&volume=%s",
      $param{base_url}, $param{journal_issn}, $param{volume});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/) && ($man_contents =~ m/$igi_tag/)) {
        if ($man_contents =~ m/<TITLE>\s*(.*)\s*<\/TITLE>/si) {
          $vol_title = $1;
          $vol_title =~ s/\s*\n\s*/ /g;
          if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
            $vol_title = "\"" . $vol_title . "\"";
          }
        }
        $result = "Manifest"
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    }
    sleep(4);
  } elsif ($plugin eq "IgiGlobalBooksPlugin") {
    #permission is different from start
    $url = sprintf("%slockss/books.aspx",
      $param{base_url});
    $perm_url = uri_unescape($url);
    #start_url for individual book
    $url = sprintf("%sgateway/book/%s",
      $param{base_url}, $param{volume});
    $start_url = uri_unescape($url);
        #for reporting at the end 
        $man_url = $start_url;
    my $req_p = HTTP::Request->new(GET, $perm_url);
    my $resp_p = $ua->request($req_p);
    my $req_s = HTTP::Request->new(GET, $start_url);
    my $resp_s = $ua->request($req_s);
    if ($resp_p->is_success && $resp_s->is_success) {
      my $perm_contents = $resp_p->content;
      my $start_contents = $resp_s->content;
      if (defined($perm_contents) && (defined($start_contents)) && 
      ($perm_contents =~ m/$lockss_tag/) && ($start_contents =~ m/$igi_book_tag/)) {
        if ($start_contents =~ m/<TITLE>\s*(.*)\s*\| IGI Global\s*<\/TITLE>/si) {
          $vol_title = $1;
          $vol_title =~ s/\s*\n\s*/ /g;
          if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
            $vol_title = "\"" . $vol_title . "\"";
          }
        }
        $result = "Manifest" ;
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--"
    }
    sleep(4);

  } elsif ($plugin eq "ClockssIgiGlobalBooksPlugin") {
    #permission is different from start
    $url = sprintf("%slockss/books.aspx",
      $param{base_url});
    $perm_url = uri_unescape($url);
    #start_url for individual book
    $url = sprintf("%sgateway/book/%s",
      $param{base_url}, $param{volume});
    $start_url = uri_unescape($url);
        #for reporting at the end 
        $man_url = $start_url;
    my $req_p = HTTP::Request->new(GET, $perm_url);
    my $resp_p = $ua->request($req_p);
    my $req_s = HTTP::Request->new(GET, $start_url);
    my $resp_s = $ua->request($req_s);
    if ($resp_p->is_success && $resp_s->is_success) {
      my $perm_contents = $resp_p->content;
      my $start_contents = $resp_s->content;
      if (defined($perm_contents) && (defined($start_contents)) && 
      ($perm_contents =~ m/$clockss_tag/) && ($start_contents =~ m/$igi_book_tag/)) {
        if ($start_contents =~ m/<TITLE>\s*(.*)\s*\| IGI Global\s*<\/TITLE>/si) {
          $vol_title = $1;
          $vol_title =~ s/\s*\n\s*/ /g;
          if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
            $vol_title = "\"" . $vol_title . "\"";
          }
        }
        $result = "Manifest" ;
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--"
    }
    sleep(4);

  } elsif ($plugin eq "ClockssRSCBooksPlugin" || $plugin eq "RSCBooksPlugin" ) {
    $url = sprintf("%sen/ebooks/lockss?copyrightyear=%d",
      $param{base_url}, $param{year});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/) && ($man_contents =~ m/en\/ebooks\/lockss\?isbn=9/)) {
          $vol_title = "RSC Books $param{year}";
          $result = "Manifest"
      } else {
        if ($man_contents =~ m/Access Denied/) {
          $result = "--ACCESS_DENIED--";
        } elsif ($man_contents !~ m/$lockss_tag/) {
          $result = "--NO_TAG--";
        } else {
          $result = "--NO_ISSUE--";
        }
      }
    } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    }
    sleep(4);

  } elsif ($plugin eq "RSC2014Plugin") {
    $url = sprintf("%sen/journals/lockss?journalcode=%s&volume=%s&year=%d",
      $param{base_url}, $param{journal_code}, $param{volume_name}, $param{year});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$lockss_tag/) && ($man_contents =~ m/\/lockss\?journalcode=$param{journal_code}&volume=$param{volume_name}&year=$param{year}/)) {
        if ($man_contents =~ m/<title>\s*RSC Journals \|(.*)\s*<\/title>/si) {
          $vol_title = $1;
          $vol_title =~ s/\s*\n\s*/ /g;
          if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
            $vol_title = "\"" . $vol_title . "\"";
          }
        }
        $result = "Manifest"
      } else {
        if ($man_contents =~ m/Access Denied/) {
          $result = "--ACCESS_DENIED--";
        } elsif ($man_contents !~ m/$lockss_tag/) {
          $result = "--NO_TAG--";
        } else {
          $result = "--NO_ISSUE--";
        }
      }
    } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    }
    sleep(4);

  } elsif ($plugin eq "ClockssRSC2014Plugin") {
    $url = sprintf("%sen/journals/lockss?journalcode=%s&volume=%s&year=%d",
      $param{base_url}, $param{journal_code}, $param{volume_name}, $param{year});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/) && ($man_contents =~ m/\/lockss\?journalcode=$param{journal_code}&volume=$param{volume_name}&year=$param{year}/)) {
        if ($man_contents =~ m/<title>\s*RSC Journals \|(.*)\s*<\/title>/si) {
          $vol_title = $1;
          $vol_title =~ s/\s*\n\s*/ /g;
          if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
            $vol_title = "\"" . $vol_title . "\"";
          }
        }
        $result = "Manifest"
      } else {
        if ($man_contents =~ m/Access Denied/) {
          $result = "--ACCESS_DENIED--";
        } elsif ($man_contents !~ m/$lockss_tag/) {
          $result = "--NO_TAG--";
        } else {
          $result = "--NO_ISSUE--";
        }
      }
    } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    }
    sleep(4);

  #University of Michigan Press Books, Amherst College Press Books
  } elsif (($plugin eq "ClockssUMichFulcrumBooksPlugin") ||
           ($plugin eq "ClockssAmherstCollegePressBooksPlugin")) {
    $url = sprintf("%s%s",
      $param{base_url}, $param{book_uri});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);

    my $p_url = "http://clockss-ingest.lockss.org/clockss.txt";
    my $perm_url = uri_unescape($p_url);
    my $p_req = HTTP::Request->new(GET, $perm_url);
    my $p_resp = $ua->request($p_req);

    if ($resp->is_success && $p_resp->is_success) {
      my $man_contents = $resp->content;
      my $perm_contents = $p_resp->content;
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($perm_contents =~ m/$clockss_tag/)) {
        #Make sure we have access or there is open access, and there is a pdf
        #if (($man_contents !~ m/Your institution does not have access to this book/) && ($man_contents =~ m/locale=en/) && ($man_contents =~ m/a data-rep-type=\"pdf\"/ || $man_contents =~ m/a data-rep-type=\"epub\"/) && ($man_contents =~ m/Access to this book has been provided by your Library/ || ($man_contents =~ m/Open Access/ && $man_contents =~ m/\(open access\)/ ) )) {
        #if (($man_contents !~ m/Your institution does not have access to this book/) && ($man_contents =~ m/locale=en/) && ($man_contents =~ m/a data-rep-type=\"pdf\"/ || $man_contents =~ m/a data-rep-type=\"epub\"/ || $man_contents =~ m/a\s+id=\"monograph-read-btn\"\s+href=\"\/epubs\/\S+\"/) && ($man_contents =~ m/Access to this book has been provided by your Library/ || ($man_contents =~ m/Open Access/ && $man_contents =~ m/\(open access\)/ ) )) {
         if (($man_contents !~ m/Your institution does not have access to this book/) && ($man_contents =~ m/locale=en/) && ($man_contents =~ m/a data-rep-type=\"pdf\"/ || $man_contents =~ m/a data-rep-type=\"epub\"/ || $man_contents =~ m/a\s+id=\"monograph-read-btn\"\s+href=\"\/epubs\/\S+\"/) && ($man_contents =~ m/Access to this book has been provided by your Library/ || ($man_contents =~ m/alt=.Open Access./ ) )) {
          if ($man_contents =~ m/<title>\s*(\S[^<]*\S)\s*<\/title>/si) {
            $vol_title = $1;
            $vol_title =~ s/\s*\n\s*/ /g;
            if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
              $vol_title = "\"" . $vol_title . "\"";
            }
          }
          $result = "Manifest"
        } else {
          $result = "--NO_CONT--";
        }
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    }
    sleep(4);

  #American Society for Microbiology Books
  #pub2web
#  } elsif ($plugin eq "ClockssASMscienceBooksPlugin") {
#    $url = sprintf("%scontent/book/%s",
#      $param{base_url}, $param{doi});
#    $man_url = uri_unescape($url);
#    my $req = HTTP::Request->new(GET, $man_url);
#    my $resp = $ua->request($req);
#    if ($resp->is_success) {
#      my $man_contents = $resp->content;
#      my $has_no_chapters = "Chapters \\(0\\)";
##      if ($man_contents =~ m/$has_no_chapters/ ) {
##	print "matches - has no chapters\n";
##      } else {
##	print "no match -has chapters\n";
##      }
#      if ($req->url ne $resp->request->uri) {
#              $vol_title = $resp->request->uri;
#              $result = "Redirected";
#      } elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/)) {
#        if ($man_contents !~ m/$has_no_chapters/) {
#          if ($man_contents =~ m/<title>\s*ASMscience \|(.*)\s*<\/title>/si) {
#            $vol_title = $1;
#            $vol_title =~ s/\s*\n\s*/ /g;
#            if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
#              $vol_title = "\"" . $vol_title . "\"";
#            }
#          }
#          $result = "Manifest"
#        } else {
#          $result = "--NO_CONT--";
#        }
#      } else {
#        $result = "--NO_TAG--"
#      }
#    } else {
#      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
#    }
#    sleep(4);
#
#  } elsif ($plugin eq "ASMscienceBooksPlugin") {
#    $url = sprintf("%scontent/book/%s",
#      $param{base_url}, $param{doi});
#    $man_url = uri_unescape($url);
#    my $req = HTTP::Request->new(GET, $man_url);
#    my $resp = $ua->request($req);
#    if ($resp->is_success) {
#      my $man_contents = $resp->content;
#      my $has_no_chapters = "Chapters \\(0\\)";
#      if ($req->url ne $resp->request->uri) {
#              $vol_title = $resp->request->uri;
#              $result = "Redirected";
#      } elsif (defined($man_contents) && ($man_contents =~ m/$lockss_tag/)) {
#        if ($man_contents !~ m/$has_no_chapters/) {
#          if ($man_contents =~ m/<title>\s*ASMscience \|(.*)\s*<\/title>/si) {
#            $vol_title = $1;
#            $vol_title =~ s/\s*\n\s*/ /g;
#            if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
#              $vol_title = "\"" . $vol_title . "\"";
#            }
#          }
#          $result = "Manifest"
#        } else {
#          $result = "--NO_CONT--";
#        }
#      } else {
#        $result = "--NO_TAG--"
#      }
#    } else {
#      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
#    }
#    sleep(4);

  # Clockss Microbiology Society
  } elsif ($plugin eq "ClockssMicrobiologySocietyJournalsPlugin") {
    $url = sprintf("%scontent/journal/%s/clockssissues?volume=%s",
      $param{base_url}, $param{journal_id}, $param{volume_name});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/)) {
        $result = "Manifest"
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    }
    sleep(4);

  } elsif ($plugin eq "MicrobiologySocietyJournalsPlugin") {
    $url = sprintf("%scontent/journal/%s/lockssissues?volume=%s",
      $param{base_url}, $param{journal_id}, $param{volume_name});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$lockss_tag/)) {
        $result = "Manifest"
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    }
    sleep(4);

  } elsif ($plugin eq "ASMscienceJournalsPlugin") {
    $url = sprintf("%scontent/journal/%s/lockssissues?volume=%s",
      $param{base_url}, $param{journal_id}, $param{volume_name});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$lockss_tag/)) {
        $result = "Manifest"
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    }
    sleep(4);

  } elsif ($plugin eq "ClockssASMscienceJournalsPlugin") {
    #pub2web
    $url = sprintf("%scontent/journal/%s/clockssissues?volume=%s",
      $param{base_url}, $param{journal_id}, $param{volume_name});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/)) {
        $result = "Manifest"
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    }
    sleep(4);

  } elsif (($plugin eq "ClockssIetJournalsPlugin") || ($plugin eq "ClockssHBKUPlugin")) {
  # note plural on journals - unique among pub2web
    $url = sprintf("%scontent/journals/%s/clockssissues?volume=%s",
      $param{base_url}, $param{journal_id}, $param{volume_name});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/) && ($man_contents =~ m/\/content\/journals\/$param{journal_id}\/$param{volume_name}\//)) {
        $result = "Manifest"
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    }
    sleep(4);

  } elsif (($plugin eq "ClockssCopernicusPublicationsPlugin") ||
           ($plugin eq "CopernicusPublicationsPlugin"))  {
    $url_p1 = sprintf("%sindex.html",
      $param{home_url});
    $url_p2 = sprintf("%sarticles/volumes.html",
      $param{base_url});
    $url_s = sprintf("%sarticles/%s/index.html",
      $param{base_url}, $param{volume_name});
    #prefix for an article link
    $url_d = sprintf("%sarticles/%s/",
     $param{base_url}, $param{volume_name});
    $man_url = uri_unescape($url_s) . " + " . uri_unescape($url_p1) . " + " . uri_unescape($url_p2);
    $man_url_p1 = uri_unescape($url_p1);
    #printf("*man_url_p1: %s\n", $man_url_p1);
    $man_url_p2 = uri_unescape($url_p2);
    #printf("*man_url_p2: %s\n", $man_url_p2);
    $man_url_s = uri_unescape($url_s);
    #printf("*man_url_s: %s\n", $man_url_s);
    my $article_prefix = uri_unescape($url_d);
    #printf("*article_prefix: %s\n", $article_prefix);
    my $req_p1 = HTTP::Request->new(GET, $man_url_p1);
    my $req_p2 = HTTP::Request->new(GET, $man_url_p2);
    my $req_s = HTTP::Request->new(GET, $man_url_s);
    my $resp_p1 = $ua->request($req_p1);
    my $resp_p2 = $ua->request($req_p2);
    my $resp_s = $ua->request($req_s);
    if ($resp_p1->is_success && $resp_p2->is_success && $resp_s->is_success) {
      my $perm1_contents = $resp_p1->content;
      my $perm2_contents = $resp_p2->content;
      my $start_contents = $resp_s->content;
      #TRY THE NORMAL SITUATION FIRST
      if ((defined($perm1_contents)) && 
          (defined($perm2_contents)) && 
          (defined($start_contents)) && 
          ($start_contents =~ m/$article_prefix/) && 
          ($perm1_contents =~ m/$cc_license_tag/) && 
          ($perm2_contents =~ m/$cc_license_tag/) && 
          ($perm1_contents =~ m/$cc_license_url/) &&
          ($perm2_contents =~ m/$cc_license_url/)) {
        if ($perm1_contents =~ m/j-name.>\s*([^<]*)\s*<\//si) {
            $vol_title = $1;
          $vol_title =~ s/\s*\n\s*/ /g;
          if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
            $vol_title = "\"" . $vol_title . "\"";
          }
        }
        $result = "Manifest"
      } elsif ($param{home_url} eq $param{base_url})  {
      #TRY THE WEIRD CASE NEXT - we know that $resp_s->is_success already here
      my $start_contents = $resp_s->content;
      if (defined($start_contents) && 
          ($start_contents =~ m/$article_prefix/) && 
          ($start_contents =~ m/$cc_license_tag/) && 
          ($start_contents =~ m/$cc_license_url/)) {
        if ($start_contents =~ m/<title>\s*([^<]*)\s*<\//si) {
            $vol_title = $1;
          $vol_title =~ s/\s*\n\s*/ /g;
          if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
            $vol_title = "\"" . $vol_title . "\"";
          }
        }
        $result = "Manifest"
        } else {
	        $result = "--NO_TAG--"
	    }
      } else {
        $result = "--NO_TAG--"
      }
    } elsif ( ($param{home_url} eq $param{base_url}) && $resp_s->is_success) {
    # a new special case where base_url = home_Url and the permission lives at the start url
      my $start_contents = $resp_s->content;
      if (defined($start_contents) && 
          ($start_contents =~ m/$article_prefix/) && 
          ($start_contents =~ m/$cc_license_tag/) && 
          ($start_contents =~ m/$cc_license_url/)) {
        if ($start_contents =~ m/<title>\s*([^<]*)\s*<\//si) {
            $vol_title = $1;
          $vol_title =~ s/\s*\n\s*/ /g;
          if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
            $vol_title = "\"" . $vol_title . "\"";
          }
        }
        $result = "Manifest"
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--"
    }
    sleep(4);

#  } elsif (($plugin eq "HindawiPublishingCorporationPlugin") || ($plugin eq "ClockssHindawiPublishingCorporationPlugin")) {
#    $url = sprintf("%sjournals/%s/%s/",
#      $param{base_url}, $param{journal_id}, $param{volume_name});
#    $man_url = uri_unescape($url);
#    my $req = HTTP::Request->new(GET, $man_url);
#    my $resp = $ua->request($req);
#    if ($resp->is_success) {
#      my $man_contents = $resp->content;
#      if ($req->url ne $resp->request->uri) {
#        $vol_title = $resp->request->uri;
#        $result = "Redirected";
#      } elsif (defined($man_contents) && ($man_contents =~ m/journals\/$param{journal_id}\/$param{volume_name}/)) {
#        if ($man_contents =~ m/<title>(.*)<\/title>/si) {
#          $vol_title = $1;
#          #$vol_title =~ s/ \| / /g;
#          $vol_title =~ s/\s*\n\s*/ /g;
#          $vol_title =~ s/An Open Access Journal//;
#          $vol_title =~ s/\s+/ /g;
#          $vol_title =~ s/&#8212;/Volume $param{volume_name}/g;
#
#        }
#        $result = "Manifest"
#      } else {
#        $result = "--NO_TAG--"
#      }
#    } else {
#      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
#    }
#    sleep(4);

  } elsif (($plugin eq "HindawiPlugin") || ($plugin eq "ClockssHindawiPlugin")) {
        $url = sprintf("%sjournals/%s/contents/year/%d/",
          $param{base_url}, $param{journal_id}, $param{year});
        $man_url = uri_unescape($url);
        my $req = HTTP::Request->new(GET, $man_url);
        my $resp = $ua->request($req);
        if ($resp->is_success) {
          my $man_contents = $resp->content;
          if ($req->url ne $resp->request->uri) {
            $vol_title = $resp->request->uri;
            $result = "Redirected";
          } elsif (defined($man_contents) && ($man_contents =~ m/journals\/$param{journal_id}\/$param{year}/)) {
            if ($man_contents =~ m/<title>(.*)<\/title>/si) {
              $vol_title = $1;
              #$vol_title =~ s/ \| / /g;
              $vol_title =~ s/\s*\n\s*/ /g;
              $vol_title =~ s/An Open Access Journal//;
              $vol_title =~ s/\s+/ /g;
              $vol_title =~ s/&#8212;/Year $param{year}/g;

            }
            $result = "Manifest"
          } else {
            $result = "--NO_TAG--"
          }
        } else {
          $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
        }
        sleep(4);

  } elsif (($plugin eq "MedknowPlugin") || ($plugin eq "ClockssMedknowPlugin")) {
    $url = sprintf("%sbackissues.asp", $param{base_url});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      #showBackIssue.asp?issn=0189-6725;year=2015;volume=12
      #showBackIssue.asp?issn=0022-3859;year=2016;volume=62
      #showBackIssue.asp?issn=$param{journal_issn};year=$param{year};volume=$param{volume_name}
      # http://www.cytojournal.com/backissues.asp => http://www.cytojournal.com/browse.asp?sabs=n
      
      if ($req->url ne $resp->request->uri) {
        $vol_title = $resp->request->uri;
        $result = "Redirected";
        $man_url .= " => ";
        $man_url .= $resp->request->uri;
        # there is one allowed redirect 
        my $i1 = index $req->url, "backissues.asp";
        my $i2 = index $resp->request->uri, "browse.asp";
        if (($i2 > 0) && ($i1 == $i2)) {
          my $s1 = substr $req->url, 0, $i1;
          my $s2 = substr $resp->request->uri, 0, $i2;
          if (($s1 eq $s2) && (defined($man_contents) && ($man_contents =~ m/browse.asp\?date=0-$param{year}/))) {
            if ($man_contents =~ m/<title>(.*)<\/title>/si) {
              $vol_title = $1;
              $vol_title =~ s/\s*\n\s*/ /g;
              $vol_title =~ s/\s+/ /g;
              $vol_title =~ s/: Browse articles?/ Volume $param{volume_name}/i;
            }
            $result = "Manifest"
          }
        }
      } elsif (defined($man_contents) && ($man_contents =~ m/showBackIssue.asp\?issn=$param{journal_issn};year=$param{year};volume=$param{volume_name}/)) {
        if ($man_contents =~ m/<title>(.*)<\/title>/si) {
          $vol_title = $1;
          $vol_title =~ s/\s*\n\s*/ /g;
          $vol_title =~ s/\s+/ /g;
          $vol_title =~ s/: Table of Contents?/ Volume $param{volume_name}/i;
        }
        $result = "Manifest"
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    }
    sleep(4);

  } elsif ($plugin eq "Emerald2020Plugin") {
    $url = sprintf("%sinsight/publication/issn/%s",
      $param{base_url}, $param{journal_issn});
      #params also include: $param{volume_name}
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if (defined($man_contents) && ($man_contents =~ m/$lockss_tag/) && ($man_contents =~ m/\/issn\/$param{journal_issn}\/vol\/$param{volume_name}\/iss\//)) {
        if ($man_contents =~ m/<title> *([^<|]*) | *Emerald Insight<\/title>/si) {
          $vol_title = $1 . " Volume " . $param{volume_name};
          $vol_title =~ s/\s*\n\s*/ /g;
          if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
            $vol_title = "\"" . $vol_title . "\"";
          }
        }
        $result = "Manifest"
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    }
    sleep(4);

  } elsif ($plugin eq "ClockssEmerald2020Plugin") {
    $url = sprintf("%sinsight/publication/issn/%s",
      $param{base_url}, $param{journal_issn});
      #params also include: $param{volume_name}
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if (defined($man_contents) && ($man_contents =~ m/$clockss_tag/) && ($man_contents =~ m/\/issn\/$param{journal_issn}\/vol\/$param{volume_name}\/iss\//)) {
        if ($man_contents =~ m/<title> *([^<|]*) | *Emerald Insight<\/title>/si) {
          $vol_title = $1 . " Volume " . $param{volume_name};
          $vol_title =~ s/\s*\n\s*/ /g;
          if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
            $vol_title = "\"" . $vol_title . "\"";
          }
        }
        $result = "Manifest"
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    }
    sleep(4);

#  #European Mathematical Society Deprecated
#  } elsif ($plugin eq "EuropeanMathematicalSocietyPlugin") {
#    $url = sprintf("%sjournals/all_issues.php?issn=%s",
#      $param{base_url}, $param{journal_issn});
#    $man_url = uri_unescape($url);
#    my $req = HTTP::Request->new(GET, $man_url);
#    my $resp = $ua->request($req);
#    if ($resp->is_success) {
#      my $man_contents = $resp->content;
##      if (defined($man_contents) && ($man_contents =~ m/$lockss_tag/) && (man_contents =~ m/issn=$param{journal_issn}.vol=$param{volume_name}/)) {
#      if ($req->url ne $resp->request->uri) {
#              $vol_title = $resp->request->uri;
#              $result = "Redirected";
#      } elsif (defined($man_contents) && ($man_contents =~ m/$lockss_tag/) && ($man_contents =~ m/vol=$param{volume_name}/)) {
#        if ($man_contents =~ m/<h1>(.*)<\/h1>/si) {
#          $vol_title = $1;
#        }
#        $result = "Manifest"
#      } else {
#        $result = "--NO_TAG--"
#      }
#    } else {
#      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
#    }
#    sleep(4);
#
#  #European Mathematical Society Deprecated
#  } elsif ($plugin eq "ClockssEuropeanMathematicalSocietyPlugin") {
#    $url = sprintf("%sjournals/all_issues.php?issn=%s",
#      $param{base_url}, $param{journal_issn});
#    $man_url = uri_unescape($url);
#    my $req = HTTP::Request->new(GET, $man_url);
#    my $resp = $ua->request($req);
#    if ($resp->is_success) {
#      my $man_contents = $resp->content;
##      if (defined($man_contents) && ($man_contents =~ m/$clockss_tag/) && (man_contents =~ m/issn=$param{journal_issn}.vol=$param{volume_name}/)) {
#      if ($req->url ne $resp->request->uri) {
#              $vol_title = $resp->request->uri;
#              $result = "Redirected";
#      } elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/) && ($man_contents =~ m/vol=$param{volume_name}/)) {
#        if ($man_contents =~ m/<h1>(.*)<\/h1>/si) {
#          $vol_title = $1;
#        }
#        $result = "Manifest"
#      } else {
#        $result = "--NO_TAG--"
#      }
#    } else {
#      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
#    }
#    sleep(4);

  #European Mathematical Society API
  } elsif ($plugin eq "ClockssEuropeanMathematicalSocietyJournalsPlugin" || 
           $plugin eq "EuropeanMathematicalSocietyJournalsPlugin") {
    $url = sprintf("%sserial-issues?filter[serial]=%s&filter[year]=%d",
      $param{api_url}, $param{journal_serial_number}, $param{year});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
#      if (defined($man_contents) && ($man_contents =~ m/$clockss_tag/) && (man_contents =~ m/issn=$param{journal_issn}.vol=$param{volume_name}/)) {
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$param{year}/) && ($man_contents =~ m/$param{journal_serial_number}/)) {
        if (($man_contents =~ m/\"name\":"([^\"]*)\"/si)) { #"
          $vol_title = $1 . " Volume " . $param{year};
        }
        $result = "Manifest"
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    }
    sleep(4);

  #European Mathematical Society Books
  } elsif ($plugin eq "ClockssEuropeanMathematicalSocietyBooks2024Plugin" || 
           $plugin eq "EuropeanMathematicalSocietyBooks2024Plugin") {
    $url = sprintf("%sbooks/%s/%s",
      $param{base_url}, $param{book_serial_id}, $param{book_id});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
#      if (defined($man_contents) && ($man_contents =~ m/$clockss_tag/) && (man_contents =~ m/issn=$param{journal_issn}.vol=$param{volume_name}/)) {
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/>Download [^<]*PDF</) && 
                                        (($man_contents =~ m/$clockss_tag/) && ($plugin eq "ClockssEuropeanMathematicalSocietyBooks2024Plugin") ||
                                         ($man_contents =~ m/$lockss_tag/) && ($plugin eq "EuropeanMathematicalSocietyBooks2024Plugin")) ) {
        if ($man_contents =~ m/<title>([^<]*)<\/title>/si) {
          $vol_title = $1;
        }
        $result = "Manifest"
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    }
    sleep(4);


  } elsif (($plugin eq "SilverchairJournalsPlugin") ||
          ($plugin eq "AmaSilverchairPlugin")) {
    $url = sprintf("%sLOCKSS/ListOfIssues.aspx?resourceId=%d&year=%d",
      $param{base_url}, $param{resource_id}, $param{year});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$lockss_tag/)) {
        $result = "Manifest"
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    }
    sleep(4);

  } elsif (($plugin eq "ClockssSilverchairJournalsPlugin") ||
           ($plugin eq "ClockssAmaSilverchairPlugin")) {
    $url = sprintf("%sLOCKSS/ListOfIssues.aspx?resourceId=%d&year=%d",
      $param{base_url}, $param{resource_id}, $param{year});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/)) {
        $result = "Manifest"
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    }
    sleep(4);


  } elsif ($plugin eq "SilverchairProceedingsPlugin") {
    $url = sprintf("%sLOCKSS/ListOfVolumes.aspx?year=%d",
      $param{base_url}, $param{year});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$lockss_tag/)) {
        $vol_title= "Proceedings for " . $param{year};
        $result = "Manifest"
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    }
    sleep(4);

  } elsif ($plugin eq "ClockssSilverchairProceedingsPlugin") {
    $url = sprintf("%sLOCKSS/ListOfVolumes.aspx?year=%d",
      $param{base_url}, $param{year});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/)) {
        $vol_title= "Proceedings for " . $param{year};
        $result = "Manifest"
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    }
    sleep(4);

  } elsif ($plugin eq "ClockssSilverchairBooksPlugin") {
    $url = sprintf("%sbook.aspx?bookid=%d",
      $param{base_url}, $param{resource_id});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/)) {
      if ($man_contents =~ m/<title>(.*)<\/title>/si) {
          $vol_title = $1;
          $vol_title =~ s/\s*\n\s*/ /g;
          }
        $result = "Manifest"
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    }
    sleep(4);

# GSW Books Silverchair CLOCKSS
  } elsif (($plugin eq "ClockssGeoscienceWorldSilverchairBooksPlugin") ||
           ($plugin eq "ClockssIwapSilverchairBooksPlugin") ||
           ($plugin eq "ClockssRoyalSocietyChemistrySilverchairBooksPlugin")) {
    $url = sprintf("%s%s",
      $param{base_url}, $param{resource_id});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($req->url ne $resp->request->uri) {
        $vol_title = $resp->request->uri;
        $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/)) {
        if ($man_contents =~ m/<title>(.*)<\/title>/si) {
          $vol_title = $1;
          $vol_title =~ s/\s*\n\s*/ /g;
          #<meta name="citation_doi" content="10.1039/9781837671595" />
          if ($man_contents =~ m/ content=.10.1039\/(\d*). \/>/si) {
            $vol_title = $1 . " * " . substr($vol_title, 0, 30);
          }
        }
        $result = "Manifest"
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    }
    sleep(4);

# GSW Books Silverchair GLN
  } elsif (($plugin eq "GeoscienceWorldSilverchairBooksPlugin") ||
           ($plugin eq "IwapSilverchairBooksPlugin") ||
           ($plugin eq "RoyalSocietyChemistrySilverchairBooksPlugin")) {
    $url = sprintf("%s%s",
      $param{base_url}, $param{resource_id});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($req->url ne $resp->request->uri) {
          $vol_title = $resp->request->uri;
          $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$lockss_tag/)) {
        if ($man_contents =~ m/<title>(.*)<\/title>/si) {
          $vol_title = $1;
          $vol_title =~ s/\s*\n\s*/ /g;
          #<meta name="citation_doi" content="10.1039/9781837671595" />
          if ($man_contents =~ m/ content=.10.1039\/(\d*). \/>/si) {
            $vol_title = $1 . " * " . substr($vol_title, 0, 30);
          }
        }
        $result = "Manifest"
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    }
    sleep(4);

  } elsif (($plugin eq "AjtmhPlugin") ||
          ($plugin eq "ClockssAjtmhPlugin")) {
    #"%slockss-manifest/journal/%s/volume/%s", base_url, journal_id, volume_name
    $url = sprintf("%slockss-manifest/journal/%s/volume/%s",
        $param{base_url}, $param{journal_id}, $param{volume_name});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    #printf("resp is %s\n",$resp->status_line);
    my $man_contents = $resp->is_success ? $resp->content : "";
    if (! $resp->is_success) {
        $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    } elsif ($req->url ne $resp->request->uri) {
        $vol_title = $resp->request->uri;
        $result = "Redirected";
    } elsif (! defined($man_contents)) {
        $result = "--NOT_DEF--";
    } elsif (($man_contents !~ m/$lockss_tag/) && ($man_contents !~ m/$lockss_tag/)) {
        $result = "--NO_TAG--";
    } elsif ($man_contents !~ m/href=\"\/view\/journals\/$param{journal_id}\/$param{volume_name}\//) {
        #/view/journals/tpmd/100/6/tpmd.100.issue-6.xml
        $result = "--BAD_VOL--";
    } else {
        $result = "Manifest";
        if ($man_contents =~ m/<h1>([^<]*)<\/h1>/si) {
            $vol_title = $1;
            $vol_title =~ s/\s*\n\s*/ /g;
        }
    }
  sleep(4);
    
  #Silverchair GLN. Includes year/volume_name.
  } elsif (($plugin eq "GeoscienceWorldSilverchairPlugin")) {
    $url = sprintf("%s%s/list-of-issues/%d",
        $param{base_url}, $param{journal_id}, $param{year});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    #printf("resp is %s\n",$resp->status_line);
    my $man_contents = $resp->is_success ? $resp->content : "";
    if (! $resp->is_success) {
        $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    } elsif ($req->url ne $resp->request->uri) {
        $vol_title = $resp->request->uri;
        $result = "Redirected";
    } elsif (! defined($man_contents)) {
        $result = "--NOT_DEF--";
    } elsif ($man_contents !~ m/$lockss_tag/) {
        $result = "--NO_TAG--";
    } elsif ($man_contents !~ m/href=\"\/$param{journal_id}\/issue\/$param{volume_name}\//) {
        $result = "--BAD_VOL--";
    } else {
        $result = "Manifest";
        if ($man_contents =~ m/<title>Browse Issues \| ([^|]*) \| GeoScienceWorld<\/title>/si) {
            $vol_title = $1;
            $vol_title =~ s/\s*\n\s*/ /g;
        }
    }
  sleep(4);
    
  #Silverchair CLOCKSS. Includes year/volume_name.
  } elsif ($plugin eq "ClockssGeoscienceWorldSilverchairPlugin") {
    $url = sprintf("%s%s/list-of-issues/%d",
        $param{base_url}, $param{journal_id}, $param{year});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    #printf("resp is %s\n",$resp->status_line);
    my $man_contents = $resp->is_success ? $resp->content : "";
    if (! $resp->is_success) {
        $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    } elsif ($req->url ne $resp->request->uri) {
        $vol_title = $resp->request->uri;
        $result = "Redirected";
    } elsif (! defined($man_contents)) {
        $result = "--NOT_DEF--";
    } elsif ($man_contents !~ m/$clockss_tag/) {
        $result = "--NO_TAG--";
    } elsif ($man_contents !~ m/href=\"\/$param{journal_id}\/issue\/$param{volume_name}\//) {
        $result = "--BAD_VOL--";
    } else {
        $result = "Manifest";
        if ($man_contents =~ m/<title>Browse Issues \| ([^|]*) \| GeoScienceWorld<\/title>/si) {
            $vol_title = $1;
            $vol_title =~ s/\s*\n\s*/ /g;
        }
    }
  sleep(4);
  
  #Silverchair GLN no year/volume_name. Common Theme
  } elsif (($plugin eq "AllenPressSilverchairPlugin") ||
           ($plugin eq "AOTASilverchairPlugin") ||
           ($plugin eq "ASHSilverchairPlugin") ||
           ($plugin eq "IwapSilverchairPlugin")) {
    $url = sprintf("%s%s/issue/browse-by-year/%d",
      $param{base_url}, $param{journal_id}, $param{year});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    #printf("resp is %s\n",$resp->status_line);
    my $man_contents = $resp->is_success ? $resp->content : "";
    if (! $resp->is_success) {
        $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    } elsif ($req->url ne $resp->request->uri) {
        $vol_title = $resp->request->uri;
        $result = "Redirected";
    } elsif (! defined($man_contents)) {
        $result = "--NOT_DEF--";
    } elsif ($man_contents !~ m/$lockss_tag/) {
        $result = "--NO_TAG--";
    } elsif ($man_contents !~ m/href=\"\/$param{journal_id}\/issue\/\d/) {
        $result = "--BAD_VOL--";
    } else {
        $result = "Manifest";
        if ($man_contents =~ m/<title>(.*) [|] IWA Publishing<\/title>/si) {
            $vol_title = $1;
            $vol_title =~ s/\s*\n\s*/ /g;
        }
    }
    sleep(4);

  #Silverchair CLOCKSS no year/volume_name. Common Theme
  } elsif (($plugin eq "ClockssAAISilverchairPlugin") || 
           ($plugin eq "ClockssAllenPressSilverchairPlugin") ||
           ($plugin eq "ClockssAOTASilverchairPlugin") ||
           ($plugin eq "ClockssIwapSilverchairPlugin")) { 
    $url = sprintf("%s%s/issue/browse-by-year/%d",
      $param{base_url}, $param{journal_id}, $param{year});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    #printf("resp is %s\n",$resp->status_line);
    my $man_contents = $resp->is_success ? $resp->content : "";
    if (! $resp->is_success) {
        $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    } elsif ($req->url ne $resp->request->uri) {
        $vol_title = $resp->request->uri;
        $result = "Redirected";
    } elsif (! defined($man_contents)) {
        $result = "--NOT_DEF--";
    } elsif ($man_contents !~ m/$clockss_tag/) {
        $result = "--NO_TAG--";
    } elsif ($man_contents !~ m/href=\"\/$param{journal_id}\/issue\/\d/) {
        #$vol_title = $1;
        $result = "--BAD_VOL--";
    } else {
        $result = "Manifest";
        if ($man_contents =~ m/<title>(.*) [|] IWA Publishing<\/title>/si) {
            $vol_title = $1;
            $vol_title =~ s/\s*\n\s*/ /g;
        }
    }
    sleep(4);
    
  #Silverchair GLN. Includes year/volume_name. Common Theme
  } elsif (($plugin eq "AAPSilverchairPlugin") || 
           ($plugin eq "AACRSilverchairPlugin") || 
           ($plugin eq "AmericanDiabetesAssociationSilverchairPlugin") || 
           ($plugin eq "CompanyBiologistsSilverchairPlugin") || 
           ($plugin eq "PortlandPressSilverchairPlugin") || 
           ($plugin eq "RockefellerUniversityPressSilverchairPlugin") || 
           ($plugin eq "UCPressSilverchairPlugin")) {
    $url = sprintf("%s%s/issue/browse-by-year/%d",
      $param{base_url}, $param{journal_id}, $param{year});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    #printf("resp is %s\n",$resp->status_line);
    my $man_contents = $resp->is_success ? $resp->content : "";
    if (! $resp->is_success) {
        $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    } elsif ($req->url ne $resp->request->uri) {
        $vol_title = $resp->request->uri;
        $result = "Redirected";
    } elsif (! defined($man_contents)) {
        $result = "--NOT_DEF--";
    } elsif ($man_contents !~ m/$lockss_tag/) {
        $result = "--NO_TAG--";
    } elsif (($man_contents !~ m/href=\"\/$param{journal_id}\/issue\/$param{volume_name}\//) && ($man_contents !~ m/href=\"\/$param{journal_id}\/issue\/volume\/$param{volume_name}/)) {
        $result = "--BAD_VOL--";
    } else {
        $result = "Manifest";
        if ($man_contents =~ m/<title>(.*) [|] IWA Publishing<\/title>/si) {
            $vol_title = $1;
            $vol_title =~ s/\s*\n\s*/ /g;
        }
    }
    sleep(5);

  #Silverchair CLOCKSS. Includes year/volume_name. Common Theme
  } elsif (($plugin eq "ClockssAAPSilverchairPlugin") || 
           ($plugin eq "ClockssAACRSilverchairPlugin") || 
           ($plugin eq "ClockssAmericanDiabetesAssociationSilverchairPlugin") || 
           ($plugin eq "ClockssCompanyBiologistsSilverchairPlugin") || 
           ($plugin eq "ClockssRockefellerUniversityPressSilverchairPlugin") || 
           ($plugin eq "ClockssPortlandPressSilverchairPlugin")) { 
    $url = sprintf("%s%s/issue/browse-by-year/%d",
      $param{base_url}, $param{journal_id}, $param{year});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    #printf("resp is %s\n",$resp->status_line);
    my $man_contents = $resp->is_success ? $resp->content : "";
    if (! $resp->is_success) {
        $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    } elsif ($req->url ne $resp->request->uri) {
        $vol_title = $resp->request->uri;
        $result = "Redirected";
    } elsif (! defined($man_contents)) {
        $result = "--NOT_DEF--";
    } elsif ($man_contents !~ m/$clockss_tag/) {
        $result = "--NO_TAG--";
    } elsif ($man_contents !~ m/href=\"\/$param{journal_id}\/issue\/$param{volume_name}\//) {
        #$vol_title = $1;
        $result = "--BAD_VOL--";
    } else {
        $result = "Manifest";
        if ($man_contents =~ m/<title>(.*) [|] IWA Publishing<\/title>/si) {
            $vol_title = $1;
            $vol_title =~ s/\s*\n\s*/ /g;
        }
    }
    sleep(5);
    
  #Silverchair GLN. Includes year/volume_name. SilverchairScholarlyPublishingCollectivePlugin
  } elsif (($plugin eq "PSUPSilverchairPlugin")) {
    $url = sprintf("%s%s/%s/issue/browse-by-year/%d",
      $param{base_url}, $param{college_id}, $param{journal_id}, $param{year});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    #printf("resp is %s\n",$resp->status_line);
    my $man_contents = $resp->is_success ? $resp->content : "";
    if (! $resp->is_success) {
        $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    } elsif ($req->url ne $resp->request->uri) {
        $vol_title = $resp->request->uri;
        $result = "Redirected";
    } elsif (! defined($man_contents)) {
        $result = "--NOT_DEF--";
    } elsif ($man_contents !~ m/$lockss_tag/) {
        $result = "--NO_TAG--";
    } elsif (($man_contents !~ m/href=\"\/$param{college_id}\/$param{journal_id}\/issue\/$param{volume_name}\//) && ($man_contents !~ m/href=\"\/$param{college_id}\/$param{journal_id}\/issue\/volume\/$param{volume_name}/)) {
        $result = "--BAD_VOL--";
    } else {
        $result = "Manifest";
        if ($man_contents =~ m/<title>(.*)<\/title>/si) {
            $vol_title = $1;
            $vol_title =~ s/\s*\n\s*/ /g;
        }
    }
    sleep(4);

  #Silverchair CLOCKSS. Includes year/volume_name. SilverchairScholarlyPublishingCollectivePlugin
  } elsif (($plugin eq "ClockssPSUPSilverchairPlugin")) { 
    $url = sprintf("%s%s/%s/issue/browse-by-year/%d",
      $param{base_url}, $param{college_id}, $param{journal_id}, $param{year});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    #printf("resp is %s\n",$resp->status_line);
    my $man_contents = $resp->is_success ? $resp->content : "";
    if (! $resp->is_success) {
        $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    } elsif ($req->url ne $resp->request->uri) {
        $vol_title = $resp->request->uri;
        $result = "Redirected";
    } elsif (! defined($man_contents)) {
        $result = "--NOT_DEF--";
    } elsif ($man_contents !~ m/$clockss_tag/) {
        $result = "--NO_TAG--";
    } elsif (($man_contents !~ m/href=\"\/$param{college_id}\/$param{journal_id}\/issue\/$param{volume_name}\//) && ($man_contents !~ m/href=\"\/$param{college_id}\/$param{journal_id}\/issue\/volume\/$param{volume_name}/)) {
#    } elsif ($man_contents !~ m/href=\"\/$param{college_id}\/$param{journal_id}\/issue\/$param{volume_name}\//) {
        #$vol_title = $1;
        $result = "--BAD_VOL--";
    } else {
        $result = "Manifest";
        if ($man_contents =~ m/<title>(.*)<\/title>/si) {
            $vol_title = $1;
            $vol_title =~ s/\s*\n\s*/ /g;
        }
    }
    sleep(4);
    
  #Silverchair CLOCKSS OUP & DUP. No year/volume_name
  } elsif (($plugin eq "ClockssOupSilverchairPlugin") || 
           ($plugin eq "ClockssDupSilverchairPlugin")) {
    $url = sprintf("%s%s/list-of-issues/%d",
        $param{base_url}, $param{journal_id}, $param{year});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    #printf("resp is %s\n",$resp->status_line);
    my $man_contents = $resp->is_success ? $resp->content : "";
    if (! $resp->is_success) {
        $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    } elsif ($req->url ne $resp->request->uri) {
        $vol_title = $resp->request->uri;
        $result = "Redirected";
    } elsif (! defined($man_contents)) {
        $result = "--NOT_DEF--";
    } elsif ($man_contents !~ m/$clockss_tag/) {
        $result = "--NO_TAG--";
    } elsif ($man_contents !~ m/href=\"\/$param{journal_id}\/issue\/\d/) {
        $result = "--NO_VOL--";
    } else {
        $result = "Manifest";
        if ($man_contents =~ m/<title>(.*) [|] \w* University Press<\/title>/si) {
            $vol_title = $1;
            $vol_title =~ s/\s*\n\s*/ /g;
        }
    }
  sleep(5);
    
  #Silverchair CLOCKSS OUP & DUP. No year/volume_name
  } elsif (($plugin eq "OupSilverchairPlugin") || 
           ($plugin eq "DupSilverchairPlugin")) {
    $url = sprintf("%s%s/list-of-issues/%d",
        $param{base_url}, $param{journal_id}, $param{year});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    #printf("resp is %s\n",$resp->status_line);
    my $man_contents = $resp->is_success ? $resp->content : "";
    if (! $resp->is_success) {
        $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    } elsif ($req->url ne $resp->request->uri) {
        $vol_title = $resp->request->uri;
        $result = "Redirected";
    } elsif (! defined($man_contents)) {
        $result = "--NOT_DEF--";
    } elsif ($man_contents !~ m/$lockss_tag/) {
        $result = "--NO_TAG--";
    } elsif ($man_contents !~ m/href=\"\/$param{journal_id}\/issue\/\d/) {
        $result = "--NO_VOL--";
    } else {
        $result = "Manifest";
        if ($man_contents =~ m/<title>(.*) [|] \w* University Press<\/title>/si) {
            $vol_title = $1;
            $vol_title =~ s/\s*\n\s*/ /g;
        }
    }
  sleep(5);
    
  } elsif (($plugin eq "ClockssAnuPlugin")) {
    #$url = sprintf("%spublications/%s", $param{base_url}, $param{journal_id});
    $url = sprintf("%spublications/journals/%s", $param{base_url}, $param{journal_id});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    #printf("resp is %s\n",$resp->status_line);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/publications\/journals\/$param{journal_id}\/\S*-$param{volume_name}/)) {
#      } elsif (defined($man_contents) && ($man_contents =~ m/\/$param{journal_id}\[^"\]*-volume-$param{volume_name}\[^"\]*\"/)) {
        # no lockss permission statement on start page. Permission statements are here: https://press.anu.edu.au/lockss.txt
        if ($man_contents =~ m/<h1>(.*)<\/h1>/si) {
          $vol_title = $1 . ": " . $param{volume_name}
          #$vol_title = $1 
        }
        $result = "Manifest"
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    }
    sleep(4);
    
  } elsif (($plugin eq "ClockssAnuBooksPlugin")) {
    $url = sprintf("%spublications/%s", $param{base_url}, $param{book_uri});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    #printf("resp is %s\n",$resp->status_line);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents)) {
        # no lockss permission statement on start page. Permission statement is here: https://press.anu.edu.au/lockss.txt
        # In order to do more better validation, would require searching all the pages for a match to the specific issue link(s)
        # CLOCKSS only so probably okay to not do this
        if ($man_contents =~ m/<title>(.*)<\/title>/si) {
          #$vol_title = $1 . ": " . $param{volume_name}
          $vol_title = $1 
        }
        $result = "Manifest"
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    }
    sleep(4);
    
  } elsif ($plugin eq "ClockssJstorCurrentScholarshipPlugin"){
      $url = sprintf("%sclockss-manifest/%s/%s",
      $param{base_url}, $param{journal_id}, $param{year});
      $man_url = uri_unescape($url);
      my $req = HTTP::Request->new(GET, $man_url);
      my $resp = $ua->request($req);
      if ($resp->is_success) {
          my $man_contents = $resp->content;
          #JSTOR Current Scholarship links http://www.jstor.org/stable/10.2972/hesperia.84.issue-1
          if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
          } elsif (defined($man_contents) && (($man_contents =~ m/$clockss_tag/) && 
                  ($man_contents =~ m/stable\/[0-9.]+\//))) {
              $result = "Manifest";
          } else {
              $result = "--NO_TAG--"
          }
      } else {
          $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
      }
      sleep(4);
  } elsif ($plugin eq "JstorCurrentScholarshipPlugin"){
      #clockss and lockss use the same manifest page
      $url = sprintf("%sclockss-manifest/%s/%s",
      $param{base_url}, $param{journal_id}, $param{year});
      $man_url = uri_unescape($url);
      my $req = HTTP::Request->new(GET, $man_url);
      my $resp = $ua->request($req);
      if ($resp->is_success) {
          my $man_contents = $resp->content;
          #JSTOR Current Scholarship links http://www.jstor.org/stable/10.2972/hesperia.84.issue-1
          if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
          } elsif (defined($man_contents) && (($man_contents =~ m/$lockss_tag/) && 
                  ($man_contents =~ m/stable\/[0-9.]+\//))) {
              $result = "Manifest";
          } else {
              $result = "--NO_TAG--"
          }
      } else {
          $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
      }
      sleep(4);
      
  } elsif (($plugin eq "PeerJ2016Plugin") || ($plugin eq "ClockssPeerJ2016Plugin")) {
        $url = sprintf("%sarchives/?year=%s&journal=%s",
          $param{base_url}, $param{volume_name}, $param{journal_id});
        $man_url = uri_unescape($url);
        my $year = $param{volume_name};
        my $req = HTTP::Request->new(GET, $man_url);
        my $resp = $ua->request($req);
        if ($resp->is_success) {
            my $man_contents = $resp->content;
            #no lockss permission statement on start page. Permission statement is here: https://peerj.com/lockss.txt
            if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
	    #make sure there is a link to an issue
            } elsif (defined($man_contents) && ($man_contents =~ m/articles\/index\.html\?month=$year-/)) {
                if ($man_contents =~ m/<h1[^>]+>(\s*<a href[^>]+>)(.*)(<\/a>\s*):([^<]+)<\/h1>/si) {
                    $vol_title = "$2$4"
                }
                $result = "Manifest"
            } else {
                $result = "--"
            }
        } else {
            $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
        }
        sleep(4);

  } elsif (($plugin eq "IUCrOaiPlugin") || ($plugin eq "ClockssIUCrOaiPlugin")) {
    #permission is different from start
    $url = sprintf("%se/issues/2010/lockss.html", $param{base_url});
    $perm_url = uri_unescape($url);
    #start_url for all OAI queries
    $url = sprintf("%scgi-bin/oai?verb=ListRecords&set=%s&metadataPrefix=oai_dc",
      $param{script_url}, $param{au_oai_set});
    if (defined($param{au_oai_date}) && $param{au_oai_date} =~ m/^[0-9-]{7}$/) {
      my ($mo) = $param{au_oai_date} =~ m/^[0-9]{4}-([0-9]{2})$/;
      my $dy = '28';
      my @mon = ('01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12');
      my @ldy = ('31', '28', '31', '30', '31', '30', '31', '31', '30', '31', '30', '31');
      #if (year is divisible by 4) then February has 29 days.
      my %lday;
      @lday{@mon} = @ldy;
      $dy = $lday{$mo};
      $url = $url . "&from=" . $param{au_oai_date} . "-01" . "&until=" . $param{au_oai_date} . "-" . $dy;
    }
    $man_url = uri_unescape($url);
    my $req_p = HTTP::Request->new(GET, $perm_url);
    my $resp_p = $ua->request($req_p);
    my $req_s = HTTP::Request->new(GET, $man_url);
    my $resp_s = $ua->request($req_s);
    if ($resp_p->is_success) {
      my $perm_contents = $resp_p->content;
      my $lcl_tag = ($plugin eq "ClockssIUCrOaiPlugin") ? $clockss_tag : $lockss_tag;
      $lcl_tag =~ s/ /./g;
      if (defined($perm_contents) && ($perm_contents =~ m/$lcl_tag/s)) {
        if ($resp_s->is_success) {
          if ($resp_s->content =~ m/results in an empty (set|list)/is) {
            $result = "--EMPTY_LIST--";
          } elsif (!($resp_s->content =~ m/<date>$param{au_oai_date}-/is)) {
            $result = "--NO_DATE_MATCH--";
          } else {
            $result = "Manifest";
          }
        } else {
          #printf("URL: %s\n", $man_url);
          $result = "--REQ_FAIL--"
        }
      } else {
        #printf("URL: %s\n", $perm_url);
        $result = "--NO_LOCKSS--"
      }
    } else {
      #printf("URL: %s\n", $perm_url);
      $result = "--PERM_REQ_FAIL--"
    }
    sleep(4);
    
#  } elsif ($plugin eq "ClockssSilvaFennicaPlugin") {
#    #Old Url with list of articles https://www.silvafennica.fi/issue/sf/volume/50
#    #$url = sprintf("%sissue/%s/volume/%s",
#    #New start url: "%svolume/%s", base_url, volume_name
#    #New permission url: "%s", base_url
#    $url = sprintf("%svolume/%s",
#        $param{base_url}, $param{volume_name});
#    $start_url = uri_unescape($url);
#    my $req_s = HTTP::Request->new(GET, $start_url);
#    my $resp_s = $ua->request($req_s);
#    #For reporting at the end
#    $man_url = $start_url;
#    if ($resp_s->is_success) {
#      my $start_contents = $resp_s->content;
#      if (defined($start_contents) && ($start_contents =~ m/>(Silva Fennica vol. $param{volume_name})</)) {
#        $vol_title = $1;
#        $result = "Manifest"
#      } else {
#        $result = "--NO_TAG--"
#      }
#    } else {
#      $result = "--REQ_FAIL--"
#    }
#    sleep(4);
    
  } elsif ($plugin eq "ClockssSilvaFennicaNoJidPlugin" || $plugin eq "ClockssSilvaFennicaPlugin") {
    #Old url with list of articles https://www.silvafennica.fi/issue/sf/volume/50
    #$url = sprintf("%sissue/%s/volume/%s",
    #Old url with list of articles https://www.dissertationesforestales.fi/issue/volume/2021
    #$url = sprintf("%sissue/volume/%s",

    #New start url: "%svolume/%s", base_url, volume_name
    $url_s = sprintf("%svolume/%s",
        $param{base_url}, $param{volume_name});
    $start_url = uri_unescape($url_s);
    my $req_s = HTTP::Request->new(GET, $start_url);
    my $resp_s = $ua->request($req_s);
    #New permission url: "%s", base_url
    $url_p = sprintf("%s",
        $param{base_url});
    $perm_url = uri_unescape($url_p);
    my $req_p = HTTP::Request->new(GET, $perm_url);
    my $resp_p = $ua->request($req_p);
    #For reporting at the end
    $man_url = $start_url;
    if ($resp_s->is_success && $resp_p->is_success) {
      my $start_contents = $resp_s->content;
      my $perm_contents = $resp_p->content;
      if (defined($perm_contents) && ($perm_contents =~ m/$cc_license_url/)) {
        if (defined($start_contents) && ($start_contents =~ m/<h1>\s*([A-Z][^<]+ $param{volume_name})[^<]*</)) {
          $vol_title = $1;
          $vol_title =~ s/\s*\n\s*/ /g;
          $result = "Manifest"
        } else {
            $result = "--NO_CONT--"
        }
      } else {
        $result = "--NO_TAG--";
        $vol_title = $perm_url
      }
    } else {
      $result = "--REQ_FAIL--"
    }
    sleep(4);
    
  } elsif ($plugin eq "ClockssSpandidos2020Plugin" || $plugin eq "Spandidos2020Plugin") {
      # manifest page has a link to the current volume only.
      # need to look at an issue page to confirm content.

      my $url_sprintf = sprintf("%s%s/archive",$param{base_url}, $param{journal_id});
      $man_url = uri_unescape($url_sprintf);
      my $man_req = HTTP::Request->new(GET, $man_url);
      my $man_resp = $ua->request($man_req);

      my $url_issue = sprintf("%s%s/%s/1",$param{base_url}, $param{journal_id}, $param{volume_name});
      $issue_url = uri_unescape($url_issue);
      my $issue_req = HTTP::Request->new(GET, $issue_url);
      my $issue_resp = $ua->request($issue_req);

      if ($man_resp->is_success && $issue_resp->is_success) {
      my $man_contents = $man_resp->content;
      my $issue_contents = $issue_resp->content;
      if ($man_req->url ne $man_resp->request->uri) {
          $vol_title = $man_resp->request->uri;
          $result = "Redirected";
      } elsif (defined($man_contents) && defined($issue_contents)) {
          #for gln, permission page is on start_url
          my $perm_contents = $man_resp->content; 
          my $lcl_tag = $lockss_tag;
          #for CLOCKSS permission is on https://www.spandidos-publications.com/lockss.txt
          if ($plugin eq "ClockssSpandidos2020Plugin" || $plugin eq "Spandidos2020Plugin") {
            if ($plugin eq "ClockssSpandidos2020Plugin") {
              $lcl_tag = $clockss_tag;
            }
            my $perm_url_sprintf = sprintf("%slockss.txt",$param{base_url});
            my $perm_url = uri_unescape($perm_url_sprintf );
            my $perm_req = HTTP::Request->new(GET, $perm_url);
            my $perm_resp = $ua->request($perm_req);
            #if this fails, it just won't reset - which will fail to have permission - so okay
            if ($perm_resp->is_success) {
                $perm_contents = $perm_resp->content;
            }
          }
          $lcl_tag =~ s/ /./g;
          if (defined($perm_contents) && ($perm_contents =~ m/$lcl_tag/s)) {
          #we have a permission statement, does the issue url contain a pointer to the journal?
          my $jour_link = sprintf("href=.+/%s",$param{journal_id});
          if ($man_contents =~ m/$jour_link/gi) {
              $vol_title = $param{journal_id}; #default_value
              if ($man_contents =~ m/<title>\s*(.*)\s*<\/title>/si) {
              $vol_title = $1; #better value
              }
              $result = "Manifest";
          } else {
              #had a manifest page but it had no pointer to the journal
              $result = "--NO_URL--";
          }
          } else {
          $result = "--TAG_FAIL--";
          }
      }
      } else {
      #printf("URL: %s\n", $man_url);
      $result = "--REQ_FAIL--";
      }

    sleep(4);
  # End Spandidos plugin check

  } elsif ($plugin eq "ClockssGigaSciencePlugin") {
      $url = sprintf("%s/api/list?start_date=%d-01-01&end_date=%d-12-31",
      $param{base_url}, $param{year}, $param{year});
      $man_url = uri_unescape($url);
      my $req = HTTP::Request->new(GET, $man_url);
      my $resp = $ua->request($req);
      if ($resp->is_success) {
          my $man_contents = $resp->content;
          if ($req->url ne $resp->request->uri) {
            $vol_title = "Giga Science " . $param{year};
            $result = "Redirected";
          } elsif (defined($man_contents)) {
              if ($man_contents =~ m/<doi>(.*)<\/doi>/si) {
                  $vol_title= "Giga Science " . $param{year};
              }
              $result = "Manifest"
          } else {
              $result = "--"
          }
      } else {
          $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
      }
      sleep(4);
  # End of Giga Science

  # Plugins for small publishers. It's not worth it to write a complete manifest page checker
  } elsif (($plugin eq "ClockssArchivesPharmacyPracticeJournalsPlugin") ||
          ($plugin eq "ClockssDeepBlueOaiPlugin") ||
          ($plugin eq "ClockssEuropeanJournalBusinessScienceTechnologyPlugin") ||
          ($plugin eq "ClockssGhentUniversityLibraryDomainModeJournalsPlugin") ||
          ($plugin eq "GhentUniversityLibraryDomainModeJournalsPlugin") ||
          ($plugin eq "ClockssSciELO2024Plugin") ||
          ($plugin eq "BMCRPlugin") ||
          ($plugin eq "MolecularVisionPlugin") ||
          ($plugin eq "JRS2022Plugin") ||
          ($plugin eq "EJCJS2022Plugin") ||
          ($plugin eq "ClockssKarePlugin") ||
          ($plugin eq "TESLEJ2022Plugin") ||
          ($plugin eq "InformationResearchPlugin") ||
          ($plugin eq "ClockssOjs3Plugin" && $param{base_url} =~ "talenta")) {
        $url = sprintf("%s", $param{base_url});
        $man_url = uri_unescape($url);
        $result = "Manifest";
        $vol_title = "Small Publisher--No MPC";
      sleep(4);
  # End of small publishers

  # begin Resilience Alliance
  } elsif ($plugin eq "ClockssResilienceAlliancePlugin") {
         $url = sprintf("%sissue/",
         $param{base_url});
         $man_url = uri_unescape($url);
         my $req = HTTP::Request->new(GET, $man_url);
         my $resp = $ua->request($req);
         if (($resp->is_success)) {
             my $man_contents = $resp->content;
             # There is a single manifest page which lists all volumes and has the permission statement: {base_url}issues/
             if ($req->url ne $resp->request->uri) {
               $vol_title = "Resilience Alliance All Issues";
               $result = "Redirected";
             } elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/) && ($man_contents =~ m/\/vol$param{volume_name}\//)) {
                 # <td style="text-align: center; vertical-align: middle" rowspan="2">14</td> - volume "14"
                 if ($man_contents =~ m/>([^>]*) ISSN/si) {
                     $volume_name = $param{volume_name};
                     $vol_title= $1 . " Volume " . $volume_name;
                 }
                 $result = "Manifest"
             } else {
                 $result = "--"
             }
         } else {
             $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
         }
         sleep(4);
  # End of Resilience Alliance

  # begin CloudPublish/Liverpool University Press
  } elsif ($plugin eq "LupPlugin" || $plugin eq "ClockssLupPlugin") {
         $url = sprintf("%slockss-manifest/%s/%s",
         $param{base_url}, $param{journal_id}, $param{volume_name});
         #$url = "https://www.liverpooluniversitypress.co.uk/lockss-manifest/archives/26/"; #debug
         #$url = "https://www.liverpooluniversitypress.co.uk"; #debug
         $man_url = uri_unescape($url);
         my $req = HTTP::Request->new(GET, $man_url);
         my $resp = $ua->request($req);
         #print($resp->headers_as_string); #debug
         #print("--------"); #debug
         #print($resp->content); #debug 
         if (($resp->is_success)) {
             my $man_contents = $resp->content;
             if ($req->url ne $resp->request->uri) {
               #$vol_title = "All Journals";
               $vol_title = $resp->request->uri;
               $result = "Redirected";
             } elsif (defined($man_contents) && ($man_contents =~ m/$lockss_tag/) && ($man_contents =~ m/$clockss_tag/) && ($man_contents =~ m/Volume $param{volume_name}/)) {
                 if ($man_contents =~ m/<title>\s*(.+)\s*<\/title>/) {
                     $vol_title= $1 . " Volume " . $param{volume_name}
                 }
                 $result = "Manifest"
             } else {
                 $result = "--"
             }
         } else {
             $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
         }
         sleep(4);
  # End of Liverpool University Press

  # begin Inter-Research Science Publisher
  #"%sjournals/%s/%s-home/", base_url, journal_id, journal_id
  } elsif ($plugin eq "ClockssInterResearchPlugin") {
         #****
         $s_url = sprintf("%sjournals/%s/%s-home/",
         #$s_url = sprintf("%sjournals/%s",
             $param{base_url}, $param{journal_id}, $param{journal_id});
             #$param{base_url}, $param{journal_id});
         $start_url = uri_unescape($s_url);
         my $s_req = HTTP::Request->new(GET, $start_url);
         my $s_resp = $ua->request($s_req);
         #****
         #"%slockss", base_url
         $p_url = sprintf("%slockss",
             $param{base_url});
         $perm_url = uri_unescape($p_url);
         my $p_req = HTTP::Request->new(GET, $perm_url);
         my $p_resp = $ua->request($p_req);
         #****
         $start_vol = $param{volume};
         $start_vol =~ s/-.*//;
         $end_vol = $param{volume};
         $end_vol =~ s/.*-//;
         $man_url = $start_url;
         #printf("* %s * * %d *\n", $param{year}, $my_year); #debug
         if (($s_resp->is_success) && ($p_resp->is_success)) {
             my $start_contents = $s_resp->content;
             my $perm_contents = $p_resp->content;
             #if ($s_req->url ne $s_resp->request->uri) || ($p_req->url ne $p_resp->request->uri){
             if ($s_req->url ne $s_resp->request->uri){
               $vol_title = $s_resp->request->uri;
               $result = "Redirected";
             #} elsif (defined($start_contents) && defined($perm_contents) && ($perm_contents =~ m/$clockss_tag/) && ($start_contents =~ m/\/$param{journal_id}\/v$start_vol/)) {
             } elsif (defined($start_contents) && 
                      defined($perm_contents) && 
                      ($perm_contents =~ m/$clockss_tag/) && 
                      ($start_contents =~ m/$param{year}:[^:"]*href=\"abstracts\/$param{journal_id}\/v$start_vol/s) && 
                      (($start_contents =~ m/$param{year}:[^:]*href=\"abstracts\/$param{journal_id}\/v$end_vol[^"]*"[^:"]*\/td/s) ||
                       ($start_contents =~ m/$param{year}:[^:]*href=\"abstracts\/$param{journal_id}\/v$end_vol[^"]*"[^:"]*"tooltiptext"[^:"]*\/td/s)) &&
                      ($param{year} < $my_year)) {
                 $vol_title= $param{journal_id} . " volume " . $param{volume} . " " . $param{year};
                 $result = "Manifest";
             } else {
                 $result = "--"
             }
         } else {
             $result = "--REQ_FAIL--" . $s_resp->code() . " " . $s_resp->message();
         }
         sleep(4);
  # End of Inter-Research Science Publisher


  # Start ClockssWroclawMedicalUniversityJournalsPlugin.
  } elsif ($plugin eq "ClockssWroclawMedicalUniversityJournalsPlugin") {
    $url = sprintf("%sen/archive/",
        $param{base_url});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    #printf("resp is %s\n",$resp->status_line);
    my $man_contents = $resp->is_success ? $resp->content : "";
    if (! $resp->is_success) {
        $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    } elsif ($req->url ne $resp->request->uri) {
        $vol_title = $resp->request->uri;
        $result = "Redirected";
    } elsif (! defined($man_contents)) {
        $result = "--NOT_DEF--";
    } elsif ($man_contents !~ m/$clockss_tag/) {
        $result = "--NO_TAG--";
    } elsif ($man_contents !~ m/\/issue\/$param{year}\/$param{volume_name}\/\d/) {
        $result = "--NO_VOL--";
    } else {
        $result = "Manifest";
        if ($man_contents =~ m/<title>(.*)<\/title>/si) {
            $vol_title = $1 . " Volume " . $param{volume_name} . " Year " . $param{year};
            $vol_title =~ s/\s*\n\s*/ /g;
        }
    }
  sleep(4);
  # End ClockssWroclawMedicalUniversityJournalsPlugin

  # Start Arkat Arkivoc.
  #https://www.arkat-usa.org/arkivoc-journal #list of volumes *NOT FOUND
  #https://www.arkat-usa.org/browse-arkivoc/browse-arkivoc/2022 #toc
  #https://www.arkat-usa.org/arkatusa/clockss/?year=2022 #metadata
  #https://www.arkat-usa.org/browse-arkivoc/ #permission statement
  } elsif ($plugin eq "ClockssArkivoc2022Plugin" || $plugin eq "Arkivoc2022Plugin") {
      $url_p = sprintf("%sbrowse-arkivoc/", $param{base_url});
      #printf("url_p is %s\n",$url_p);
      $man_url_p = uri_unescape($url_p);
      my $p_req = HTTP::Request->new(GET, $man_url_p);
      my $p_resp = $ua->request($p_req);
      #printf("resp is %s\n",$p_resp->status_line); #debug
      my $perm_contents = $p_resp->is_success ? $p_resp->content : "";
      ###
      $url_s = sprintf("%sbrowse-arkivoc/browse-arkivoc/%s", $param{base_url}, $param{year});
      $man_url_s = uri_unescape($url_s);
      my $s_req = HTTP::Request->new(GET, $man_url_s);
      my $s_resp = $ua->request($s_req);
      #printf("resp is %s\n",$s_resp->status_line); #debug
      my $start_contents = $s_resp->is_success ? $s_resp->content : "";
      ###
      $man_url = $man_url_s;
      if (! $s_resp->is_success || ! $p_resp->is_success ) {
        $result = "--REQ_FAIL--" . $p_resp->code() . " " . $p_resp->message() . " " . $s_resp->code() . " " . $s_resp->message();
      } elsif ($p_req->url ne $p_resp->request->uri) {
        $vol_title = $p_resp->request->uri;
        $result = "Redirected";
      } elsif (! defined($perm_contents)) {
        $result = "--NOT_DEF--";
      #} elsif ($perm_contents !~ m/$clockss_tag/ || $perm_contents !~ m/$lockss_tag/) {
      } elsif (($perm_contents !~ m/$clockss_tag/ && $plugin eq "ClockssArkivoc2022Plugin") || ($perm_contents !~ m/$lockss_tag/ && $plugin eq "Arkivoc2022Plugin")) {
        #printf("%s", $start_contents); #debug
        $result = "--NO_TAG--";
      #} elsif ($start_contents !~ m/\/arkivoc-journal\/browse-arkivoc\/$param{year}\/%d\d/) {
      } elsif ($start_contents !~ m/\/browse-arkivoc\/browse-arkivoc\/$param{year}\/\d*\//s) {
        $result = "--NO_ART--";
      } else {
        $result = "Manifest";
        if ($start_contents =~ m/<title>(.*)<\/title>/si) {
            $vol_title = $1; # . " Volume " . $param{volume_name} . " Year " . $param{year};
            $vol_title =~ s/\s*\n\s*/ /g;
        }
      }
  sleep(4);

#  # OECD Publisher
#  # Books and Working Papers
#  } elsif ($plugin eq "OecdWorkingpapersPlugin" || $plugin eq "OecdBooksPlugin") {
#           $url = sprintf("%s%s_%s",
#               $param{base_url}, $param{pub_path}, $param{pub_id});
#           $start_url = uri_unescape($url);
#           $man_url = $start_url;
#           my $req = HTTP::Request->new(GET, $start_url);
#           my $resp = $ua->request($req);
#           if (($resp->is_success)) {
#               my $contents = $resp->content;
#               if ($req->url ne $resp->request->uri){
#                 $vol_title = $resp->request->uri;
#                 $result = "Redirected";
#               } elsif (defined($contents)) {
#                   $vol_title= $param{pub_id};
#                   $result = "Manifest";
#               } else {
#                   $result = "--"
#               }
#           } else {
#               $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
#           }
#           sleep(4);
#  # Journals
#  } elsif ($plugin eq "OecdJournalsPlugin") {
#           $url = sprintf("%s%s/%s_%s/lockssissues?volume=%s",
#               $param{base_url}, $param{topic}, $param{journal_dir}, $param{journal_issn}, $param{volume_name});
#           $start_url = uri_unescape($url);
#           $man_url = $start_url;
#           my $req = HTTP::Request->new(GET, $start_url);
#           my $resp = $ua->request($req);
#           if (($resp->is_success)) {
#               my $contents = $resp->content;
#               if ($req->url ne $resp->request->uri){
#                 $vol_title = $resp->request->uri;
#                 $result = "Redirected";
#               } elsif (defined($contents)) {
#                   $vol_title= $param{journal_dir} . " Volume " .  $param{volume_name};
#                   $result = "Manifest";
#               } else {
#                   $result = "--"
#               }
#           } else {
#               $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
#           }
#           sleep(4);
#    # End of OECD Publisher
  }
  
  if($result eq "Plugin Unknown") {
    printf("*PLUGIN UNKNOWN*, %s, %s\n",$auid,$man_url);
    $total_missing_plugin = $total_missing_plugin + 1;
  } elsif ($result eq "Manifest") {
    printf("*MANIFEST*, %s, %s, %s\n",$vol_title,$auid_long,$man_url);
    $total_manifests = $total_manifests + 1;
    #printf("%s\n",$vol_title);
    #printf("%s\n",decode_entities($vol_title));
    #my $new_title = encode("utf8", $vol_title);
    #printf("%s\n",$new_title);
    #printf("%s\n",decode_entities($new_title));
  } else {
    printf("*NO MANIFEST*(%s), %s, %s, %s\n",$result,$vol_title,$auid_long,$man_url);
    $total_missing = $total_missing + 1;
    #$tmp = "AINS - An&auml;sthesiologie &middot; Intensivmedizin &middot; Notfallmedizin &middot; Schmerztherapie";
    #printf("%s\n",$tmp);
    #printf("%s\n",decode_entities($tmp));
  }
}
printf("*Today: %s\n", $datestring);
printf("*Total manifests found: %d\n", $total_manifests);
printf("*Total missing manifests: %d\n", $total_missing);
printf("*Total AUs with unknown plugin: %d\n", $total_missing_plugin);
#printf("*Total IP Address: %s", `hostname -I`);
printf("*Total Simple IP Address: %s\n", $ip_address);
exit(0);
