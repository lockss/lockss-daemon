#!/usr/bin/awk -f
#tdbout -t publisher,plugin,auid,publisher:info[tester],status,year *.tdb 
#           #1        #2      #3   #4                     #5     #6  

BEGIN {
  #After Feb 1 of _this_ year, start reporting on _last_ year. Before that, ignore _last_ year.
  str = "date +%m-%d-%Y";
  str | getline date;
  str = "date +%Y";
  str | getline current_year;
  str = "date +%W";
  str | getline week;
  current_year=current_year-1;
  if (week<9) {
      current_year=current_year-1
  }
  #printf "DEBUG: Current-Year:%s Current-Week:%s\n", current_year, week;
  FS="\t"
  pn = 0
}

{
  # add a loop to add line only if either [status is _not_ manifest] OR [journal_year (or journal end year) is gt or eq to 2010 && journal year (or journal end year) is lt or eq to the current year we are testing]
  #current_year = 2020
  end_year = 0
  incontract = 0
  test_year = ""
  test_year = 2010
  if (length($6) > 3) {
    end_year = substr($6,length($6)-3,4)
  }
  #printf "%s\n", $0
  if (($5 != "manifest") || ((end_year >= test_year) && (end_year <= current_year))) {
    incontract = 1
  }

  if (incontract == 1) {
    #split up the plugin. nn=number of fields. na is the name of the array.
    nn = split($2,na,/\./)
    #save the last element into 1p2.
    lp2 = na[nn]
    p[pn] = $1        #publisher
    n[pn] = lp2       #plugin
    auid[pn] = $3     #auid
    status[pn] = $5   #status
    r[pn] = $4        #tester
    if (r[pn] == "") {
      if ((substr(p[pn],1,2) <= "Mz")) {
        r[pn] = "5"
      } else if (substr(p[pn],1,2) >= "NA") {
        r[pn] = "8"
      }
    }
    pn++
    x[$5]++
    tt++
  }
}

END {
  s[0] = "expected"
  s[1] = "exists"
  s[2] = "manifest"
  s[3] = "wanted"
  s[4] = "testing"
  s[5] = "notReady"
  s[6] = "ready"
  s[7] = "released"
  s[8] = "down"
  s[9] = "superseded"
  s[10] = "doNotProcess"
  s[11] = "doesNotExist"
  sn = 12
  
  sc[0] = "expe"
  sc[1] = "exis"
  sc[2] = "mani"
  sc[3] = "want"
  sc[4] = "test"
  sc[5] = "notR"
  sc[6] = "read"
  sc[7] = "rele"
  sc[8] = "down"
  sc[9] = "supe"
  sc[10] = "doNP"
  sc[11] = "dNoE"
  scn = 12

  #print out header
  printf "Publisher\tPlugin\tAUid\tT\tTotal\tStatus"
  #for (j = 0 ; j < scn ; j++) {
  #  if (x[s[j]] > 0) {
  #  printf "\t%s", sc[j]
  #  }
  #}
  printf "\n"

  #print out publisher, plugin, auid, tester, status
  for (i = 0 ; i < pn ; i++) {
    printf "%s\t%s\t%s\t%s\t1", p[i], n[i], auid[i], r[i], status[i]
    #for (j = 0 ; j < sn ; j++) {
    #  if (x[s[j]] > 0){
#   #   if (status[i] == 0) {
    #  printf "\t.." 
    #} else {
    #    printf "1"
    #  }
    #}
#    }
    printf "\n"
  }
    #print out bottom line sums
    printf "Publisher\tPlugin\tAUid\tT\t%d", tt
    for (j = 0 ; j < sn ; j++) {
      if (x[s[j]] > 0) {
        printf "\t%d", x[s[j]]
      }
    }

  printf "\n"
  #printf "%s", date
  #printf "\n"
}

