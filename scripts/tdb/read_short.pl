#!/usr/bin/perl -w

use HTTP::Request;
use LWP::UserAgent;
use HTTP::Cookies;

use URI::Escape;
use Getopt::Long;
use HTML::Entities;
use utf8;
use Encode qw(decode encode);
use IO::Socket::SSL 'debug3';

my $cjar = HTTP::Cookies->new();
my $ua = LWP::UserAgent->new( cookie_jar => $cjar, agent => "LOCKSS cache" );
#my $ua = LWP::UserAgent->new( cookie_jar => $cjar, agent => "LOCKSS cache", ssl_opts => { verify_hostname => 0 } );
$url = "https://www.liverpooluniversitypress.co.uk/lockss-manifest/archives/26/";
$man_url = uri_unescape($url);
my $req = HTTP::Request->new(GET, $man_url);
my $resp = $ua->request($req);
print($resp->headers_as_string);
print($resp->content);
