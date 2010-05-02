perl TransformPlugin.pl < ../../tdb/clockssingest/sage_publications.tdb | ./tdbproc.py -s xml -T > ../frameworks/title_db_files/z_sage_publications.xml
perl TransformPlugin.pl < ../../tdb/clockssingest/oxford_university_press.tdb | ./tdbproc.py -s xml -T > ../frameworks/title_db_files/z_oxford_university_press.xml
cp ../frameworks/title_db_files/z_sage_publications.xml ../frameworks/title_db_files/a_sage_publications.xml
cp ../frameworks/title_db_files/z_oxford_university_press.xml ../frameworks/title_db_files/a_oxford_university_press.xml
