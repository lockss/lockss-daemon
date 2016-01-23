## LOCKSS-Daemon

This is the source tree for the LOCKSS daemon.
See [http://www.lockss.org/](#) for information about the LOCKSS project.

### Obtaining Source
A one-time copy can be obtained with wget or curl:

`wget https://github.com/lockss/lockss-daemon/archive/master.zip`

or

`curl -L -o master.zip https://github.com/lockss/lockss-daemon/archive/master.zip`

A buildable snapshot can be obtained by cloning the master branch
`git clone --depth 1 --branch master https://github.com/lockss/lockss-daemon.git`

To establish a local copy of the LOCKSS git repository you can
use the “git clone” command to establish the repository and “git pull” to pull in updates:

`git clone https://github.com/lockss/lockss-daemon.git`

To update the local copy run within you local lockss-daemon dir:

`git pull`

### Building and Installing

### Dependencies:
- Sun JDK 7.  Java 8 is not yet supported.
- Ant 1.7.1 or greater.  (http://ant.apache.org/)
- Python 2.5 or greater (but not 3.x).

On Debian or Ubuntu you can grab all of the above with:

`apt-get install python sun-java7-jdk ant ant-optional`

### Other Dependencies:

#### Junit
Junit is included in the LOCKSS source distribution, but the Ant targets that invoke JUnit (test-xxx) require the JUnit jar to be on Ant's classpath.  The easiest way to do that is to copy lib/junit.jar into Ant's lib directory (\<ant-install-dir\>/ant/lib) or your local .ant/lib directory.

##### JAVAHOME Environment variable
For some of the tools the JAVAHOME env var must be set to the directory in which the JDK is installed.  (I.e., it's expected that tools.jar can be found in $JAVAHOME/lib)

### To Build
-`ant test-all`

Builds the system and runs all unit tests

-`ant test-one -Dclass=org.lockss.foo.TestBar`

Builds the system and runs one JUnit test class.

-`ant -projecthelp`

Lists other build options

-`ant btf`

Build out the test frameworks to allow running a daemon and testing on local machine.
