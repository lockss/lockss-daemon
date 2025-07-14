#!/usr/bin/awk -f

BEGIN { FS = ","; OFS = "," }
NR==FNR {
    if (FNR > 1) {
        line[$1] = $0;
        for (i=2; i<=NF; i++) {
            b[$1, i] = $i;
        }
    }
    next
}

FNR > 1 {
    changedV = 0;
    changedS = 0;
    for (i=3; i<=7; i++) { # Columns for GIT, C5-8, D1-13, C1-4, I1-4
        if (b[$1, i] != $i) {
            changedV++;
        }
    }
    if (b[$1, NF] != $NF) { # Status column (last column)
        changedS++;
    }

    if (changedV > 0 && changedS == 0) {
        versionChanged[versionCount++] = line[$1] "\n" $0;
    }
    if (changedS > 0) {
        statusChanged[statusCount++] = line[$1] "\n" $0;
    }
}

END {
    if (statusCount > 0) {
        print "Status changed";
        for (i = 0; i < statusCount; i++) {
            print statusChanged[i];
        }
    print " ";
    }

    if (versionCount > 0) {
        print "Version changed only";
        for (i = 0; i < versionCount; i++) {
            print versionChanged[i];
        }
    print " ";
    }

}