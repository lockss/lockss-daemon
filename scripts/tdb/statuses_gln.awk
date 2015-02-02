#!/usr/bin/awk -f
#tdbout -t publisher,plugin,publisher:info[tester],status,year *.tdb 

BEGIN {
  FS="\t"
  pn = 0
}

{
  # add a loop to add line only if either status is _not_ manifest or ending year is gt or eq to contract year
  current_year = 2014
  end_year = 0
  incontract = 0
  test_year = ""
  test_year = 2005
  if (length($5) > 3) {
    end_year = substr($5,length($5)-3,4)
  }
  #printf "%s\n", $0
  if (($4 != "manifest") || ((end_year >= test_year) && (end_year <= current_year))) {
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
        if ((substr(p[pn],1,2) <= "American R")) {
          r[pn] = "4"
        } else if ((substr(p[pn],1,2) >= "American S") && (substr(p[pn],1,2) <= "Bz")) {
          r[pn] = "4"
        } else if ((substr(p[pn],1,2) >= "CA") && (substr(p[pn],1,2) <= "Rz")) {
          r[pn] = "5"
        } else if (substr(p[pn],1,2) >= "SA") {
          r[pn] = "8"
        }
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
  s[4] = "crawling"
  s[5] = "testing"
  s[6] = "notReady"
  s[7] = "released"
  s[8] = "down"
  s[9] = "superseded"
  s[10] = "zapped"
  sn = 11
  
  sc[0] = "expe"
  sc[1] = "exis"
  sc[2] = "mani"
  sc[3] = "want"
  sc[4] = "craw"
  sc[5] = "test"
  sc[6] = "notR"
  sc[7] = "rele"
  sc[8] = "down"
  sc[9] = "supe"
  sc[10] = "zapp"
  scn = 11

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

