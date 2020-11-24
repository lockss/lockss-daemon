# Classic LOCKSS Daemon

![LOCKSS Program logo](https://lockss.github.io/images/lockss-logo-v1-1000w.png)

This is the source tree for the classic LOCKSS daemon (version 1.x). See <https://www.lockss.org/> for information about the LOCKSS Program.

## Source

A one-time copy can be obtained with Wget or Curl:

```shell
# With Wget
wget https://github.com/lockss/lockss-daemon/archive/master.zip

# With Curl
curl -L -o master.zip https://github.com/lockss/lockss-daemon/archive/master.zip
```

A buildable snapshot can be obtained by cloning the `master` branch:

```shell
# GitHub account with SSH key
git clone --depth 1 --branch master git@github.com:lockss/lockss-daemon.git

# Anonymous
git clone --depth 1 --branch master https://github.com/lockss/lockss-daemon.git
```

To obtain a local copy of the LOCKSS daemon Git repository, you can use the `git clone` command to establish the repository:

```shell
# GitHub account with SSH key
git clone git@github.com:lockss/lockss-daemon.git

# Anonymous
git clone https://github.com/lockss/lockss-daemon.git
```

You can subsquently use `git pull` to update.

## Prerequisites

For development purposes, you will need:

*   [Oracle JDK 8](https://www.oracle.com/java/technologies/javase-downloads.html#JDK8) or [OpenJDK 8](https://openjdk.java.net/projects/jdk8/).

*   [Apache Ant](http://ant.apache.org/) 1.7.1 or later.

*   Some tools require [Python 2.7](https://www.python.org/download/releases/2.7/) (invoked as `python2`).

*   Some tools require [Python 3.2](https://www.python.org/download/releases/3.2/) (invoked as `python3`) or greater.

### Additional Prerequisites

*   In order to process split Zip files at runtime, the command-line `zip` program must be installed. (Most Linux systems have `zip` and `unzip` installed by default, or they can be installed easily from the software package manager.)

### Other Prerequisites

*   [JUnit 3.8.1](http://junit.sourceforge.net/junit3.8.1/) is included in the LOCKSS source distribution, but the Ant targets that invoke JUnit (`test-xxx`) require the JUnit JAR to be on Ant's `CLASSPATH`. The easiest way to do this is to copy `lib/junit.jar` into Ant's `lib/` directory (`/path/to/ant/lib`).

*   For some of the tools, the `JAVA_HOME` environment variable must be set to the directory in which the JDK is installed, i.e.it is expected that `tools.jar` can be found in `${JAVA_HOME}/lib`.

*   Some of the tests also require the command line `zip` program to be installed.

## Build

```text
ant test-all
    Builds the system and runs all unit tests

ant test-one -Dclass=org.lockss.foo.TestBar
    Builds the system and runs the given JUnit test class

ant -projecthelp
ant -p
    Lists other build options
```

## Other Documentation

See the LOCKSS documentation portal at <https://lockss.github.io/>.
