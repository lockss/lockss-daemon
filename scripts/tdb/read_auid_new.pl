#!/usr/bin/perl -w

use URI::Escape;
use Getopt::Long;
use LWP::UserAgent;
use HTTP::Request;
use HTTP::Cookies;
use HTML::Entities;
use utf8;
#use encoding 'utf8';
use Encode qw(decode encode);

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
my $total_manifests = 0;
my $total_missing = 0;
my $total_missing_plugin = 0;
my $datestring = localtime();

# Set up "cookie jar" to hold session cookies for Web access.
# Don't save these cookies from run to run.
my $cjar = HTTP::Cookies->new();

# Create user agent.
my $ua = LWP::UserAgent->new( cookie_jar => $cjar, agent => "LOCKSS cache" );
$ua->proxy('http', 'http://proxy.lockss.org:3128/');
$ua->no_proxy('localhost', '127.0.0.1');

while (my $line = <>) {
  chomp $line;
  my $auid_long = $line;
  my @input_rec = split(/\|/, $line);
  my $num_elements = int(@input_rec);
  my $auid = $input_rec[$num_elements-1];
  my @auid_rec = split(/\&/, $auid);
  my $plugin = shift(@auid_rec);
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
  #my $url_permission = "cant_create_url";
  my $man_url = "cant_create_url";
  my $man_url_p = "cant_create_url";
  my $man_url_s = "cant_create_url";
  my $man_url_d = "cant_create_url";
  my $man_url_e = "cant_create_url";
  my $man_url_de = "cant_create_url";
  my $vol_title = "NO TITLE FOUND";
  my $result = "Plugin Unknown";


  #if ($plugin eq "HighWirePressH20Plugin" || $plugin eq "HighWirePressPlugin") {
  if ($plugin eq "HighWirePressH20Plugin") {
    $url = sprintf("%slockss-manifest/vol_%s_manifest.dtl",
      $param{base_url}, $param{volume_name});
    $url_d = sprintf("%slockss-manifest/vol_%s_manifest.html",
      $param{base_url}, $param{volume_name});
    $man_url = uri_unescape($url);
    $man_url_d = uri_unescape($url_d);
    $base_url_short = substr(uri_unescape($param{base_url}), 0, -1);
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

  } elsif ($plugin =~ m/^(?!Clockss).+DrupalPlugin/) {
        $url = sprintf("%slockss-manifest/vol_%s_manifest.html",
            $param{base_url}, $param{volume_name});
        #printf("********************\n");  #debug
        #printf("url=%s\n",$url);  #debug
        $man_url = uri_unescape($url);
        #printf("man_url=%s\n",$man_url);  #debug
        $base_url_short = substr(uri_unescape($param{base_url}), 0, -1);
        my $req = HTTP::Request->new(GET, $man_url);
        my $resp = $ua->request($req);
        if ($resp->is_success) {
            my $man_contents = $resp->content;
            # printf("%s\n",$man_contents);
            if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
            } elsif (defined($man_contents) && (($man_contents =~ m/\/cgi\/reprint\/$param{volume_name}\//) || ($man_contents =~ m/$base_url_short" lockss-probe/))) { #"
                $result = "CGI_probe_link";
                if ($man_contents =~ m/<title>\s*(.*)\s+C?LOCKSS\s+Manifest\s+Page.*<\/title>/si) {
                    $vol_title = $1;
                    $vol_title =~ s/\s*\n\s*/ /g;
                    if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
                        $vol_title = "\"" . $vol_title . "\"";
                    }
                }
            } elsif (defined($man_contents) && ($man_contents =~ m/$lockss_tag/) && (($man_contents =~ m/\/content\/$param{volume_name}\//) || ($man_contents =~ m/\/content\/vol$param{volume_name}\//))) {
                $result = "Manifest";
                if ($man_contents =~ m/<title>\s*(.*)\s+C?LOCKSS\s+Manifest\s+Page.*<\/title>/si) {
                    $vol_title = $1;
                    $vol_title =~ s/\s*\n\s*/ /g;
                    if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
                        $vol_title = "\"" . $vol_title . "\"";
                    }
                }
                if ($man_contents =~ m/="([^"]*)" lockss-probe="true"/si) { #"
                    my $pl_url = $1;
                    #printf("probe-link=%s\n",$pl_url);  #debug
                    my $req_pl = HTTP::Request->new(GET, $pl_url);
                    my $resp_pl = $ua->request($req_pl);
                    if ($resp_pl->is_success) {
                        if ($req_pl->url ne $resp_pl->request->uri) {
                            $result = "ProbeLinkRedirect";
                            $vol_title = $pl_url;
                        }
                    } else {
                        $vol_title = $pl_url;
                        $result = "BadProbeLink-" . $resp_pl->code();
                    }
                } else {
                    $result = "MissingProbeLink";
                }
            } else {
                $result = "--NO_TAG--"
            }
        } else {
            $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
        }
        sleep(4);

  } elsif ($plugin =~ m/^Clockss.+DrupalPlugin/) {
        $url = sprintf("%sclockss-manifest/vol_%s_manifest.html",
            $param{base_url}, $param{volume_name});
        $man_url = uri_unescape($url);
        $base_url_short = substr(uri_unescape($param{base_url}), 0, -1);
        my $req = HTTP::Request->new(GET, $man_url);
        my $resp = $ua->request($req);
        #printf("%s\" lockss-probe\n",$base_url_short);
        if ($resp->is_success) {
            my $man_contents = $resp->content;
            if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
            } elsif (defined($man_contents) && (($man_contents =~ m/\/cgi\/reprint\/$param{volume_name}\//) || ($man_contents =~ m/$base_url_short" lockss-probe/))) {
                $result = "CGI_probe_link";
                if ($man_contents =~ m/<title>\s*(.*)\s+C?LOCKSS\s+Manifest\s+Page.*<\/title>/si) {
                    $vol_title = $1;
                    $vol_title =~ s/\s*\n\s*/ /g;
                    if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
                        $vol_title = "\"" . $vol_title . "\"";
                    }
                }
            } elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/) && (($man_contents =~ m/\/content\/$param{volume_name}\//) || ($man_contents =~ m/\/content\/vol$param{volume_name}\//))) {
                if ($man_contents =~ m/<title>\s*(.*)\s+C?LOCKSS\s+Manifest\s+Page.*<\/title>/si) {
                    $vol_title = $1;
                    $vol_title =~ s/\s*\n\s*/ /g;
                    if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
                        $vol_title = "\"" . $vol_title . "\"";
                    }
                }
                $result = "Manifest";
                if ($man_contents =~ m/="([^"]*)" lockss-probe="true"/si) {
                    my $pl_url = $1;
                    my $req_pl = HTTP::Request->new(GET, $pl_url);
                    my $resp_pl = $ua->request($req_pl);
                    if ($resp_pl->is_success) {
                        if ($req_pl->url ne $resp_pl->request->uri) {
                            $result = "ProbeLinkRedirect";
                        }
                    } else {
                        $vol_title = $pl_url;
                        $result = "BadProbeLink-" . $resp_pl->code();
                    }
                } else {
                    $result = "MissingProbeLink";
                }
            } else {
                $result = "--NO_TAG--"
            }
        } else {
            $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
        }
        sleep(4);

#  } elsif ($plugin eq "ProjectMusePlugin") {
#        $url = sprintf("%sjournals/%s/v%03d/",
#            $param{base_url}, $param{journal_dir}, $param{volume});
#        $man_url = uri_unescape($url);
#    my $req = HTTP::Request->new(GET, $man_url);
#    my $resp = $ua->request($req);
#    if ($resp->is_success) {
#      my $man_contents = $resp->content;
#      if (defined($man_contents) && (($man_contents =~ m/$lockss_tag/) || ($man_contents =~ m/$oa_tag/)) && ($man_contents !~ "No issues avail")) {
#        if ($man_contents =~ m/<h1>(.*)<\/h1>/si) {
#          $vol_title = $1;
#          if ($man_contents =~ m/<h2>(.*)<\/h2>/si) {
#            $vol_title = $vol_title . " " . $1;
#          }
#          $vol_title =~ s/\s*\n\s*/ /g;
#          $vol_title =~ s/,//;
#        }
#    $result = "Manifest"
#      } else {
#    $result = "--NO_TAG--"
#      }
#  } else {
#      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
#  }
#        sleep(4);

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

#  } elsif ($plugin eq "GPOFDSysSitemapsPlugin") {
#      $url = sprintf("%ssmap/fdsys/sitemap_%d/%d_%s_sitemap.xml",
#      $param{base_url}, $param{year}, $param{year}, $param{collection_id});
#      $man_url = uri_unescape($url);
#      my $req = HTTP::Request->new(GET, $man_url);
#      my $resp = $ua->request($req);
#      if ($resp->is_success) {
#          my $man_contents = $resp->content;
#          if ($req->url ne $resp->request->uri) {
#              $vol_title = $resp->request->uri;
#              $result = "Redirected";
#          } elsif (defined($man_contents) && (($man_contents =~ m/$lockss_tag/) || ($man_contents =~ m/$oa_tag/))) {
#              $vol_title = $param{collection_id};
#              $result = "Manifest"
#          } else {
#              $result = "--NO_TAG--"
#          }
#      } else {
#          $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
#      }
#      sleep(5);

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

  } elsif ($plugin eq "UbiquityPartnerNetworkPlugin") {
      ####start url & permission url
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

  } elsif ($plugin eq "ClockssUbiquityPartnerNetworkPlugin") {
      ####start url
      $url = sprintf("%slockss/year/%d/",
          $param{base_url}, $param{year});
      $man_url = uri_unescape($url);
      my $req = HTTP::Request->new(GET, $man_url);
      my $resp = $ua->request($req);
      ####permission url
      $url_p = sprintf("%sabout",
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
              if ($man_contents_p =~ m/<title>\s*(.*)\s*<\/title>/si) {
                  $vol_title = $1;
              }
              my $man_contents = $resp->content;
              if (defined($man_contents) && ($man_contents =~ m/\/volume\//) && ($man_contents =~ m/\($param{year}\)/)) {
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
            } elsif (defined($man_contents) && defined($start_contents) && (($man_contents =~ m/$clockss_tag/) || ($man_contents =~ m/$oa_tag/)) && (($start_contents =~ m/\($param{year}\)/) || ($start_contents =~ m/: $param{year}/))) {
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
  } elsif ($plugin eq "ClockssJidcOJS2Plugin" || $plugin eq "ClockssOjs3Plugin") {
    #OJS3 allows an attr to define variants for location of manifest
        if ($param{base_url} =~ m/scholarworks/) {
            $url = sprintf("%sjournals/index.php/%s/gateway/clockss?year=%d",
            $param{base_url}, $param{journal_id}, $param{year});
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
            } elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/) && (($man_contents =~ m/\($param{year}\)/) || ($man_contents =~ m/: $param{year}/))) {
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
            } elsif (defined($man_contents) && ($man_contents =~ m/$lockss_tag/) && (($man_contents =~ m/\($param{year}\)/) || ($man_contents =~ m/: $param{year}/))) {
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

#  } elsif ($plugin eq "ClockssCoActionPublishingPlugin") {
#        #Url with list of urls for issues
#        $url = sprintf("%sindex.php/%s/gateway/lockss?year=%d",
#            $param{base_url}, $param{journal_id}, $param{year});
#        $start_url = uri_unescape($url);
#        my $req_s = HTTP::Request->new(GET, $start_url);
#        my $resp_s = $ua->request($req_s);
#        #For reporting at the end
#        $man_url = $start_url ;
#    if ($resp_s->is_success) {
#      my $start_contents = $resp_s->content;
#      if (defined($start_contents) && (($start_contents =~ m/$cc_license_tag/) && ($start_contents =~ m/$cc_license_url/)) && (($start_contents =~ m/\($param{year}\)/) || ($start_contents =~ m/: $param{year}/))) {
#         if ($start_contents =~ m/meta name=.description. content=.(.*) is an international/si) {
#            $vol_title = $1;
#        }
#         $result = "Manifest"
#      } else {
#         $result = "--NO_TAG--"
#      }
#    } else {
#      $result = "--REQ_FAIL--"
#    }
#        sleep(4);
#
#  } elsif ($plugin eq "PensoftPlugin" || $plugin eq "ClockssPensoftPlugin") {
#        #Url with list of urls for issues
#        $url = sprintf("%sjournals/%s/archive?year=%d",
#            $param{base_url}, $param{journal_name}, $param{year});
#        $start_url = uri_unescape($url);
#        my $req_s = HTTP::Request->new(GET, $start_url);
#        my $resp_s = $ua->request($req_s);
#        #For reporting at the end
#        $man_url = $start_url ;
#    if ($resp_s->is_success) {
#      my $start_contents = $resp_s->content;
#      if (defined($start_contents) && (($start_contents =~ m/$cc_license_tag/) && ($start_contents =~ m/$cc_license_url/)) && ($start_contents =~ m/\($param{year}\)/)) {
#         if ($start_contents =~ m/<title>(.*) - Pensoft<\/title>/si) {
#            $vol_title = $1 . "Volume " . $param{year};
#        }
#         $result = "Manifest"
#      } else {
#         $result = "--NO_TAG--"
#      }
#    } else {
#      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
#    }
#        sleep(4);

  } elsif ($plugin eq "PensoftOaiPlugin" || $plugin eq "ClockssPensoftOaiPlugin") {
    #permission is different from start
    $perm_url = uri_unescape($param{base_url});
    #start_url for all OAI queries https://bdj.pensoft.net/oai.php?verb=ListRecords&identifier=bdj&metadataPrefix=oai_dc
    $url = sprintf("%soai.php?verb=ListRecords&set=%s&metadataPrefix=oai_dc",
      $param{base_url}, $param{au_oai_set});
    if (defined($param{au_oai_date}) && $param{au_oai_date} =~ m/^[0-9]{4}$/) {
      $url = $url . "&from=" . $param{au_oai_date} . "-01-01" . "&until=" . $param{au_oai_date} . "-12-31";
    }
    $man_url = uri_unescape($url);
    my $req_p = HTTP::Request->new(GET, $perm_url);
    my $resp_p = $ua->request($req_p);
    my $req_s = HTTP::Request->new(GET, $man_url);
    my $resp_s = $ua->request($req_s);
    
    if ($resp_p->is_success) {
      my $perm_contents = $resp_p->content;
      my $lcl_tag = $cc_license_tag;
      if (defined($perm_contents) && ($perm_contents =~ m/$lcl_tag/s)) {
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
        
  } elsif (($plugin eq "TaylorAndFrancisPlugin") ||
           ($plugin eq "GenericAtyponPlugin") ||
           ($plugin eq "AIAAPlugin") ||
           ($plugin eq "AllenPressJournalsPlugin") ||
           ($plugin eq "AmericanSpeechLanguageHearingAssocAtyponPlugin") ||
           ($plugin eq "AmPublicHealthAssocPlugin") ||
           ($plugin eq "AMetSocPlugin") ||
           ($plugin eq "AmPhysSocAtyponPlugin") ||
           ($plugin eq "AmPsychPubAtyponPlugin") ||
           ($plugin eq "ARRSPlugin") ||
           ($plugin eq "AscoJournalsPlugin") ||
           ($plugin eq "AtsJournalsPlugin") ||
           ($plugin eq "BIRAtyponPlugin") ||
           ($plugin eq "EdinburghUniversityPressPlugin") ||
           ($plugin eq "EmeraldGroupPlugin") ||
           ($plugin eq "EndocrineSocietyPlugin") ||
           ($plugin eq "FasebAtyponPlugin") ||
           ($plugin eq "FutureSciencePlugin") ||
           ($plugin eq "IndersciencePlugin") ||
#           ($plugin eq "JstorPlugin") ||
           ($plugin eq "LiverpoolJournalsPlugin") ||
#           ($plugin eq "ManeyAtyponPlugin") ||
           ($plugin eq "MarkAllenPlugin") ||
           ($plugin eq "MultiSciencePlugin") ||
           ($plugin eq "MassachusettsMedicalSocietyPlugin") ||
           ($plugin eq "RoyalSocietyPublishingAtyponPlugin") ||
           ($plugin eq "RsnaJournalsPlugin") ||
           ($plugin eq "SageAtyponJournalsPlugin") ||
           ($plugin eq "SiamPlugin") ||
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

  } elsif (($plugin eq "ClockssTaylorAndFrancisPlugin") ||
           ($plugin eq "ClockssGenericAtyponPlugin") ||
           ($plugin eq "ClockssAIAAPlugin") ||
           ($plugin eq "ClockssAllenPressJournalsPlugin") ||
           ($plugin eq "ClockssAmericanSpeechLanguageHearingAssocAtyponPlugin") ||
           ($plugin eq "ClockssAMetSocPlugin") ||
           ($plugin eq "ClockssAmmonsScientificPlugin") ||
           ($plugin eq "ClockssAmPhysSocAtyponPlugin") ||
           ($plugin eq "ClockssAmPsychPubAtyponPlugin") ||
           ($plugin eq "ClockssASCEPlugin") ||
           ($plugin eq "ClockssAscoJournalsPlugin") ||
           ($plugin eq "ClockssBIRAtyponPlugin") ||
           ($plugin eq "ClockssEdinburghUniversityPressPlugin") ||
           ($plugin eq "ClockssEmeraldGroupPlugin") ||
           ($plugin eq "ClockssEndocrineSocietyPlugin") ||
           ($plugin eq "ClockssFasebAtyponPlugin") ||
           ($plugin eq "ClockssFutureSciencePlugin") ||
           ($plugin eq "ClockssIndersciencePlugin") ||
#           ($plugin eq "ClockssJstorPlugin") ||
           ($plugin eq "ClockssLiverpoolJournalsPlugin") ||
#           ($plugin eq "ClockssManeyAtyponPlugin") ||
           ($plugin eq "ClockssMarkAllenPlugin") ||
           ($plugin eq "ClockssMultiSciencePlugin") ||
           ($plugin eq "ClockssNRCResearchPressPlugin") ||
           ($plugin eq "ClockssPracticalActionJournalsPlugin") ||
           ($plugin eq "ClockssRoyalSocietyPublishingAtyponPlugin") ||
           ($plugin eq "ClockssSageAtyponJournalsPlugin") ||
           ($plugin eq "ClockssSEGPlugin") ||
           ($plugin eq "ClockssSiamPlugin") ||
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
                  #"/toc/rsbl/2014/10/12"
                  ($man_contents =~ m/\"\/toc\/$jid\/[12][890]\d\d\/$param{volume_name}\/\d*\"/) || 
                  #"/toc/rsbm/2018/64"
                  ($man_contents =~ m/\"\/toc\/$jid\/[12][890]\d\d\/$param{volume_name}\"/) || 
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
              if ($man_contents =~ m/doi\/book\/([^\/]+)\/([^"']+)/) {
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
                      # what we're looking for on the page is href="/doi/pdf/doi1/doi2" OR href="/doi/pdfplus/doi1/doi2
                      #printf("href=\"pdfplus/%s/%s\"",${doi1},${doi2});
                      #if (defined($b_contents) && ($b_contents =~ m/href=\"[^"]+pdf(plus)?\/${doi1}\/${doi2}/)) {
                      if (defined($b_contents) && ($b_contents =~ m/href=\"[^"]+pdf(plus)?\/${doi1}\//)) {
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
           ($plugin eq "ClockssEmeraldGroupBooksPlugin") ||
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
              if ($man_contents =~ m/doi\/book\/([^\/]+)\/([^"']+)/) {
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
                      # what we're looking for on the page is href="/doi/pdf/doi1/doi2" OR href="/doi/pdfplus/doi1/doi2
                      #printf("href=\"pdfplus/%s/%s\"",${doi1},${doi2});
                      #if (defined($b_contents) && ($b_contents =~ m/href=\"[^"]+pdf(plus)?\/${doi1}\/${doi2}/)) {
                      if (defined($b_contents) && ($b_contents =~ m/href=\"[^"]+pdf(plus)?\/${doi1}\//)) {
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

  } elsif ($plugin eq "BerghahnJournalsPlugin") {
      $url = sprintf("%slockss-manifest/journal/%s/volume/%s",
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
        
  } elsif ($plugin eq "ClockssBerghahnJournalsPlugin") {
      $url = sprintf("%slockss-manifest/journal/%s/volume/%s",
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

# use "\w+" at beginning of match to indicate something other than needs.SourcePlugin
#file transfer: old style (Warc|Source)Plugin (base+year), new style SourcePlugin (base+dir), DeliveredSourcePlugin (base + year + dir)
#the url can be built up from the available parameters
  } elsif (($plugin =~ m/\w+SourcePlugin/) || 
           ($plugin =~ m/\w+WarcPlugin/)) {
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
      sleep(4);

#  } elsif ($plugin eq "EdinburghUniversityPressPlugin") {
#    $url = sprintf("%slockss/%s/%s/index.html",
#      $param{base_url}, $param{journal_id}, $param{volume_name});
#    $man_url = uri_unescape($url);
#    #printf("\nUrl: %s\n", $man_url);
#    my $req = HTTP::Request->new(GET, $man_url);
#    my $resp = $ua->request($req);
#    if ($resp->is_success) {
#      my $man_contents = $resp->content;
#      if (defined($man_contents) && (($man_contents =~ m/$lockss_tag/) || ($man_contents =~ m/$oa_tag/)) && ($man_contents =~ m/\/$param{journal_id}\//)) {
#        if ($man_contents =~ m/<title>(.*)LOCKSS Manifest Page<\/title>/si) {
#          $vol_title = $1;
#          $vol_title =~ s/\s*\n\s*/ /g;
#          if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
#            $vol_title = "\"" . $vol_title . "\"";
#          }
#        }
#        $result = "Manifest";
#      } else {
#        $result = "--NO_TAG--"
#      }
#    } else {
#      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
#    }
#    sleep(4);
#
#  } elsif ($plugin eq "ClockssEdinburghUniversityPressPlugin") {
#    $url = sprintf("%sclockss/%s/%s/index.html",
#      $param{base_url}, $param{journal_id}, $param{volume_name});
#    $man_url = uri_unescape($url);
#    #printf("\nUrl: %s\n", $man_url);
#    my $req = HTTP::Request->new(GET, $man_url);
#    my $resp = $ua->request($req);
#    if ($resp->is_success) {
#      my $man_contents = $resp->content;
#      if (defined($man_contents) && ($man_contents =~ m/$clockss_tag/) && ($man_contents =~ m/\/$param{journal_id}\//)) {
#        if ($man_contents =~ m/<title>(.*)CLOCKSS Manifest Page<\/title>/si) {
#          $vol_title = $1;
#          $vol_title =~ s/\s*\n\s*/ /g;
#          if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
#            $vol_title = "\"" . $vol_title . "\"";
#          }
#        }
#        $result = "Manifest";
#      } else {
#        $result = "--NO_TAG--"
#      }
#    } else {
#      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
#    }
#    sleep(4);

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
    $url = sprintf("%sclockssdata?p=%s", $param{base_url}, $param{collection_id});
    $man_url = uri_unescape($url);
    # printf("\nUrl: %s\n", $man_url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/)) {
        $vol_title = $param{collection_id} . " " . $param{year_string};
        if (($man_contents =~ m/\/$param{collection_id}\//) && ($man_contents =~ m/$param{year_string}/) &&
            ($man_contents =~ m/\/books\/$param{collection_id}\/year\/$param{year_string}/)) {
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

#  } elsif ($plugin eq "ClockssPionPlugin") {
#    $url = sprintf("%scontents.cgi?journal=%s&amp;volume=%s",
#      $param{base_url}, $param{short_journal_code}, $param{volume_name});
#    $man_url = uri_unescape($url);
#    #printf("\nUrl: %s\n", $man_url);
#    my $req = HTTP::Request->new(GET, $man_url);
#    my $resp = $ua->request($req);
#    if ($resp->is_success) {
#      my $man_contents = $resp->content;
#      if (defined($man_contents) && ($man_contents =~ m/$clockss_tag/) && ($man_contents =~ m/href=.abstract\.cgi\?id=/)) {
#        if ($man_contents =~ m/<title>(.*)<\/title>/si) {
#          $vol_title = $1;
#          $vol_title =~ s/\s*\n\s*/ /g;
#          if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
#            $vol_title = "\"" . $vol_title . "\"";
#          }
#        }
#        $result = "Manifest";
#      } else {
#        $result = "--NO_TAG--"
#      }
#    } else {
#      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
#    }
#    sleep(4);
#
  } elsif ($plugin eq "MaffeyPlugin") {
    $url = sprintf("%slockss.php?t=lockss&pa=issue&j_id=%s&year=%d",
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
      } elsif (defined($man_contents) && ($man_contents =~ m/$lockss_tag/ || $man_contents =~ m/$oa_tag/) && ($man_contents =~ m/a href=.lockss.php/)) {
        if ($man_contents =~ m/<title>LOCKSS - Published Issues: (.*)<\/title>/si) {
          $vol_title = $1;
          $vol_title =~ s/\s*\n\s*/ /g;
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

  } elsif ($plugin eq "ClockssMaffeyPlugin") {
    $url = sprintf("%slockss.php?t=clockss&pa=issue&j_id=%s&year=%d",
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
      } elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/) && ($man_contents =~ m/a href=.lockss.php/)) {
        if ($man_contents =~ m/<title>CLOCKSS - Published Issues: (.*)<\/title>/si) {
          $vol_title = $1;
          $vol_title =~ s/\s*\n\s*/ /g;
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

  } elsif ($plugin eq "BioOneAtyponPlugin") {
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
      } elsif (defined($man_contents) && (($man_contents =~ m/$lockss_tag/) || ($man_contents =~ m/$oa_tag/))) {
        if ($man_contents =~ m/<title>(.*)\s*LOCKSS Manifest Page<\/title>/si) {
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

  } elsif ($plugin eq "ClockssBioOneAtyponPlugin") {
    $url = sprintf("%sclockss/%s/%s/index.html",
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
        if ($man_contents =~ m/<title>(.*)\s*CLOCKSS Manifest Page<\/title>/si) {
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

#  } elsif ($plugin eq "PortlandPressPlugin") {
#    $url = sprintf("%s%s/%s/lockss.htm",
#      $param{base_url}, $param{journal_id}, $param{volume_name});
#    $man_url = uri_unescape($url);
#    my $req = HTTP::Request->new(GET, $man_url);
#    my $resp = $ua->request($req);
#    if ($resp->is_success) {
#      my $man_contents = $resp->content;
#      if (defined($man_contents) && ($man_contents =~ m/$lockss_tag/)) {
#        $result = "Manifest"
#      } else {
#        $result = "--NO_TAG--"
#      }
#    } else {
#      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
#    }
#    sleep(4);
#
#  } elsif ($plugin eq "ClockssPortlandPressPlugin") {
#    $url = sprintf("%s%s/%s/lockss.htm",
#      $param{base_url}, $param{journal_id}, $param{volume_name});
#    $man_url = uri_unescape($url);
#    my $req = HTTP::Request->new(GET, $man_url);
#    my $resp = $ua->request($req);
#    if ($resp->is_success) {
#      my $man_contents = $resp->content;
#      if (defined($man_contents) && ($man_contents =~ m/$clockss_tag/)) {
#        $result = "Manifest"
#      } else {
#        $result = "--NO_TAG--"
#      }
#    } else {
#      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
#    }
#    sleep(4);
#
  } elsif ($plugin eq "ClockssNaturePublishingGroupPlugin") {
    $url = sprintf("%s%s/clockss/%s_clockss_%d.html",
      $param{base_url}, $param{journal_id}, $param{journal_id}, $param{year});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/) && (($man_contents =~ m/\/$param{journal_id}\/journal\/v$param{volume_name}/) || ($man_contents =~ m/\/$param{journal_id}\/archive\//) || ($man_contents =~ m/\/$param{journal_id}\/index_ja.html/))) {
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

  } elsif ($plugin eq "ClockssPalgraveBookPlugin") {
    $url = sprintf("%spc/doifinder/10.1057/%s",
      $param{base_url}, $param{book_isbn});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/) && ($man_contents =~ m/\/download\/10.1057\/$param{book_isbn}(\.pdf|\.epub|\")/)) {
        if ($man_contents =~ m/<h1 class="product-title"\s*>\s*([^<>]*)\s*<\/h1>/si) {
          $vol_title = $1;
          $vol_title =~ s/\s*\n\s*/ /g;
          $vol_title =~ s/\s*<br \/>\s*/: /;
          $vol_title =~ s/<\/?span>//g;
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
      } elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/) && ($man_contents =~ m/<a href="[^"]*$param{publisher_id}\/[^"]+\/art00001/)) {
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
#  } elsif ($plugin eq "MetaPressPlugin") {
#    $url = sprintf("%sopenurl.asp?genre=volume&eissn=%s&volume=%s",
#      $param{base_url}, $param{journal_issn}, $param{volume_name});
#    $man_url = uri_unescape($url);
#    my $req = HTTP::Request->new(GET, $man_url);
#    my $resp = $ua->request($req);
#    if ($resp->is_success) {
#      my $man_contents = $resp->content;
#      if (defined($man_contents) && ($man_contents =~ m/$lockss_tag/) && ($man_contents =~ m/genre=volume&amp;eissn=$param{journal_issn}&amp;volume=$param{volume_name}/)) {
#        if ($man_contents =~ m/<td class=.labelName.>Journal<\/td><td class=.labelValue.><a href=\"\S*\">([^<]*)<\/a>\s*<\/td>/si) {
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
#  } elsif ($plugin eq "ClockssMetaPressPlugin") {
#    $url = sprintf("%sopenurl.asp?genre=volume&eissn=%s&volume=%s",
#      $param{base_url}, $param{journal_issn}, $param{volume_name});
#    $man_url = uri_unescape($url);
#    my $req = HTTP::Request->new(GET, $man_url);
#    my $resp = $ua->request($req);
#    if ($resp->is_success) {
#      my $man_contents = $resp->content;
#      if (defined($man_contents) && ($man_contents =~ m/$clockss_tag/) && ($man_contents =~ m/genre=volume&amp;eissn=$param{journal_issn}&amp;volume=$param{volume_name}/)) {
#        if ($man_contents =~ m/<td class=.labelName.>Journal<\/td><td class=.labelValue.><a href=\"\S*\">([^<]*)<\/a>\s*<\/td>/si) {
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
  } elsif ($plugin eq "ClockssIOPSciencePlugin") {
    $url = sprintf("%s%s/%s",
      $param{base_url}, $param{journal_issn}, $param{volume_name});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/)) {
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
        #for reporting at the end 
        $man_url = $start_url
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
        #for reporting at the end 
        $man_url = $start_url
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--"
    }
    sleep(4);

#  } elsif ($plugin eq "ClockssRoyalSocietyOfChemistryPlugin") {
#    $url = sprintf("%spublishing/journals/lockss/?journalcode=%s&volume=%s&year=%d",
#      $param{base_url}, $param{journal_code}, $param{volume_name}, $param{year});
#    $man_url = uri_unescape($url);
#    my $req = HTTP::Request->new(GET, $man_url);
#    my $resp = $ua->request($req);
#    if ($resp->is_success) {
#      my $man_contents = $resp->content;
#      if (defined($man_contents) && ($man_contents =~ m/$clockss_tag/)) {
#        if ($man_contents =~ m/<title>\s*(.*)\s*<\/title>/si) {
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
#
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
          $vol_title = "Royal Society of Chemistry Books $param{year}";
          $result = "Manifest"
      } else {
        $result = "--NO_TAG--"
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

  #American Society for Microbiology Books
  } elsif ($plugin eq "ClockssASMscienceBooksPlugin") {
    $url = sprintf("%scontent/book/%s",
      $param{base_url}, $param{doi});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      my $has_no_chapters = "Chapters \\(0\\)";
#      if ($man_contents =~ m/$has_no_chapters/ ) {
#	print "matches - has no chapters\n";
#      } else {
#	print "no match -has chapters\n";
#      }
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/)) {
        if ($man_contents !~ m/$has_no_chapters/) {
          if ($man_contents =~ m/<title>\s*ASMscience \|(.*)\s*<\/title>/si) {
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

  } elsif ($plugin eq "ASMscienceBooksPlugin") {
    $url = sprintf("%scontent/book/%s",
      $param{base_url}, $param{doi});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      my $has_no_chapters = "Chapters \\(0\\)";
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$lockss_tag/)) {
        if ($man_contents !~ m/$has_no_chapters/) {
          if ($man_contents =~ m/<title>\s*ASMscience \|(.*)\s*<\/title>/si) {
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

  } elsif ($plugin eq "ClockssIetJournalsPlugin") {
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
      } elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/)) {
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
    $url_p2 = sprintf("%svolumes.html",
      $param{base_url});
    $url_s = sprintf("%s%s/index.html",
      $param{base_url}, $param{volume_name});
    #prefix for an article link
    $url_d = sprintf("%s%s/",
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

#  } elsif ($plugin eq "CopernicusPublicationsPlugin") {
#    $url_p = sprintf("%s",
#      $param{home_url});
#    $url_s = sprintf("%s%s/index.html",
#      $param{base_url}, $param{volume_name});
#    $url_d = sprintf("%s%s/",
#      $param{base_url}, $param{volume_name});
#    $man_url = uri_unescape($url_s) . " + " . uri_unescape($url_p) ;
#    $man_url_p = uri_unescape($url_p);
##    printf("*man_url_p: %s\n", $man_url_p);
#    $man_url_s = uri_unescape($url_s);
##    printf("*man_url_s: %s\n", $man_url_s);
#    my $article_prefix = uri_unescape($url_d);
##    printf("*article_prefix: %s\n", $article_prefix);
#    my $req_p = HTTP::Request->new(GET, $man_url_p);
#    my $req_s = HTTP::Request->new(GET, $man_url_s);
#    my $resp_p = $ua->request($req_p);
#    my $resp_s = $ua->request($req_s);
#    if ($resp_p->is_success && $resp_s->is_success) {
#      my $perm_contents = $resp_p->content;
#      my $start_contents = $resp_s->content;
#      if (defined($perm_contents) && defined($start_contents) && 
#         ($start_contents =~ m/$param{base_url}$param{volume_name}\//) && 
#         (($perm_contents =~ m/$lockss_tag/) || (($perm_contents =~ m/$cc_license_tag/) && ($perm_contents =~ m/$cc_license_url/)) )) {
#        if ($perm_contents =~ m/j-name.>\s*([^<]*)\s*<\//si) {
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

  } elsif (($plugin eq "BMCPlugin") || ($plugin eq "ClockssBMCPlugin")) {
    $url = sprintf("%s%s/%s",
      $param{base_url}, $param{journal_issn}, $param{volume_name});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$bmc_tag/) && ($man_contents =~ m/content\/$param{volume_name}/)) {
        if ($man_contents =~ m/<title>(.*)<\/title>/si) {
          $vol_title = $1;
          $vol_title =~ s/ \| / /g;
          $vol_title =~ s/2013/Volume $param{volume_name}/g;
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

  } elsif (($plugin eq "BioMedCentralPlugin") || ($plugin eq "ClockssBioMedCentralPlugin")) {
    $url = sprintf("%scontent/%s",
      $param{base_url}, $param{volume_name});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && (($man_contents =~ m/$bmc_tag/) || ($man_contents =~ m/$bmc2_tag/)) && ($man_contents =~ m/content\/$param{volume_name}/)) {
        if ($man_contents =~ m/<title>(.*)<\/title>/si) {
          $vol_title = $1;
          $vol_title =~ s/ \| / /g;
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

  } elsif (($plugin eq "HindawiPublishingCorporationPlugin") || ($plugin eq "ClockssHindawiPublishingCorporationPlugin")) {
    $url = sprintf("%sjournals/%s/%s/",
      $param{base_url}, $param{journal_id}, $param{volume_name});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($req->url ne $resp->request->uri) {
        $vol_title = $resp->request->uri;
        $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/journals\/$param{journal_id}\/$param{volume_name}/)) {
        if ($man_contents =~ m/<title>(.*)<\/title>/si) {
          $vol_title = $1;
          #$vol_title =~ s/ \| / /g;
          $vol_title =~ s/\s*\n\s*/ /g;
          $vol_title =~ s/An Open Access Journal//;
          $vol_title =~ s/\s+/ /g;
          $vol_title =~ s/&#8212;/Volume $param{volume_name}/g;

        }
        $result = "Manifest"
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    }
    sleep(4);

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

#  } elsif ($plugin eq "EmeraldPlugin") {
#    $url = sprintf("%scrawlers/lockss.htm?issn=%s&volume=%s",
#      $param{base_url}, $param{journal_issn}, $param{volume_name});
#    $man_url = uri_unescape($url);
#    my $req = HTTP::Request->new(GET, $man_url);
#    my $resp = $ua->request($req);
#    if ($resp->is_success) {
#      my $man_contents = $resp->content;
#      if (defined($man_contents) && ($man_contents =~ m/$lockss_tag/) && ($man_contents =~ m/crawlers\/lockss.htm\?issn=$param{journal_issn}&amp;volume=$param{volume_name}/)) {
#        if ($man_contents =~ m/<strong>Journal title:<\/strong>(.*)<br \/>/si) {
#          $vol_title = $1 . " Volume " . $param{volume_name};
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
#
  } elsif ($plugin eq "ACSESSJournalsPlugin") {
    $url = sprintf("%spublications/%s/tocs/%s",
      $param{base_url}, $param{journal_id}, $param{volume_name});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$lockss_tag/) && ($man_contents =~ m/\/publications\/$param{journal_id}\/tocs\/$param{volume_name}\//)) {
        if ($man_contents =~ m/<title>(.*)<\/title>/si) {
          $vol_title = $1;
          $vol_title =~ s/ \| Digital Library//;
          $vol_title =~ s/ - / /;
          $vol_title =~ s/ &amp\; / & /;
          }
        $result = "Manifest"
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    }
    sleep(4);

  } elsif ($plugin eq "ClockssACSESSJournalsPlugin") {
    $url = sprintf("%spublications/%s/tocs/%s",
      $param{base_url}, $param{journal_id}, $param{volume_name});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/) && ($man_contents =~ m/\/publications\/$param{journal_id}\/tocs\/$param{volume_name}\//)) {
        if ($man_contents =~ m/<title>(.*)<\/title>/si) {
          $vol_title = $1;
          $vol_title =~ s/ \| Digital Library//;
          $vol_title =~ s/ - / /;
          $vol_title =~ s/ &amp\; / & /;
          }
        $result = "Manifest"
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--" . $resp->code() . " " . $resp->message();
    }
    sleep(4);

  } elsif ($plugin eq "EuropeanMathematicalSocietyPlugin") {
    $url = sprintf("%sjournals/all_issues.php?issn=%s",
      $param{base_url}, $param{journal_issn});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
#      if (defined($man_contents) && ($man_contents =~ m/$lockss_tag/) && (man_contents =~ m/issn=$param{journal_issn}.vol=$param{volume_name}/)) {
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$lockss_tag/) && ($man_contents =~ m/vol=$param{volume_name}/)) {
        if ($man_contents =~ m/<h1>(.*)<\/h1>/si) {
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

  } elsif ($plugin eq "ClockssEuropeanMathematicalSocietyPlugin") {
    $url = sprintf("%sjournals/all_issues.php?issn=%s",
      $param{base_url}, $param{journal_issn});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
#      if (defined($man_contents) && ($man_contents =~ m/$clockss_tag/) && (man_contents =~ m/issn=$param{journal_issn}.vol=$param{volume_name}/)) {
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/) && ($man_contents =~ m/vol=$param{volume_name}/)) {
        if ($man_contents =~ m/<h1>(.*)<\/h1>/si) {
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

  } elsif ($plugin eq "EuropeanMathematicalSocietyBooksPlugin") {
    $url = sprintf("%sbooks/book.php?proj_nr=%s",
      $param{base_url}, $param{book_number});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
#      if (defined($man_contents) && ($man_contents =~ m/$lockss_tag/) && (man_contents =~ m/issn=$param{journal_issn}.vol=$param{volume_name}/)) {
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$lockss_tag/) && ($man_contents =~ m/proj_nr=$param{book_number}/)) {
        if ($man_contents =~ m/<h4>([^<]*)<\/h4>/si) {
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

  } elsif ($plugin eq "ClockssEuropeanMathematicalSocietyBooksPlugin") {
    $url = sprintf("%sbooks/book.php?proj_nr=%s",
      $param{base_url}, $param{book_number});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
#      if (defined($man_contents) && ($man_contents =~ m/$clockss_tag/) && (man_contents =~ m/issn=$param{journal_issn}.vol=$param{volume_name}/)) {
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/) && ($man_contents =~ m/proj_nr=$param{book_number}/)) {
        if ($man_contents =~ m/<h4>([^<]*)<\/h4>/si) {
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

  } elsif ($plugin eq "SilverchairJournalsPlugin") {
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

  } elsif ($plugin eq "SilverchairBooksPlugin") {
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
      } elsif (defined($man_contents) && ($man_contents =~ m/$lockss_tag/)) {
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
  } elsif ($plugin eq "OupSilverchairPlugin") {
    $url = sprintf("%s%s/list-of-issues/%d",
      $param{base_url}, $param{journal_id}, $param{year});
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

  } elsif (($plugin eq "ClockssOupSilverchairPlugin")) {
    $url = sprintf("%s%s/list-of-issues/%d",
      $param{base_url}, $param{journal_id}, $param{year});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    #printf("resp is %s\n",$resp->status_line);
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
    
  } elsif (($plugin eq "IwapSilverchairPlugin") || ($plugin eq "RockefellerUniversityPressSilverchairPlugin") || ($plugin eq "PortlandPressSilverchairPlugin") || ($plugin eq "GeoscienceWorldSilverchairPlugin")) {
    $url = sprintf("%s%s/issue/browse-by-year/%d",
      $param{base_url}, $param{journal_id}, $param{year});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$lockss_tag/)) {
        if ($man_contents =~ m/<title>(.*) [|] IWA Publishing<\/title>/si) {
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

  } elsif (($plugin eq "ClockssIwapSilverchairPlugin") || ($plugin eq "ClockssRockefellerUniversityPressSilverchairPlugin") || ($plugin eq "ClockssPortlandPressSilverchairPlugin") || ($plugin eq "ClockssGeoscienceWorldSilverchairPlugin")) {
    $url = sprintf("%s%s/issue/browse-by-year/%d",
      $param{base_url}, $param{journal_id}, $param{year});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    #printf("resp is %s\n",$resp->status_line);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/)) {
        if ($man_contents =~ m/<title>(.*) [|] IWA Publishing<\/title>/si) {
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
    
  } elsif (($plugin eq "ClockssDupSilverchairPlugin")) {
    $url = sprintf("%s%s/list-of-issues/%d",
      $param{base_url}, $param{journal_id}, $param{year});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    #printf("resp is %s\n",$resp->status_line);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($req->url ne $resp->request->uri) {
              $vol_title = $resp->request->uri;
              $result = "Redirected";
      } elsif (defined($man_contents) && ($man_contents =~ m/$clockss_tag/)) {
        if ($man_contents =~ m/<title>(.*) [|] Duke University Press<\/title>/si) {
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
    
  } elsif (($plugin eq "ClockssAnuPlugin")) {
    $url = sprintf("%spublications/%s", $param{base_url}, $param{journal_id});
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
        if ($man_contents =~ m/<title>(.*) - ANU Press - ANU<\/title>/si) {
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
        if ($man_contents =~ m/<title>(.*) - ANU Press - ANU<\/title>/si) {
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
    
  } elsif ($plugin eq "ClockssSilvaFennicaPlugin") {
    #Url with list of articles https://www.silvafennica.fi/issue/sf/volume/50
    $url = sprintf("%sissue/%s/volume/%s",
        $param{base_url}, $param{journal_id}, $param{volume_name});
    $start_url = uri_unescape($url);
    my $req_s = HTTP::Request->new(GET, $start_url);
    my $resp_s = $ua->request($req_s);
    #For reporting at the end
    $man_url = $start_url;
    if ($resp_s->is_success) {
      my $start_contents = $resp_s->content;
      if (defined($start_contents) && ($start_contents =~ m/>(Silva Fennica vol. $param{volume_name})</)) {
        $vol_title = $1;
        $result = "Manifest"
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--"
    }
    sleep(4);
    
      } elsif ($plugin eq "ClockssSilvaFennicaNoJidPlugin") {
    $url = sprintf("%sissue/volume/%s",
        $param{base_url}, $param{volume_name});
    $start_url = uri_unescape($url);
    my $req_s = HTTP::Request->new(GET, $start_url);
    my $resp_s = $ua->request($req_s);
    #For reporting at the end
    $man_url = $start_url;
    if ($resp_s->is_success) {
      my $start_contents = $resp_s->content;
      if (defined($start_contents) && ($start_contents =~ m/<h1>([A-Z][^<]+ $param{volume_name})</)) {
        $vol_title = $1;
        $result = "Manifest"
      } else {
        $result = "--NO_TAG--"
      }
    } else {
      $result = "--REQ_FAIL--"
    }
    sleep(4);
    
  } elsif ($plugin eq "ClockssSpandidosPlugin" || $plugin eq "SpandidosPlugin") {
      # manifest page is the entire journal archive
      # we will confirm an appropriate volume issue 
      my $url_sprintf = sprintf("%s%s/archive",$param{base_url}, $param{journal_id});
      $man_url = uri_unescape($url_sprintf);
      my $req = HTTP::Request->new(GET, $man_url);
      my $resp = $ua->request($req);
      if ($resp->is_success) {
      my $man_contents = $resp->content;
      if ($req->url ne $resp->request->uri) {
          $vol_title = $resp->request->uri;
          $result = "Redirected";
      } elsif (defined($man_contents)) {
          my $perm_contents = $resp->content; #same as manifest page
          my $lcl_tag = $lockss_tag;
          #for CLOCKSS permission is on lockss.txt
          if ($plugin eq "ClockssSpandidosPlugin") {
          $lcl_tag = $clockss_tag;
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
          #we have a permission statement, do we have a link to <a href="/ijo/39/...." at least one issue of this volume
          my $vol_link = sprintf("href=.+/%s/%s/",$param{journal_id},$param{volume_name});
          if ($man_contents =~ m/$vol_link/gi) {
              $vol_title = $param{journal_id}; #default_value
              if ($man_contents =~ m/<title>\s*(.*)\s*<\/title>/si) {
              $vol_title = $1; #better value
              }
              $result = "Manifest";
          } else {
              #had a manifest page but it had no volume links
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
          #no lockss permission statement on start page. Permission statement is here: http://gigadb.org/lockss.txt
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
   } elsif ($plugin eq "ClockssResilienceAlliancePlugin") {
         $url = sprintf("%sissues/",
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
    } # End of Resilience Alliance
  
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
exit(0);
