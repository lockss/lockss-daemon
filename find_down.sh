#! /bin/bash
x=""
for x in tdb/prod/a*.tdb
do
if grep --quiet " down ; " $x
then
  if ! grep --quiet " released ; " $x
  then
    echo $x
    ./scripts/tdb/tdbout -t status,plugin $x | sort | uniq -c
  fi
fi
done
exit 0
