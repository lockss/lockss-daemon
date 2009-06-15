import base64
import cookielib
import glob
import httplib
import mimetools
import mimetypes
import os
import random
import re
import sha
import shutil
import signal
import socket
import sys
import time
import types
import urllib
import urllib2
import urlparse
import xml.dom.minidom

from os import path

from lockss_util import *


# Constants

DEF_TIMEOUT = 60 * 30  # 30 minute default timeout for waits.
DEF_SLEEP = 5         # 5 second default sleep between loops.
#DEF_SLEEP = 1          # Save 2 seconds per sleep at minimal cost
FILE_TYPE_TEXT = 1
FILE_TYPE_HTML = 2
FILE_TYPE_PDF = 4
FILE_TYPE_JPEG = 8
FILE_TYPE_BIN = 16

###########################################################################
##
## Configuration file strings, used when creating default config files.
##
###########################################################################

globalConfigTemplate = """\
# LOCKSS & LCAP tuning parameters
org.lockss.log.default.level=%(logLevel)s

org.lockss.config.reloadInterval=60m

#comm settings
org.lockss.comm.enabled=false
org.lockss.scomm.enabled=true

# lcap protocol settings
org.lockss.protocol.ttl=2
org.lockss.protocol.hashAlgorithm=SHA-1

# crawl settings
org.lockss.crawler.startCrawlsInitialDelay=2m
org.lockss.crawler.startCrawlsInterval=2m
org.lockss.crawler.odc.queueRecalcAfterNewAu=5s

# poll settings
org.lockss.comm.enabled=false
org.lockss.scomm.enabled=true
org.lockss.scomm.maxMessageSize=33554432
org.lockss.scomm.timeout.connect=15s

org.lockss.poll.pollStarterInitialDelay=1m
org.lockss.poll.queueEmptySleep=30s
org.lockss.poll.queueRecalcInterval=30s
org.lockss.poll.v3.maxSimultaneousV3Pollers=1
org.lockss.poll.v3.maxSimultaneousV3Voters=100
org.lockss.poll.v3.deleteExtraFiles=true
org.lockss.poll.v3.quorum=4
arg.lockss.poll.v3.minNominationSize=0
org.lockss.poll.v3.maxNominationSize=0
org.lockss.poll.v3.voteDurationPadding=30s
org.lockss.poll.v3.tallyDurationPadding=30s
org.lockss.poll.v3.voteDurationMultiplier=3
org.lockss.poll.v3.tallyDurationMultiplier=3
org.lockss.poll.v3.receiptPadding=30s

# Set the v3 poll state dir to /tmp
org.lockss.poll.v3.messageDir=/tmp

org.lockss.metrics.slowest.hashrate=250
org.lockss.state.recall.delay=5m

# Turn down logging on areas that we normally
# don't care much about during polling tests.
org.lockss.log.LockssServlet.level=info
org.lockss.log.GenericHasher.level=info
org.lockss.log.GoslingHtmlParser.level=info
org.lockss.log.StringFilter.level=info
org.lockss.log.PluginMgr.level=info
org.lockss.log.DaemonStatus.level=info
"""

localConfigTemplate = """\
org.lockss.platform.ui.username=%(username)s
org.lockss.platform.ui.password=%(password)s
org.lockss.platform.diskSpacePaths=%(dir)s
org.lockss.cache.location=%(dir)s
org.lockss.proxy.start=%(proxyStart)s
org.lockss.proxy.port=7999
org.lockss.ui.port=%(uiPort)s
org.lockss.localIPAddress=%(ipAddr)s
org.lockss.comm.unicast.port=%(unicastPort)s
org.lockss.comm.unicast.sendToPort=%(unicastSendToPort)s
org.lockss.comm.unicast.sendToAddr=127.0.0.1
org.lockss.proxy.icp.port=%(icpPort)s
"""


# Classes


class LockssDaemon:
    """Wrapper around a daemon instance.  Controls starting and stopping a LOCKSS Java daemon."""

    def __init__( self, dir, cp, configList ):
        self.daemonDir = dir
        self.cp = cp
        self.configList = configList
        try:
            javaHome = os.environ[ 'JAVA_HOME' ]
        except KeyError:
            raise LockssError( 'JAVA_HOME must be set' )
        self.javaBin = path.join( javaHome, 'bin', 'java' )
        self.logfile = path.join( self.daemonDir, 'test.out' )
        self.isRunning = False

    def start( self ):
        if not self.isRunning:
            oldcwd = os.getcwd()
            os.chdir( self.daemonDir )
            cmd = '%s -server -cp %s -Dorg.lockss.defaultLogLevel=debug org.lockss.app.LockssDaemon %s >> %s 2>&1 & echo $! > %s/dpid' % ( self.javaBin, self.cp, ' '.join( self.configList ), self.logfile, self.daemonDir )
            os.system( cmd )
            self.pid = int( open( path.join( self.daemonDir, 'dpid' ), 'r' ).readline() )
            self.isRunning = True
            os.chdir( oldcwd )

    def stop( self ):
        if self.isRunning:
            try:
                os.kill( self.pid, signal.SIGKILL )
            except OSError:
                log.debug( 'Daemon already dead?' )
            else:
                log.debug( 'Daemon stopped' )
            finally:
                self.isRunning = False

    def requestThreadDump(self):
        if self.isRunning:
            os.kill( self.pid, signal.SIGQUIT )
            # Horrible kludge.  Pause for one second so that the VM has time to comply and flush the daemon log file.
            time.sleep( 1 )


class Node:
    """In keeping with Python written in a Java accent, here's a simple
    object that could just as easily have been a tuple or a dict."""
    def __init__( self, url, file ):
        self.url = url
        self.file = file

    def __cmp__( self, other ):
        return cmp( self.url, other.url )

    def __str__( self ):
        return "%s" % self.url


class Framework:
    """A framework is a set of LOCKSS daemons and associated test
    clients.  The framework daemons can be started or stopped as a
    group, and their status can be accessed by the clients the
    framework creates and returns at initialization time.

    To use a framework, it should be created by passing it two paths:
    A path to a LOCKSS build environment (where lockss.jar and
    lockss-dev.jar can be found), and a work directory in which the
    daemons will be run.  Optionally, host name and port numbers for
    each daemon can be passed in.  If not supplied, these default to
    'localhost' and ports 8041 through 8041+n, where 'n' is the number
    of daemons that have been created.

    When the framework is created, it returns a tuple of associated
    clients, one per daemon.  These clients are used to perform
    functional test interactions with the daemons."""

    def __init__(self, daemonCount = None, urlList = None,
                 startUiPort = None, startV3Port = None):
        self.workDir = path.abspath(config.get('workDir', './'))
        self.frameworkDir = self.__makeTestDir()
        self.projectDir = config.get('projectDir')
        if self.projectDir is None:
            # will raise LockssError if not found.
            self.projectDir = self.__findProjectDir()
        self.localLibDir = path.join(self.workDir, 'lib')
        self.projectLibDir = path.join(self.projectDir, 'lib')
        if not daemonCount:
            self.daemonCount = int(config.get('daemonCount', 4))
        else:
            self.daemonCount = daemonCount
        if not startUiPort:
            self.startUiPort = int(config.get('startUiPort', 8041))
        else:
            self.startUiPort = startUiPort
        if not startV3Port:
            self.startV3Port = int(config.get('startV3Port', 8801))
        else:
            self.startV3Port = startV3Port
        self.username = config.get( 'username', 'lockss-u' )
        self.password = config.get( 'password', 'lockss-p' )
        self.logLevel = config.get( 'daemonLogLevel', 'debug' )
        self.hostname = config.get( 'hostname', 'localhost' )

        self.clientList = [] # ordered list of clients.
        self.daemonList = [] # ordered list of daemons.
        self.configCount = 0 # used when writing daemon properties

        self.isRunning = False

        # Assert that the project directory exists and that
        # the necessary libraries exist.
        if not self.__isProjectDirReady():
            raise LockssError("Project dir %s is not ready." % self.projectDir)

        # write the framework global config file.
        globalConfigFile = path.join(self.frameworkDir, 'lockss.txt')
        self.__writeConfig( globalConfigFile, globalConfigTemplate % {"logLevel": self.logLevel} )

        # write the 'extra' config file.  This may be empty if there
        # are no LOCKSS daemon properties defined, but that's OK.
        extraConfigFile = path.join(self.frameworkDir, 'lockss.opt')
        self.__writeConfig( extraConfigFile, config.daemonItems() )

        # Copy the LOCKSS libs to a local working dir
        self.__setUpLibDir()

        # Set up a each daemon and create a work directory for it.
        for ix in range(0, self.daemonCount):
            port = self.startUiPort + ix
            v3Port = self.startV3Port + ix

            daemonDir = path.abspath( path.join( self.frameworkDir, 'daemon-' + str( port ) ) )
            # local config
            localConfigFile = path.join(daemonDir, 'local.txt')
            # Init the directory
            os.mkdir(daemonDir)
            # write the daemon-specific config file
            self.__writeLocalConfig(localConfigFile, daemonDir, port)
            # Create daemon
            if not urlList:
                daemonConfUrls = (globalConfigFile, localConfigFile, extraConfigFile)
            else:
                daemonConfUrls = urlList + (localConfigFile,)
            daemon = LockssDaemon(daemonDir, self.__makeClasspath(),
                                  daemonConfUrls)

            # create client for this daemon
            client = Daemon_Client(daemon, self.hostname, port, v3Port,
                                   self.username, self.password)

            self.clientList.append(client)
            self.daemonList.append(daemon)

    def start(self):
        """Start each daemon in the framework."""
        # if 'client' is none, start all daemons.
        for daemon in self.daemonList:
            daemon.start()

        self.isRunning = True

    def stop(self):
        """Stop each daemon in the framework."""
        for daemon in self.daemonList:
            daemon.stop()

        self.isRunning = False

    def waitForFrameworkReady(self):
        """Convenience function to ensure all daemons are ready"""
        for client in self.clientList:
            assert client.waitForDaemonReady()

    def clean(self):
        """Delete the current framework working directory and local library directory."""
        shutil.rmtree(self.frameworkDir)
        if self.localLibDirCreated:
            shutil.rmtree(self.localLibDir)

    def appendLocalConfig(self, conf, client):
        """Append the supplied configuration in the string or dictionary
        'conf' (name/value pairs) to the local config file of the specified
        client daemon 'client'"""
        self.__writeConfig(path.join(client.daemonDir, 'local.txt'), conf, True)

    def appendExtraConfig(self, conf):
        """Append the supplied configuration in the string or dictionary
        'conf' (name/value pairs) to the extra config file for all
        client daemons"""
        self.__writeConfig(path.join(self.frameworkDir, 'lockss.opt'), conf, True)

    def reloadAllConfiguration(self):
        for client in self.clientList:
            client.reloadConfiguration()

    def checkForDeadlock(self):
        """Request that all the daemons in the framework dump their
        threads, and then look for deadlock messages.  Returns a list of
        log files to check, or an empty list if no deadlocks are found."""
        deadlockList = []
        for daemon in self.daemonList:
            logfile = daemon.logfile
            daemon.requestThreadDump()
            if self.__grepDeadlock(logfile):
                deadlockList.append(logfile)
        return deadlockList

    def __grepDeadlock(self, f):
        """Very naive implementation."""
        return (open(f, 'r').read()).find('FOUND A JAVA LEVEL DEADLOCK') > -1

    def __setUpLibDir(self):
        """Create the directory 'lib' if it does not already exist, then copy
        lockss.jar, lockss-test.jar, and lockss-plugin.jar from self.projectDir/lib
        to 'lib'."""
        self.localLibDirCreated = not path.isdir(self.localLibDir)
        if self.localLibDirCreated:
            os.mkdir(self.localLibDir)

        lockssJar = path.join(self.projectLibDir, 'lockss.jar')
        lockssTestJar = path.join(self.projectLibDir, 'lockss-test.jar')
        lockssPluginJar = path.join(self.projectLibDir, 'lockss-plugins.jar')

        # Copy from buildLib to localLib

        shutil.copy(lockssJar, self.localLibDir)
        shutil.copy(lockssTestJar, self.localLibDir)
        shutil.copy(lockssPluginJar, self.localLibDir)

    def __makeClasspath(self):
        """Return a list of all *.jar and *.zip files under self.projectDir/lib,
        plus all *.jar and *.zip files under self.localLibDir."""

        fd = open(path.join(self.projectDir, 'test', 'test-classpath'), 'r')
        line = fd.readline().strip()
        fd.close()

        return (":".join(glob.glob(path.join(self.localLibDir, "*.jar"))) +
                 ":" + line)

    def makeClasspath(self):
        return self.__makeClasspath()

    def __encPasswd(self, passwd):
        """Return a SHA1 encoded version of the specified password."""
        digest = sha.new()
        digest.update(passwd)
        return digest.hexdigest()

    def __findProjectDir(self):
        """Walk up the tree until 'build.xml' is found.  Assume this
        is the root of the project."""
        ## Shortcut for cwd.
        cwd = os.getcwd()
        if (path.isfile(path.join(cwd, 'build.xml'))):
            return cwd

        path_elements = os.getcwd().split(os.sep)
        while len(path_elements) > 1:
            p = os.sep.join(path_elements)
            if path.isfile(path.join(p, 'build.xml')):
                return p
            else:
                path_elements = path_elements[:len(path_elements) - 1]

        # Wasn't found, raise LockssError
        raise LockssError("No LOCKSS project directory found.")

    def __isProjectDirReady(self):
        if not path.isdir(self.projectDir):
            return False
        if not (path.isfile(path.join(self.projectDir, 'lib', 'lockss.jar')) and
                path.isfile(path.join(self.projectDir, 'lib', 'lockss-test.jar')) and
                path.isfile(path.join(self.projectDir, 'lib', 'lockss-plugins.jar'))):
            return False

        return True

    def __makeTestDir(self):
        """Construct the name of a directory under the top-level work
        directory in which to create the framework.  This allows each
        functional test to work in its own directory, and for all the test
        results to be left for examination if deleteAfter is false."""
        global frameworkCount
        frameworkCount += 1
        fwDir = path.join(self.workDir, 'testcase-' + str(frameworkCount))
        if path.isdir(fwDir):
            log.info("Old directory exists.  Renaming to %s " % (fwDir + "-" + time.time().__str__()))
            os.rename(fwDir, (fwDir + "-" + time.time().__str__()))
        os.mkdir(fwDir)
        return fwDir

    def __writeLocalConfig(self, file, dir, uiPort):
        """Write local config file for a daemon.  Daemon num is
        assumed to start at 0 and go up to n, for n-1 daemons."""
        baseUnicastPort = 9000
        baseIcpPort = 3031
        tr = str(self.configCount + 1)
        # Kluge for proper handling of multiple daemons.  If this is
        # the first dameon, enable the proxy.
        if self.configCount == 0:
            proxyStart = 'yes'
        else:
            proxyStart = 'no'
        ipAddr = tr + '.' + tr + '.' + tr + '.' + tr
        unicastPort = str(baseUnicastPort + self.configCount)
        icpPort = str(baseIcpPort + self.configCount)
        unicastSendToPort = str(baseUnicastPort)
        self.__writeConfig( file, localConfigTemplate % {"username": self.username,
                                                         "password": "SHA1:%s" % self.__encPasswd(self.password),
                                                         "dir": dir, "proxyStart": proxyStart,
                                                         "uiPort": uiPort, "ipAddr": ipAddr,
                                                         "unicastPort": unicastPort,
                                                         "unicastSendToPort": unicastSendToPort,
                                                         "icpPort": icpPort} )
        self.configCount += 1

    def __writeConfig(self, filename, config, append=False ):
        """Write a properties string or dictionary to a configuration file"""
        f = open( filename, 'a' if append else 'w' )
        if type( config ) is str:
            f.write( config )
        else:
            for key, value in config.iteritems():
                f.write( '%s=%s\n' % ( key, value ) )
        f.close()


class Client:
    """LOCKSS server client interface"""

    def __init__( self, hostname, port, v3Port, username, password ):
        self.hostname = hostname
        self.port = port
        self.v3Port = v3Port
        self.base_url = 'http://' + self.hostname + ':' + str( port ) + '/'
        authentication_handler = urllib2.HTTPBasicAuthHandler()
        authentication_handler.add_password( 'LOCKSS Admin', self.base_url, username, password )
        self.URL_opener = urllib2.build_opener( urllib2.HTTPCookieProcessor( cookielib.CookieJar() ), authentication_handler, Multipart_Form_HTTP_Handler )

    def ID( self ):
       """Standardized notation"""
       return self.hostname + ':' + str( self.port )
        
    def getPeerId(self):
        #return ("TCP:[127.0.0.1]:%d" % self.v3Port)
        return "TCP:[%s]:%d" % ( socket.gethostbyname( self.hostname ), self.v3Port )

    def createAu(self, au):
        """Create an AU."""
        post = self.__makeAuPost(au, 'Create')
        post.execute()
#       if not self.waitForCreateAu(au):
#           raise LockssError("Timed out while waiting for AU %s to appear." % au)

    def waitAu(self, au):
        """Block until the AU appears in the server's status table."""
        if not self.waitForCreateAu(au):
            raise LockssError("Timed out while waiting for AU %s to appear." % au)

    def deleteAu(self, au):
        """Delete a simulated AU.  This will block until the AU no longer
        appears in the server's status table."""
        post = self.__makeAuPost(au, 'Confirm Delete')
        post.execute()
        if not self.waitForDeleteAu(au):
            raise LockssError("Timed out while waiting for "\
                              "AU %s to be deleted." % au)

    def setPublisherDown(self, au):
        """Modify the configuration of the specified AU to set it's 'pub_down'
        parameter."""
        post = self.__makeAuPost(au, 'Update')
        post.add_data( 'lfp.pub_down', 'true' ) # Override setting in AU
        post.execute()
        if not self.waitForPublisherDown(au):
            raise LockssError("Timed out waiting for AU %s to be marked "\
                              "'Publisher Down'." % au)

    def reactivateAu(self, au, doWait=True):
        """Re-activate a simulated AU.  If doWait is set, wait for the AU
        to disappear from the server status table before returning."""

        if self.isActiveAu(au):
            return

        post = self.__makeAuPost(au, 'DoReactivate')
        post.execute()
        if doWait:
            if not self.waitForReactivateAu(au):
                raise LockssError("Timed out while waiting for "\
                                  "AU %s to be reactivated." % au)

    def deactivateAu(self, au, doWait=True):
        """Deactivate a simulated AU.  If doWait is set, wait for the AU
        to disappear from the server status table before returning."""
        if not self.isActiveAu(au):
            return

        post = self.__makeAuPost(au, 'Confirm Deactivate')
        post.execute()

        if doWait:
            if not self.waitForDeactivateAu(au):
                raise LockssError("Timed out while waiting for "\
                                  "AU %s to be deactivated." % au)

    ##
    ## Reload, Backup, Restore the configuration
    ##

    def reloadConfiguration(self):
        """Cause the daemon to reload its config."""

        post = self.__makePost('DebugPanel', {'action': 'Reload Config'})
        post.execute()


    def backupConfiguration(self):
        """Very quick and dirty way to download the config backup."""

        backupData = HTTP_Request( self.URL_opener, self.base_url + "BatchAuConfig?lockssAction=Backup" ).execute().read()

        # Write the data into a file
        f = file("configbackup.zip", "w")
        f.write(backupData)
        f.close()


    def restoreConfiguration(self, au):

        post = HTTP_Request( self.URL_opener, self.base_url + "BatchAuConfig" )
        post.add_data( 'lockssAction', 'SelectRestoreTitles' )
        post.add_data( 'Verb', '5' )
        post.add_file( 'AuConfigBackupContents', 'configbackup.zip' )

        result = post.execute().read()

        log.debug3("Got result from Batch AU Config servlet\n%s" % result)

        # Expect to see the strings 'Simulated Content: foo' and
        # 'Restore Selected AUs' in the response.  FRAGILE; obviously
        # this will break if the servlet UI is changed.
        if au.title not in result or 'Restore Selected AUs' not in result:
            raise LockssError( 'Unexpected response from BatchAuConfig servlet' )

        # Now confirm the restoration.
        post = HTTP_Request( self.URL_opener, self.base_url + "BatchAuConfig" )
        post.add_data( 'lockssAction', 'DoAddAus' )
        post.add_data( 'Verb', '5' )
        post.add_data( 'auid', au.auId ) 
        result = post.execute()

        # If this was successful, delete the configbackup.zip file
        os.unlink("configbackup.zip")

    ##
    ## General status accessors
    ##

    def getCrawlStatus(self, au=None):
        """Return the current crawl status of this cache."""
        key = None
        if not au == None:
            key = au.auId
        (summary, table) = self._getStatusTable('crawl_status_table', key)
        return table

    def getSingleCrawlStatus(self, au, key):
        """Return the detailed crawl status table for the specified AU and key."""
        return self._getStatusTable('single_crawl_status_table', key)

    def getAuHashFile( self, AU ):
        """Return the hash file contents for the whole AU."""
        post = self.__makePost( 'HashCUS', { 'action': 'Hash' } )
        post.add_data( 'auid', AU.auId )
        post.add_data( 'url', 'lockssau:' )
        post.add_data( 'hashtype', 4 ) # (sic)
        response = post.execute()
        match = re.search( '<td>Hash file:</td.*?><td><a href="/(.+?)">HashFile</a.*?>', response.read(), re.DOTALL )
        if match is None:
            raise LockssError( 'Hash file URL not found' )
        return self.__makePost( match.group( 1 ) ).execute().read()

    def getPollResults( self, AU ):
        """Return the current poll results for the AU."""
        table = self._getStatusTable( 'ArchivalUnitTable', AU.auId )[ 0 ]
        if not table:
            raise LockssError( 'AUID "%s" not found' % AU.auId )
        return table.get( 'Last Poll Result' ), table[ 'Status' ]

    def hasAu(self, au):
        """Return true iff the status table lists the given au."""
        auid = au.auId
        (summary, tab) = self._getStatusTable('AuIds')
        for row in tab:
            if row["AuId"] == auid:
                return True
        # au wasn't found
        return False

    def isActiveAu(self, au):
        """Return true iff the au is activated."""
        (summary, tab) = self._getStatusTable('RepositoryTable')
        for row in tab:
            status = row["status"]
            auRef = row["au"]
            # Repository status table may report with two types,
            # a <reference> for 'au', or a string for 'au'.  We want to
            # ignore the string versions.
            if isinstance(auRef, types.DictType):
                if au.auId == auRef['key']:
                    return (status == "Active")
            else:
                continue

        # au wasn't found
        return False

    def isAuDamaged(self, au, node=None):
        (summary, table) = self._getStatusTable('ArchivalUnitTable', au.auId)
        if node:
            url = node.url
        else:
            url = 'lockssau'
        for row in table:
            rowUrl = row['NodeName']
            if not row.has_key('NodeStatus'):
                continue
            if url == rowUrl:
                return row['NodeStatus'] == "Damaged"
        # au wasn't found.
        return False

    def hasCompletedFullAuCrawl(self, au):
        tbl = self.getCrawlStatus(au)
        return tbl and \
            tbl[0]['crawl_status']['value'] == 'Successful' and \
            tbl[0]['num_urls_fetched']['value'] == str(au.expectedUrlCount())

    def getActualCrawledUrlCount(self, au):
        tbl = self.getCrawlStatus(au)
        if tbl[0]['crawl_status']['value'] == 'Successful':
            return int(tbl[0]['num_urls_fetched']['value'])
        else:
            return -1

    def isPublisherDown( self, au ):
        """Return true if the AU is marked 'publisher down' (i.e., if the ArchivalUnitTable lists 'Available From Publisher' as 'No')"""
        ( summary, table ) = self._getStatusTable( 'ArchivalUnitTable', au.auId )
        return summary.get( 'Available From Publisher' ) == "No"


    def isAuOK(self, au):
        """Return true if the top level of the AU has been repaired."""
        tab = self.getAuV1Polls(au)
        for row in tab:
            if (row.has_key('Range') or not row['PollType'] == 'C'):
                continue
            if (row['URL'] == 'lockssau:'):
                return row['Status'] == 'Won'
        # Poll wasn't found.
        return False

    def isContentRepaired(self, au, node):
        """Return true if the AU has been repaired by a SNCUSS poll."""
        tab = self.getAuV1Polls(au)
        for row in tab:
            if not row.has_key('Range'):
                continue
            if not (row['PollType'] == 'C' and row['Range'] == 'single node'):
                continue
            if row['URL'] == node.url:
                return row['Status'] == 'Repaired'
        # Poll wasn't found
        return False

    def startV3Poll( self, AU ):
        """Start a V3 poll of an AU."""
        post = self.__makePost( 'DebugPanel', { 'action': 'Start V3 Poll' } )
        post.add_data( 'auid', AU.auId )
        post.execute()

    def getV3PollKey(self, au, excludePolls=[]):
        """Return the key of a poll on the au, excluding any poll
        keys in excludePolls."""
        tab = self.getAuV3Pollers(au)
        for row in tab:
            pollKey = row['pollId']['key']
            if not pollKey in excludePolls and self.isAuIdOrRef(row['auId'], au):
                return pollKey
        return None

    def getV3PollInvitedPeers(self, pollKey, au):
        (summary, table) = self.getV3PollerDetail(pollKey)
        res = []
        for row in table:
            peer = row['identity']
            res.append(self.valueOfRef(peer))
        return res

    def isCompleteV3Repaired(self, au):
        """Return true if the given AU has had all its nodes repaired by V3.
            Used in testing complete loss recovery via V3."""
        tab = self.getAuV3Pollers(au)
        for row in tab:
            if self.isAuIdOrRef(row['auId'], au) and row['status'] == "Complete":
                # Found the right entry
                pollKey = row['pollId']['key']
                (summary, table) = self.getV3PollerDetail(pollKey)

                try:
                    allUrls = int(summary['Total URLs In Vote'])
                except KeyError, e: # Not available
                    allUrls = 0

                try:
                    agreeUrls = int(summary['Agreeing URLs']['value'])
                except KeyError, e: # Not available
                    agreeUrls = 0

                try:
                    repairs = int(summary['Completed Repairs']['value'])
                except KeyError, e: # Not available
                    repairs = 0
                log.debug("%d urls, %d agree, %d urls" %
                          (allUrls, agreeUrls, repairs))

                return ((repairs == allUrls) and (agreeUrls == allUrls))
        return False

    def hasWonV3Poll(self, au):
        """Return true if a poll has been called and no repairs have been made."""
        return self.isV3Repaired( au )

# XXX look for poll w/ high agreement?
#         tab = self.getAuV3Pollers(au)
#         for row in tab:
#             if self.isAuIdOrRef(row['auId'], au) and row['status'] == "Complete" and row['agreement'] > nnn:
#                 # Found the right entry
#                 pollKey = row['pollId']['key']
#                 (summary, table) = self.getV3PollerDetail(pollKey)

    def isV3Repaired(self, au, nodeList=[]):
        """Return true if the given content node has been repaired via V3."""
        tab = self.getAuV3Pollers(au)
        for row in tab:
            if self.isAuIdOrRef(row['auId'], au) and row['status'] == "Complete":
                # Found the right entry
                pollKey = row['pollId']['key']
                (summary, table) = self.getV3PollerDetail(pollKey)

                try:
                    allUrls = int(summary['Total URLs In Vote'])
                except KeyError: # Not available
                    allUrls = 0

                try:
                    agreeUrls = int(summary['Agreeing URLs']['value'])
                except KeyError: # Not available
                    agreeUrls = 0

                try:
                    repairs = int(summary['Completed Repairs']['value'])
                except KeyError: # Not available
                    repairs = 0

                return (len(nodeList) == repairs and (agreeUrls == allUrls))
                # TODO: This will really need to be improved when the status
                # tables are better!  Need a way to determine whether this particular NODE was
                # repaired.
        return False

    def isV3RepairedExtraFiles(self, au):
        tab = self.getAuV3Pollers(au)
        for row in tab:
            if self.isAuIdOrRef(row['auId'], au) and row['status'] == "Complete":
                # Found the right entry
                pollKey = row['pollId']['key']
                (summary, table) = self.getV3PollerDetail(pollKey)
                allUrls = int(summary['Total URLs In Vote'])
                agreeUrls = int(summary['Agreeing URLs']['value'])
                return (agreeUrls == allUrls)
        return False

    def isNodeRepairedFromPeerByV3(self, au, node):
        """Return true if the given content node has been repaired
        from a V3 peer."""
        tab = self.getAuV3Pollers(au)
        for row in tab:
            if self.isAuIdOrRef(row['auId'], au) and row['status'] == "Complete":
                # Found the right entry.
                pollKey = row['pollId']['key']
                (summary,repairTable) = self.getV3CompletedRepairsTable(pollKey)
                for repairRow in repairTable:
                    if repairRow['url'] == node.url:
                        return re.match('^TCP:\[.*\]:.*', repairRow['repairFrom'])
        # Didn't find it.
        return False

    def isNodeRepairedFromPublisherByV3(self, au, node):
        """Return true if the given content node has been repaired
        from the publisher."""
        tab = self.getAuV3Pollers(au)
        for row in tab:
            if self.isAuIdOrRef(row['auId'], au) and row['status'] == "Complete":
                # Found the right entry.
                pollKey = row['pollId']['key']
                log.debug("Found the right row in the V3 Pollers table.  Key: %s" % pollKey)
                (summary,repairTable) = self.getV3CompletedRepairsTable(pollKey)
                log.debug("Got a repairTable with %d rows." % len(repairTable))
                for repairRow in repairTable:
                    log.debug("repairRow['url'] == %s ; repairRow['repairFrom'] == %s" % (repairRow['url'], repairRow['repairFrom']))
                    if repairRow['url'] == node.url:
                        return repairRow['repairFrom'] == 'Publisher'
        # Didn't find it.
        return False

    def isV3NoQuorum(self, au):
        tab = self.getAuV3Pollers(au)
        for row in tab:
            if self.isAuIdOrRef(row['auId'], au):
                return row['status'] == "No Quorum"
        return False

    def isContentRepairedFromCache(self, au, node=None):
        """Return true if the given content node has been repaired from
        another cache via the proxy."""
        if node:
            url = node.url
        else:
            url = au.startUrl
        tab = self.getCrawlStatus(au)
        # Look for each 'Repair' crawl.
        for row in tab:
            if not row['crawl_type'] == "Repair" and \
                   row['crawl_status']['key'] == "Successful":
                continue
            # This is a successful repair crawl, get the key to look
            # into the detail status table.
            crawlkey = row['crawl_status']['key']
            (summary, repairTab) = self.getSingleCrawlStatus(au, crawlkey)

            return (summary['Starting Url(s)'] == ("[%s]" % node.url) and \
                    not summary['Source'] == 'Publisher')

        #otherwise
        return False

    def isContentRepairedFromPublisher(self, au, node):
        """Return true iff the given content node has been repaired by
        crawling the publisher."""
        tab = self.getCrawlStatus(au)
        # Look for each 'Repair' crawl.
        for row in tab:
            if not row['crawl_type'] == "Repair" and \
                   row['crawl_status']['key'] == "Successful":
                continue
            # This is a successful repair crawl, get the key to look
            # into the detail status table.
            crawlkey = row['crawl_status']['key']
            (summary, repairTab) = self.getSingleCrawlStatus(au, crawlkey)

            return (summary['Starting Url(s)'] == ("[%s]" % node.url) and \
                    summary['Source'] == 'Publisher')

        #otherwise
        return False

    def isNameRepaired(self, au, node=None):
        """Return true if the AU has been repaired by non-ranged name poll."""
        tab = self.getAuV1Polls(au)
        for row in tab:
            if row.has_key('Range') or not row['PollType'] == 'N':
                continue
            rowUrl = row['URL']
            rowStatus = row['Status']
            if not node:
                if rowUrl == au.baseUrl:
                    return rowStatus == 'Repaired'
            else:
                if rowUrl == node.url:
                    return rowStatus == 'Repaired'

        # Poll wasn't found
        return False

    def isRangedNameRepaired(self, au, node=None):
        """Return true if the AU has been repaired by a ranged name poll."""
        tab = self.getAuV1Polls(au)
        for row in tab:
            rowPollType = row['PollType']
            # some rows do not have a range.
            if row.has_key('Range') or not rowPollType == 'N':
                continue
            rowUrl = row['URL']
            rowStatus = row['Status']
            if not node:
                if rowUrl == au.baseUrl:
                    return rowStatus == 'Repaired'
            else:
                if rowUrl == node.url:
                    return rowStatus == 'Repaired'

        # Poll wasn't found
        return False

    def getAuV1Polls(self, au, activeOnly=False):
        """Return the full poll status of an AU."""
        key = 'AU~%s' % urllib.quote(au.auId) # requires a pre-quoted key
        if activeOnly:
            key = key + '&Status~Active'
        (summary, table) = self._getStatusTable('PollManagerTable', key)
        return table

    def getAuV3Pollers(self, au):
# Mea culpa:  I broke accessing V3 Pollers by key when I was enabling
# status references from the AU table to the V3 Poller table.  It should
# be OK just to comment this out permanently. [sethm]
#        key = 'AU~%s' % urllib.quote(au.auId) # requires a pre-quoted key
#        (summary, table) = self._getStatusTable('V3PollerTable', key)
        (summary, table) = self._getStatusTable('V3PollerTable')
        return table

    def getAuV3Voters(self, au):
        key = 'AU~%s' % urllib.quote(au.auId) # requires a pre-quoted key
        (summary, table) = self._getStatusTable('V3VoterTable', key)
        return table

    def getV3PollerDetail(self, key):
        """Returns both the summary and table."""
        return self._getStatusTable('V3PollerDetailTable', key)

    def getV3CompletedRepairsTable(self, key):
        """Returns the V3 completed repairs status table."""
        return self._getStatusTable('V3CompletedRepairsTable', key)

    def getAuNodesWithContent(self, au):
        """Return a list of all nodes that have content."""
        # Hack!  Pass the table a very large number for numrows, and
        # pray we don't have more than that.
        (summary, tab) = self._getStatusTable('ArchivalUnitTable', key=au.auId, numrows=100000)
        nodes = []
        for row in tab:
            url = row['NodeName']
            if row.has_key('NodeContentSize') \
                   and not row['NodeContentSize'] == '-':
                nodes.append(self.getAuNode(au, url))
        return nodes

    def getAuNodesWithChildren(self, au):
        """Return a list of all nodes that have children."""
        (summary, tab) = self._getStatusTable('ArchivalUnitTable', key=au.auId, numrows=100000)
        nodes = []
        for row in tab:
            url = row['NodeName']
            if row.has_key('NodeChildCount') \
                   and not row['NodeChildCount'] == '-':
                nodes.append(self.getAuNode(au, url))
        return nodes

    def getAuRepairerInfo(self, au):
        """ Return a dict: peer -> dict w/ highestAgree and highestHint. """
        (summary, table) = self._getStatusTable('PeerRepair', au.auId)
        peerDict = {}
        p = re.compile('(\d+)%')
        for row in table:
            peer = row['Box']
            if row.has_key('HighestPercentAgreement'):
                highestAgree = int(p.match(row['HighestPercentAgreement']).group(1))
            else:
                highestAgree = 0
            if row.has_key('HighestPercentAgreementHint'):
                highestHint = int(p.match(row['HighestPercentAgreementHint']).group(1))
            else:
                highestHint = 0

            peerDict[peer] = {'highestAgree': highestAgree,
                              'highestHint': highestHint}
        return peerDict

    def getNoAuPeers(self, au):
        """Return list of peers who have said they don't have the AU."""
        (summary, table) = self._getStatusTable('NoAuPeers', au.auId)
        peers = []
        for row in table:
            peer = row['Peer']
            peers.append(peer)
        return peers

    def isDaemonReady( self ):
        """Ask DaemonStatus whether the server is fully started.
           When given arg isDaemonReady it returns either True or False."""
        post = self.__makePost( 'DaemonStatus' )
        post.add_data( 'isDaemonReady', '1' )
        try:
            return post.execute().read() == 'true\n'
        except urllib2.URLError:
            return False
        except LockssError:
            # If a Lockss error is raised, pass it on.
            raise
        #except Exception, exception:
        #    # On any other error, just return false.
        #    log.debug("Got exception: %s" % exception)
        #    return False

    def isAuIdOrRef(self, auRef, au):
        if isinstance(auRef, types.DictType):
            if auRef['key'] == au.auId:
                return True
        if auRef == au.title:
            return True
        return False

    def valueOfRef(self, possibleref):
        if isinstance(possibleref, types.DictType):
            return possibleref['value']
        else:
            return possibleref

    def getAdminUi(self):
        """Fetch the contents of the top-level admin UI.  Useful for testing
        the Tiny UI.  May throw urllib2.URLError or urllib2.HTTPError."""
        return HTTP_Request( self.URL_opener, self.base_url ).execute()

    def hasV3Poller(self, au, excludePolls=[]):
        """Return true if the client has an active or completed V3 Poller
           not matching one of the keys in excludePolls."""
        return self.getV3PollKey(au, excludePolls) is not None

    def hasV3Voter(self, au):
        """Return true if the client has an active V3 Poller."""
        tab = self.getAuV3Voters(au)
        for row in tab:
            if self.isAuIdOrRef(row["auId"], au):
                return True
        # Poll wasn't found.
        return False

    def hasTopLevelContentPoll(self, au):
        """Return true if the client has an active or won top level
        content poll."""
        tab = self.getAuV1Polls(au)
        for row in tab:
            if not row['PollType'] == 'C':
                continue
            if row['URL'] == 'lockssau:':
                ## return (row['Status'] == 'Active' or row['Status'] == 'Won')
                return row['Status'] == 'Active'
        # Poll wasn't found
        return False

    def hasTopLevelNamePoll(self, au):
        """Wait for a top level name poll to be called."""
        tab = self.getAuV1Polls(au)
        for row in tab:
            if not row['PollType'] == 'N':
                continue
            if row['URL'] == 'lockssau:':
                ## return (row['Status'] == 'Active' or row['Status'] == 'Won')
                return row['Status'] == 'Active'
        # Poll wasn't found
        return False

    def hasSNCUSSPoll(self, au, node):
        """Wait for a Single-Node CUSS poll for the given AU and node."""
        tab = self.getAuV1Polls(au)
        for row in tab:
            if not (row['PollType'] == 'C' and row.has_key('Range')):
                continue
            if row['URL'] == node.url:
                return row['Range'] == 'single node' and \
                       (row['Status'] == 'Active' or row['Status'] == 'Won')
        # Poll wasn't found
        return False

    def hasLostTopLevelContentPoll(self, au):
        """Return true if a top level content poll has been lost."""
        tab = self.getAuV1Polls(au)
        for row in tab:
            if not row['PollType'] == 'C':
                continue
            if row['URL'] == 'lockssau:':
                return row['Status'] == 'Lost'
        # Poll wasn't found
        return False

    def hasWonTopLevelContentPoll(self, au):
        """Return true if a top level content poll has been won."""
        tab = self.getAuV1Polls(au)
        for row in tab:
            if not row['PollType'] == 'C':
                continue
            if row['URL'] == 'lockssau:':
                return row['Status'] == 'Won'
        # Poll wasn't found
        return False

    def hasLostTopLevelNamePoll(self, au):
        """Wait for a top level name poll to be lost."""
        tab = self.getAuV1Polls(au)
        for row in tab:
            if not row['PollType'] == 'N':
                continue
            if row['URL'] == 'lockssau:':
                return row['Status'] == 'Lost'
        # Poll wasn't found
        return False

    def hasWonTopLevelNamePoll(self, au):
        """Wait for a top level name poll to be won."""
        tab = self.getAuV1Polls(au)
        for row in tab:
            if not row['PollType'] == 'N':
                continue
            if row['URL'] == 'lockssau:':
                return row['Status'] == 'Won'
        # Poll wasn't found
        return False

    def hasNamePoll(self, au, node):
        """Wait for a name poll to run on a given node (won or active)."""
        tab = self.getAuV1Polls(au)
        for row in tab:
            if not row['PollType'] == 'N':
                continue
            if row['URL'] == node.url:
                return (row['Status'] == 'Active' or row['Status'] == 'Won')
        # Poll wasn't found
        return False


    def hasLostNamePoll(self, au, node):
        """Wait for a name poll to be won on the given node."""
        if not node:
            url = au.baseUrl
        else:
            url = node.url
        tab = self.getAuV1Polls(au)
        for row in tab:
            if row.has_key('Range') or not row['PollType'] == 'N':
                continue
            if row['URL'] == url:
                return row['Status'] == 'Lost'
        # Poll wasn't found
        return False

    def hasWonNamePoll(self, au, node):
        """Wait for a name poll to be won on the given node."""
        if not node:
            url = au.baseUrl
        else:
            url = node.url
        tab = self.getAuV1Polls(au)
        for row in tab:
            if row.has_key('Range') or not row['PollType'] == 'N':
                continue
            if row['URL'] == url:
                return row['Status'] == 'Won'
        # Poll wasn't found
        return False

    def hasRangedNamePoll(self, au, node):
        """Wait for a ranged name poll on the given node."""
        if not node:
            url = au.baseUrl
        else:
            url = node.url
        tab = self.getAuV1Polls(au)
        for row in tab:
            if not (row['PollType'] == 'N' and row.has_key('Range')):
                continue
            if row['URL'] == url:
                return row['Status'] == 'Won' or row['Status'] == 'Active'
        # Poll wasn't found
        return False

    def hasLostRangedNamePoll(self, au, node):
        """Wait for a ranged name poll to be lost on the given node."""
        if not node:
            url = au.baseUrl
        else:
            url = node.url
        tab = self.getAuV1Polls(au)
        for row in tab:
            if not (row['PollType'] == 'N' and row.has_key('Range')):
                continue
            if row['URL'] == url:
                return row['Status'] == 'Lost'
        # Poll wasn't found
        return False

    def hasWonRangedNamePoll(self, au, node):
        """Wait for a ranged name poll to be won on the given node."""
        if not node:
            url = au.baseUrl
        else:
            url = node.url
        tab = self.getAuV1Polls(au)
        for row in tab:
            if not (row['PollType'] == 'N' and row.has_key('Range')):
                continue
            if row['URL'] == url:
                return row['Status'] == 'Won'
        # Poll wasn't found
        return False

    def hasLostSNCUSSPoll(self, au, node):
        """Wait for a Single-Node CUSS poll for the given AU and node
        to be lost."""
        tab = self.getAuV1Polls(au)
        for row in tab:
            if not (row['PollType'] == 'C' and row.has_key('Range')):
                continue
            if row['URL'] == node.url:
                return row['Range'] == 'single node' and \
                       row['Status'] == 'Lost'
        # Poll wasn't found
        return False

    def hasWonSNCUSSContentPoll(self, au, node):
        """Wait for a Single-Node CUSS poll for the given AU and node
        to be won."""
        tab = self.getAuV1Polls(au)
        for row in tab:
            if not (row['PollType'] == 'C' and row.has_key('Range')):
                continue
            if row['URL'] == node.url:
                return row['Range'] == 'single node' and \
                       row['Status'] == 'Won'
        # Poll wasn't found
        return False

    ###
    ### Methods that block while waiting for various events
    ###

    def waitForCanConnectToHost(self, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        def waitFunc():
            return self.__canConnectToHost()
        return self.wait(waitFunc, timeout, sleep)

    def waitForCreateAu(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        """Wait for the au to appear, or for the timeout to expire."""
        def waitFunc():
            return self.hasAu(au)
        return self.wait(waitFunc, timeout, sleep)

    def waitForDeleteAu(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        """Wait for the au to be deleted, or for the timeout to
        expire."""
        def waitFunc():
            return not self.hasAu(au)
        return self.wait(waitFunc, timeout, sleep)

    def waitForPublisherDown(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        """Wait for the specified AU to be marked 'publisher down'."""
        def waitFunc():
            return self.isPublisherDown(au)
        return self.wait(waitFunc, timeout, sleep)

    def waitForReactivateAu(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        """Wait for the au to be activated, or for the timeout to expire."""
        def waitFunc():
            return self.isActiveAu(au)
        return self.wait(waitFunc, timeout, sleep)

    def waitForDeactivateAu(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        """Wait for the au to be deactivated, or for the timeout to expire."""
        def waitFunc():
            return not self.isActiveAu(au)
        return self.wait(waitFunc, timeout, sleep)

    def waitForSuccessfulCrawl( self, AU, timeout = DEF_TIMEOUT, sleep = DEF_SLEEP ):
        """Wait until the AU has been crawled successfully."""
        def waitFunc():
            return self._getStatusTable( 'ArchivalUnitTable', AU.auId )[ 0 ].get( 'Last Crawl Result' ) == 'Successful'
        return self.wait( waitFunc, timeout, sleep )

    def waitForSuccessfulNewCrawl(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        """Block until the specified au has had at least one successful new content crawl."""
        def waitFunc():
            tbl = self.getCrawlStatus(au)
            try:
                status = tbl[0]['crawl_status']['value']
            except ( TypeError, IndexError ):
                log.debug( 'AU not found; continuing to wait' )
                return False
            if status == 'Successful':
                numCrawled = int(self.valueOfRef(tbl[0]['num_urls_fetched']))
                if hasattr( au, 'expectedUrlCount' ):
                    numExpected = au.expectedUrlCount()
                else:
                    numExpected = None
                if numExpected is None or numCrawled == numExpected:
                    return True
                else:
                    raise LockssError("Crawl on client " +
                                      str(self.port) +
                                      " should have collected " +
                                      str(numExpected) +
                                      " urls, but only collected " +
                                      str(numCrawled))
            elif (status == 'No permission from publisher'):
                raise LockssError("Crawl on client " +
                                  str(self.port) +
                                  " did not receive permission!")
            else:
                return False

        return self.wait(waitFunc, timeout, sleep)

    def waitForPollResults( self, AU, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP ):
        """Wait until poll results are available for an AU."""
        def waitFunc():
            return self.getPollResults( AU )[ 0 ]
        return self.wait( waitFunc, timeout, sleep )

    def waitForV3Poller(self, au, excludePolls=[], timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        def waitFunc():
            return self.hasV3Poller(au, excludePolls)
        return self.wait(waitFunc, timeout, sleep)

    def waitForWonV3Poll(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        def waitFunc():
            return self.hasWonV3Poll(au)
        return self.wait(waitFunc, timeout, sleep)

    def waitForCompleteV3Repair(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        def waitFunc():
            return self.isCompleteV3Repaired(au)
        return self.wait(waitFunc, timeout, sleep)

    def waitForV3Voter(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        def waitFunc():
            return self.hasV3Voter(au)
        return self.wait(waitFunc, timeout, sleep)

    def waitForTopLevelContentPoll(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        def waitFunc():
            return self.hasTopLevelContentPoll(au)
        return self.wait(waitFunc, timeout, sleep)

    def waitForTopLevelNamePoll(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        def waitFunc():
            return self.hasTopLevelNamePoll(au)
        return self.wait(waitFunc, timeout, sleep)

    def waitForSNCUSSContentPoll(self, au, node, timeout=DEF_TIMEOUT,
                                 sleep=DEF_SLEEP):
        def waitFunc():
            return self.hasSNCUSSPoll(au, node)
        return self.wait(waitFunc, timeout, sleep)

    def waitForLostTopLevelContentPoll(self, au, timeout=DEF_TIMEOUT,
                                       sleep=DEF_SLEEP):
        """Block until a top level content poll is lost."""
        def waitFunc():
            return self.hasLostTopLevelContentPoll(au)
        return self.wait(waitFunc, timeout, sleep)

    def waitForLostTopLevelNamePoll(self, au, timeout=DEF_TIMEOUT,
                                    sleep=DEF_SLEEP):
        """Block until a top level name poll is lost."""
        def waitFunc():
            return self.hasLostTopLevelNamePoll(au)
        return self.wait(waitFunc, timeout, sleep)

    def waitForLostNamePoll(self, au, node, timeout=DEF_TIMEOUT,
                                    sleep=DEF_SLEEP):
        """Block until a node name poll is lost."""
        def waitFunc():
            return self.hasLostNamePoll(au, node)
        return self.wait(waitFunc, timeout, sleep)


    def waitForLostSNCUSSContentPoll(self, au, node, timeout=DEF_TIMEOUT,
                                     sleep=DEF_SLEEP):
        """Block until a top level name poll is lost."""
        def waitFunc():
            return self.hasLostSNCUSSPoll(au, node)
        return self.wait(waitFunc, timeout, sleep)

    def waitForWonTopLevelContentPoll(self, au, timeout=DEF_TIMEOUT,
                                       sleep=DEF_SLEEP):
        """Block until a top level content poll is won."""
        def waitFunc():
            return self.hasWonTopLevelContentPoll(au)
        return self.wait(waitFunc, timeout, sleep)

    def waitForWonTopLevelNamePoll(self, au, timeout=DEF_TIMEOUT,
                                    sleep=DEF_SLEEP):
        """Block until a top level name poll is won."""
        def waitFunc():
            return self.hasWonTopLevelNamePoll(au)
        return self.wait(waitFunc, timeout, sleep)

    def waitForNamePoll(self, au, node, timeout=DEF_TIMEOUT,
                        sleep=DEF_SLEEP):
        """Block until a node level name poll is run (active or won)."""
        def waitFunc():
            return self.hasNamePoll(au, node)
        return self.wait(waitFunc, timeout, sleep)

    def waitForWonNamePoll(self, au, node, timeout=DEF_TIMEOUT,
                           sleep=DEF_SLEEP):
        """Block until a node level name poll is won."""
        def waitFunc():
            return self.hasWonNamePoll(au, node)
        return self.wait(waitFunc, timeout, sleep)

    def waitForWonSNCUSSContentPoll(self, au, node, timeout=DEF_TIMEOUT,
                                     sleep=DEF_SLEEP):
        """Block until a SNCUSS content poll is won."""
        def waitFunc():
            return self.hasWonSNCUSSContentPoll(au,  node)
        return self.wait(waitFunc, timeout, sleep)

    def waitForRangedNamePoll(self, au, node, timeout=DEF_TIMEOUT,
                              sleep=DEF_SLEEP):
        """Block until a ranged name poll has occured (active or won)"""
        def waitFunc():
            return self.hasRangedNamePoll(au, node)
        return self.wait(waitFunc, timeout, sleep)

    def waitForWonRangedNamePoll(self, au, node, timeout=DEF_TIMEOUT,
                                 sleep=DEF_SLEEP):
        def waitFunc():
            return self.hasWonRangedNamePoll(au, node)
        return self.wait(waitFunc, timeout, sleep)

    def waitForLostRangedNamePoll(self, au, node, timeout=DEF_TIMEOUT,
                                  sleep=DEF_SLEEP):
        def waitFunc():
            return self.hasLostRangedNamePoll(au, node)
        return self.wait(waitFunc, timeout, sleep)

    def waitForTopLevelDamage(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        """Block until an au is marked 'Damaged' at the top level
        (lockssau:)."""
        def waitFunc():
            return self.isAuDamaged(au, None)
        return self.wait(waitFunc, timeout, sleep)

    def waitForDamage(self, au, node, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        """Block until a node is marked 'repairing', or until the
        timeout expires."""
        def waitFunc():
            return self.isAuDamaged(au, node)
        return self.wait(waitFunc, timeout, sleep)

    def waitForTopLevelRepair(self, au, timeout=DEF_TIMEOUT,
                              sleep=DEF_SLEEP):
        """Block until the top-level of the AU is marked repaired."""
        def waitFunc():
            return self.isAuOK(au)
        return self.wait(waitFunc, timeout, sleep)

    def waitForContentRepair(self, au, node, timeout=DEF_TIMEOUT,
                             sleep=DEF_SLEEP):
        """Block until a node has been successfully repaired, or until
        the timeout expires."""
        def waitFunc():
            return self.isContentRepaired(au, node)
        return self.wait(waitFunc, timeout, sleep)

    def waitForV3Repair(self, au, nodeList=[], timeout=DEF_TIMEOUT,
                        sleep=DEF_SLEEP):
        """Wait for a successful repair of the specified node by a V3 Poll."""
        def waitFunc():
            return self.isV3Repaired(au, nodeList)
        return self.wait(waitFunc, timeout, sleep)

    def waitForV3RepairExtraFiles(self, au, timeout=DEF_TIMEOUT,
                                  sleep=DEF_SLEEP):
        def waitFunc():
            return self.isV3RepairedExtraFiles(au)
        return self.wait(waitFunc, timeout, sleep)

    def waitForV3NoQuorum(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        """Wait for a V3 poll to be marked No Quorum."""
        def waitFunc():
            return self.isV3NoQuorum(au)
        return self.wait(waitFunc, timeout, sleep)

    def waitForNameRepair(self, au, node=None, timeout=DEF_TIMEOUT,
                          sleep=DEF_SLEEP):
        """Block until a node has been successfully repaired, or until
        the timeout expires.  If 'node' is None, this will just wait until
        the base URL has been marked repaired."""
        def waitFunc():
            return self.isNameRepaired(au, node)
        return self.wait(waitFunc, timeout, sleep)

    def waitForRangedNameRepair(self, au, node=None, timeout=DEF_TIMEOUT,
                                sleep=DEF_SLEEP):
        def waitFunc():
            return self.isRangedNameRepaired(au, node)
        return self.wait(waitFunc, timeout, sleep)

    def waitForDaemonReady(self, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        """Block until the framework is ready for client communication."""
        def waitFunc():
            return self.isDaemonReady()
        return self.wait(waitFunc, timeout, sleep)

    def wait(self, condFunc, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        """ Given a function to evaluate, loop until the function evals to
        true, or until the timeout has expired. """
        start = time.time()
        while True:
            if condFunc():
                return True
            if time.time() - start >= timeout:
                return False
            time.sleep( sleep )

    ###
    ### Methods for causing damage
    ###

    def randomDamageSingleNode(self, au):
        """Randomly select a node with content, and cause damage in it."""
        node = self.__getRandomContentNode(au)
        self.damageNode(node)
        return node

    def randomDamageRandomNodes(self, au, minCount=1, maxCount=5):
        """Damage a random selection of between minCount and maxCount
        nodes with content for the given au.  Returns the list of
        damaged nodes."""
        nodeList = self.__getRandomContentNodeList(au, minCount, maxCount)
        for node in nodeList:
            self.damageNode(node)
        nodeList.sort()
        return nodeList

    def randomDelete(self, au):
        """Randomly select a node with content, and delete it."""
        node = self.__getRandomContentNode(au)
        self.deleteNode(node)
        return node

    def randomDeleteRandomNodes(self, au, minCount=1, maxCount=5):
        """Delete a random selection of between minCount and maxCount
        nodes with content for the given au.  Returns the list of
        damaged nodes as a tuple."""
        nodeList = self.__getRandomContentNodeList(au, minCount, maxCount)
        for node in nodeList:
            self.deleteNode(node)
        nodeList.sort()
        return nodeList

    def nodeContentFile(self, node):
        """ Return the file with the content of a node. """
        return path.join(node.file, '#content', 'current')

    def readNode(self, node):
        """ Return the content of a specific node. """
        fullPath = path.join(node.file, '#content', 'current')
        if path.isfile(fullPath):
            f = open(fullPath, 'a')
            try:
                content = f.read( )
            finally:
                f.close( )
            return content
        else:
            raise LockssError("File does not exist: %s" % f)

    def damageNode(self, node):
        """Damage a specific node."""
        # Only want to damage the file contents
        fullPath = path.join(node.file, '#content', 'current')
        if path.isfile(fullPath):
            log.debug2("Damaging: " + fullPath)
            f = open(fullPath, 'a')
            f.write('*** DAMAGE ***')
            f.close()
        else:
            raise LockssError("File does not exist: %s" % f)

    def deleteNode(self, node):
        """Delete a specific node."""
        f = node.file
        if path.isfile(f):
            os.unlink(f)
        elif path.isdir(f):
            shutil.rmtree(f)
        else:
            raise LockssError("File does not exist: %s" % f)

    def createFile(self, au, filespec):
        """Create an extra file in the repository under the given AU and
        filespec.  The filespec should be relative to the AU's root, for example
        'foo/bar' would attempt to create the file
        /path/to/au/repository/www.example.com/http/foo/bar.  Returns
        the new file."""
        root = self.getAuRoot(au)
        output = path.join(root, filespec)
        if path.isfile(output):
            raise LockssError("File already exists: %s" % filespec)
        f = open(output, 'w')
        f.write('Garbage File')
        f.close()
        return output

    # NOTE!  These two methods are extremely dependent on the
    # current implementation of the repository.  If that changes,
    # thesee must change.
    def createNode(self, au, filespec, node=None):
        """Given a branch node, create a new child under it."""

        if not node:
            root = self.getAuRoot(au)
        else:
            root = node.file

        now = time.strftime("%s000") # No ms precision
        nowComment = time.strftime("#%a %b %d %H:%M:%S %Z %Y")

        auRoot = self.getAuRoot(au)
        nodeRoot = path.join(root, filespec)
        url = au.baseUrl + nodeRoot[len(auRoot):]

        os.mkdir(nodeRoot)
        os.mkdir(path.join(nodeRoot, '#content'))

        contentFile = path.join(nodeRoot, '#content', 'current')
        f = open(contentFile, 'w')
        f.write('Garbage File')
        f.close()

        propsFile = path.join(nodeRoot, '#content', 'current.props')
        f = open(propsFile, 'w')
        f.write("#HTTP headers for %s\n" % url)
        f.write("%s\n" % nowComment)
        f.write("last-modified=0\n")
        f.write("org.lockss.version.number=1\n")
        f.write("x-lockss-content-type=text/plain\n")
        f.write("x-lockss-node-url=%s\n" % url)
        f.write("x-lockss-orig-url=%s\n" % url)
        f.write("x_lockss-server-date=%s\n" % now)
        f.close()

        nodeProps = path.join(nodeRoot, '#node_props')
        f = open(nodeProps, 'w')
        f.write("#Node properties\n")
        f.write("%s\n" % nowComment)
        f.write("node.child.count=0\n")
        f.write("node.tree.size=12\n")
        f.close()

        return Node(url, nodeRoot)

    def randomCreateRandomNodes(self, au, minCount=1, maxCount=5):
        """Create a random number of between minCount and maxCount
        nodes on the given au.  Return the list of new nodes."""
        nodeList = []
        numNodes = random.randint(minCount, maxCount)
        for nodeNum in range(minCount, maxCount+1):
            newNode = self.createNode(au, '%sextrafile.txt' % nodeNum)
            nodeList.append(newNode)
        nodeList.sort()
        return nodeList

    def getAuNode(self, au, url, checkForContent=False):
        """Construct a node from a url on an AU."""
        root = self.getAuRoot(au)
        # Kludge for getting "lockssau" node.
        if url == 'lockssau':
            f = root
        else:
            f = path.join(root, url[(len(au.baseUrl) + 1):])
        node = Node(url, f)
        if checkForContent:
            if path.isfile(path.join(f, '#content', 'current')):
                return node
            else:
                raise LockssError("Node has no content: %s" % node.url)
        else:
            if path.isdir(f) or path.isfile(f):
                return node
            else:
                raise LockssError("Node does not exist: %s" % node.url)

    ###
    ### Internal methods
    ###

    def _getStatusTable( self, statusTable, key = None, numrows = None ):
        """Given an XML string, parse it as a status table and return
        a list of dictionaries representing the data.  Each item in
        the list is a dictionary of column names to values, stored as
        Unicode strings."""
        post = self.__makePost('DaemonStatus')
        post.add_data( 'table', statusTable )
        if not key == None:
            post.add_data( 'key', key )
        if not numrows == None:
            post.add_data( 'numrows', numrows )
        post.add_data( 'output', 'xml' )

        XML = post.execute().read()
        log.debug3( 'Received XML response:\n' + XML )
        doc = xml.dom.minidom.parseString( XML )
        doc.normalize()

        summaryList = doc.getElementsByTagName('st:summaryinfo')
        summaryDict = {}
        for summary in summaryList:
            summaryTitle = None
            summaryValue = None
            if summary.getElementsByTagName('st:title')[0] and \
               summary.getElementsByTagName('st:title')[0].firstChild:
                summaryTitle = summary.getElementsByTagName('st:title')[0].firstChild.data
            if summary.getElementsByTagName('st:value')[0]:
                summaryValue = summary.getElementsByTagName('st:value')[0]
            # See if this is a reference, or CDATA
            refList = summary.getElementsByTagName('st:reference')
            if summaryTitle and refList:
                ref = refList[0] # should be only one!
                name = ref.getElementsByTagName('st:name')[0].firstChild.data
                key = ref.getElementsByTagName('st:key')[0].firstChild.data
                value = ref.getElementsByTagName('st:value')[0].firstChild.data
                summaryDict[summaryTitle] = {'name': name,
                                             'key': key,
                                             'value': value}
            elif summaryTitle and summaryValue and summaryValue.firstChild is not None:
                summaryDict[summaryTitle] = summaryValue.firstChild.data

        rowList = doc.getElementsByTagName('st:row')
        data = []
        for row in rowList:
            rowDict = {}
            for cell in row.getElementsByTagName('st:cell'):
                try:
                    colName = cell.getElementsByTagName('st:columnname')[0].firstChild.data
                    colVal = cell.getElementsByTagName('st:value')[0]

                    # See if this is a reference, or CDATA
                    refList = colVal.getElementsByTagName('st:reference')
                    if refList:
                        ref = refList[0] # should only be one!
                        name = ref.getElementsByTagName('st:name')[0].firstChild.data
                        key = ref.getElementsByTagName('st:key')[0].firstChild.data
                        value = ref.getElementsByTagName('st:value')[0].firstChild.data
                        rowDict[colName] = {'name': name,
                                            'key': key,
                                            'value': value}
                    else:
                        rowDict[colName] = colVal.firstChild.data
                except (IndexError, AttributeError):
                    # Unlikely to happen, but just in case...
                    continue
            data.append(rowDict)
        return (summaryDict, data)

    def __canConnectToHost(self):
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            s.connect((self.hostname, self.port))
            s.close()
            return True
        except Exception, e:
            log.debug2("Connect error: %s" % (e))
            return False

    def __makePost( self, page, form_data = None ):
        post = HTTP_Request( self.URL_opener, self.base_url + page )
        if form_data:
            for key, value in form_data.iteritems():
                post.add_data( key, value )
        return post

    def __makeAuPost(self, au, lockssAction):
        post = self.__makePost('AuConfig', {'lockssAction': lockssAction})
        post.add_data( 'PluginId', au.pluginId )
        post.add_data( 'auid', au.auId )
        simulated_AU = hasattr( au, 'title' ) and au.title.startswith( 'Simulated Content: ' )
        if simulated_AU:
        #    excluded_attributes.update( ( 'auId', 'baseUrl', 'dirStruct', 'fileTypeArray', 'pluginId', 'startUrl', 'title' ) )
            post.add_data( 'lfp.root', au.root )
            if au.depth != -1:
                post.add_data( 'lfp.depth', au.depth )
            if au.branch != -1:
                post.add_data( 'lfp.branch', au.branch )
            if au.numFiles != -1:
                post.add_data( 'lfp.numFiles', au.numFiles )
            if au.binFileSize != -1:
                post.add_data( 'lfp.binFileSize', au.binFileSize )
            if au.binRandomSeed != -1:
                post.add_data( 'lfp.binRandomSeed', au.binRandomSeed )
            if au.maxFileName != -1:
                post.add_data( 'lfp.maxFileName', au.maxFileName )
            if au.fileTypes != -1:
                post.add_data( 'lfp.fileTypes', au.fileTypes )
            if au.oddBranchContent != -1:
                post.add_data( 'lfp.odd_branch_content', au.oddBranchContent )
            if au.badFileLoc is not None:
                post.add_data( 'lfp.badFileLoc', au.badFileLoc )
            if au.badFileNum != -1:
                post.add_data( 'lfp.badFileNum', au.badFileNum )
            if au.publisherDown:
                post.add_data( 'lfp.pub_down', 'true' )
            if au.protocolVersion > 0:
                post.add_data( 'lfp.protocol_version', au.protocolVersion )
        else:
            excluded_attributes = set( ( 'auId', 'pluginId' ) )
            for key in set( vars( au ) ) - excluded_attributes:
                value = getattr( au, key )
                #if not simulated_AU or value is not None and value is not False and Value != -1:
                post.add_data( 'lfp.' + key, value )
        return post

    def __getRandomContentNode(self, au):
        """Raise an error if the AU hasn't been created or crawled."""
        repository = self.getAuRepository(au)
        if repository == None:
            raise LockssError("No repository for au: %s" % au)

        # Randomly select a node to damage
        nodeList = self.getAuNodesWithContent(au)
        randomNode = nodeList[random.randint(0, len(nodeList) - 1)]

        return randomNode

    def __getRandomContentNodeList(self, au, minCount, maxCount):
        """Return a randomly sized list of nodes with content,
        between min and max in length. If there are fewer than 'max'
        nodes, this will return a list between 'min' and
        'len(allnodes)'."""
        nodes = self.getAuNodesWithContent(au)
        maxCount = min(maxCount, len(nodes))

        returnListLength = random.randint(minCount, maxCount)

        # Build a list of nodes.
        ix = 0
        returnList = []
        while ix < returnListLength:
            node = nodes[random.randint(0, len(nodes) - 1)]
            if node in returnList:
                continue
            returnList.append(node)
            ix += 1

        return returnList

    def __getRandomBranchNodeList(self, au, minCount, maxCount):
        """Return a randomly sized list of nodes with children,
        between min and max in length. If there are fewer than 'max'
        nodes, this will return a list between 'min' and
        'len(allnodes)'."""
        nodes = self.getAuNodesWithChildren(au)
        maxCount = min(maxCount, len(nodes))
        if (maxCount == 0):
            log.warn("getAuNodesWithChildren returned no nodes!")
            return []

        returnListLength = random.randint(minCount, maxCount)

        # Build a list of nodes.
        ix = 0
        returnList = []
        while ix < returnListLength:
            node = nodes[random.randint(0, len(nodes) - 1)]
            if node in returnList:
                continue
            returnList.append(node)
            ix += 1

        return returnList


    def __str__(self):
        return "%s" % self.base_url


class Daemon_Client( Client ):
    """Test framework daemon client interface"""

    def __init__( self, daemon, hostname, port, v3Port, username, password ):
        Client.__init__( self, hostname, port, v3Port, username, password )
        self.daemon = daemon
        self.daemonDir = daemon.daemonDir

    def getAuRepository(self, au):
        """RepositoryStatus table does not accept key, so loop through it until
        the corresponding au is found."""
        auid = au.auId
        (summary, table) = self._getStatusTable('RepositoryTable')
        for row in table:
            auRef = row['au']
            if auRef['key'] == auid:
                d = row['dir']
                repo = path.abspath(path.join(self.daemonDir, d))
                return repo
        ## Wasn't found.
        return None

    def getAuRoot(self, au):
        """Return the full path to the AU's root.  This is used by methods
        that damage, create, or delete repository files behind the server's
        back."""
        repository = self.getAuRepository(au)
        return path.join(repository, au.dirStruct)

    def simulateDiskFailure(self):
        """Delete the entire contents of this client's cache.  Obviously,
        very destructive."""
        cacheDir = path.join(self.daemonDir, "cache")
        configDir = path.join(self.daemonDir, "config")
        pluginsDir = path.join(self.daemonDir, "plugins")
        if path.isdir(cacheDir) and path.isdir(configDir)\
               and path.isdir(pluginsDir):
            shutil.rmtree(cacheDir)
            shutil.rmtree(configDir)
            shutil.rmtree(pluginsDir)
        else:
            raise LockssError("Cache not in an expected state!")


class AU:
    """General-purpose Archival Unit"""

    def __init__( self, auId ):
        self.auId = auId.strip()
        try:
            self.pluginId, auKey = self.auId.split( '&', 1 )
            if '|' not in self.pluginId or '.' in self.pluginId:
                raise ValueError
        except ValueError:
            raise LockssError( 'Invalid AU ID "%s"' % self.auId )
        self.pluginId = self.pluginId.replace( '|', '.' )
        self.title = urllib.unquote_plus( auKey )
        for property in auKey.split( '&' ):
            key, value = ( urllib.unquote_plus( encoded ) for encoded in property.split( '~' ) )
            assert not hasattr( self, key )
            setattr( self, urllib.unquote_plus( key ), urllib.unquote_plus( value ) )

    def __str__(self):
        return self.title

# Redundant?
#   def getAuId(self):
#       auIdKey = self.pluginId.replace('.', '|')
#       return "%s&root~%s" % (auIdKey, self.root)


class SimulatedAU( AU ):
    """A Python abstraction of a SimulatedPlugin Archival Unit for use
    in the functional test framework."""
    def __init__(self, root, depth=-1, branch=-1, numFiles=-1,
                 binFileSize=-1, binRandomSeed=long(time.time()),
                 maxFileName=-1, fileTypes=[],
                 oddBranchContent=-1, badFileLoc=None, badFileNum=-1,
                 publisherDown=False, protocolVersion=-1 ):
        self.root = root
        self.depth = depth
        self.branch = branch
        self.numFiles = numFiles
        self.binFileSize = binFileSize
        self.binRandomSeed = binRandomSeed
        self.maxFileName = maxFileName
        self.fileTypeArray = fileTypes # Redundant?
        self.fileTypes = sum(self.fileTypeArray)
        self.oddBranchContent = oddBranchContent
        self.badFileLoc = badFileLoc
        self.badFileNum = badFileNum
        self.publisherDown = publisherDown
        self.protocolVersion = protocolVersion
        self.baseUrl = 'http://www.example.com'
        self.dirStruct = path.join('www.example.com', 'http')
        self.pluginId = 'org.lockss.plugin.simulated.SimulatedPlugin'
        self.auId = "%s&root~%s" % (self.pluginId.replace('.', '|'), self.root)
        self.title = "Simulated Content: " + root
        self.startUrl = self.baseUrl + '/index.html'

    def expectedUrlCount(self):
        if self.depth == 0:
            dp = 1
        else:
            dp = self.depth

        numFileTypes = len(self.fileTypeArray)
        # Each branch has an index, plus there's a top level index, and
        # the top-level starting URL counts too.
        return numFileTypes*self.numFiles*self.branch + numFileTypes*self.numFiles*dp + ( self.branch + 2 )


assert sys.version_info >= ( 2, 5 )
frameworkCount = 0
