import base64, glob, httplib, os, random, shutil, signal
import sys, time, types, urllib, urllib2
from os import path
from xml.dom import minidom

# Constants

DEF_TIMEOUT = 60 * 30  # 30 minute default timeout for waits.
DEF_SLEEP = 10         # 10 second default sleep between loops.

# Statics

frameworkCount = 0

# Classes

class Framework:
    """
    A framework is a set of LOCKSS daemons and associated test
    clients.  The framework daemons can be started or stopped as a
    group, and their status can be accessed by the clients the
    framework creates and returns at initialization time.

    To use a framework, it should be created by passing it two paths:
    A path to a LOCKSS build environment (where lockss.jar and
    lockss-dev.jar can be found), and a work directory in which the
    daemons will be run.  Optionally, host name and port numbers for
    each daemon can be passed in.  If not supplied, these default to
    'localhost' and ports 8081 through 8081+n, where 'n' is the number
    of daemons that have been created.

    When the framework is created, it returns a tuple of associated
    clients, one per daemon.  These clients are used to perform
    functional test interactions with the daemons.
    """
    def __init__(self, config):
        wd = path.abspath(config.get('workDir', './'))
        self.frameworkDir = self.__makeTestDir(wd)
        self.projectDir = config.get('projectDir')
        self.daemonCount = int(config.get('daemonCount', 1))
        self.startPort = int(config.get('startPort', 8081))
        self.username = config.get('username', 'polltestuser')
        self.password = config.get('password', 'polltestpass')
        self.debugLevel = config.get('debugLevel', 'debug')
        self.hostname = config.get('hostname', 'localhost')

        self.clientList = [] # ordered list of clients.
        self.daemonList = [] # ordered list of daemons.
        self.configCount = 0 # used when writing daemon properties
        self.isRunning = False

        if self.projectDir == None:
            # will raise LockssError if not found.
            self.projectDir = self.__findProjectDir()

        # Assert that the project directory exists and that
        # the necessary libraries exist.
        if not self.__isProjectDirReady():
            raise LockssError("Project dir %s is not ready." % self.projectDir)

        # write the framework global config file.
        globalConfigFile = path.join(self.frameworkDir, 'lockss.txt')
        self.__writeLockssConfig(globalConfigFile, self.debugLevel)

        # write the 'extra' config file.  This may be empty if there
        # are no LOCKSS daemon properties defined, but that's OK.
        extraConfigFile = path.join(self.frameworkDir, 'lockss.opt')
        self.__writeExtraConfig(extraConfigFile, config)

        # Set up a each daemon and create a work directory for it.
        for port in range(self.startPort, self.startPort + self.daemonCount):
            url = 'http://' + self.hostname + ':' + str(port)
            daemonDir = path.abspath(path.join(self.frameworkDir,
                                               'daemon-' + str(port)))
            # create client
            client = Client(daemonDir, url, self.username, self.password)
            # local config
            localConfigFile = path.join(daemonDir, 'local.txt')
            # Init the directory
            os.mkdir(daemonDir)
            # write the daemon-specific config file
            self.__writeLocalConfig(localConfigFile, daemonDir, port)
            # Create daemon
            daemon = LockssDaemon(daemonDir, self.__makeClasspath(),
                                  (globalConfigFile, localConfigFile,
                                   extraConfigFile))
            # Add client and daemon to their lists
            self.clientList.append(client)
            self.daemonList.append(daemon)

    def start(self):
        if self.isRunning:
            raise LockssError("Test framework is already started.")

        cp = self.__makeClasspath()
        for daemon in self.daemonList:
            daemon.start()

        self.isRunning = True

    def stop(self):
        if not self.isRunning:
            raise LockssError("Daemon is not running.")

        for daemon in self.daemonList:
            daemon.kill()

        self.isRunning = False

    def clean(self):
        shutil.rmtree(self.frameworkDir)

    def __makeClasspath(self):
        cp = []
        testCpFile = path.join(self.projectDir, 'test', 'test-classpath')
        f = open(testCpFile)
        for line in f.readlines():
            cp.append(line)
        f.close()

        return os.pathsep.join(cp)

    def __findProjectDir(self):
        """ Walk up the tree until 'build.xml' is found.  Assume this
        is the root of the project. """
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

    def __makeTestDir(self, dir):
        """ Construct the name of a directory under the top-level work
        directory in which to create the framework.  This allows each
        functional test to work in its own directory, and for all the test
        results to be left for examination if deleteAfter is false. """
        global frameworkCount
        frameworkCount += 1
        fwDir = path.join(dir, 'testcase-' + str(frameworkCount))
        if path.isdir(fwDir):
            raise LockssError("Directory %s already exists." % fwDir)
        os.mkdir(fwDir)
        return fwDir

    def __writeLockssConfig(self, file, debugLevel):
        f = open(file, 'w')
        # defined at the end of this file
        f.write(globalConfigTemplate % (debugLevel))
        f.close()

    def __writeLocalConfig(self, file, dir, uiPort):
        """ Write local config file for a daemon.  Daemon num is
        assumed to start at 0 and go up to n, for n-1 daemons """
        f = open(file, 'w')
        tr = str(self.configCount + 1)
        # Kluge for proper handling of multiple daemons.  If this is
        # the first dameon, enable the proxy.
        if self.configCount == 0:
            proxyStart = 'yes'
        else:
            proxyStart = 'no'
        ipAddr = tr + '.' + tr + '.' + tr + '.' + tr
        unicastPort = '900%d' % self.configCount
        unicastSendToPort = '9000'
        f.write(localConfigTemplate % {"username": self.username,
                                       "password": self.password,
                                       "dir": dir, "proxyStart": proxyStart,
                                       "uiPort": uiPort, "ipAddr": ipAddr,
                                       "unicastPort": unicastPort,
                                       "unicastSendToPort": unicastSendToPort})
        f.close()
        self.configCount += 1 # no ++ in python, oops.

    def __writeExtraConfig(self, file, config):
        """ Write out any LOCKSS daemon properties. """
        f = open(file, 'w')
        for (key, val) in config.daemonItems():
            f.write('%s=%s\n' % (key, val))
        f.close()

class Client:
    """ Client interface to a test framework. """
    def __init__(self, daemonDir, url, username, password):
        if not url.endswith('/'):
            url = url + '/'
        self.url = url
        self.daemonDir = daemonDir
        self.username = username
        self.password = password

    def createAu(self, au):
        """
        Create a simulated AU.  This will block until the AU appears in
        the daemon's status table.
        """
        post = self.__makeAuPost(au, 'Create')
        post.execute()
        if not self.waitForCreateAu(au):
            raise LockssError("Timed out while waiting for AU %s to appear." % au)

    def deleteAu(self, au):
        """
        Delete a simulated AU.  This will block until the AU no longer
        appears in the daemon's status table.
        """
        post = self.__makeAuPost(au, 'Confirm Delete')
        post.execute()
        if not self.waitForDeleteAu(au):
            raise LockssError("Timed out while waiting for "\
                              "AU %s to be deleted." % au)

    def requestTreeWalk(self, au):
        """ Possibly a poorly named method.  This will merely deactivate
        and then reactivate the specified AU, which may or may not trigger
        a tree walk.  Worst case, it actually pushes the schedule back and
        the tree walk occurs later than it would have.  Best case, a tree walk
        happens in about 10 seconds. """
        self.deactivateAu(au, True)
        self.reactivateAu(au, True)

    def reactivateAu(self, au, doWait=True):
        """
        Re-activate a simulated AU.  If doWait is set, wait for the AU to
        disappear from the daemon status table before returning.
        """
        if self.isActiveAu(au):
            return

        post = self.__makeAuPost(au, 'DoReactivate')
        post.execute()
        if doWait:
            if not self.waitForReactivateAu(au):
                raise LockssError("Timed out while waiting for "\
                                  "AU %s to be reactivated." % au)

    def deactivateAu(self, au, doWait=True):
        """
        Deactivate a simulated AU.  If doWait is set, wait for the AU to
        disappear from the daemon status table before returning.
        """
        if not self.isActiveAu(au):
            return

        post = self.__makeAuPost(au, 'Confirm Deactivate')
        post.execute()

        if doWait:
            if not self.waitForDeactivateAu(au):
                raise LockssError("Timed out while waiting for "\
                                  "AU %s to be deactivated." % au)

    def getCrawlStatus(self, au=None):
        """
        Return the current crawl status of this cache.
        """
        key = None
        if not au == None:
            key = au.getAuId()

        return self.__getStatusTable('crawl_status_table', key)

    def getAuRepository(self, au):
        """ RepositoryStatus table does not accept key, so loop through it until
        the corresponding au is found. """
        auid = au.getAuId()
        table = self.__getStatusTable('RepositoryTable')
        for row in table:
            auRef = row['au']
            if auRef['key'] == auid:
                d = row['dir']
                repo = path.abspath(path.join(self.daemonDir, d))
                return repo

        ## Wasn't found.
        return None

    def getAuRoot(self, au):
        """ Return the full path to the AU's root.  This is used by methods
        that damage, create, or delete repository files behind the daemon's
        back.  """
        repository = self.getAuRepository(au)
        return path.join(repository, au.dirStruct)

    def hasAu(self, au):
        """ Return true iff the status table lists the given au. """
        auid = au.getAuId()
        tab = self.__getStatusTable('AuIds')
        for row in tab:
            if row["AuId"] == auid:
                return True
        # au wasn't found
        return False

    def isActiveAu(self, au):
        """ Return true iff the au is activated."""
        auid = au.getAuId()
        tab = self.__getStatusTable('RepositoryTable')
        for row in tab:
            status = row["status"]
            auRef = row["au"]
            # Repository status table may report with two types,
            # a <reference> for 'au', or a string for 'au'.  We want to
            # ignore the string versions.
            if isinstance(auRef, types.DictType):
                if au.getAuId() == auRef['key']:
                    return (status == "Active")
            else:
                continue

        # au wasn't found
        return False

    def isAuDamaged(self, au, node=None):
        table = self.__getStatusTable('ArchivalUnitTable', au.getAuId())
        if not node:
            node = self.getAuNode(au, 'lockssau')
        url = node.url
        for row in table:
            rowUrl = row['NodeName']
            if not row.has_key('NodeStatus'):
                continue
            if url == rowUrl:
                return row['NodeStatus'] == "Damaged"
        # au wasn't found.
        return False

    def isAuRepaired(self, au):
        table = self.__getStatusTable('ArchivalUnitTable', au.getAuId())
        node = self.getAuNode(au, 'lockssau')
        url = node.url
        for row in table:
            rowUrl = row['NodeName']
            if not row.has_key('NodeStatus'):
                continue
            if url == rowUrl:
                return (not row['NodeStatus'] == "Damaged") and \
                       (not row['NodeStatus'] == "Repairing")

        # au wasn't found.
        return False

    def isContentRepaired(self, au, node):
        """ Return true if the AU has been repaired by a SNCUSS poll """
        tab = self.getAuPolls(au)
        for row in tab:
            if not row.has_key('Range'):
                continue
            if not (row['PollType'] == 'C' and row['Range'] == 'single node'):
                continue
            rowUrl = row['URL']
            rowStatus = row['Status']
            if rowUrl == node.url:
                return rowStatus == 'Repaired'
        # Poll wasn't found
        return False

    def isNameRepaired(self, au, node=None):
        """ Return true if the AU has been repaired by non-ranged name poll """
        tab = self.getAuPolls(au)
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
        """ Return true if the AU has been repaired by a ranged name poll """
        tab = self.getAuPolls(au)
        for row in tab:
            rowPollType = row['PollType']
            # some rows do not have a range.
            if not (row.has_key('Range') and rowPollType == 'N'):
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

    def getAuPolls(self, au, activeOnly=False):
        """ Return the full poll status of an AU """
        key = 'AU~%s' % urllib.quote(au.getAuId()) # requires a pre-quoted key
        if activeOnly:
            key = key + '&Status~Active'
        return self.__getStatusTable('PollManagerTable', key)

    def getAuNodesWithContent(self, au):
        """ Return a list of all nodes that have content. """
        # Hack!  Pass the table a very large number for numrows, and
        # pray we don't have more than that.
        tab = self.__getStatusTable('ArchivalUnitTable',
                                    key=au.getAuId(),
                                    numrows=100000)
        nodes = []
        for row in tab:
            url = row['NodeName']
            if row.has_key('NodeContentSize') \
                   and not row['NodeContentSize'] == '-':
                nodes.append(self.getAuNode(au, url))
        return nodes

    def isFrameworkReady(self):
        """ Simply try to get the AU status.  If it responds, return
        true.  If it raises any kind of error, return False.  Not the
        best implementation in the world, but such is life."""
        try:
            self.__getStatusTable('ArchivalUnitStatusTable')
            return True
        except LockssError, e:
            ## If a Lockss error is raised, pass it on.
            raise e
        except Exception, e:
            ## On any other error, just return false.
            return False

    def hasTopLevelContentPoll(self, au):
        """ Return true if the client has an active or won top level
        content poll """
        tab = self.getAuPolls(au)
        for row in tab:
            if not row['PollType'] == 'C':
                continue
            if row['URL'] == 'lockssau:':
                return (row['Status'] == 'Active' or row['Status'] == 'Won')
        # Poll wasn't found
        return False

    def hasTopLevelNamePoll(self, au):
        """ Wait for a top level name poll to be called. """
        tab = self.getAuPolls(au)
        for row in tab:
            if not row['PollType'] == 'N':
                continue
            if row['URL'] == 'lockssau:':
                return (row['Status'] == 'Active' or row['Status'] == 'Won')
        # Poll wasn't found
        return False

    def hasSNCUSSPoll(self, au, node):
        """ Wait for a Single-Node CUSS poll for the given AU and node. """
        tab = self.getAuPolls(au)
        for row in tab:
            if not (row['PollType'] == 'C' and row.has_key('Range')):
                continue
            if row['URL'] == node.url:
                return row['Range'] == 'single node' and \
                       (row['Status'] == 'Active' or row['Status'] == 'Won')
        # Poll wasn't found
        return False

    def hasLostTopLevelContentPoll(self, au):
        """ Return true if a top level content poll has been lost. """
        tab = self.getAuPolls(au)
        for row in tab:
            if not row['PollType'] == 'C':
                continue
            if row['URL'] == 'lockssau:':
                return row['Status'] == 'Lost'
        # Poll wasn't found
        return False

    def hasWonTopLevelContentPoll(self, au):
        """ Return true if a top level content poll has been won. """
        tab = self.getAuPolls(au)
        for row in tab:
            if not row['PollType'] == 'C':
                continue
            if row['URL'] == 'lockssau:':
                return row['Status'] == 'Won'
        # Poll wasn't found
        return False

    def hasLostTopLevelNamePoll(self, au):
        """ Wait for a top level name poll to be lost. """
        tab = self.getAuPolls(au)
        for row in tab:
            if not row['PollType'] == 'N':
                continue
            if row['URL'] == 'lockssau:':
                return row['Status'] == 'Lost'
        # Poll wasn't found
        return False

    def hasWonTopLevelNamePoll(self, au):
        """ Wait for a top level name poll to be won. """
        tab = self.getAuPolls(au)
        for row in tab:
            if not row['PollType'] == 'N':
                continue
            if row['URL'] == 'lockssau:':
                return row['Status'] == 'Won'
        # Poll wasn't found
        return False

    def hasNamePoll(self, au, node):
        """ Wait for a name poll to run on a given node (won or active) """
        tab = self.getAuPolls(au)
        for row in tab:
            if not row['PollType'] == 'N':
                continue
            if row['URL'] == node.url:
                return (row['Status'] == 'Active' or row['Status'] == 'Won')
        # Poll wasn't found
        return False


    def hasLostNamePoll(self, au, node):
        """ Wait for a name poll to be won on the given node """
        if not node:
            url = au.baseUrl
        else:
            url = node.url
        tab = self.getAuPolls(au)
        for row in tab:
            if row.has_key('Range') or not row['PollType'] == 'N':
                continue
            if row['URL'] == url:
                return row['Status'] == 'Lost'
        # Poll wasn't found
        return False

    def hasWonNamePoll(self, au, node):
        """ Wait for a name poll to be won on the given node """
        if not node:
            url = au.baseUrl
        else:
            url = node.url
        tab = self.getAuPolls(au)
        for row in tab:
            if row.has_key('Range') or not row['PollType'] == 'N':
                continue
            if row['URL'] == url:
                return row['Status'] == 'Won'
        # Poll wasn't found
        return False

    def hasRangedNamePoll(self, au, node):
        """ Wait for a ranged name poll on the given node. """
        if not node:
            url = au.baseUrl
        else:
            url = node.url
        tab = self.getAuPolls(au)
        for row in tab:
            if not (row['PollType'] == 'N' and row.has_key('Range')):
                continue
            if row['URL'] == url:
                return row['Status'] == 'Won' or row['Status'] == 'Active'
        # Poll wasn't found
        return False

    def hasLostRangedNamePoll(self, au, node):
        """ Wait for a ranged name poll to be lost on the given node """
        if not node:
            url = au.baseUrl
        else:
            url = node.url
        tab = self.getAuPolls(au)
        for row in tab:
            if not (row['PollType'] == 'N' and row.has_key('Range')):
                continue
            if row['URL'] == url:
                return row['Status'] == 'Lost'
        # Poll wasn't found
        return False

    def hasWonRangedNamePoll(self, au, node):
        """ Wait for a ranged name poll to be won on the given node """
        if not node:
            url = au.baseUrl
        else:
            url = node.url
        tab = self.getAuPolls(au)
        for row in tab:
            if not (row['PollType'] == 'N' and row.has_key('Range')):
                continue
            if row['URL'] == url:
                return row['Status'] == 'Won'
        # Poll wasn't found
        return False

    def hasLostSNCUSSPoll(self, au, node):
        """ Wait for a Single-Node CUSS poll for the given AU and node to be lost """
        tab = self.getAuPolls(au)
        for row in tab:
            if not (row['PollType'] == 'C' and row.has_key('Range')):
                continue
            if row['URL'] == node.url:
                return row['Range'] == 'single node' and \
                       row['Status'] == 'Lost'
        # Poll wasn't found
        return False

    def hasWonSNCUSSContentPoll(self, au, node):
        """ Wait for a Single-Node CUSS poll for the given AU and node to be won """
        tab = self.getAuPolls(au)
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
    def waitForCreateAu(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        """ Wait for the au to appear, or for the timeout to expire."""
        def waitFunc():
            return self.hasAu(au)
        return wait(waitFunc, timeout, sleep)

    def waitForDeleteAu(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        """ Wait for the au to be deleted, or for the timeout to
        expire. """
        def waitFunc():
            return not self.hasAu(au)
        return wait(waitFunc, timeout, sleep)

    def waitForReactivateAu(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        """ Wait for the au to be activated, or for the timeout to
        expire. """
        def waitFunc():
            return self.isActiveAu(au)
        return wait(waitFunc, timeout, sleep)

    def waitForDeactivateAu(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        """ Wait for the au to be deactivated, or for the timeout to
        expire."""
        def waitFunc():
           return not self.isActiveAu(au)
        return wait(waitFunc, timeout, sleep)

    def waitForSuccessfulCrawl(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        """ Block until the specified au has had at least one
        successful new content crawl."""
        def waitFunc():
            tbl = self.getCrawlStatus(au)
            return tbl and tbl[0]['crawl_status'] == 'Successful'
        return wait(waitFunc, timeout, sleep)

    def waitForTopLevelContentPoll(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        def waitFunc():
            return self.hasTopLevelContentPoll(au)
        return wait(waitFunc, timeout, sleep)

    def waitForTopLevelNamePoll(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        def waitFunc():
            return self.hasTopLevelNamePoll(au)
        return wait(waitFunc, timeout, sleep)

    def waitForSNCUSSContentPoll(self, au, node, timeout=DEF_TIMEOUT,
                                 sleep=DEF_SLEEP):
        def waitFunc():
            return self.hasSNCUSSPoll(au, node)
        return wait(waitFunc, timeout, sleep)

    def waitForLostTopLevelContentPoll(self, au, timeout=DEF_TIMEOUT,
                                       sleep=DEF_SLEEP):
        """ Block until a top level content poll is lost. """
        def waitFunc():
            return self.hasLostTopLevelContentPoll(au)
        return wait(waitFunc, timeout, sleep)

    def waitForLostTopLevelNamePoll(self, au, timeout=DEF_TIMEOUT,
                                    sleep=DEF_SLEEP):
        """ Block until a top level name poll is lost. """
        def waitFunc():
            return self.hasLostTopLevelNamePoll(au)
        return wait(waitFunc, timeout, sleep)

    def waitForLostNamePoll(self, au, node, timeout=DEF_TIMEOUT,
                                    sleep=DEF_SLEEP):
        """ Block until a node name poll is lost. """
        def waitFunc():
            return self.hasLostNamePoll(au, node)
        return wait(waitFunc, timeout, sleep)


    def waitForLostSNCUSSContentPoll(self, au, node, timeout=DEF_TIMEOUT,
                                     sleep=DEF_SLEEP):
        """ Block until a top level name poll is lost. """
        def waitFunc():
            return self.hasLostSNCUSSContentPoll(au, node)
        return wait(waitFunc, timeout, sleep)

    def waitForWonTopLevelContentPoll(self, au, timeout=DEF_TIMEOUT,
                                       sleep=DEF_SLEEP):
        """ Block until a top level content poll is won. """
        def waitFunc():
            return self.hasWonTopLevelContentPoll(au)
        return wait(waitFunc, timeout, sleep)

    def waitForWonTopLevelNamePoll(self, au, timeout=DEF_TIMEOUT,
                                    sleep=DEF_SLEEP):
        """ Block until a top level name poll is won. """
        def waitFunc():
            return self.hasWonTopLevelNamePoll(au)
        return wait(waitFunc, timeout, sleep)

    def waitForNamePoll(self, au, node, timeout=DEF_TIMEOUT,
                        sleep=DEF_SLEEP):
        """ Block until a node level name poll is run (active or won) """
        def waitFunc():
            return self.hasNamePoll(au, node)
        return wait(waitFunc, timeout, sleep)

    def waitForWonNamePoll(self, au, node, timeout=DEF_TIMEOUT,
                           sleep=DEF_SLEEP):
        """ Block until a node level name poll is won. """
        def waitFunc():
            return self.hasWonNamePoll(au, node)
        return wait(waitFunc, timeout, sleep)

    def waitForWonSNCUSSContentPoll(self, au, node, timeout=DEF_TIMEOUT,
                                     sleep=DEF_SLEEP):
        """ Block until a SNCUSS content poll is won. """
        def waitFunc():
            return self.hasWonSNCUSSContentPoll(au,  node)
        return wait(waitFunc, timeout, sleep)

    def waitForRangedNamePoll(self, au, node, timeout=DEF_TIMEOUT,
                              sleep=DEF_SLEEP):
        """ Block until a ranged name poll has occured (active or won)"""
        def waitFunc():
            return self.hasRangedNamePoll(au, node)
        return wait(waitFunc, timeout, sleep)

    def waitForWonRangedNamePoll(self, au, node, timeout=DEF_TIMEOUT,
                                 sleep=DEF_SLEEP):
        def waitFunc():
            return self.hasWonRangedNamePoll(au, node)
        return wait(waitFunc, timeout, sleep)

    def waitForLostRangedNamePoll(self, au, node, timeout=DEF_TIMEOUT,
                                  sleep=DEF_SLEEP):
        def waitFunc():
            return self.hasLostRangedNamePoll(au, node)
        return wait(waitFunc, timeout, sleep)

    def waitForTopLevelDamage(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        """ Block until an au is marked 'Damaged' at the top level (lockssau:). """
        def waitFunc():
            return self.isAuDamaged(au, None)
        return wait(waitFunc, timeout, sleep)

    def waitForDamage(self, au, node, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        """ Block until a node is marked 'repairing', or until the
        timeout expires. """
        def waitFunc():
            return self.isAuDamaged(au, node)
        return wait(waitFunc, timeout, sleep)

    def waitForTopLevelRepair(self, au, timeout=DEF_TIMEOUT,
                              sleep=DEF_SLEEP):
        """ Block until the top-level of the AU is marked repaired. """
        def waitFunc():
            return self.isAuRepaired(au, None)
        return wait(waitFunc, timeout, sleep)

    def waitForContentRepair(self, au, node, timeout=DEF_TIMEOUT,
                             sleep=DEF_SLEEP):
        """ Block until a node has been successfully repaired ,or until
        the timeout expires."""
        def waitFunc():
            return self.isContentRepaired(au, node)
        return wait(waitFunc, timeout, sleep)

    def waitForNameRepair(self, au, node=None, timeout=DEF_TIMEOUT,
                          sleep=DEF_SLEEP):
        """ Block until a node has been successfully repaired, or until
        the timeout expires.  If 'node' is None, this will just wait until
        the base URL has been marked repaired."""
        def waitFunc():
            return self.isNameRepaired(au, node)
        return wait(waitFunc, timeout, sleep)

    def waitForRangedNameRepair(self, au, node=None, timeout=DEF_TIMEOUT,
                                sleep=DEF_SLEEP):
        def waitFunc():
            return self.isRangedNameRepaired(au, node)
        return wait(waitFunc, timeout, sleep)

    def waitForDaemonReady(self, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        """ Block until the framework is ready for client communication """
        def waitFunc():
            return self.isFrameworkReady()
        return wait(waitFunc, timeout, sleep)

    ###
    ### Methods for causing damage
    ###

    def randomDamageSingleNode(self, au):
        node = self.__getRandomNode(au)
        self.damageNode(node)
        return node

    def randomDamageRandomNodes(self, au, minCount=1, maxCount=10):
        """ Damage a random selection of nodes with content for the given au.
        Returns the list of damaged nodes as a tuple. """
        nodeList = self.__getRandomNodeList(au, minCount, maxCount)
        for node in nodeList:
            self.damageNode(node)
        return nodeList

    def randomDelete(self, au):
        node = self.__getRandomNode(au)
        self.deleteNode(node)
        return node

    def randomDeleteRandomNodes(self, au, minCount=1, maxCount=10):
        """ Delete a random selection of nodes with content for the given au.
        Returns the list of damaged nodes as a tuple. """
        nodeList = self.__getRandomNodeList(au, minCount, maxCount)
        for node in nodeList:
            self.deleteNode(node)
        return nodeList

    def damageNode(self, node):
        # Only want to damage the file contents
        file = node.file
        fullPath = path.join(file, '#content', 'current')
        if path.isfile(fullPath):
            f = open(fullPath, 'a')
            f.write('*** DAMAGE ***')
            f.close()
        else:
            raise LockssError("File does not exist: %s" % f)

    def deleteNode(self, node):
        f = node.file
        if path.isfile(f):
            os.unlink(f)
        elif path.isdir(f):
            shutil.rmtree(f)
        else:
            raise LockssError("File does not exist: %s" % f)

    def createFile(self, au, filespec):
        """ Create an extra file in the repository under the given AU and
        filespec.  The filespec should be relative to the AU's root, for example
        'foo/bar' would attempt to create the file
        /path/to/au/repository/www.example.com/http/foo/bar """
        root = self.getAuRoot(au)
        output = path.join(root, filespec)

        if path.isfile(output):
            raise LockssError("File already exists: %s" % filespec)
        f = open(output, 'w')
        f.write('Garbage File')
        f.close()
        return output

    def createNode(self, au, filespec):
        """ Create an extra node with no node properties. """
        root = self.getAuRoot(au)

        nodeRoot = path.join(root, filespec)
        os.mkdir(nodeRoot)
        os.mkdir(path.join(nodeRoot, '#content'))
        contentFile = path.join(nodeRoot, '#content', 'current')
        f = open(contentFile, 'w')
        f.write('Garbage File')
        f.close()

        url = au.baseUrl + '/' + filespec
        return Node(nodeRoot, url)


    def getAuFile(self, au, filespec):
        """ Construct a node from an au and a relative file spec.
        This method will not check to see if the file exists, it will
        simply construct a Node object and return it. """
        root = self.getAuRoot(au)

        f = path.join(root, filespec)


    def getAuNode(self, au, url, checkForContent=False):
        """ Construct a node from a url on an AU """
        root = self.getAuRoot(au)
        ### Kludge for getting "lockssau" node.
        if url == 'lockssau':
            file = root
        else:
            file = path.join(root, url[(len(au.baseUrl) + 1):])

        node = Node(url, file)

        if checkForContent:
            if path.isfile(path.join(file, '#content', 'current')):
                return node
            else:
                raise LockssError("Node has no content: %s" % node.url)
        else:
            if path.isdir(file) or path.isfile(file):
                return node
            else:
                raise LockssError("Node does not exist: %s" % node.url)

    ###
    ### Internal methods
    ###

    def __getStatusTable(self, statusTable, key=None, numrows=None):
        """ Given an XML string, parse it as a status table and return
        a list of dictionaries representing the status table.  Each
        item in the list is a dictionary of column names to values,
        stored as Unicode strings."""

        post = self.__makePost('DaemonStatus')
        post.add('table', statusTable)
        if not key == None:
            post.add('key', key)
        if not numrows == None:
            post.add('numrows', numrows)
        post.add('output', 'xml')

        doc = minidom.parseString(post.execute().read())
        doc.normalize() # required for python 2.2
        rowList = doc.getElementsByTagName('st:row')

        data = []
        for row in rowList:
            dict = {}
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
                        dict[colName] = {'name': name,
                                         'key': key,
                                         'value': value}
                    else:
                        dict[colName] = colVal.firstChild.data
                except IndexError:
                    # Unlikely to happen, but just in case...
                    continue
            data.append(dict)
        return data

    def __makePost(self, page, lockssAction=None):
        postUrl = self.url + page
        post = Post(postUrl, self.username, self.password)
        if lockssAction:
            post.add('lockssAction', lockssAction)
        return post

    def __makeAuPost(self, au, lockssAction):
        post = self.__makePost('AuConfig', lockssAction)
        post.add('PluginId', au.pluginId)
        post.add('lfp.root', au.root)

        if (not au.depth == -1):
            post.add('lfp.depth', au.depth)
        if (not au.branch == -1):
            post.add('lfp.branch', au.branch)
        if (not au.numFiles == -1):
            post.add('lfp.numFiles', au.numFiles)
        if (not au.binFileSize == -1):
            post.add('lfp.binFileSize', au.binFileSize)
        if (not au.maxFileName == -1):
            post.add('lfp.maxFileName', au.maxFileName)
        if (not au.fileTypes == -1):
            post.add('lfp.fileTypes', au.fileTypes)
        if (not au.oddBranchContent == -1):
            post.add('lfp.odd_branch_content', au.oddBranchContent)
        if (not au.badFileLoc == None):
            post.add('lfp.badFileLoc', au.badFileLoc)
        if (not au.badFileNum == -1):
            post.add('lfp.badFileNum', au.badFileNum)
        post.add('auid', au.getAuId())

        return post

    def __getRandomNode(self, au):
        ### Raise an error if the AU hasn't been created or
        ### crawled.
        repository = self.getAuRepository(au)
        if repository == None:
            raise LockssError("No repository for au: %s" % au)

        ### Randomly select a node to damage
        random.seed(time.time())
        nodeList = self.getAuNodesWithContent(au)
        randomNode = nodeList[random.randint(0, len(nodeList) - 1)]

        return randomNode

    def __getRandomNodeList(self, au, minCount, maxCount):
        """ Return a randomly sized list of nodes with content,
        between min and max in length. If there are fewer than 'max'
        nodes, this will return a list between 'min' and
        'len(allnodes)' """
        nodes = self.getAuNodesWithContent(au)
        maxCount = min(maxCount, len(nodes))

        random.seed(time.time())
        returnListLength = random.randint(minCount, maxCount)

        # Build a list of nodes.
        ix = 0
        returnList = []
        while ix < returnListLength:
            random.seed(time.time())
            node = nodes[random.randint(0, len(nodes) - 1)]
            if node in returnList:
                continue
            returnList.append(node)
            ix += 1

        return returnList

    def __str__(self):
        return "%s" % self.url

class SimulatedAu:
    """ A Python abstraction of a SimulatedPlugin ArchivalUnit for use
    in the functional test framework. """
    def __init__(self, root, depth=-1, branch=-1, numFiles=-1,
                 binFileSize=-1, maxFileName=-1, fileTypes=-1,
                 oddBranchContent=-1, badFileLoc=None, badFileNum=-1):
        self.root = root
        self.depth = depth
        self.branch = branch
        self.numFiles = numFiles
        self.binFileSize = binFileSize
        self.maxFileName = maxFileName
        self.fileTypes = fileTypes
        self.oddBranchContent = oddBranchContent
        self.badFileLoc = badFileLoc
        self.badFileNum = badFileNum
        self.baseUrl = 'http://www.example.com'
        self.dirStruct = path.join('www.example.com', 'http')
        self.pluginId = 'org.lockss.plugin.simulated.SimulatedPlugin'

    def __str__(self):
        return "Simulated Content: " + self.root

    def setRoot(self, root):
        self.root = root

    def setDepth(self, depth):
        self.depth = depth

    def setBranch(self, branch):
        self.branch = branch

    def setNumFiles(self, numFiles):
        self.numFiles = numFiles

    def setBinFileSize(self, binFileSize):
        self.binFileSize = binFileSize

    def setMaxFileName(self, maxFileName):
        self.maxFileName = maxFileName

    def setFileTypes(self, fileTypes):
        self.fileTypes = fileTypes

    def setOddBranchCount(self, oddBranchContent):
        self.oddBranchContent = oddBranchContent

    def setBadFileLoc(self, badFileLoc):
        self.badFileLoc = badFileLoc

    def setBadFileNum(self, badFileNum):
        self.badFileNum = badFileNum

    def getAuId(self):
        auIdKey = self.pluginId.replace('.', '|')
        return "%s&root~%s" % (auIdKey, self.root)

class Node:
    """ In keeping with python written in a Java accent, here's a
    simple object that could just as easily have been a tuple or a
    dict. """
    def __init__(self, url, file):
        self.url = url
        self.file = file

    def __str__(self):
        return "%s" % self.url

class LockssDaemon:
    def __init__(self, dir, cp, configList):
        self.dir = dir
        self.cp = cp
        self.configList = configList
        if not os.environ.has_key('JAVA_HOME'):
            raise LockssError("JAVA_HOME must be set.")
        javaHome = os.environ['JAVA_HOME']
        self.javaBin = path.join(javaHome, 'bin', 'java')

    def start(self):
        cmd = '%s -cp %s -Dorg.lockss.defaultLogLevel=debug '\
              'org.lockss.app.LockssDaemon %s > %s/test.out 2>&1 & '\
              'echo $! > %s/dpid'\
              % (self.javaBin, self.cp, ' '.join(self.configList), self.dir, self.dir)
        os.system(cmd)
        self.pid = open(path.join(self.dir, 'dpid'), 'r').readline()

    def kill(self):
        os.kill(int(self.pid), signal.SIGTERM)


class LockssError(Exception):
    """ Base class for daemon exceptions """
    def __init__(self, msg):
        Exception.__init__(self, msg)

class Post:
    """ A simple wrapper for HTTP post management. """
    def __init__(self, url='', username=None, password=None):
        self.__request = urllib2.Request(url)
        if not username == None and not password == None:
            # Attempt to set authentication
            encoded = base64.encodestring('%s:%s' % (username, password))[:-1]
            authheader = "Basic %s" % encoded
            self.__request.add_header("Authorization", authheader)
        self.__postData = {}

    def add(self, name, val):
        self.__postData[name] = val

    def execute(self):
        """ Send a POST with the previously constructed form data,
        and return the contents of the file. """
        args = urllib.urlencode(self.__postData)
        opener = urllib2.build_opener()
        return opener.open(self.__request, args)


###########################################################################
###
### Utility and miscellaneous classes and functions.
###
###########################################################################

class Config:
    """ A safe wrapper around a dictionary.  Handles KeyErrors by
    returning None values. """
    def __init__(self):
        self.dict = {}

    def put(self, prop, val):
        self.dict[prop] = val

    def get(self, prop, default=None):
        try:
            return self.dict[prop]
        except KeyError:
            return default

    ##
    ## Set views
    ##
    def items(self):
        """ Return the entire set of properties. """
        return dict.items()

    def testItems(self):
        """ Return only the testsuite properties. (anything
        with a key not starting with 'org.lockss') """
        retDict = {}
        for (key, val) in self.dict.items():
            if not key.startswith('org.lockss'):
                retDict[key] = val
        return retDict.items()

    def daemonItems(self):
        """ Return only the daemon config properties. (anything
        with a key starting with 'org.lockss') """
        retDict = {}
        for (key, val) in self.dict.items():
            if key.startswith('org.lockss'):
                retDict[key] = val
        return retDict.items()



def loadConfig(f):
    """ Return a dictionary representing a property file. """
    fd = open(f)
    config = Config()
    inMultiline = False

    while True:
        line = fd.readline()
        if not line:
            break

        line = line.replace('\r', '').replace('\n', '')
        line = line.strip()

        # Ignore comments and blank lines.
        if line.startswith('#') or line == '':
            continue

        if line.endswith('\\'):
            inMultiline = True

        if line.find('=') > -1:
            (key, val) = line.split('=')
            # Allow comments on the same line, then strip and
            # clean the value.
            val = val.split('#')[0].replace('\\', '').strip()
            if inMultiline:
                continue
        elif inMultiline:
            # Last line of the multi-line?
            if not line.endswith('\\'):
                inMultiline = False
            line = line.replace('\\', '').strip()
            val = val + line
        else:
            # Not a comment or a multiline or a proper key=val pair,
            # ignore
            continue

        key = key.strip()

        if key and val:
            config.put(key, val)

    fd.close()
    return config


def wait(condFunc, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
    """ Given a function to evaluate, loop until the function evals to
    true, or until the timeout has expired. """
    start = time.time()
    while(time.time() - start) < timeout:
        if condFunc():
            return True
        else:
            time.sleep(sleep)
    # fall out, condition wasn't met.
    return False

def log(msg):
    """ Write to stdout with a timestamp """
    now = time.time()
    t = time.strftime("%H:%M:%S", time.localtime(time.time()))
    msec = int((now%1.0) * 1000)
    mmm = "%03d" % msec
    timestamp = t + '.' + mmm
    sys.stdout.write("%s: %s\n" % (timestamp, msg))
    sys.stdout.flush()


###########################################################################
##
## Config file strings, used when creating default config files.
##
###########################################################################

globalConfigTemplate = """\
# LOCKSS & LCAP tuning parameters
org.lockss.log.default.level=%s

#lockss config stuff
org.lockss.platform.diskSpacePaths=./

org.lockss.config.reloadInterval=30m

#comm settings
org.lockss.ui.start=yes
org.lockss.proxy.start=no
org.lockss.comm.multicast.group=239.4.5.6
#org.lockss.comm.multicast.port=3456
org.lockss.comm.multicast.port=localIp
org.lockss.comm.unicast.port=1025
org.lockss.comm.multicast.verify=no

# lcap protocol settings
org.lockss.protocol.ttl=2
org.lockss.protocol.hashAlgorithm=SHA-1

# poll settings
org.lockss.poll.maxpolls=20
org.lockss.poll.quorum=3
org.lockss.poll.agreeVerify=10
org.lockss.poll.disagreeVerify=50
org.lockss.poll.voteMargin=51
org.lockss.poll.trustedWeight=350
org.lockss.poll.namepoll.deadline=5m

org.lockss.poll.contentpoll.min=5m
org.lockss.poll.contentpoll.max=15m

org.lockss.treewalk.initial.estimate=20s
org.lockss.treewalk.interval.min=5m
org.lockss.treewalk.interval.max=10m
org.lockss.treewalk.start.delay=20s
org.lockss.comm.router.beacon.interval=1m
org.lockss.baseau.toplevel.poll.interval.min=5m
org.lockss.baseau.toplevel.poll.interval.max=10m
org.lockss.baseau.toplevel.poll.prob.initial=1.0

org.lockss.metrics.slowest.hashrate = 250
org.lockss.state.recall.delay=5m
"""

localConfigTemplate = """\
org.lockss.platform.ui.username=%(username)s
org.lockss.platform.ui.password=%(password)s
org.lockss.platform.diskSpacePaths=%(dir)s
org.lockss.cache.location=%(dir)s
org.lockss.proxy.start=%(proxyStart)s
org.lockss.ui.port=%(uiPort)s
org.lockss.localIPAddress=%(ipAddr)s
org.lockss.comm.unicast.port=%(unicastPort)s
org.lockss.comm.unicast.sendToPort=%(unicastSendToPort)s
org.lockss.comm.unicast.sendToAddr=127.0.0.1
"""
