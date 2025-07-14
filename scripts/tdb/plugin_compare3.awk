#!/usr/bin/awk -f

BEGIN { FS = ","; OFS = "," }
NR==FNR {
    if (FNR > 1) {
        previousLine[$1] = $0;  # Collecting all lines from the previous file
        previousItems[$1] = 1;   # Tracking unique plugin identifiers
    }
    next
}

FNR > 1 {
    changedV = 0;
    changedS = 0;

    # Check if the current line's key exists in previousItems for "New Plugins"
    if (!($1 in previousItems)) {
        newPlugins[newCount++] = $0;  # Line item is new
    } else {
        for (i=3; i<=7; i++) { # Columns for GIT, C5-8, D1-13, C1-4, I1-4
            if (previousLine[$1, i] != $i) {
                changedV++;
            }
        }
        if (previousLine[$1, NF] != $NF) { # Status column (last column)
            changedS++;
        }
        
        # Only output if there's a change in either version or status
        if (changedV > 0 || changedS > 0) {
            if (changedS > 0) {
                statusChanged[statusCount++] = previousLine[$1] "\n" $0;
            }
            if (changedV > 0 && changedS == 0) {
                versionChanged[versionCount++] = previousLine[$1] "\n" $0;
            }
        }
    }
}

END {
    if (newCount > 0) {
        print "New Plugins";
        for (i = 0; i < newCount; i++) {
            print newPlugins[i];
        }
    }
    
    if (statusCount > 0) {
        print "Status changed";
        for (i = 0; i < statusCount; i++) {
            print statusChanged[i];
        }
    }
    
    if (versionCount > 0) {
        print "Version changed only";
        for (i = 0; i < versionCount; i++) {
            print versionChanged[i];
        }
    }
}