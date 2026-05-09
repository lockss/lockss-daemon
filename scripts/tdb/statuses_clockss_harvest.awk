#!/usr/bin/awk -f
# tdbout -t publisher,plugin,publisher:info[contract],year,publisher:info[tester],status,publisher:info[back],publisher:info[platform],file *.tdb 
#                                                            $1         $2    $3                 $4                    $5
# INPUT: tdbout -Q 'publisher:info[contract] is "2025"' -t publisher,plugin,year,publisher:info[tester],publisher:info[contract],status,publisher:info[platform],file *.tdb 
# OUTPUT: stage, file, tester, platform

BEGIN {
  str = "date +%m-%d-%Y";
  str | getline date;
  str = "date +%Y";
  str | getline current_year;
  FS="\t"
  pn = 0
}

{
  #Look at back content only, where year < contract.
  # add a loop to add line only if either status is (wanted or testing) or ending year is gt or eq to contract year

  end_year = 0
  # test_year is the contract year
  if ($5 <= current_year) {
    test_year = $5
  } else {
    test_year = current_year
  }
  if (length($3) > 3) {
    end_year = substr($3,length($3)-3,4)
  } else {
    end_year = $3
  }
  #printf "DEBUG: %s\t%s\t%s\t%s\t%s\t%s\t%s\n", $1,$2,$3,end_year,$4,test_year,$5
  if ((end_year < test_year) && (end_year <= current_year)) {
    backcontent = 1;
  }

  if (backcontent == 1) {
    stage[pn] = 0
    file[pn] = $8
    tester[pn] = $4
    platform[pn] = $7

    if (!(($8,$4,$7) in b)) {
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
  }
}

END {
  s[0] = "Undefined"
  s[1] = "Development"
  s[2] = "Production"
  s[3] = "Ready to Bill"
  sn = 4

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

  #print out stage, file, tester, platform
  for (i = 0 ; i < pn ; i++) {
    printf "%s\t%s\t%s\t%s\t%s\t%s\t%s\t%d", s[i], n[i], t[i], k[i], d[i], r1[i], r2[i], b[p[i],n[i],d[i]]
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

  printf "\n"
  printf "%s", date
  printf "\n"

}

