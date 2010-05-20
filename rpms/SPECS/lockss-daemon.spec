%define name lockss-daemon
%define version @RELEASENAME@
%define release @RPMRELEASE@

Summary: LOCKSS daemon and configuration files
Name: %{name}
Version: %{version}
Release: %{release}
License: BSD
Group: Archiving
BuildArch: noarch
BuildRoot: %{_builddir}/%{name}-root
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

%install
exit 0

%clean
exit 0

%pre
grep -q ^lockss: /etc/passwd || ( useradd -d /home/lockss -s /bin/false lockss ; echo "Created user 'lockss'" )
if [ -x /etc/init.d/lockss -a -s /etc/lockss/config.dat ]; then /etc/init.d/lockss stop ; echo "Stopped LOCKSS" ; fi

%post
if [ -s /etc/lockss/config.dat ]; then /etc/init.d/lockss start ; echo "Started LOCKSS" ; else echo Please run /etc/lockss/hostconfig .  See /etc/lockss/README for details. ; fi

%files
%defattr(-,root,root)
/usr/share/lockss
%attr(755,root,root) /etc/lockss
%attr(755,root,root) /etc/init.d/lockss
/etc/logrotate.d/lockss

