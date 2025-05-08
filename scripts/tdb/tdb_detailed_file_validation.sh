#!/bin/bash

# Function to display usage
usage() {
    echo "Usage: $0 <file1> [<file2> ...]"
    echo "       or"
    echo "       $0 <wildcard>"
    exit 1
}

# Ensure at least one argument is passed
if [ "$#" -lt 1 ]; then
    echo "Error: No files specified"
    usage
fi

# Loop through each file specified
for FILENAME in "$@"; do

    # Check if the file exists
    if [ ! -f "$FILENAME" ]; then
        echo "Error: File '$FILENAME' not found"
        continue
    fi

    echo "Processing file: $FILENAME"

    # Test 1: Run tdbxml and pass error output to standard out
    echo "****Running tdbxml on $FILENAME..."
    ERROR_OUTPUT=$(./scripts/tdb/tdbxml --keep-going -o /dev/null "$FILENAME" 2>&1)
    TDBXML_STATUS=$?

    # Check if tdbxml encountered an error
    if [ $TDBXML_STATUS -ne 0 ]; then
        echo "Error detected in tdbxml for file $FILENAME:"
        echo "$ERROR_OUTPUT"
        # Exit the script immediately
        exit 1
    fi

    # Test 2: Run tdbout, sort and uniq the list, and compare with a second sorted run
    echo "****Finding duplicate AUids in $FILENAME..."
    # Find the lines that are not uniq
    DUPLICATES=$(./scripts/tdb/tdbout -VXEa "$FILENAME" | uniq -d)
    
    if [ -z "$DUPLICATES" ]; then
        echo "**No duplicate AUids found"
    else
        echo "**Duplicate AUids found"
        echo "$DUPLICATES" | head
    fi

    # Test 3: Run tdbout, sort and uniq the list, and compare with a second sorted run
    echo "****Finding duplicate name/plugin pairs in $FILENAME..."
    # Find the lines that are not uniq
    DUPLICATES=$(./scripts/tdb/tdbout -VXE -c plugin,name "$FILENAME" | uniq -d)
    
    if [ -z "$DUPLICATES" ]; then
        echo "**No duplicate name/plugin pairs found"
    else
        echo "**Duplicate name/plugin pairs found"
        echo "$DUPLICATES" | head
    fi

    echo "-----------------------------------"
done

exit 0

###########################################


#!/bin/bash
#
# Script to provide alerts to problems in the title database
tpath="/home/$LOGNAME/tmp"
mkdir -p $tpath

date
# Find incorrect status
echo "---------------------"
echo "---------------------"
echo "*Status typos gln: "
scripts/tdb/tdbout -t status tdb/prod/ | sort | uniq -c | grep -vw manifest \
                                                        | grep -vw released \
                                                        | grep -vw expected \
                                                        | grep -vw exists \
                                                        | grep -vw testing \
                                                        | grep -vw wanted \
                                                        | grep -vw ready \
                                                        | grep -vw down \
                                                        | grep -vw superseded \
                                                        | grep -vw doNotProcess \
                                                        | grep -vw notReady \
                                                        | grep -vw doesNotExist \
                                                        | grep -vw zapped \
                                                        | grep -vw finished \
                                                        | grep -vw readySource

#
# Find all reingest
echo "---------------------"
echo "---------------------"
echo "Clockss AUs with status2=manifest. Ready to release to production machines."
echo "reingest: 1:8082, 2:8085, 3:8083, 4:8082, 5:8082"
scripts/tdb/tdbout -F -t "au:hidden[proxy]" -Q 'status2 is "manifest"' tdb/clockssingest/ | sort | uniq -c
echo "No reingest set."
scripts/tdb/tdbout -F -t "publisher,title" -Q 'status2 is "manifest" and au:hidden[proxy] is ""' tdb/clockssingest/ | sort | uniq -c
echo " "

# GLN
# Find number of AUs ready for release in the prod title database
echo "----------------------"
./scripts/tdb/tdbout -Y -t status tdb/prod/ | sort | uniq -c
#
# Find plugin names with "Clockss" in the prod title database
echo "----------------------"
./scripts/tdb/tdbout -t publisher,name,plugin -Q 'plugin ~ "Clockss" and plugin !~ "needs"' tdb/prod/{,*/}*.tdb
echo " "

#
# Find tdb files possibly ready to be moved to retirement or needing first processing.
echo "CLOCKSS. tdb files not assigned to content testing"
scripts/tdb/tdbout -t publisher:info[tester],publisher -Q 'publisher:info[tester] is not "8" and publisher:info[tester] is not "5"' tdb/clockssingest/*.tdb | sort | uniq -c

# Find issn problems in gln title database
echo "---------------------"
echo "---------------------"
echo "GLN. ISSN issues"
#Use tdb out to generate a list of publisher, title, issn, eissn. Replace all amp with and. Remove all starting The. Ignore sub-titles.
scripts/tdb/tdbout -t publisher,title,issn,eissn tdb/prod/ | sed 's/\t\(.*\) & /\t\1 and /' | sed 's/\tThe /\t/' | sed 's/\(.*\t.*\): .*\(\t.*\t\)/\1\2/' | sort -u > $tpath/issn
scripts/tdb/scrub_table.pl $tpath/issn
#
# Find issn problems in clockss title database
echo "---------------------"
echo "---------------------"
echo "CLOCKSS. ISSN issues"
scripts/tdb/tdbout -t publisher,title,issn,eissn tdb/clockssingest/ | sed 's/\t\(.*\) & /\t\1 and /' | sed 's/\tThe /\t/' | sed 's/\(.*\t.*\): .*\(\t.*\t\)/\1\2/' | sort -u > $tpath/issn
scripts/tdb/scrub_table.pl $tpath/issn
#
echo "---------------------"
echo "Missing Slashes"
grep "param\[base_url\]" tdb/*/*.tdb tdb/*/*/*.tdb | grep "http.*://" | grep -v "/\s*$" | grep -v ":\s*#" | grep -v "\/\s*#"
echo "---------------------"
echo "---------------------"


