%define name lockss-daemon
%define version @RELEASENAME@
%define release @RPMRELEASE@
%define protoroot %{_builddir}/%{name}-root

# suppress brp-java-repack-jars
%define __jar_repack %{nil}

# Newer rpmbuild clears buildroot before running install macro, so can't
# set buildroot to already set up dir created by build.xml.
#
# Some versions of rpmbuild pay attention to buildroot macro, others to
# BuildRoot directive.
#
%define buildroot %{_builddir}/%{name}-root-installed
BuildRoot: %{_builddir}/%{name}-root-installed

Summary: LOCKSS daemon and configuration files
Name: %{name}
Version: %{version}
Release: %{release}
License: BSD
Group: Archiving
BuildArch: noarch
URL: http://www.lockss.org/
Distribution: any
Provides: lockss-daemon
Packager: LOCKSS group
#Requires: jdk >= 1.4.2
#Source: http://projects.sourceforge.net/lockss/

%description
Install the LOCKSS daemon and scripts to configure and run it
as a system service.

%prep
exit 0

%build
exit 0

# Newer rpmbuild clears buildroot before running install macro.
#
# Older rpmbuild doesn't clear buildroot, complains about any unpackaged
# files remaining below topdir.
%install
if [ ! -d $RPM_BUILD_ROOT ]; then
    mkdir $RPM_BUILD_ROOT
fi
for x in  %{protoroot}/*; do
    mv "$x" $RPM_BUILD_ROOT/`basename "$x"`
done
exit 0

%clean
exit 0

%pre
grep -q ^lockss: /etc/passwd || ( useradd -d /home/lockss -s /bin/false lockss ; echo "Created user 'lockss'" )
if [ -x /etc/init.d/lockss -a -s /etc/lockss/config.dat ]; then /etc/init.d/lockss stop ; echo "Stopped LOCKSS" ; fi
#if [ -x /etc/init.d/lockss -a -s /etc/lockss/config.dat ]; then /etc/init.d/lockss stop ; echo "Stopped LOCKSS" ; rm -f ~lockss/KeepGoing ; PS=`ps a | grep 'startdaemon lockss' | sed 's/\([0-9][0-9]*\) .*/\1/'` ; if [ "X${PS}" != X ]; then for A in ${PS} ; do if kill -9 ${A} >/dev/null 2>&1 ; then echo "Killed ${A}" ; fi ; done ; fi ; fi

%post
if [ -s /etc/lockss/config.dat ]; then /etc/init.d/lockss start ; echo "Started LOCKSS" ; else echo Please run /etc/lockss/hostconfig .  See /etc/lockss/README for details. ; fi

%files
%defattr(-,root,root)
/usr/share/lockss
%attr(755,root,root) /etc/lockss
%attr(755,root,root) /etc/init.d/lockss
%attr(644,root,root) /etc/logrotate.d/lockss

