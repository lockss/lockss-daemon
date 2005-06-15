%define name lockss-daemon
%define version @RELEASENAME@
%define release @RPMRELEASE@

Summary: LOCKSS daemon and configuration files
Name: %{name}
Version: %{version}
Release: %{release}
Copyright: BSD
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

%post
chmod +x /etc/lockss/* /etc/init.d/lockss
echo Please run /etc/lockss/hostconfig

%files
%defattr(-,root,root)
/usr/share/lockss
/etc/lockss
/etc/init.d/lockss

