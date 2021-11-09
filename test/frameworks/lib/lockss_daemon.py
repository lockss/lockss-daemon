"""LOCKSS daemon interface library."""

# $Id$

__copyright__ = '''\
Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.
'''

import glob
import hashlib
import os
import random
import re
import shutil
import signal
import socket
import subprocess
import sys
import threading
import time
import urllib
import urllib2
import urlparse
import xml.dom.minidom

import lockss_util
from lockss_util import LockssError, log


# Constants

DEF_TIMEOUT = 30*60 # 30 minute default timeout for waits.
DEF_SLEEP = 5       # 5 second default sleep between loops.
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

GLOBAL_CONFIGURATION_TEMPLATE = """\
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

LOCAL_CONFIGURATION_TEMPLATE = """\
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

CONTENT_PROPERTIES_TEMPLATE = """\
#HTTP headers for %(url)s
#%(nowComment)s
last-modified=0
org.lockss.version.number=1
x-lockss-content-type=text/plain
x-lockss-node-url=%(url)s
x-lockss-orig-url=%(url)s
x_lockss-server-date=%(now)s
"""

NODE_PROPERTIES_TEMPLATE = """\
#Node properties
#%(nowComment)s
node.child.count=0
node.tree.size=12
"""

# Classes


class LockssDaemon:
    """Wrapper around a daemon instance.  Controls starting and stopping a LOCKSS Java daemon."""

    def __init__( self, daemonDir, classpath, configList ):
        self.daemonDir = daemonDir
        self.classpath = classpath
        self.configList = configList
        try:
            self.javaBin = os.path.join( os.environ[ 'JAVA_HOME' ], 'bin', 'java' )
        except KeyError:
            raise LockssError( 'JAVA_HOME must be set' )
        self.logfile = open( os.path.join( daemonDir, 'test.out' ), 'a+' )
        self.daemon = None

    def start( self ):
        if not self.daemon:
            args = [self.javaBin, '-server', '-cp', self.classpath, '-Dorg.lockss.defaultLogLevel=debug']
            tmpdir = os.environ[ 'java.io.tmpdir' ]
            if tmpdir is not None:
                args.append('-Djava.io.tmpdir=%s' % tmpdir)
            args.append('org.lockss.app.LockssDaemon')
            args.extend(self.configList)

            self.daemon = subprocess.Popen( args,
                                            stdout = self.logfile, stderr = self.logfile, cwd = self.daemonDir )
            lockss_util.write_to_file( '%i\n' % self.daemon.pid, os.path.join( self.daemonDir, 'dpid' ) )

    def stop( self ):
        if self.daemon:
            try:
                os.kill( self.daemon.pid, signal.SIGKILL )
                #self.daemon.kill() # Python 2.6+
                self.daemon.wait()
            except OSError:
                log.debug( 'Daemon already dead?' )
            else:
                log.debug( 'Daemon stopped' )
            finally:
                self.daemon = None

    def requestThreadDump( self ):
        if self.daemon:
            os.kill( self.daemon.pid, signal.SIGQUIT )
            #self.daemon.send_signal( signal.SIGQUIT ) # Python 2.6+


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

    def __init__( self, daemonCount = None, urlList = None, startUiPort = None, startV3Port = None ):
        self.workDir = os.path.abspath( lockss_util.config.get( 'workDir', './' ) )
        self.frameworkDir = self.__makeTestDir()
        self.projectDir = lockss_util.config.get( 'projectDir' )
        if self.projectDir is None:
            # will raise LockssError if not found.
            self.projectDir = self.__findProjectDir()
        self.localLibDir = os.path.join( self.workDir, 'lib' )
        self.projectLibDir = os.path.join( self.projectDir, 'lib' )
        self.daemonCount = daemonCount if daemonCount else int( lockss_util.config.get( 'daemonCount', 4 ) )
        self.startUiPort = startUiPort if startUiPort else int( lockss_util.config.get( 'startUiPort', 8041 ) )
        self.startV3Port = startV3Port if startV3Port else int( lockss_util.config.get( 'startV3Port', 8801 ) )
        self.username = lockss_util.config.get( 'username', 'lockss-u' )
        self.password = lockss_util.config.get( 'password', 'lockss-p' )
        self.logLevel = lockss_util.config.get( 'daemonLogLevel', 'debug' )
        self.hostname = lockss_util.config.get( 'hostname', 'localhost' )

        self.clientList = []    # Ordered list of clients
        self.daemonList = []    # Ordered list of daemons
        self.configCount = 0    # Used when writing daemon properties

        self.isRunning = False

        # Assert that the project directory and the necessary libraries exist.
        if not all( os.path.isfile( os.path.join( self.projectDir, 'lib', filename ) ) for filename in ( 'lockss.jar', 'lockss-test.jar', 'lockss-plugins.jar' ) ):
            raise LockssError( 'Project directory %s is not ready' % self.projectDir )

        # Write the framework global config file.
        globalConfigFile = os.path.join( self.frameworkDir, 'lockss.txt' )
        self.__writeConfig( globalConfigFile, GLOBAL_CONFIGURATION_TEMPLATE % { "logLevel": self.logLevel } )

        # Write the 'extra' config file (may be empty if no LOCKSS daemon properties are defined).
        extraConfigFile = os.path.join( self.frameworkDir, 'lockss.opt' )
        self.__writeConfig( extraConfigFile, lockss_util.config.daemonItems() )

        # Establish the local working library directory
        self.temporaryLocalLibDir = not os.path.isdir( self.localLibDir )
        if self.temporaryLocalLibDir:
            os.mkdir( self.localLibDir )

        # Copy the LOCKSS libraries from the project
        for library in ( 'lockss.jar', 'lockss-test.jar', 'lockss-plugins.jar' ):
            shutil.copy( os.path.join( self.projectLibDir, library ), self.localLibDir )

        # Set up a each daemon and create a work directory for it.
        for daemon_index in range( self.daemonCount ):
            port = self.startUiPort + daemon_index
            daemonDir = os.path.abspath( os.path.join( self.frameworkDir, 'daemon-%i' % port ) )
            # local config
            localConfigFile = os.path.join( daemonDir, 'local.txt' )
            # Init the directory
            os.mkdir(daemonDir)
            # write the daemon-specific config file
            self.__writeLocalConfig(localConfigFile, daemonDir, port)
            # Create daemon
            daemon = LockssDaemon( daemonDir, self.__makeClasspath(),
                                   urlList + ( localConfigFile, ) if urlList else ( globalConfigFile, localConfigFile, extraConfigFile ) )
            self.daemonList.append( daemon )
            # create client for this daemon
            self.clientList.append( Local_Client( daemon, self.hostname, port, self.startV3Port + daemon_index, self.username, self.password ) )

    def start(self):
        """Start each daemon in the framework."""
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
        assert not self.isRunning
        shutil.rmtree(self.frameworkDir)
        if self.temporaryLocalLibDir:
            shutil.rmtree(self.localLibDir)

    def appendLocalConfig( self, configuration, client ):
        """Append the configuration to the local config file for a client daemon."""
        self.__writeConfig( os.path.join( client.daemon.daemonDir, 'local.txt' ), configuration, False )

    def appendExtraConfig( self, configuration ):
        """Append the configuration to the extra config file for all client daemons."""
        self.__writeConfig( os.path.join( self.frameworkDir, 'lockss.opt' ), configuration, False )

    def reloadAllConfiguration(self):
        for client in self.clientList:
            client.reloadConfiguration()

    def checkForDeadlock(self):
        """Request that all the daemons in the framework dump their
        threads, and then look for deadlock messages.  Returns a list of
        log files to check, or an empty list if no deadlocks are found."""
        for daemon in self.daemonList:
            daemon.requestThreadDump()
        time.sleep( 1 ) # Horrible kludge: pause to allow the VM's to flush the daemon log files.
        return [ daemon.logfile.name for daemon in self.daemonList if 'FOUND A JAVA LEVEL DEADLOCK' in open( daemon.logfile.name ).read() ]

    def checkForLogErrors(self):
        """Look for NPEs and IllegalArgumentException in daemon logs.
        Returns a list of log files, or an empty list if no errors are found."""
        return [ daemon.logfile.name for daemon in self.daemonList if re.search('UnsupportedOperationException|IllegalArgumentException|NullPointerException', open( daemon.logfile.name ).read()) ]

    def __makeClasspath(self):
        """Return a list of the jar and zip files in self.localLibDir and self.projectDir/lib."""
        return ':'.join( glob.glob( os.path.join( self.localLibDir, '*.jar' ) ) + 
                         [ open( os.path.join( self.projectDir, 'test', 'test-classpath' ) ).readline().strip() ] )

    @staticmethod
    def __findProjectDir():
        """Walk up the tree until 'build.xml' is found in the presumed project root."""
        search_path = os.getcwd()
        while search_path:
            if os.path.isfile( os.path.join( search_path, 'build.xml' ) ):
                return search_path
            else:
                search_path = search_path.rsplit( os.sep, 1 )[ 0 ]
        raise LockssError( 'No LOCKSS project directory found' )

    def __makeTestDir( self ):
        """Construct the name of a directory under the top-level work
        directory in which to create the framework.  This allows each
        functional test to work in its own directory, and for all the test
        results to be left for examination if deleteAfter is false."""
        global frameworkCount
        frameworkCount += 1
        framework_directory = os.path.join( self.workDir, 'testcase-%i' % frameworkCount )
        if os.path.isdir( framework_directory ):
            unique_name = framework_directory + "-" + str( time.time() )
            log.info( 'Old directory exists; renaming it to ' + unique_name )
            os.rename( framework_directory, unique_name )
        os.mkdir( framework_directory )
        return framework_directory

    def __writeLocalConfig( self, filename, directory, uiPort ):
        """Write local config file for a daemon.  Daemon num is
        assumed to start at 0 and go up to n, for n-1 daemons."""
        baseUnicastPort = 9000
        baseIcpPort = 3031
        self.__writeConfig( filename, LOCAL_CONFIGURATION_TEMPLATE % { 'username': self.username,
                                                                       'password': 'SHA1:' + hashlib.sha1( self.password ).hexdigest(),
                                                                       'dir': directory,
                                                                       'proxyStart': not self.configCount, # First daemon only
                                                                       'uiPort': uiPort,
                                                                       'ipAddr': '.'.join( 4*[ str( self.configCount + 1 ) ] ),
                                                                       'unicastPort': baseUnicastPort + self.configCount,
                                                                       'unicastSendToPort': baseUnicastPort,
                                                                       'icpPort': baseIcpPort + self.configCount } )
        self.configCount += 1

    @staticmethod
    def __writeConfig( filename, configuration, overwrite = True ):
        """Write a properties string or dictionary to a configuration file."""
        if isinstance( configuration, dict ):
            configuration = ''.join( [ '%s=%s\n' % ( key, value ) for key, value in configuration.iteritems() ] )
        lockss_util.write_to_file( configuration, filename, overwrite )


class Node:
    """AU node"""
    def __init__( self, url, path ):
        self.url = url
        self.path = path

    def __cmp__( self, other ):
        return cmp( self.url, other.url )

    def __str__( self ):
        return self.url

    def filename( self ):
        """Return the file with the content of this node."""
        return os.path.join( self.path, '#content', 'current' )

    def read( self ):
        """Return the content of a specific node."""
        try:
            return open( self.filename() ).read()
        except IOError:
            raise LockssError( 'File is not readable: ' + self.filename() )


class Client:
    """LOCKSS server client interface"""

    def __init__( self, hostname, port, username, password ):
        self.hostname = hostname
        self.port = port
        self.base_URL = 'http://' + self.hostname + ':' + str( port ) + '/'
        authentication_handler = urllib2.HTTPBasicAuthHandler()
        authentication_handler.add_password( 'LOCKSS Admin', self.base_URL, username, password )
        self.URL_opener = urllib2.build_opener( urllib2.HTTPCookieProcessor(), authentication_handler, lockss_util.Multipart_Form_HTTP_Handler )

    def __str__( self ):
        return self.base_URL[ 7 : -1 ]  # Between "http://" and "/"

    def createAu( self, AU ):
        """Create an AU by Manual Journal Configuration."""
        # Note that this is probably not what you want. You
        # probably want addByAuid, unless the AU isn't in the TDB.
        # now works with nondef parameters
        self.__execute_AU_post( AU, 'Create' )

    def addByAuid( self, AU ):
        """Create an AU by lookup in the TDB."""
        self.__execute_AU_post( AU, 'AddByAuid' )
 
    def waitAu( self, AU ):
        """Block until the AU appears in the server's status table."""
        if not self.waitForCreateAu( AU ):
            raise LockssError( 'Timed out while waiting for AU %s to appear' % AU )

    def deleteAu( self, AU ):
        """Delete a simulated AU.  This will block until the AU no longer appears in the server's status table."""
        self.__execute_AU_post( AU, 'Confirm Delete' )
        if not self.waitForDeleteAu( AU ):
            raise LockssError( 'Timed out while waiting for AU %s to be deleted' % AU )

    def setPublisherDown( self, AU ):
        """Set pub_down for the specified AU."""
        self.__execute_AU_post( AU, 'Update', { 'lfp.pub_down': True } )
        if not self.waitForPublisherDown( AU ):
            raise LockssError( 'Timed out waiting for AU %s to be marked "Publisher down"' % AU )

    def reactivateAu( self, AU, doWait = True ):
        """Re-activate a simulated AU.  If doWait is set, wait for the AU
        to reappear in the server status table before returning."""
        if not self.isActiveAu( AU ):
            self.__execute_AU_post( AU, 'DoReactivate' )
            if doWait and not self.waitForReactivateAu( AU ):
                raise LockssError( 'Timed out while waiting for AU %s to be reactivated' % AU )

    def deactivateAu( self, AU, doWait = True ):
        """Deactivate a simulated AU.  If doWait is set, wait for the AU
        to disappear from the server status table before returning."""
        if self.isActiveAu( AU ):
            self.__execute_AU_post( AU, 'Confirm Deactivate' )
            if doWait and not self.waitForDeactivateAu( AU ):
                raise LockssError( 'Timed out while waiting for AU %s to be deactivated' % AU )

    ##
    ## Reload, Backup, Restore the configuration
    ##

    def reloadConfiguration( self ):
        """Cause the daemon to reload its configuration."""
        self.__execute_post( 'DebugPanel', { 'action': 'Reload Config' } )

    def backupConfiguration(self):
        """Very quick and dirty way to download the config backup."""
        lockss_util.write_to_file( self.__execute_post( 'BatchAuConfig', { 'lockssAction': 'Backup' } ).read(), 'configbackup.zip' )

    def restoreConfiguration(self, au):
        result = self.__execute_post( 'BatchAuConfig', { 'lockssAction': 'SelectRestoreTitles', 'Verb': 5 },
                                      { 'AuConfigBackupContents': 'configbackup.zip' } ).read()
        log.debug3( 'Got result from Batch AU Config servlet\n' + result )
        # Expect to see the strings 'Simulated Content: foo' and
        # 'Restore Selected AUs' in the response.  FRAGILE; obviously
        # this will break if the servlet UI is changed.
        if au.title not in result or 'Restore Selected AUs' not in result:
            raise LockssError( 'Unexpected response from BatchAuConfig servlet' )
        # Now confirm the restoration.
        assert re.search( '\d+ AUs? restored', self.__execute_post( 'BatchAuConfig', { 'lockssAction': 'DoAddAus', 'Verb': 5, 'auid': au.auId } ).read() ), 'Restore failed'
        # If this was successful, delete the configbackup.zip file
        os.remove( 'configbackup.zip' )

    ##
    ## General status accessors
    ##

    def getV3Identity( self ):
        """Returns the V3 identity from the platform config table."""
        return self._getStatusTable( 'PlatformStatus' )[ 0 ][ 'V3 Identity' ]

    def getListOfCrawlStatuses( self ):
        """Return the historical list of crawls and statuses."""
        return self._getStatusTable( 'crawl_status_table', outputVersion=2 )[ 1 ]

    def getCrawlStatus( self, AU = None ):
        """Return the current crawl status of this cache."""
        return self._getStatusTable( 'crawl_status_table', AU.auId if AU else None )[ 1 ]

    def getSingleCrawlStatus( self, key ):
        """Return the detailed crawl status table for the specified AU and key."""
        return self._getStatusTable( 'single_crawl_status_table', key )

    def getListOfAuids(self):
        '''Returns the list of AUIDs held by the daemons'''
        return [map['AuId'] for map in self._getStatusTable('AuIds')[1]]
    
    def getDictOfVolumes(self):
        d = dict()
        for map in self._getStatusTable('AuIds', outputVersion=2,
                                        columns="AuId;AuName")[1]:
            d[map['AuId']] = map['AuName']['value']
        return d
    
    def getDictOfCrawlPools(self):
        d = dict()
        for map in self._getStatusTable('AuIds', outputVersion=2,
                                        columns="AuId;CrawlPool")[1]:
            d[map['AuId']] = map['CrawlPool']
        return d

    def getListOfArticles(self, au):
        '''Gets a list of article URLs for the AU'''
        post = self.__execute_post('ListObjects', {'type': 'articles', 'auid': au.auId})
        r = post.read()
        lst = r.splitlines()
        if lst[-1] != '# end': raise LockssError, 'Incomplete or malformed article list'
        return filter(lambda x: not (len(x) == 0 or x.isspace() or x.startswith('#')), lst)
        
    def getListOfUrls(self, au):
        post = self.__execute_post('ListObjects', {'type': 'urls', 'auid': au.auId})
        return post.read()

    def getAuHashFile( self, AU ):
        """Return the hash file contents for the whole AU."""
        hash_lock = hash_locks.setdefault( urlparse.urlparse( self.base_URL ).netloc, threading.Lock() )
        hash_lock.acquire() # Efficient hashing through queueing at each server
        try:    
            match = re.search( '<td>Hash file:</td\n><td><a href="/(.+)">HashFile</a\n>', self.__execute_post( 'HashCUS', { 'action': 'Hash', 'auid': AU.auId, 'url': 'lockssau:', 'hashtype': 4 } ).read() )
            if not match:
                raise LockssError( 'Hash file URL not found' )
            return self.__execute_post( match.group( 1 ) ).read()
        finally:
            hash_lock.release()

    def getViewContentUrl( self, AU, url, filter ):
        content_url = '%sViewContent?frame=content&filter=1&' \
            'auid=%s&url=%s' % \
            (self.base_URL, urllib.quote_plus(AU.auId),
             urllib.quote_plus(url))
        return content_url

    def getViewContent( self, AU, url, filter ):
        """Return the contents of the url, as stored at the daemon."""
        content_url = self.getViewContentUrl( AU, url, filter )
        return lockss_util.HTTP_request( self.URL_opener, content_url )

    def getPollResults( self, AU ):
        """Return the current poll results for the AU."""
        table = self._getStatusTable( 'ArchivalUnitTable', AU.auId )[ 0 ]
        if not table:
            raise LockssError( 'AUID "%s" not found' % AU.auId )
        return table.get( 'Last Poll Result' ), table[ 'Status' ]

    def hasAu( self, AU ):
        """Return true iff the status table lists the given AU."""
        for row in self._getStatusTable( 'AuIds' )[ 1 ]:
            if row[ 'AuId' ] == AU.auId:
                return True

    def isActiveAu( self, AU ):
        """Return true iff the AU is activated."""
        for row in self._getStatusTable( 'RepositoryTable' )[ 1 ]:
            # Repository status table may report with two types,
            # a <reference> for 'au', or a string for 'au'.  We want to
            # ignore the string versions.
            if isinstance( row[ 'au' ], dict ) and row[ 'au' ][ 'key' ] == AU.auId:
                return row[ 'status' ] == 'Active'

    def isAuDamaged( self, AU, node = None ):
        for row in self._getStatusTable( 'ArchivalUnitTable', AU.auId )[ 1 ]:
            if 'NodeStatus' in row and row[ 'NodeName' ] == ( node.url if node else 'lockssau' ):
                return row[ 'NodeStatus' ] == 'Damaged'

    def hasNoSubstance( self, AU ):
        """Return False if the AU has substance, True if it does not,
        and None if the AU has no substance checking. The annoying
        negative logic allows code to check 'if hasNoSubstance()' and
        have both False (meaning Yes, there is substance) and None
        (meaning there is no substance checking) to go down one
        branch, while the other branch means that the AU's substance
        checkers have run, and have found that there is no
        substance."""
        table = self._getStatusTable( 'ArchivalUnitTable', AU.auId,
                                      noRows = True )[ 0 ]
        try:
            return table[ 'Has Substance' ] == 'No'
        except KeyError:
            return None

    def crawl_successful( self, AU, new_crawl = True ):
        """True if the specified AU has (newly if new_crawl) crawled successfully."""
        try:
            if new_crawl:
                table = self.getCrawlStatus( AU )
                status = table[ 0 ][ 'crawl_status' ][ 'value' ]
		log.debug( 'New crawl on %s status %s' % ( self, status ) )
            else:
                status = self._getStatusTable( 'ArchivalUnitTable', AU.auId )[ 0 ][ 'Last Crawl Result' ]
		log.debug( 'Old crawl on %s status %s' % ( self, status ) )
        except ( TypeError, IndexError, KeyError ):
            log.info( 'AU not found' )
            return False
        if status == 'Successful':
	    if not new_crawl:
		log.debug( 'Not new crawl' )
	    if not isinstance( AU, Simulated_AU ):
		log.debug( 'Not simulated' )
	    if int(self.valueOfRef( table[ 0 ][ 'num_urls_fetched' ] )) == AU.expectedUrlCount():
		log.debug( 'Count is expected' )
            if not new_crawl or not isinstance( AU, Simulated_AU ) or int(self.valueOfRef( table[ 0 ][ 'num_urls_fetched' ] )) == AU.expectedUrlCount():
		log.debug( 'Not new_crawl etc. so True' )
                return True
            raise LockssError( "Crawl on client %s collected only %s of %i URL's" % ( self, self.valueOfRef( table[ 0 ][ 'num_urls_fetched' ] ), AU.expectedUrlCount() ) )
        elif status not in ( 'Pending', 'Active', 'Interrupted by daemon exit' ):
            raise LockssError( '%s in crawl on %s' % ( status, self ) )
	log.debug( 'Fall through' )

    def isPublisherDown( self, AU ):
        """Return true if the AU is marked 'publisher down' (i.e., if the ArchivalUnitTable lists 'Available From Publisher' as 'No')"""
        return self._getStatusTable( 'ArchivalUnitTable', AU.auId )[ 0 ].get( 'Available From Publisher' ) == 'No'

    def isAuOK( self, AU ):
        """Return true if the top level of the AU has been repaired."""
        for row in self.getAuV1Polls( AU ):
            if 'Range' not in row and row[ 'PollType' ] == 'C' and row[ 'URL' ] == 'lockssau:':
                return row['Status'] == 'Won'

    def isContentRepaired( self, AU, node ):
        """Return true if the AU has been repaired by a SNCUSS poll."""
        for row in self.getAuV1Polls( AU ):
            if 'Range' in row and row[ 'PollType' ] == 'C' and row[ 'Range' ] == 'single node' and row[ 'URL' ] == node.url:
                return row[ 'Status' ] == 'Repaired'

    def startV3Poll( self, AU ):
        """Start a V3 poll of an AU."""
        self.__execute_post( 'DebugPanel', { 'action': 'Start V3 Poll', 'auid': AU.auId } )

    def getV3PollKey( self, AU, excluded_poll_keys = [] ):
        """Return the key of a poll on the AU, excluding poll keys in excluded_poll_keys."""
        for row in self.getAuV3Pollers():
            if 'pollId' in row and row[ 'pollId' ][ 'key' ] not in excluded_poll_keys and self.isAuIdOrRef( row[ 'auId' ], AU ):
                return row[ 'pollId' ][ 'key' ]

    def getV3PollKeys( self, AU ):
        """Return the keys of all polls on the AU."""
        return [ row[ 'pollId' ][ 'key' ] for row in self.getAuV3Pollers() if 'pollId' in row ]

    def getV3PollInvitedPeers( self, poll_key ):
        return [ self.valueOfRef( row[ 'identity' ] ) for row in self.getV3PollerDetail( poll_key )[ 1 ] ]

    def isV3Repaired( self, au, nodes = None ):
        """Return true if the whole AU (or specified nodes) have been repaired via V3."""
        for poll in self.getAuV3Pollers():
            if self.isAuIdOrRef( poll[ 'auId' ], au ) and poll[ 'status' ] == 'Complete':
                # Found the right entry
                summary = self.getV3PollerDetail( poll[ 'pollId' ][ 'key' ] )[ 0 ]
                if  summary[ 'Type' ] == 'Local':
                    continue
                URL_total = int( summary[ 'Total URLs In Vote' ] )
                agreed_total = int( summary[ 'Agreeing URLs' ][ 'value' ] )
                repaired_total = int( summary.get( 'Completed Repairs', { 'value': 0 } )[ 'value' ] )
                log.debug( "%d urls: %d agreed, %d repaired" % ( URL_total, agreed_total, repaired_total ) )
                return repaired_total == agreed_total == URL_total if nodes is None else repaired_total == len( nodes ) and agreed_total == URL_total
                # TODO: This will really need to be improved when the status
                # tables are better!  Need a way to determine whether these
                # particular nodes were repaired.

    def hasWonV3Poll( self, AU ):
        """Return true if a poll has been called and no repairs have been made."""
        return self.isV3Repaired( AU, [] )

    def isNodeRepairedFromServerByV3( self, AU, node, publisher_not_peer ):
        """Determines whether the given content node has been repaired from a V3 publisher/peer."""
        for row in self.getAuV3Pollers():
            if self.isAuIdOrRef( row[ 'auId' ], AU ) and row[ 'status' ] == 'Complete':
                poll_key = row[ 'pollId' ][ 'key' ]
                log.debug( 'Found the right row in the V3 Pollers table.  Key: ' + poll_key )
                repair_table = self.getV3CompletedRepairsTable( poll_key )[ 1 ]
                log.debug( 'Got a repairTable with %d rows.' % len( repair_table ) )
                for repair_row in repair_table:
                    log.debug( "repair_row[ 'url' ] == %s; repair_row[ 'repairFrom' ] == %s" %
                               ( repair_row[ 'url' ], repair_row[ 'repairFrom' ] ) )
                    if repair_row[ 'url' ] == node.url:
                        return repair_row[ 'repairFrom' ] == 'Publisher' if publisher_not_peer else re.match( r'TCP:\[.*\]:', repair_row[ 'repairFrom' ] )

    def isV3NoQuorum( self, AU ):
        for row in self.getAuV3Pollers():
            if self.isAuIdOrRef( row[ 'auId' ], AU ):
                return row[ 'status' ] == 'No Quorum'

    def startCrawl( self, AU ):
        """Start a crawl of an AU."""
        self.__execute_post( 'DebugPanel', { 'action': 'Start Crawl', 'auid': AU.auId } )

    def isContentRepairedFromServer( self, AU, node, publisher_not_proxy ):
        """Determines whether the content node has been repaired from another cache from the publisher / via the proxy."""
        for row in self.getCrawlStatus( AU ):
            if row[ 'crawl_type' ] == 'Repair' and row[ 'crawl_status' ][ 'key' ] == 'Successful':
                # Successful repair crawl: look into the detail status table
                summary = self.getSingleCrawlStatus( 'Successful' )[ 0 ]
                return summary[ 'Starting Url(s)' ] == '[%s]' % node.url and ( summary[ 'Source' ] == 'Publisher' ) == publisher_not_proxy

    def isNameRepaired( self, ranged, AU, node = None ):
        """Return true if the AU has been repaired by range/non-ranged name poll."""
        for row in self.getAuV1Polls( AU ):
            if ( 'Range' in row ) == ranged and row[ 'PollType' ] == 'N' and row[ 'URL' ] == ( node.url if node else AU.baseUrl ):
                return row[ 'Status' ] == 'Repaired'

    def getAuV1Polls( self, AU, activeOnly = False ):
        """Return the full poll status of an AU."""
        key = 'AU~%s' % urllib.quote( AU.auId ) # requires a pre-quoted key
        if activeOnly:
            key += '&Status~Active'
        return self._getStatusTable( 'PollManagerTable', key )[ 1 ]

    def getAuV3Pollers( self ):
        return self._getStatusTable( 'V3PollerTable' )[ 1 ]

    def getAuV3Voters( self, AU ):
        # Requires a pre-quoted key
        return self._getStatusTable( 'V3VoterTable', 'AU~' + urllib.quote( AU.auId ) )[ 1 ]

    def findCompletedAuV3Poll( self, au ):
        for poll in self.getAuV3Pollers():
            if self.isAuIdOrRef( poll[ 'auId' ], au ) and poll[ 'status' ] == 'Complete':
                return poll
        return None

    def getPollSummary( self, poll ):
        return self.getV3PollerDetail( poll[ 'pollId' ][ 'key' ] )[ 0 ]

    def getPollSummaryFromKey( self, pollKey ):
        return self.getV3PollerDetail( pollKey )[ 0 ]

    def getV3PollerDetail( self, key ):
        """Returns both the summary and table."""
        return self._getStatusTable( 'V3PollerDetailTable', key )

    def getV3PollVotersCounts( self, key ):
        """Returns the count detail columns for each voter, as a
        dictionary keyed by peerId string of dictionaries keyed by
        string of column name."""
        columns = "identity;numagree;numdisagree;numpolleronly;numvoteronly"
        d = dict()
        for row in self._getStatusTable( 'V3PollerDetailTable', key, columns = columns )[ 1 ]:
            identity = self.valueOfRef( row[ 'identity' ] )
            del row[ 'identity' ]
            d[ identity ] = row
        return d

    def getV3CompletedRepairsTable( self, key ):
        """Returns the V3 completed repairs status table."""
        return self._getStatusTable( 'V3CompletedRepairsTable', key )

    def getAuRepairerInfo( self, AU, key ):
        """Returns a mapping of peers to poll agreement data."""
        return dict( ( row[ 'Box' ], row.get( key, '0.00' ).split( '%', 1 )[ 0 ] ) for row in self._getStatusTable( 'PeerRepair', AU.auId )[ 1 ] )

    def getAllAuRepairerInfo(self, auid):
        '''Returns a map from peers to a map, which maps from keys in
        the PeerRepair table to the corresponding data. Unlike in
        getAuRepairerInfo(), the argument is an AUID (not an AU object)
        and the percentages are converted to float.'''
        tab = self._getStatusTable('PeerRepair', auid)[1]
        ret = dict()
        for row in tab:
            d = dict()
            for k in ['Last', 'LastAgree']:
                d[k] = row[k]
            for k in ['HighestPercentAgreement', 'LastPercentAgreement',
                      'HighestPercentAgreementHint', 'LastPercentAgreementHint']:
                v = row.get(k)
                if v is not None: v = float(v[0:-1])
                d[k] = v
            ret[row['Box']] = d
        return ret

    def getAllCommPeerData(self):
        '''Returns the (summary, table) pair for the Comm Peer Data
        status table (SCommPeers).'''
        return self._getStatusTable('SCommPeers')

    def getCommPeerData(self):
        '''Returns the tabular data of the Comm Peer Data status table
        (SCommPeers). Return a dictionary from peer to a map of the data
        for that peer.'''
        tab = self.getAllCommPeerData()[1]
        ret = dict()
        for row in tab:
            d = dict()
            for k in ['Orig', 'Fail', 'Accept', 'Sent', 'Rcvd', 'SendQ']:
                v = row.get(k)
                if v is not None: v = int(v)
                d[k] = v
            for k in ['Chan', 'LastRetry', 'NextRetry']:
                d[k] = row.get(k)
            ret[row['Peer']] = d
        return ret

    def getNoAuPeers( self, AU ):
        """Return list of peers who have said they don't have the AU."""
        return [ row[ 'Peer' ] for row in self._getStatusTable( 'NoAuPeers', AU.auId )[ 1 ] ]

    def isDaemonReady( self ):
        """Ask DaemonStatus whether the server is ready for action."""
        try:
            return self.__execute_post( 'DaemonStatus', { 'isDaemonReady': 1 } ).read() == 'true\n'
        except urllib2.URLError:
            return False

    @staticmethod
    def isAuIdOrRef( AU_reference, AU ):
        return isinstance( AU_reference, dict ) and AU_reference[ 'key' ] == AU.auId or AU_reference == AU.title

    @staticmethod
    def valueOfRef( possibleref ):
        return possibleref[ 'value' ] if isinstance( possibleref, dict ) else possibleref

    def getAdminUi(self):
        """Fetch the contents of the top-level admin UI.  Useful for testing
        the Tiny UI.  May throw urllib2.URLError or urllib2.HTTPError."""
        return self.__execute_post()

    def hasV3Poller( self, AU, excluded_poll_keys = [] ):
        """Return true if the client has an active or completed V3 Poller not identified in excluded_poll_keys."""
        return self.getV3PollKey( AU, excluded_poll_keys ) is not None

    def hasV3Voter( self, AU ):
        """Return true if the client has an active V3 Poller."""
        for row in self.getAuV3Voters( AU ):
            if self.isAuIdOrRef( row[ 'auId' ], AU ):
                return True

    def hasTopLevelPoll( self, AU, content_not_name ):
        """Determines whether the client has an active top-level content/name poll."""
        for row in self.getAuV1Polls( AU ):
            if row[ 'PollType' ] == ( 'C' if content_not_name else 'N' ) and row[ 'URL' ] == 'lockssau:':
                return row[ 'Status' ] == 'Active'

    def hasSNCUSSPoll( self, AU, node ):
        """Wait for a Single-Node CUSS poll for the given AU and node."""
        for row in self.getAuV1Polls( AU ):
            if row[ 'PollType' ] == 'C' and 'Range' in row and row[ 'URL' ] == node.url:
                return row[ 'Range' ] == 'single node' and row[ 'Status' ] in ( 'Active', 'Won' )

    def hasCompletedTopLevelPoll( self, AU, content_not_name, won_not_lost ):
        """Determines whether the client has won/lost an active top-level content/name poll."""
        for row in self.getAuV1Polls( AU ):
            if row[ 'PollType' ] == ( 'C' if content_not_name else 'N' ) and row[ 'URL' ] == 'lockssau:':
                return row[ 'Status' ] == ( 'Won' if won_not_lost else 'Lost' )

    def hasNamePoll( self, AU, node ):
        """Wait for a name poll to run on a given node (won or active)."""
        for row in self.getAuV1Polls( AU ):
            if row[ 'PollType' ] == 'N' and row[ 'URL' ] == node.url:
                return row[ 'Status' ] in ( 'Active', 'Won' )

    def hasCompletedNamePoll( self, AU, node, won_not_lost ):
        """Wait for a name poll to be won/lost on the given node."""
        for row in self.getAuV1Polls( AU ):
            if row[ 'PollType' ] == 'N' and 'Range' not in row and row[ 'URL' ] == ( node.url if node else AU.baseUrl ):
                return row[ 'Status' ] == ( 'Won' if won_not_lost else 'Lost' )

    def hasRangedNamePoll( self, AU, node ):
        """Wait for a ranged name poll on the given node."""
        for row in self.getAuV1Polls( AU ):
            if row[ 'PollType' ] == 'N' and 'Range' in row and row[ 'URL' ] == ( node.url if node else AU.baseUrl ):
                return row[ 'Status' ] in ( 'Active', 'Won' )

    def hasCompletedRangedNamePoll( self, AU, node, won_not_lost ):
        """Wait for a ranged name poll to be won/lost on the given node."""
        for row in self.getAuV1Polls( AU ):
            if row[ 'PollType' ] == 'N' and 'Range' in row and row[ 'URL' ] == ( node.url if node else AU.baseUrl ):
                return row[ 'Status' ] == ( 'Won' if won_not_lost else 'Lost' )

    def hasCompletedSNCUSSPoll( self, AU, node, won_not_lost ):
        """Wait for a Single-Node CUSS poll for the given AU and node to be won/lost."""
        for row in self.getAuV1Polls( AU ):
            if row[ 'PollType' ] == 'C' and 'Range' in row and row[ 'URL' ] == node.url:
                return row[ 'Range' ] == 'single node' and row[ 'Status' ] == ( 'Won' if won_not_lost else 'Lost' )

    def hasCompletedV3Poll( self, AU, excluded_polls ):
        key = self.getV3PollKey( AU, excluded_polls )
        if key:
            summary = self.getV3PollerDetail( key )[ 0 ]
            return summary and summary[ 'Status' ] in ( 'No Time Available', 'Complete', 'No Quorum', 'Error', 'Expired', 'Aborted' )
        else: return False

    def isCompletedV3Poll( self, AU, poll_key ):
        summary = self.getV3PollerDetail( poll_key )[ 0 ]
        return summary and summary[ 'Status' ] in ( 'No Time Available', 'Complete', 'No Quorum', 'Error', 'Expired', 'Aborted' )

    ###
    ### Methods that block while waiting for various events
    ###

    def waitForCanConnectToHost(self, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        return self.wait( lambda: lockss_util.server_is_listening( self.hostname, self.port ), timeout, sleep )

    def waitForCreateAu(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        """Wait for the au to appear or for the timeout to expire."""
        return self.wait( lambda: self.hasAu( au ), timeout, sleep )

    def waitForDeleteAu(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        """Wait for the au to be deleted or for the timeout to expire."""
        return self.wait( lambda: not self.hasAu( au ), timeout, sleep )

    def waitForPublisherDown(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        """Wait for the specified AU to be marked 'publisher down'."""
        return self.wait( lambda: self.isPublisherDown( au ), timeout, sleep )

    def waitForReactivateAu(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        """Wait for the au to be activated, or for the timeout to expire."""
        return self.wait( lambda: self.isActiveAu( au ), timeout, sleep )

    def waitForDeactivateAu(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        """Wait for the au to be deactivated, or for the timeout to expire."""
        return self.wait( lambda: not self.isActiveAu( au ), timeout, sleep )

    def waitForSuccessfulCrawl( self, AU, new_crawl = True, timeout = DEF_TIMEOUT, sleep = DEF_SLEEP ):
        """Wait until the specified AU has (newly if new_crawl) crawled successfully."""
        return self.wait( lambda: self.crawl_successful( AU, new_crawl ), timeout, sleep )

    def waitForPollResults( self, AU, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP ):
        """Wait until poll results are available for an AU."""
        return self.wait( lambda: self.getPollResults( AU )[ 0 ], timeout, sleep )

    def waitForCompletedV3Poll( self, AU, excluded_polls = [], timeout = DEF_TIMEOUT, sleep = DEF_SLEEP ):
        """Wait until a poll not in excluded_polls has completed."""
        return self.wait( lambda: self.hasCompletedV3Poll( AU, excluded_polls ), timeout, sleep )

    def waitForThisCompletedV3Poll( self, AU, poll_key, timeout = DEF_TIMEOUT, sleep = DEF_SLEEP ):
        """Wait until the poll with the specified key has completed."""
        return self.wait( lambda: self.isCompletedV3Poll( AU, poll_key ), timeout, sleep )

    def waitForV3Poller(self, AU, excluded_poll_keys = [], timeout = DEF_TIMEOUT, sleep = DEF_SLEEP):
        return self.wait( lambda: self.hasV3Poller( AU, excluded_poll_keys ), timeout, sleep )

    def waitForWonV3Poll(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        return self.wait( lambda: self.hasWonV3Poll( au ), timeout, sleep )

    def waitForV3Voter(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        return self.wait( lambda: self.hasV3Voter( au ), timeout, sleep )

    def waitForTopLevelContentPoll( self, AU, timeout = DEF_TIMEOUT, sleep = DEF_SLEEP ):
        return self.wait( lambda: self.hasTopLevelPoll( AU, True ), timeout, sleep )

    def waitForTopLevelNamePoll( self, AU, timeout = DEF_TIMEOUT, sleep = DEF_SLEEP ):
        return self.wait( lambda: self.hasTopLevelPoll( AU, False ), timeout, sleep )

    def waitForSNCUSSContentPoll( self, au, node, timeout = DEF_TIMEOUT, sleep = DEF_SLEEP ):
        return self.wait( lambda: self.hasSNCUSSPoll( au, node ), timeout, sleep )

    def waitForLostTopLevelContentPoll( self, AU, timeout = DEF_TIMEOUT, sleep = DEF_SLEEP ):
        """Block until a top-level content poll is lost."""
        return self.wait( lambda: self.hasCompletedTopLevelPoll( AU, True, False ), timeout, sleep )

    def waitForLostTopLevelNamePoll( self, AU, timeout = DEF_TIMEOUT, sleep = DEF_SLEEP ):
        """Block until a top-level name poll is lost."""
        return self.wait( lambda: self.hasCompletedTopLevelPoll( AU, False, False ), timeout, sleep )

    def waitForLostNamePoll( self, au, node, timeout = DEF_TIMEOUT, sleep = DEF_SLEEP ):
        """Block until a node name poll is lost."""
        return self.wait( lambda: self.hasCompletedNamePoll( au, node, False ), timeout, sleep )

    def waitForLostSNCUSSContentPoll( self, au, node, timeout = DEF_TIMEOUT, sleep = DEF_SLEEP ):
        """Block until a top-level name poll is lost."""
        return self.wait( lambda: self.hasCompletedSNCUSSPoll( au, node, False ), timeout, sleep )

    def waitForWonTopLevelContentPoll( self, AU, timeout = DEF_TIMEOUT, sleep = DEF_SLEEP ):
        """Block until a top-level content poll is won."""
        return self.wait( lambda: self.hasCompletedTopLevelPoll( AU, True, True ), timeout, sleep )

    def waitForWonTopLevelNamePoll( self, AU, timeout = DEF_TIMEOUT, sleep = DEF_SLEEP ):
        """Block until a top-level name poll is won."""
        return self.wait( lambda: self.hasCompletedTopLevelPoll( AU, False, True ), timeout, sleep )

    def waitForNamePoll( self, au, node, timeout = DEF_TIMEOUT, sleep = DEF_SLEEP ):
        """Block until a node level name poll is run (active or won)."""
        return self.wait( lambda: self.hasNamePoll( au, node ), timeout, sleep )

    def waitForWonNamePoll( self, au, node, timeout = DEF_TIMEOUT, sleep = DEF_SLEEP ):
        """Block until a node level name poll is won."""
        return self.wait( lambda: self.hasCompletedNamePoll( au, node, True ), timeout, sleep )

    def waitForWonSNCUSSContentPoll( self, au, node, timeout = DEF_TIMEOUT, sleep = DEF_SLEEP ):
        """Block until a SNCUSS content poll is won."""
        return self.wait( lambda: self.hasCompletedSNCUSSPoll( au, node, True ), timeout, sleep )

    def waitForRangedNamePoll( self, au, node, timeout = DEF_TIMEOUT, sleep = DEF_SLEEP ):
        """Block until a ranged name poll has occured (active or won)."""
        return self.wait( lambda: self.hasRangedNamePoll( au, node ), timeout, sleep )

    def waitForWonRangedNamePoll( self, au, node, timeout = DEF_TIMEOUT, sleep = DEF_SLEEP ):
        return self.wait( lambda: self.hasCompletedRangedNamePoll( au, node, True ), timeout, sleep )

    def waitForLostRangedNamePoll( self, au, node, timeout = DEF_TIMEOUT, sleep = DEF_SLEEP ):
        return self.wait( lambda: self.hasCompletedRangedNamePoll( au, node, False ), timeout, sleep )

    def waitForTopLevelDamage( self, au, timeout = DEF_TIMEOUT, sleep = DEF_SLEEP ):
        """Block until an au is marked 'Damaged' at the top level (lockssau:)."""
        return self.wait( lambda: self.isAuDamaged( au, None ), timeout, sleep )

    def waitForDamage( self, au, node, timeout = DEF_TIMEOUT, sleep = DEF_SLEEP ):
        """Block until a node is marked 'repairing', or until the timeout expires."""
        return self.wait( lambda: self.isAuDamaged( au, node ), timeout, sleep )

    def waitForTopLevelRepair( self, au, timeout = DEF_TIMEOUT, sleep = DEF_SLEEP ):
        """Block until the top-level of the AU is marked repaired."""
        return self.wait( lambda: self.isAuOK( au ), timeout, sleep )

    def waitForContentRepair( self, au, node, timeout = DEF_TIMEOUT, sleep = DEF_SLEEP ):
        """Block until a node has been successfully repaired, or until the timeout expires."""
        return self.wait( lambda: self.isContentRepaired( au, node ), timeout, sleep )

    def waitForV3Repair( self, au, nodeList = None, timeout = DEF_TIMEOUT, sleep = DEF_SLEEP ):
        """Wait for a successful repair of the complete AU (or specified nodes) by a V3 poll."""
        return self.wait( lambda: self.isV3Repaired( au, nodeList ), timeout, sleep )

    def waitForV3NoQuorum( self, au, timeout = DEF_TIMEOUT, sleep = DEF_SLEEP ):
        """Wait for a V3 poll to be marked No Quorum."""
        return self.wait( lambda: self.isV3NoQuorum( au ), timeout, sleep )

    def waitForNameRepair( self, ranged, au, node = None, timeout = DEF_TIMEOUT, sleep = DEF_SLEEP ):
        """Block until a node has been successfully repaired or the timeout expires.
        If 'node' is None, this will just wait until the base URL has been marked repaired."""
        return self.wait( lambda: self.isNameRepaired( au, ranged, node ), timeout, sleep )

    def waitForDaemonReady( self, timeout = DEF_TIMEOUT, sleep = DEF_SLEEP ):
        """Block until the framework is ready for client communication."""
        return self.wait( lambda: self.isDaemonReady(), timeout, sleep )

    @staticmethod
    def wait( test, timeout = DEF_TIMEOUT, sleep = DEF_SLEEP ):
        """Loop until test() is True or the timeout expires."""
        start = time.time()
        while True:
            if test():
                return True
            if time.time() - start >= timeout:
                return False
            time.sleep( sleep )

    ###
    ### Internal methods
    ###

    def _getStatusTable( self, statusTable, key = None, unlimited_rows = False,
                         outputVersion = None, columns = None, noRows = None ):
        """Given an XML string, parse it as a status table and return
        a list of dictionaries representing the data.  Each item in
        the list is a dictionary of column names to values, stored as
        Unicode strings."""
        form_data = { 'table': statusTable, 'output': 'xml' }
        if key:
            form_data[ 'key' ] = key
        if unlimited_rows:
            form_data[ 'numrows' ] = 0x7fffffff # JAVA Integer.MAX_VALUE
        if outputVersion is not None:
            form_data[ 'outputVersion' ] = outputVersion
        if columns:
            form_data[ 'columns' ] = columns
        if noRows:
            form_data[ 'options' ] = 'norows'
        XML = self.__execute_post( 'DaemonStatus', form_data ).read()
        log.debug3( 'Received XML response:\n' + XML )
        doc = xml.dom.minidom.parseString( XML )
        doc.normalize()

        summaries = {}
        for summary in doc.getElementsByTagName( 'st:summaryinfo' ):
            if summary.getElementsByTagName( 'st:title' )[ 0 ] and summary.getElementsByTagName( 'st:title' )[ 0 ].firstChild:
                # See if this is a reference, or CDATA
                if summary.getElementsByTagName( 'st:reference' ):
                    summaries[ summary.getElementsByTagName( 'st:title' )[ 0 ].firstChild.data ] = dict( ( key, summary.getElementsByTagName( 'st:reference' )[ 0 ].getElementsByTagName( 'st:' + key )[ 0 ].firstChild.data ) for key in ( 'name', 'key', 'value' ) )
                else:
                    stvalue = summary.getElementsByTagName( 'st:value' )
                    if stvalue and stvalue[0] and stvalue[0].firstChild:
                        summaries[ summary.getElementsByTagName( 'st:title' )[ 0 ].firstChild.data ] = stvalue[0].firstChild.data
        data = []
        for row in doc.getElementsByTagName( 'st:row' ):
            cells = {}
            for cell in row.getElementsByTagName( 'st:cell' ):
                try:
                    # See if this is a reference, or CDATA
                    if cell.getElementsByTagName( 'st:value' )[ 0 ].getElementsByTagName( 'st:reference' ):
                        cells[ cell.getElementsByTagName( 'st:columnname' )[ 0 ].firstChild.data ] = dict( ( key, cell.getElementsByTagName( 'st:value' )[ 0 ].getElementsByTagName( 'st:reference' )[ 0 ].getElementsByTagName( 'st:' + key )[ 0 ].firstChild.data ) for key in ( 'name', 'key', 'value' ) )
                    else:
                        cells[ cell.getElementsByTagName( 'st:columnname' )[ 0 ].firstChild.data ] = cell.getElementsByTagName( 'st:value' )[ 0 ].firstChild.data
                except ( IndexError, AttributeError ):
                    # Unlikely to happen, but just in case...
                    continue
            data.append( cells )
        return ( summaries, data )

    def __execute_post( self, page = '', form_data = None, attached_files = None ):
        return lockss_util.HTTP_request( self.URL_opener, self.base_URL + page, form_data, attached_files )

    def __execute_AU_post( self, AU, lockssAction, form_data = None ):
        if form_data is None:
            form_data = {}
        form_data.update( { 'lockssAction': lockssAction, 'PluginId': AU.pluginId, 'auid': AU.auId } )
        # Removing from incoming AU's vars- ['pluginId, auId' (etc) were set in the python AU]
        for key in set( vars( AU ) ) - set( ( 'pluginId', 'auId', 'title', 'baseUrl' ) ):
            form_data[ 'lfp.' + key ] = getattr( AU, key )
        self.__execute_post( 'AuConfig', form_data )


class Local_Client( Client ):
    """Test framework daemon client interface"""

    def __init__( self, daemon, hostname, port, V3_port, username, password ):
        Client.__init__( self, hostname, port, username, password )
        self.daemon = daemon
        self.V3_port = V3_port

    def getV3Identity( self ):
        """Generates a V3 identity from the framework-provided parameters."""
        return "TCP:[%s]:%i" % ( socket.gethostbyname( self.hostname ), self.V3_port )

    def getAuNode( self, au, url, check_for_content = False ):
        """Construct a node from a url on an AU."""
        root = self.getAuRoot( au )
        # Kludge for getting "lockssau" node.
        if url == 'lockssau':
            path = root
        else:
            path = os.path.join( root, url[ ( len( au.baseUrl ) + 1 ) : ] )
        if check_for_content:
            if os.path.isfile( os.path.join( path, '#content', 'current' ) ):
                return Node( url, path )
            else:
                raise LockssError( 'Node has no content: ' + url )
        else:
            if os.path.isdir( path ) or os.path.isfile( path ):
                return Node( url, path )
            else:
                raise LockssError( 'Node does not exist: ' + url )

    def isAuNode( self, au, url, check_for_content = False ):
        """Construct a node from a url on an AU."""
        root = self.getAuRoot( au )
        # Kludge for getting "lockssau" node.
        if url == 'lockssau':
            path = root
        else:
            path = os.path.join( root, url[ ( len( au.baseUrl ) + 1 ) : ] )
        if check_for_content:
            if os.path.isfile( os.path.join( path, '#content', 'current' ) ):
                return True
            else:
                return False
        else:
            if os.path.isdir( path ) or os.path.isfile( path ):
                return True
            else:
                return False

    def getAuNodesWithContent( self, au ):
        """Return a list of all nodes that have content."""
        table = self._getStatusTable( 'ArchivalUnitTable', au.auId, True )[ 1 ]
        return [ self.getAuNode( au, row[ 'NodeName' ] ) for row in table if row.get( 'NodeContentSize', '-' ) != '-' ]

    def getAuNodesWithChildren( self, au ):
        """Return a list of all nodes that have children."""
        table = self._getStatusTable( 'ArchivalUnitTable', au.auId, True )[ 1 ]
        return [ self.getAuNode( au, row[ 'NodeName' ] ) for row in table if row.get( 'NodeChildCount', '-' ) != '-' ]

    def getRandomContentNode( self, au ):
        return self.__getRandomContentNode( au )

    def __getRandomContentNode( self, au ):
        """Raise an error if the AU has not been created or crawled."""
        if not self.getAuRepository( au ):
            raise LockssError( 'No repository for au: %s' % au )
        # Randomly select a node to damage
        return random.choice( self.getAuNodesWithContent( au ) )

    def __getRandomContentNodeList( self, au, minCount, maxCount ):
        """Return a list, randomly sized between minCount and maxCount, of nodes with content."""
        nodes = self.getAuNodesWithContent( au )
        return sorted( random.sample( nodes, min( random.randint( minCount, maxCount ), len( nodes ) ) ) )

    def __getRandomBranchNodeList( self, au, minCount, maxCount ):
        """Return a list, randomly sized between minCount and maxCount, of nodes with children."""
        nodes = self.getAuNodesWithChildren( au )
        if not nodes:
            log.warn( "getAuNodesWithChildren returned no nodes!" )
            return nodes
        return random.sample( nodes, random.randint( minCount, min( maxCount, len( nodes ) ) ) )

    def getAuRepository( self, au ):
        # RepositoryStatus table does not accept a key
        for row in self._getStatusTable( 'RepositoryTable' )[ 1 ]:
            if row[ 'au' ][ 'key' ] == au.auId:
                return os.path.abspath( os.path.join( self.daemon.daemonDir, row[ 'dir' ] ) )

    def getAuRoot( self, au ):
        """Return the full path to the AU's root.  This is used by methods that
        damage, create, or delete repository files behind the server's back."""
        return os.path.join( self.getAuRepository( au ), os.path.join( urlparse.urlparse( au.baseUrl ).netloc, 'http' ) )

    ###
    ### Methods for causing damage
    ###

    def damageNode( self, node ):
        """Damage a specific node."""
        # Only want to damage the file contents
        content_path = node.filename()
        if os.path.isfile( content_path ):
            log.debug2( 'Damaging: ' + content_path )
            lockss_util.write_to_file( '*** DAMAGE ***', content_path, False )
        else:
            raise LockssError( 'File does not exist: ' + content_path )

    def randomDamageSingleNode( self, au ):
        """Randomly select and damage a node with content."""
        node = self.__getRandomContentNode( au )
        self.damageNode( node )
        return node

    def randomDamageRandomNodes( self, au, minCount = 1, maxCount = 5 ):
        """Damage a random selection of between minCount and maxCount nodes
        with content for the given au and return the list of damaged nodes."""
        nodeList = self.__getRandomContentNodeList( au, minCount, maxCount )
        for node in nodeList:
            self.damageNode( node )
        return nodeList

    @staticmethod
    def deleteNode( node ):
        """Delete a specific node."""
        if os.path.isfile( node.path ):
            os.unlink( node.path )
        elif os.path.isdir( node.path ):
            shutil.rmtree( node.path )
        else:
            raise LockssError( 'File does not exist: ' + node.path )

    def randomDelete( self, au ):
        """Randomly select a node with content and delete it."""
        node = self.__getRandomContentNode( au )
        self.deleteNode( node )
        return node

    def randomDeleteRandomNodes( self, au, minCount = 1, maxCount = 5 ):
        """Delete a random selection of between minCount and maxCount nodes
        with content for the given au and return the list of deleted nodes."""
        nodeList = self.__getRandomContentNodeList( au, minCount, maxCount )
        for node in nodeList:
            self.deleteNode( node )
        return nodeList

    def createFile( self, au, filespec ):
        """Create and return an extra file in the repository under the given
        AU and filespec relative to the AU's root, e.g. 'foo/bar' creates
        /path/to/au/repository/www.example.com/http/foo/bar."""
        output = os.path.join( self.getAuRoot( au ), filespec )
        if os.path.isfile( output ):
            raise LockssError( 'File already exists: ' + filespec )
        lockss_util.write_to_file( 'Garbage file', output )
        return output

    # NOTE!  These two methods are extremely dependent on the
    # current implementation of the repository.  If that changes,
    # then these must change.

    def createNode( self, au, filespec, node = None ):
        """Given a branch node, create a new child under it."""
        auRoot = self.getAuRoot( au )
        nodeRoot = os.path.join( node.path if node else auRoot, filespec )
        url = au.baseUrl + nodeRoot[ len( auRoot ) : ]
        nowComment = time.strftime( '%a %b %d %H:%M:%S %Z %Y' )
        now = int( 1000*time.time() )
        content_path = os.path.join( nodeRoot, '#content' )
        os.makedirs( content_path )
        lockss_util.write_to_file( 'Garbage File', os.path.join( content_path, 'current' ) )
        lockss_util.write_to_file( CONTENT_PROPERTIES_TEMPLATE % locals(), os.path.join( content_path, 'current.props' ) )
        lockss_util.write_to_file( NODE_PROPERTIES_TEMPLATE % locals(), os.path.join( nodeRoot, '#node_props' ) )
        return Node( url, nodeRoot )

    def randomCreateRandomNodes( self, au, minCount = 1, maxCount = 5 ):
        """Create a random number of between minCount and maxCount
        nodes on the given AU and return the list of new nodes."""
        return [ self.createNode( au, '%03iextrafile.txt' % node_index ) for node_index in range( random.randint( minCount, maxCount ) ) ]

    def simulateDiskFailure( self ):
        """Delete the entire contents of this client's cache!"""
        for subdirectory in ( 'cache', 'config', 'plugins' ):
            path = os.path.join( self.daemon.daemonDir, subdirectory )
            if os.path.isdir( path ):
                shutil.rmtree( path )
            else:
                raise LockssError( 'Cache not in an expected state!' )


class AU:
    """General-purpose Archival Unit.

    >>> AU("foo")
    Traceback (most recent call last):
      ...
    LockssError: Could not find a & character in AU ID "foo"

    >>> AU("foo&bar")
    Traceback (most recent call last):
      ...
    LockssError: Plugin id "foo" should have | as separators in AU ID "foo&bar"

    >>> AU("org.foo|baz&bar")
    Traceback (most recent call last):
      ...
    LockssError: Plugin id "org.foo|baz" should have no . characters in AU ID "org.foo|baz&bar"

    >>> AU("org|foo|baz&bar")
    Traceback (most recent call last):
      ...
    LockssError: Expected to find a ~ character in property "bar" in AU ID "org|foo|baz&bar"

    >>> AU("org|foo|baz&bar~42")
    Traceback (most recent call last):
      ...
    LockssError: Failed to find required key "base_url" in AU ID "org|foo|baz&bar~42"

    >>> AU("org|foo|baz&bar~42&base_url~http:yab") #doctest: +ELLIPSIS
    <__main__.AU instance at 0x...>

    """

    def __init__( self, auId):
        # handle nondef params if they're present
        params = auId.partition('@@@NONDEF@@@')
        self.auId = params[0].strip()
        try:
            self.pluginId, properties = self.auId.split( '&', 1 )
        except ValueError:
            raise LockssError( 'Could not find a & character in'
                               ' AU ID "%s"' % self.auId )
        if '|' not in self.pluginId:
            raise LockssError( 'Plugin id "%s" should have | as separators in'
                               ' AU ID "%s"' % ( self.pluginId, self.auId ) )
        if '.' in self.pluginId:
            raise LockssError( 'Plugin id "%s" should have no . characters in'
                               ' AU ID "%s"' % ( self.pluginId, self.auId ) )
        self.pluginId = self.pluginId.replace( '|', '.' )
        self.title = urllib.unquote_plus( properties )
        # first search string for params in auId
        # then if necessary, search for nondefparams
        for property in properties.split( '&' ):
            self._handleProperty(property)    
        if (params[2]) :
            nondefparams = params[2].strip()
            for property in nondefparams.split('&'):
                self._handleProperty(property)
 
        if self.pluginId != Simulated_AU.SIMULATED_PLUGIN \
          and not hasattr( self, "base_url" ):
            raise LockssError( 'Failed to find required key "base_url" in'
                               ' AU ID "%s"' % self.auId )

    def _handleProperty(self, property):
        """ get the key/value parts of the given property, checks for collisions with AU props
        """
        try:
            key, value = property.split( '~', 1 )
        except ValueError:
            raise LockssError( 'Expected to find a ~ character in'
                               ' property "%s" in AU ID "%s"' %
                               ( property, self.auId ) )
                
        # unquote_plus doesn't raise any exceptions.
        key = urllib.unquote_plus( key )
        value = urllib.unquote_plus( value )

        if hasattr( self, key ):
            # This could be a dup or something we use in class AU,
            # like 'title'.

            # todo: Make properties a dict, not attributes, or
            # make the internal attribues less likely to collide
            # with AU properties.
            raise LockssError( 'Duplicate or illegal key "%s" in' 
                               ' property "%s" in AU ID "%s"' %
                               ( key, property, self.auId ) )
        setattr( self, key, value )
         
    def __str__( self ):
        return self.title

class Simulated_AU( AU ):
    """SimulatedPlugin Archival Unit."""
    SIMULATED_PLUGIN = 'org.lockss.plugin.simulated.SimulatedPlugin'

    def __init__( self, root = 'simContent', depth = 0, branch = 0, numFiles = 10,
                  binFileSize = 1024, binRandomSeed = None, fileTypes = FILE_TYPE_TEXT | FILE_TYPE_BIN,
                  repairFromPeerIfMissingUrlPatterns = ''):
        self.root = root
        self.depth = depth
        self.branch = branch
        self.numFiles = numFiles
        self.binFileSize = binFileSize
        self.binRandomSeed = int( time.time() ) if binRandomSeed is None else binRandomSeed
        self.fileTypes = fileTypes
        self.repairFromPeerIfMissingUrlPatterns = repairFromPeerIfMissingUrlPatterns
        self.pluginId = Simulated_AU.SIMULATED_PLUGIN
        self.auId = "%s&root~%s" % ( self.pluginId.replace( '.', '|' ), self.root )
        self.title = "Simulated Content: " + root
        self.baseUrl = 'http://www.example.com'

    def expectedUrlCount( self ):

        def bit_count( flags ):
            # http://www.amd.com/us-en/assets/content_type/white_papers_and_tech_docs/25112.PDF
            flags = flags - ( flags >> 1 & 0x55555555 )
            flags = ( flags & 0x33333333 ) + ( flags >> 2 & 0x33333333 )
            return ( flags + ( flags >> 4 ) & 0x0f0f0f0f )*0x01010101 >> 24

        # There is an index for each branch and the top level, plus the initial URL
        return self.numFiles*bit_count( self.fileTypes )*( self.branch + max( 1, self.depth ) ) + self.branch + 1


hash_locks = {}
frameworkCount = 0


if __name__ == "__main__":
    import doctest
    doctest.testmod()
