#CLASSLIST=org.lockss.daemon org.lockss.proxy org.lockss.crawler org.lockss.poller org.lockss.protocol org.lockss.util org.lockss.servlet org.lockss.plugin
CLASSLIST=org.lockss.daemon
DIRLIST=org/lockss/daemon org/lockss/proxy org/lockss/crawler org/lockss/poller org/lockss/protocol org/lockss/util org/lockss/servlet org/lockss/plugin

javadoc:
	javadoc -sourcepath . $(CLASSLIST)

clean:
	for A in $(DIRLIST) ; do rm -f $${A}/*.html $${A}/*.css ; done
	rm -f *.html *.css
