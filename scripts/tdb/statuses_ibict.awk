#!/usr/bin/awk -f
#tdbout -t publisher,plugin,publisher:info[tester],status,year *.tdb 

BEGIN {
  FS="\t"
  pn = 0
}

{
  # add a loop to add line only if either [status is _not_ manifest] OR [journal year (or journal end year) is lt or eq to the current year we are testing]
  current_year = 2014
  end_year = 0
  incontract = 0
  #test_year = ""
  #test_year = 2005
  if (length($5) > 3) {
    end_year = substr($5,length($5)-3,4)
  }
  #printf "%s\n", $0
  #if (($4 != "manifest") || ((end_year >= test_year) && (end_year <= current_year))) {
  if (($4 != "manifest") || (end_year <= current_year)) {
    incontract = 1
  }

  if (incontract == 1) {
    nn = split($2,na,/\./)
    lp2 = na[nn]
    if (!(($1,lp2) in b)) {
      p[pn] = $1
      n[pn] = lp2
  #    n[pn] = $2
      r[pn] = $3
      if (r[pn] == "") {
          r[pn] = "8"
      }
      pn++
    }
    b[$1,lp2]++
    c[$1,lp2,$4]++
    x[$4]++
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
  printf "Publisher\tPlugin\tT\tTotal"
  for (j = 0 ; j < scn ; j++) {
    if (x[s[j]] > 0) {
    printf "\t%s", sc[j]
    }
  }
  printf "\n"

  #print out publisher, plugin, tester, total aus
  for (i = 0 ; i < pn ; i++) {
    printf "%s\t%s\t%s\t%d", p[i], n[i], r[i], b[p[i],n[i]]
    for (j = 0 ; j < sn ; j++) {
      if (x[s[j]] > 0){
      if (c[p[i],n[i],s[j]] == 0) {
      printf "\t.." 
    } else {
        printf "\t%d", c[p[i],n[i],s[j]]
      }
    }
    }
    printf "\n"
  }
    #print out bottom line sums
    printf "Publisher\tPlugin\tT\t%d", tt
    for (j = 0 ; j < sn ; j++) {
      if (x[s[j]] > 0) {
        printf "\t%d", x[s[j]]
      }
    }

  printf "\n"
}

