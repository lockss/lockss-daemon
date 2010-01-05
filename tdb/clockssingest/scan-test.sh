#!/bin/bash

# This one-line program searches through a .tdb file to find all
# 'testing' AUs, then puts them in order.  This vastly helps in
# comparing against the ingest machine: AUs should be in the same
# order as the list in the ingest machine.

# The parameters:
#   - The .tdb file being examined.
#   - The ingest machine to look for.

# Example execution:

# % ./scan-test.sh american_physiological_society.tdb ingest4
#    au < testing ; 1947-1948 ; American Journal of Physiology Volume 152 ; 152 ; reingest4.clockss.org:8082 >
#    au < testing ; 1951 ; American Journal of Physiology Volume 165 ; 165 ; reingest4.clockss.org:8082 > 
#    au < testing ; 1951 ; American Journal of Physiology Volume 166 ; 166 ; reingest4.clockss.org:8082 > 
#    au < testing ; 1951 ; American Journal of Physiology Volume 167 ; 167 ; reingest4.clockss.org:8082 >
#    au < testing ; 1951-1952 ; American Journal of Physiology Volume 168 ; 168 ; reingest4.clockss.org:8082 >
#    au < testing ; 1955 ; American Journal of Physiology Volume 181 ; 181 ; reingest4.clockss.org:8082 >
# . . .

grep "testing" $1 | grep $2 | sort --key=7 -V