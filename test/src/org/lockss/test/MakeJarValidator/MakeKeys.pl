#!/usr/bin/perl -W

# This program must be run with sudo (or su).

# This program generates the keys that we will use.
# For VERY obvious reasons, these keys should not be used for anything
# other than these tests.  (A big clue of how insecure these keys are
# should come from the keypass...)

# We need to run this program every 640 years (640 * 365.2422 days/year =
# 233755)  As Bill Gates says, 640 years should be enough for anyone.

my $CURRENT;

# Remove the old keys.
system("rm keystore");
system("rm keystore2");

# Generate a good signature
system( "keytool -genkey -alias good -keypass \"goodgood\" -storepass \"keyskeys\" -validity 233755 -dname \"CN=Good Signature, OU=LOCKSS, O=Stanford Libraries, L=Palo Alto, S=California, C=USA\" -keystore \"keystore\"");
system( "keytool -genkey -alias good2 -keypass \"goodgood\" -storepass \"keyskeys\" -validity 233755 -dname \"CN=Good Signature 2, OU=LOCKSS, O=Stanford Libraries, L=Palo Alto, S=California, C=USA\" -keystore \"keystore2\"");

# Generate an expired signature
open(CURRENT, "date -u |");
$CURRENT = <CURRENT>;
close(CURRENT);

# Set date to when Eli and Brent got married.
system( "date --set \"Sat Aug 18 13:00:00 PDT 2001\"");
system( "keytool -genkey -alias expired -keypass \"expired\" -storepass \"keyskeys\" -validity 1 -dname \"CN=Expired Signature, OU=LOCKSS, O=Stanford Libraries, L=Palo Alto, S=California, C=USA\" -keystore \"keystore\"");

# Set date to near the End Of The World As We Know It
# (See: http://en.wikipedia.org/wiki/Year_2038_problem)
system( "date --set \"Tue Jan 19 03:13:00 UTC 2038\"");
system( "keytool -genkey -alias future -keypass \"future\" -storepass \"keyskeys\" -validity 1 -dname \"CN=Future Signature, OU=LOCKSS, O=Stanford Libraries, L=Palo Alto, S=California, C=USA\" -keystore \"keystore\"");

# Restore the date to now.
system( "date --set \"" . $CURRENT . "\"");

