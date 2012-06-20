#!/bin/bash
#
# Creates content folders for testing
for a in content1 content2 content3 content4 content5 content6 
do 
mkdir "$a"a
echo "[Pylorus]" > "$a"a/pylorus.conf
echo "" "" >> "$a"a/pylorus.conf
echo "local_servers =" >> "$a"a/pylorus.conf
echo "" >> "$a"a/pylorus.conf
echo "    validate://$a.lockss.org:8081"  >> "$a"a/pylorus.conf
echo "    validate://$a.lockss.org:8082"  >> "$a"a/pylorus.conf
echo ""  >> "$a"a/pylorus.conf
echo "username = lockss-u"  >> "$a"a/pylorus.conf
echo "password = lockss-p"  >> "$a"a/pylorus.conf
echo ""  >> "$a"a/pylorus.conf
echo "verbosity = 4"  >> "$a"a/pylorus.conf
mkdir "$a"b
echo "[Pylorus]" > "$a"b/pylorus.conf
echo "" "" >> "$a"b/pylorus.conf
echo "local_servers =" >> "$a"b/pylorus.conf
echo "" >> "$a"b/pylorus.conf
echo "    validate://$a.lockss.org:8083"  >> "$a"b/pylorus.conf
echo "    validate://$a.lockss.org:8084"  >> "$a"b/pylorus.conf
echo ""  >> "$a"a/pylorus.conf
echo "username = lockss-u"  >> "$a"b/pylorus.conf
echo "password = lockss-p"  >> "$a"b/pylorus.conf
echo ""  >> "$a"b/pylorus.conf
echo "verbosity = 4"  >> "$a"b/pylorus.conf
#echo "[Pylorus]\n\nlocal_servers" > content"$a"b/pylorus.conf
#cat "[Pylorus]\n\nlocal_servers =\n    validate://content1.lockss.org:8083\n    validate://content1.lockss.org:8084\n\nusername = lockss-u\npassword = lockss-p\n\nverbosity = 4" > pylorus.conf
done

exit 0

