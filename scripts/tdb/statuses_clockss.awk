#!/usr/bin/awk -f
# input: publisher\tplugin\tcontract_year\tyear\ttester\tstatus

BEGIN {
  FS="\t"
  pn = 0
}

{
  # add a loop to add line only if ending year is gt or eq to contract year
  if ((substr($4,1,4) >= $3) || ((length($4) == 9) && (substr($4,6,4) >= $3))) {
      nn = split($2,na,/\./)
      lp2 = na[nn]
    if (!(($1,lp2,$3,$4) in b)) {
      p[pn] = $1
      n[pn] = lp2
  #    n[pn] = $2
      t[pn] = $3
      d[pn] = $4
      r[pn] = $5
      pn++
    }
    b[$1,lp2,$3,$4]++
    c[$1,lp2,$3,$4,$6]++
    x[$6]++
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
  
  sc[0] = "X"
  sc[1] = "E"
  sc[2] = "M"
  sc[3] = "W"
  sc[4] = "C"
  sc[5] = "T"
  sc[6] = "N"
  sc[7] = "R"
  sc[8] = "D"
  sc[9] = "S"
  sc[10] = "Z"
  scn = 11

  #print out header
  printf "Publisher\tPlugin\tContr\tYear\tT\tTotal"
  for (j = 0 ; j < scn ; j++) {
    if (x[s[j]] > 0) {
    printf "\t%s", sc[j]
    }
  }
  printf "\n"

  #print out publisher, plugin, contract year, year, total aus
  for (i = 0 ; i < pn ; i++) {
    printf "%s\t%s\t%s\t%s\t%s\t%d", p[i], n[i], t[i], d[i], r[i], b[p[i],n[i],t[i],d[i]]
    for (j = 0 ; j < sn ; j++) {
      if (x[s[j]] > 0){
      if (c[p[i],n[i],t[i],d[i],s[j]] == 0) {
      printf "\t.." 
    } else {
        printf "\t%d", c[p[i],n[i],t[i],d[i],s[j]]
      }
    }
    }
    printf "\n"
  }
    #print out bottom line sums
    printf "Publisher\tPlugin\tContr\tYear\tT\t%d", tt
    for (j = 0 ; j < sn ; j++) {
      if (x[s[j]] > 0) {
        printf "\t%d", x[s[j]]
      }
    }

  printf "\n"
}

