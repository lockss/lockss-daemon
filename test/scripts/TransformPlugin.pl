#!/usr/bin/env perl -W

# Creating an XML file from a TDB file is called "converting".
# This program changes all instances of "ClockssHighWirePlugin" to 
# "HighWirePressPlugin".  I call this process 'transforming'; feel
# free to change this file name to something better.

my $line;

while ( <> ) {
	s/ClockssHighWirePlugin/HighWirePressPlugin/g;
	print;
}

