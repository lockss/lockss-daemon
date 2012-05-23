#!/usr/bin/awk -f
# input: publisher\tplugin\tcontract_year\tyear\tstatus

BEGIN {
  FS="\t"
  pn = 0
}

{
# add a loop to add line only if first 4 char or last 4 char of year are gt or eq to contract
if ((substr($4,0,4) >= $3) || ((length($4) == 9) && (substr($4,6,4) >= $3))) {
    nn = split($2,na,/\./)
    lp2 = na[nn]
  if (!(($1,lp2,$3,$4) in b)) {
    p[pn] = $1
    n[pn] = lp2
#    n[pn] = $2
    t[pn] = $3
    d[pn] = $4
    pn++
  }
  b[$1,lp2,$3,$4]++
  c[$1,lp2,$3,$4,$5]++
  x[$5]++
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

  printf "Publisher\tPlugin\tContr\tYear\tTotal"
  for (j = 0 ; j < scn ; j++) {
    printf "\t%s", sc[j]
  }
  printf "\n"

  for (i = 0 ; i < pn ; i++) {
    printf "%s\t%s\t%s\t%s\t%d", p[i], n[i], t[i], d[i], b[p[i],n[i],t[i],d[i]]
    for (j = 0 ; j < sn ; j++) {
      printf "\t%d", c[p[i],n[i],t[i],d[i],s[j]]
    }
    printf "\n"
  }
    printf "Publisher\tPlugin\tContract\tYear\t%d", tt
    for (j = 0 ; j < sn ; j++) {
    	printf "\t%d", x[s[j]]
    }

  printf "\n"
}

