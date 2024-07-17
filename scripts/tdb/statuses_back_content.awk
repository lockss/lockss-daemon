#!/usr/bin/awk -f
# tdbout -t publisher,plugin,publisher:info[contract],year,publisher:info[tester],status,publisher:info[back],publisher:info[poller] *.tdb 
#                   $1         $2    $3                 $4                    $5
# INPUT: tdbout -t publisher,plugin,year,publisher:info[back],publisher:info[contract],status,publisher:info[platform] *.tdb 
# OUTPUT: publisher, plugin, contract, back, min_year, M year<back, M year>=back, platform

BEGIN {
  #After July 1 of this year, start reporting on this year. Before that, ignore this year.
  str = "date +%m-%d-%Y";
  str | getline date;
  str = "date +%Y";
  str | getline current_year;
  #close str;
  str = "date +%W";
  str | getline week;
  #close str;
  if (week<26) {
      current_year=current_year-1
  }
  #printf "DEBUG: Current-Year:%s Current-Week:%s\n", current_year, week;
  FS="\t"
  pn = 0
}

{
  # add a loop to add line only if either status is (wanted or testing) or ending year is gt or eq to contract year

  end_year = 0
  # test_year is the contract year, or the back year if there is one.
  test_year = ""
  if ($4 == "") {
    test_year = $5
  } else {
    test_year = $4
  }
  if (length($3) > 3) {
    end_year = substr($3,length($3)-3,4)
  }
  #printf "DEBUG: %s\t%s\t%s\t%s\t%s\t%s\t%s\n", $1,$2,$3,end_year,$4,test_year,$5
#  } else if ((end_year >= test_year) && ((end_year < current_year) || (end_year == current_year && incl_cur == 1))) {
   incontract = 1;
  }

#  if (incontract == 1) {
    nn = split($2,na,/\./)
    lp2 = na[nn]
    if (!(($1,lp2,$4) in b)) {
      p[pn] = $1
      n[pn] = lp2
  #    n[pn] = $2
      t[pn] = $3
      if ($7 == "") {
        k[pn] = ".."
      } else {
        k[pn] = $7
      }
      d[pn] = $4
      r1[pn] = $5
      if ($8 == "") {
      	r2[pn] = ".."
      } else {
      	r2[pn] = $8
      }
      pn++
    }
    b[$1,lp2,$4]++
    c[$1,lp2,$4,$6]++
    x[$6]++
#  }
}

END {
#  s[0] = "expected"
#  s[1] = "exists"
#  s[2] = "manifest"
#  s[3] = "wanted"
#  s[4] = "testing"
#  s[5] = "notReady"
#  s[6] = "ready"
#  s[7] = "crawling"
#  s[8] = "deepCrawl"
#  s[9] = "frozen"
#  s[10] = "ingNotReady"
#  s[11] = "finished"
#  s[12] = "down"
#  s[13] = "superseded"
#  s[14] = "zapped"
#  s[15] = "doNotProcess"
#  s[16] = "readySource"
#  sn = 17
#  
#  sc[0] = "expe"
#  sc[1] = "exis"
#  sc[2] = "mani"
#  sc[3] = "want"
#  sc[4] = "test"
#  sc[5] = "notR"
#  sc[6] = "read"
#  sc[7] = "craw"
#  sc[8] = "deep"
#  sc[9] = "froz"
#  sc[10] = "ingN"
#  sc[11] = "fini"
#  sc[12] = "down"
#  sc[13] = "supe"
#  sc[14] = "zapp"
#  sc[15] = "doNP"
#  sc[16] = "rdSc"
#  scn = 17

  #print out header
  # OUTPUT: publisher, plugin, contract, back, min_year, M year<back, M year>=back, platform
  printf "Publisher\tPlugin\tContr\tBack\tMinYear\tBackContent\tProcessing\tNote"
#  for (j = 0 ; j < scn ; j++) {
#    if (x[s[j]] > 0) {
#    printf "\t%s", sc[j]
#    }
  }
  printf "\n"

  #print out publisher, plugin, contract year, back, min_year, M year<back, M year>=back, platform
  for (i = 0 ; i < pn ; i++) {
    printf "%s\t%s\t%s\t%s\t%s\t%s\t%s\t%d", p[i], n[i], t[i], k[i], d[i], r1[i], r2[i], b[p[i],n[i],d[i]]
    for (j = 0 ; j < sn ; j++) {
      if (x[s[j]] > 0){
      if (c[p[i],n[i],d[i],s[j]] == 0) {
        printf "\t.." 
      } else {
        printf "\t%d", c[p[i],n[i],d[i],s[j]]
      }
    }
    }
    printf "\n"
  }
#    #print out bottom line sums
#    printf "Publisher\tPlugin\tContr\tBack\tYear\tT\tP\t%d", tt
#    for (j = 0 ; j < sn ; j++) {
#      if (x[s[j]] > 0) {
#        printf "\t%d", x[s[j]]
#      }
#    }

  printf "\n"
  printf "%s", date
  printf "\n"

}

