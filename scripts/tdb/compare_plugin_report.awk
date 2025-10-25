#!/usr/bin/awk -f
#FNR is the line number in the current file
#NR is the total line number and never resets

BEGIN {
    FS = ",";
    OFS = ",";
    columns[1] = 4;  # C5-8
    columns[2] = 5;  # D1-13
    columns[3] = 6;  # C1-4
    columns[4] = 7;  # I1-4
    columns[5] = 11; # Status
}


NR==FNR && FNR > 1 {
    key = $1;
    if ($2 && $0 ~ "Plugin") {
        first[key] = $0;                   # Store the complete line by the Plugin key
        for (i = 1; i <= 11; i++) {        # Store all columns for later comparison
            firstCols[key,i] = $i;
        }
    } else {
        firstDate = $0;
    }
}

NR!=FNR && FNR > 1 {
    key = $1;
    if ($2 && $0 ~ "Plugin") {
        second[key] = $0;                  # Store the complete line by the Plugin key
        for (i = 1; i <= 11; i++) {        # Store all columns for later comparison
            secondCols[key,i] = $i;
        }
    } else {
        secondDate = $0;
    }
}

END {
    print "..." firstDate "...VS..." secondDate "...";
    print "**New Plugins:";
    for (key in second) {
        if (!(key in first)) {
            print "  " second[key];          # Print the complete line that is new in second file
        }
    }
    print "**Plugin updates:";
    k = 0;
    for (key in first) {
        if (key in second) {
            isDifferent = 0;
            for (i in columns) {
                col = columns[i];
                if (firstCols[key,col] != secondCols[key,col]) {
                    isDifferent = 1;
                    break;
                }
            }
            if (isDifferent) {
                k = k + 1;
                print "  " k "-1: " first [key];
                print "  " k "-2: " second[key];
            }
        }
    }
}
 