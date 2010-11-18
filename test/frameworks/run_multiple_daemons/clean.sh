#!/bin/sh

usage() {
  echo "usage: $0 [-fg] [-all] [-keepau]"
  echo "  Deletes files gathered during testing.  By default all the content files"
  echo "  and the AU config (au.txt) are deleted, mostly in the background."
  echo "    -fg      Do all deletion in foreground"
  echo "    -all     Also delete configuration info"
  echo "    -keepau  Keep AU configuration (config/au.txt)"
  exit 2
}

do_bg="1"
do_all=
do_auconf="1"

while true ; do
  case "$1" in
    "-fg" )
      do_bg=
      shift; continue;;
    "-bg" )
      do_bg="1"
      shift; continue;;
    "-all" )
      do_all="1"
      shift; continue;;
    -keepau* )
      do_auconf=
      shift; continue;;
    -h | -help )
      usage
      exit 0;;
    -* )
      echo "Unknown flag $1"
      usage
      exit 2
  esac
  break
done

for i in 1 2 3 4; do
    cd test$i
    rm -rf V1 V3 dpid history iddb jvm_args localA localB plugins simcontent test.out v3state
    if [ ${do_auconf} ]; then
	rm -rf config/au.txt
    fi
    if [ ${do_all} ]; then
	rm -rf config local.opt
    fi
    if [ -n "${do_bg}" ]; then
	../../lib/bgrm cache
    else
	rm -rf cache
    fi
 
#    rm -rf V1 V3 cache config dpid history iddb jvm_args local.opt localA localB plugins simcontent test.out v3state
    cd ..
done
