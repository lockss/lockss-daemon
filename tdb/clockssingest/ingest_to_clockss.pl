#!/usr/bin/perl

# Suggestions by BEE:
# 'iterate_lines' has two modes: write to file, or write to screen.
# The previous version of this program would write single files to screen;
# I've changed that to always writing files.

# I recommend that we either remove the ability to write to screen, or 
# that we add a parameter that tells the program to write to screen.

use Cwd 'abs_path';

if ($ARGV[0] eq "-h") {

    print "To convert the entire 'clockssingest' directory use -d [directory_name].\n";
    print "To convert a single .tdb file use [filename].\n";
    
} elsif ($ARGV[0] eq "-d") {
    
    print "directory mode\n";
    
    $dir = abs_path($ARGV[1])."/";
    
    if (!($dir =~ m/clockssingest\/$/)) {
	
	print "This script takes files from the 'clockssingest' directory and puts them into 'clocks' directory.  Your declared directory is not 'clockssingest'.\n";
	exit;
	
    }
    
    opendir(DIR, $dir) || die "Can't open directory $dir: $!";
    @files= readdir(DIR);
    
    foreach $fn (0..@files-1) {
  
	if ($files[$fn] =~ m/^[a-z_]*\.tdb$/) {
	    
	    my $source_location = $dir.$files[$fn];
	    my $dest_location = $source_location;
	    $dest_location =~ s/clockssingest/clockss/i;
	    
	    print "converting: $files[$fn]\n";
	    
	    iterate_lines ($source_location, $dest_location, "tofile");  
	    
	}
	
    }
    
    print "done\n";
    closedir(DIR);
    
} else {
    
    print "single file mode\n";
    
    
    if (!($ARGV[0] =~ m/\.tdb$/)) {
	
	print "This is not a .tdb file.\n";
	exit;
	
    }
    
    my $source_location = abs_path($ARGV[0]);
    my $dest_location = $source_location;
    $dest_location =~ s/clockssingest/clockss/i;
    
    iterate_lines ($source_location, $dest_location, "tofile");      
}



sub iterate_lines {
    
    my $source_location = shift();
    my $dest_location = shift();
    my $write_to = shift();
    
    open (INPUT, $source_location) || die "Can't open $source_location: $!";
    open (OUTPUT, ">$dest_location") || die "Can't open $dest_location: $!";
    
    my @lines = <INPUT>;
    
    foreach $k (0..@lines-1) {
	
	if ($lines[$k] =~ m/au\s*<\s*(reTesting|released|ready|down|superseded)/) {
	    
	    if ($write_to eq "tostdout") { print $lines[$k]; } else { print OUTPUT $lines[$k]; }
	    
	} 
	elsif ($lines[$k] =~ m/au\s*</) {
	    
	}
	elsif ($lines[$k] =~ m/implicit <.*? comment\[proxy\] .*?>/) {
	    
	    $lines[$k] =~ s/comment\[proxy\]/proxy/i;
	    
	    if ($write_to eq "tostdout") { print $lines[$k]; } else { print OUTPUT $lines[$k]; }    
	    
	}
	else {
	    
	    if ($write_to eq "tostdout") { print $lines[$k]; } else { print OUTPUT $lines[$k]; }
	    
	}
	
    }
    
    if ($write_to eq "tostdout") { print "\n"; } else { print OUTPUT "\n"; } 
    
    close (INPUT);
    close (OUTPUT);    
}

