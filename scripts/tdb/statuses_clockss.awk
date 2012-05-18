#!/usr/bin/awk -f
# input: publisher\tcontract_year\tyear\tstatus
# add a loop to add line only if first 4 char or last 4 char of year are gt or eq to contract

BEGIN {
  FS="\t"
  pn = 0
}

{
  if (substr($3,0,4) >= $2) {
  if (!(($1,$2,$3) in b)) {
    p[pn] = $1
    t[pn] = $2
    d[pn] = $3
    pn++
  }
  b[$1,$2,$3]++
  c[$1,$2,$3,$4]++
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
  sn = 10

  printf "Publisher\tContract\tYear\tTotal"
  for (j = 0 ; j < sn ; j++) {
    printf "\t%s", s[j]
  }
  printf "\n"

  for (i = 0 ; i < pn ; i++) {
    printf "%s\t%s\t%s\t%d", p[i], t[i], d[i], b[p[i],t[i],d[i]]
    for (j = 0 ; j < sn ; j++) {
      printf "\t%d", c[p[i],t[i],d[i],s[j]]
    }
    printf "\n"
  }
    printf "Publisher\tContract\tYear\t%d", tt
    for (j = 0 ; j < sn ; j++) {
    	printf "\t%d", x[s[j]]
    }

}

