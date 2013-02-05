#!/usr/bin/perl -w

use URI::Escape;
use Getopt::Long;
use LWP::UserAgent;
use HTTP::Request;
use HTTP::Cookies;

my $lockss_tag  = "LOCKSS system has permission to collect, preserve, and serve this Archival Unit";
my $oa_tag      = "LOCKSS system has permission to collect, preserve, and serve this open access Archival Unit";
my $clockss_tag = "CLOCKSS system has permission to ingest, preserve, and serve this Archival Unit";
my $cc_tag = "rel..license. href..http://creativecommons.org/licenses/by";
my $bmc_tag = "<span>Archive</span>";
my $igi_tag = "/gateway/issue/";
my $total_manifests = 0;
my $total_missing = 0;
my $total_missing_plugin = 0;

# Set up "cookie jar" to hold session cookies for Web access.
# Don't save these cookies from run to run.
my $cjar = HTTP::Cookies->new();

# Create user agent.
my $ua = LWP::UserAgent->new( cookie_jar => $cjar, agent => "LOCKSS cache" );
$ua->proxy('http', 'http://lockss.org:8888/');
$ua->no_proxy('localhost', '127.0.0.1');

while (my $line = <>) {
  chomp $line;
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
  my $url_permission = "cant_create_url";
  my $man_url = "cant_create_url";
  my $vol_title = "NO TITLE FOUND";
  my $result = "Plugin Unknown";
  if ($plugin eq "HighWirePressH20Plugin" || $plugin eq "HighWirePressPlugin") {
    $url = sprintf("%slockss-manifest/vol_%s_manifest.dtl",
      $param{base_url}, $param{volume_name});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if (defined($man_contents) && (($man_contents =~ m/$lockss_tag/) || ($man_contents =~ m/$oa_tag/)) && ($man_contents =~ m/\/content\/$param{volume_name}\//)) {
        if ($man_contents =~ m/<title>\s*(.*)\s+C?LOCKSS\s+Manifest\s+Page.*<\/title>/si) {
          $vol_title = $1;
          $vol_title =~ s/\s*\n\s*/ /g;
          if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
            $vol_title = "\"" . $vol_title . "\"";
          }
        } 
        $result = "Manifest"
      } else {
        $result = "--"
      }
    } else {
      $result = "--"
    }
    sleep(5);

  } elsif ($plugin eq "ClockssHighWirePressH20Plugin") {
        $url = sprintf("%sclockss-manifest/vol_%s_manifest.dtl",
            $param{base_url}, $param{volume_name});
        $man_url = uri_unescape($url);
  my $req = HTTP::Request->new(GET, $man_url);
  my $resp = $ua->request($req);
  if ($resp->is_success) {
      my $man_contents = $resp->content;
      if (defined($man_contents) && ($man_contents =~ m/$clockss_tag/) && ($man_contents =~ m/\/content\/$param{volume_name}\//)) {
    if ($man_contents =~ m/<title>\s*(.*)\s+C?LOCKSS\s+Manifest\s+Page.*<\/title>/si) {
        $vol_title = $1;
        $vol_title =~ s/\s*\n\s*/ /g;
        if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
      $vol_title = "\"" . $vol_title . "\"";
        }
    } 
    $result = "Manifest"
      } else {
    $result = "--"
      }
  } else {
      $result = "--"
  }
        sleep(5);

  } elsif ($plugin eq "ProjectMusePlugin") {
        $url = sprintf("%sjournals/%s/v%03d/", 
            $param{base_url}, $param{journal_dir}, $param{volume});
        $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if (defined($man_contents) && (($man_contents =~ m/$lockss_tag/) || ($man_contents =~ m/$oa_tag/))) {
    if ($man_contents =~ m/<title>Project MUSE -\s*(.*)<\/title>/si) {
        $vol_title = $1;
        $vol_title =~ s/\s*\n\s*/ /g;
        if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
      $vol_title = "\"" . $vol_title . "\"";
        }
    } 
    $result = "Manifest"
      } else {
    $result = "--"
      }
  } else {
      $result = "--"
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
      if (defined($man_contents) && (($man_contents =~ m/$lockss_tag/) || ($man_contents =~ m/$oa_tag/))) {
    if ($man_contents =~ m/<title>(.*) --.*<\/title>/si) {
        $vol_title = $1;
        $vol_title =~ s/\s*\n\s*/ /g;
        if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
      $vol_title = "\"" . $vol_title . "\"";
        }
    } 
    $result = "Manifest"
      } else {
    $result = "--"
      }
  } else {
      $result = "--"
  }
        sleep(5);
        
  } elsif ($plugin eq "ClockssBerkeleyElectronicPressPlugin") {
        $url = sprintf("%s%s/lockss-volume%d.html", 
            $param{base_url}, $param{journal_abbr}, $param{volume});
        $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if (defined($man_contents) && (($man_contents =~ m/$clockss_tag/))) {
    if ($man_contents =~ m/<title>(.*) --.*<\/title>/si) {
        $vol_title = $1;
        $vol_title =~ s/\s*\n\s*/ /g;
        if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
      $vol_title = "\"" . $vol_title . "\"";
        }
    } 
    $result = "Manifest"
      } else {
    $result = "--"
      }
  } else {
      $result = "--"
  }
        sleep(5);
        
  } elsif ($plugin eq "OJS2Plugin") {
        $url = sprintf("%sindex.php/%s/gateway/lockss?year=%d", 
            $param{base_url}, $param{journal_id}, $param{year});
        $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if (defined($man_contents) && (($man_contents =~ m/$lockss_tag/) || ($man_contents =~ m/$oa_tag/)) && (($man_contents =~ m/\($param{year}\)/) || ($man_contents =~ m/: $param{year}/))) {
    if ($man_contents =~ m/<title>(.*)<\/title>/si) {
        $vol_title = $1;
        $vol_title =~ s/\s*\n\s*/ /g;
        if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
      $vol_title = "\"" . $vol_title . "\"";
        }
    } 
    $result = "Manifest"
      } else {
    $result = "--"
      }
  } else {
      $result = "--"
  }
        sleep(5);
        
  } elsif ($plugin eq "ClockssOJS2Plugin") {
        $url = sprintf("%sindex.php/%s/gateway/lockss?year=%d", 
            $param{base_url}, $param{journal_id}, $param{year});
        $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if (defined($man_contents) && (($man_contents =~ m/$clockss_tag/) || ($man_contents =~ m/$oa_tag/) || ($man_contents =~ m/$cc_tag/)) && (($man_contents =~ m/\($param{year}\)/) || ($man_contents =~ m/: $param{year}/))) {
    if ($man_contents =~ m/<title>(.*)<\/title>/si) {
        $vol_title = $1;
        $vol_title =~ s/\s*\n\s*/ /g;
        if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
      $vol_title = "\"" . $vol_title . "\"";
        }
    } 
    $result = "Manifest"
      } else {
    $result = "--"
      }
  } else {
      $result = "--"
  }
        sleep(5);
        
  } elsif ($plugin eq "TaylorAndFrancisPlugin") {
        $url = sprintf("%slockss/%s/%s/index.html", 
            $param{base_url}, $param{journal_id}, $param{volume_name});
        $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if (defined($man_contents) && (($man_contents =~ m/$lockss_tag/) && ($man_contents =~ m/$param{journal_id}\/$param{volume_name}/))) {
    if ($man_contents =~ m/<title>(.*) LOCKSS Manifest Page<\/title>/si) {
        $vol_title = $1;
        $vol_title =~ s/\s*\n\s*/ /g;
        $vol_title =~ s/2013/Volume $param{volume_name}/g;
        if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
          $vol_title = "\"" . $vol_title . "\"";
        }
    } 
    $result = "Manifest";
      } else {
    $result = "--"
      }
  } else {
      $result = "--"
  }
        sleep(5);
        
  } elsif ($plugin eq "ClockssTaylorAndFrancisPlugin") {
        $url = sprintf("%sclockss/%s/%s/index.html", 
            $param{base_url}, $param{journal_id}, $param{volume_name});
        $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if (defined($man_contents) && (($man_contents =~ m/$clockss_tag/) && ($man_contents =~ m/$param{journal_id}\/$param{volume_name}/))) {
    if ($man_contents =~ m/<title>(.*) CLOCKSS Manifest Page<\/title>/si) {
        $vol_title = $1;
        $vol_title =~ s/\s*\n\s*/ /g;
        $vol_title =~ s/2013/Volume $param{volume_name}/g;
        if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
          $vol_title = "\"" . $vol_title . "\"";
        }
    } 
    $result = "Manifest";
      } else {
    $result = "--"
      }
  } else {
      $result = "--"
  }
        sleep(5);
        
  } elsif ($plugin eq "EdinburghUniversityPressPlugin") {
    $url = sprintf("%slockss/%s/%s/index.html", 
      $param{base_url}, $param{journal_id}, $param{volume_name});
    $man_url = uri_unescape($url);
    printf("\nUrl: %s\n", $man_url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if (defined($man_contents)) {
        printf("Page Found!\n");
        if ($man_contents =~ m/$lockss_tag/) {
          printf("Lockss tag found!");
        } else {
          printf("No Lockss tag found!\n");
        }
      } else {
        printf("No Page Found!\n");
      }
      if (defined($man_contents) && (($man_contents =~ m/$lockss_tag/) || ($man_contents =~ m/$oa_tag/))) {
        if ($man_contents =~ m/<title>(.*)LOCKSS Manifest Page<\/title>/si) {
          $vol_title = $1;
          $vol_title =~ s/\s*\n\s*/ /g;
          if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
            $vol_title = "\"" . $vol_title . "\"";
          }
        } 
        $result = "Manifest";
      } else {
        $result = "--"
      }
    } else {
      $result = "--"
    }
    sleep(5);
        
  } elsif ($plugin eq "ClockssEdinburghUniversityPressPlugin") {
    $url = sprintf("%sclockss/%s/%s/index.html", 
      $param{base_url}, $param{journal_id}, $param{volume_name});
    $man_url = uri_unescape($url);
    printf("\nUrl: %s\n", $man_url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if (defined($man_contents)) {
        printf("Page Found!\n");
        if ($man_contents =~ m/$lockss_tag/) {
          printf("Clockss tag found!");
        } else {
          printf("No Clockss tag found!\n");
        }
      } else {
        printf("No Page Found!\n");
      }
      if (defined($man_contents) && (($man_contents =~ m/$clockss_tag/))) {
        if ($man_contents =~ m/<title>(.*)CLOCKSS Manifest Page<\/title>/si) {
          $vol_title = $1;
          $vol_title =~ s/\s*\n\s*/ /g;
          if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
            $vol_title = "\"" . $vol_title . "\"";
          }
        } 
        $result = "Manifest";
      } else {
        $result = "--"
      }
    } else {
      $result = "--"
    }
    sleep(5);
        
  } elsif ($plugin eq "BioOneAtyponPlugin") {
    $url = sprintf("%slockss/%s/%s/index.html", 
      $param{base_url}, $param{journal_id}, $param{volume_name});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if (defined($man_contents) && (($man_contents =~ m/$lockss_tag/) || ($man_contents =~ m/$oa_tag/))) {
        if ($man_contents =~ m/<title>(.*)\s*LOCKSS Manifest Page<\/title>/si) {
          $vol_title = $1;
          $vol_title =~ s/\s*\n\s*/ /g;
          if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
            $vol_title = "\"" . $vol_title . "\"";
          }
        } 
        $result = "Manifest"
      } else {
        $result = "--"
      }
    } else {
      $result = "--"
    }
    sleep(5);
        
  } elsif ($plugin eq "ClockssNaturePublishingGroupPlugin") {
    $url = sprintf("%s%s/clockss/%s_clockss_%d.html", 
      $param{base_url}, $param{journal_id}, $param{journal_id}, $param{year});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if (defined($man_contents) && ($man_contents =~ m/$clockss_tag/) && (($man_contents =~ m/\/$param{journal_id}\/journal\/v$param{volume_name}/) || ($man_contents =~ m/\/$param{journal_id}\/archive\/date\/$param{year}/))) {
        if ($man_contents =~ m/<TITLE>\s*(.*)\s*<\/TITLE>/si) {
          $vol_title = $1;
          $vol_title =~ s/\s*\n\s*/ /g;
          if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
            $vol_title = "\"" . $vol_title . "\"";
          }
        } 
        $result = "Manifest"
      } else {
        $result = "--"
      }
    } else {
      $result = "--"
    }
    sleep(5);
  } elsif ($plugin eq "IngentaJournalPlugin") {
    $url = sprintf("%scontent/%s?format=lockss&volume=%s", 
      $param{base_url}, $param{journal_issn}, $param{volume_name});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if (defined($man_contents) && ($man_contents =~ m/$lockss_tag/)) {
        if ($man_contents =~ m/<TITLE>\s*(.*)\s*<\/TITLE>/si) {
          $vol_title = $1;
          $vol_title =~ s/\s*\n\s*/ /g;
          if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
            $vol_title = "\"" . $vol_title . "\"";
          }
        } 
        $result = "Manifest"
      } else {
        $result = "--"
      }
    } else {
      $result = "--"
    }
    sleep(5);
  } elsif ($plugin eq "ClockssIngentaJournalPlugin") {
    $url = sprintf("%scontent/%s?format=clockss&volume=%s", 
      $param{base_url}, $param{journal_issn}, $param{volume_name});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if (defined($man_contents) && ($man_contents =~ m/$clockss_tag/)) {
        if ($man_contents =~ m/<TITLE>\s*(.*)\s*<\/TITLE>/si) {
          $vol_title = $1;
          $vol_title =~ s/\s*\n\s*/ /g;
          if (($vol_title =~ m/</) || ($vol_title =~ m/>/)) {
            $vol_title = "\"" . $vol_title . "\"";
          }
        } 
        $result = "Manifest"
      } else {
        $result = "--"
      }
    } else {
      $result = "--"
    }
    sleep(5);
  } elsif ($plugin eq "MetaPressPlugin") {
    $url = sprintf("%sopenurl.asp?genre=volume&eissn=%s&volume=%s", 
      $param{base_url}, $param{journal_issn}, $param{volume_name});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if (defined($man_contents) && ($man_contents =~ m/$lockss_tag/)) {
        if ($man_contents =~ m/<td class=.labelName.>Journal<\/td><td class=.labelValue.><a href=\"\S*\">([^<]*)<\/a>\s*<\/td>/si) {
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
    sleep(5);
  } elsif ($plugin eq "ClockssMetaPressPlugin") {
    $url = sprintf("%sopenurl.asp?genre=volume&eissn=%s&volume=%s", 
      $param{base_url}, $param{journal_issn}, $param{volume_name});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if (defined($man_contents) && ($man_contents =~ m/$clockss_tag/)) {
        if ($man_contents =~ m/<td class=.labelName.>Journal<\/td><td class=.labelValue.><a href=\"\S*\">([^<]*)<\/a>\s*<\/td>/si) {
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
    sleep(5);
  } elsif ($plugin eq "BloomsburyQatarPlugin") {
    $url = sprintf("%slockss/%s/%s/index.html", 
      $param{base_url}, $param{journal_dir}, $param{volume_name});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if (defined($man_contents) && ($man_contents =~ m/$lockss_tag/)) {
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
      $result = "--REQ_FAIL--"
    }
    sleep(5);
  } elsif ($plugin eq "ClockssBloomsburyQatarPlugin") {
    $url = sprintf("%sclockss/%s/%s/index.html", 
      $param{base_url}, $param{journal_dir}, $param{volume_name});
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
      $result = "--REQ_FAIL--"
    }
    sleep(5);
  } elsif ($plugin eq "ClockssIOPSciencePlugin") {
    $url = sprintf("%s%s/%s", 
      $param{base_url}, $param{journal_issn}, $param{volume_name});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if (defined($man_contents) && ($man_contents =~ m/$clockss_tag/)) {
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
      $result = "--REQ_FAIL--"
    }
    sleep(5);

  } elsif ($plugin eq "IgiGlobalPlugin") {
    $url = sprintf("%slockss/journal-issues.aspx?issn=%s&volume=%s", 
      $param{base_url}, $param{journal_issn}, $param{volume});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if (defined($man_contents) && ($man_contents =~ m/$lockss_tag/) && ($man_contents =~ m/$igi_tag/)) {
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
      $result = "--REQ_FAIL--"
    }
    sleep(5);
        
  } elsif ($plugin eq "ClockssIgiGlobalPlugin") {
    $url = sprintf("%slockss/journal-issues.aspx?issn=%s&volume=%s", 
      $param{base_url}, $param{journal_issn}, $param{volume});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if (defined($man_contents) && ($man_contents =~ m/$clockss_tag/) && ($man_contents =~ m/$igi_tag/)) {
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
      $result = "--REQ_FAIL--"
    }
    sleep(5);
                
  } elsif ($plugin eq "RoyalSocietyOfChemistryPlugin") {
    $url = sprintf("%spublishing/journals/lockss/?journalcode=%s&volume=%s&year=%d", 
      $param{base_url}, $param{journal_code}, $param{volume_name}, $param{year});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if (defined($man_contents) && ($man_contents =~ m/$lockss_tag/)) {
        if ($man_contents =~ m/<title>\s*(.*)\s*<\/title>/si) {
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
    sleep(5);
        
  } elsif ($plugin eq "ClockssRoyalSocietyOfChemistryPlugin") {
    $url = sprintf("%spublishing/journals/lockss/?journalcode=%s&volume=%s&year=%d", 
      $param{base_url}, $param{journal_code}, $param{volume_name}, $param{year});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if (defined($man_contents) && ($man_contents =~ m/$clockss_tag/)) {
        if ($man_contents =~ m/<title>\s*(.*)\s*<\/title>/si) {
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
    sleep(5);
                
  } elsif ($plugin eq "ClockssCopernicusPublicationsPlugin") {
    $url = sprintf("%s%s/index.html", 
      $param{base_url}, $param{volume_name});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if (defined($man_contents) && ($man_contents =~ m/$clockss_tag/)) {
        if ($man_contents =~ m/farbe_auf_journalhintergrund.>\s*(.*)\s*<\/h1>/si) {
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
    sleep(5);
                
  } elsif ($plugin eq "CopernicusPublicationsPlugin") {
    $url = sprintf("%s%s/index.html", 
      $param{base_url}, $param{volume_name});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if (defined($man_contents) && ($man_contents =~ m/$lockss_tag/)) {
        if ($man_contents =~ m/farbe_auf_journalhintergrund.>\s*(.*)\s*<\/h1>/si) {
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
    sleep(5);
    
  } elsif (($plugin eq "BMCPlugin") || ($plugin eq "ClockssBMCPlugin")) {
    $url = sprintf("%s%s/%s", 
      $param{base_url}, $param{journal_issn}, $param{volume_name});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if (defined($man_contents) && ($man_contents =~ m/$bmc_tag/) && ($man_contents =~ m/content\/$param{volume_name}/)) {
        if ($man_contents =~ m/<title>(.*)<\/title>/si) {
          $vol_title = $1;
          $vol_title =~ s/ | / /g;
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
    sleep(5);
                
  } elsif (($plugin eq "BioMedCentralPlugin") || ($plugin eq "ClockssBioMedCentralPlugin")) {
    $url = sprintf("%scontent/%s", 
      $param{base_url}, $param{volume_name});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if (defined($man_contents) && ($man_contents =~ m/$bmc_tag/) && ($man_contents =~ m/content\/$param{volume_name}/)) {
        if ($man_contents =~ m/<title>(.*)<\/title>/si) {
          $vol_title = $1;
          $vol_title =~ s/ | / /g;
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
    sleep(5);
                
  } elsif ($plugin eq "EmeraldPlugin") {
    $url = sprintf("%scrawlers/lockss.htm?issn=%s&volume=%s", 
      $param{base_url}, $param{journal_issn}, $param{volume_name});
    $man_url = uri_unescape($url);
    my $req = HTTP::Request->new(GET, $man_url);
    my $resp = $ua->request($req);
    if ($resp->is_success) {
      my $man_contents = $resp->content;
      if (defined($man_contents) && ($man_contents =~ m/$lockss_tag/) && ($man_contents =~ m/crawlers\/lockss.htm\?issn=$param{journal_issn}&amp;volume=$param{volume_name}/)) {
        if ($man_contents =~ m/<strong>Journal title:<\/strong>(.*)<br \/>/si) {
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
      $result = "--REQ_FAIL--"
    }
    sleep(5);
                
  } 
  if ($result eq "Plugin Unknown") {
    printf("*PLUGIN UNKNOWN* %s, %s, %s, %s\n",$result,$vol_title,$auid,$man_url);
    $total_missing_plugin = $total_missing_plugin + 1;
  } elsif ($result eq "Manifest") {
    printf("*MANIFEST* %s, %s, %s, %s\n",$result,$vol_title,$auid,$man_url);
    $total_manifests = $total_manifests + 1;
  } else {
    printf("*NO MANIFEST*(%s) %s, %s \n",$result, $auid,$man_url);
    $total_missing = $total_missing + 1;
  }
}
printf("*Total manifests found: %d\n", $total_manifests);
printf("*Total missing manifests: %d\n", $total_missing);
printf("*Total AUs with unknown plugin: %d\n", $total_missing_plugin);
exit(0);
