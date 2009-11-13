#!/usr/bin/env python
"""This test suite requires at minimum a top-level work directory in which to
build frameworks.  If desired, optional parameters may also be set to change
the default behavior.  See testsuite.props for details."""

import filecmp
import os
import re
import sys
import time
import unittest
import urllib2

sys.path.append( os.path.normpath( os.path.join( os.path.dirname( sys.argv[ 0 ] ), '../lib' ) ) )
import lockss_daemon
import lockss_util
from lockss_util import log


class LockssTestCases( unittest.TestCase ):
    """Astract superclass for all STF test cases"""

    def __init__( self, methodName = 'runTest' ):
        unittest.TestCase.__init__( self, methodName )
        self.delayShutdown = lockss_util.config.get( 'delayShutdown', False )
        self.timeout = int( lockss_util.config.get( 'timeout', 8*60*60 ) )
        # Assert that the workDir exists and is writable
        self.workDir = lockss_util.config.get( 'workDir', './' )
        self.assert_( os.path.isdir( self.workDir ) and os.access( self.workDir, os.W_OK ), 'Work dir %s does not exist or is not writable' % self.workDir )
        self.config_URLs = None
        self.start_UI_port = None

    def _start_framework( self ):
        log.info( 'Starting framework in %s' % self.framework.frameworkDir )
        self.framework.start()
        self.assert_( self.framework.isRunning, 'Framework failed to start' )

    def setUp( self ):
        unittest.TestCase.setUp( self )
        # Log start of test
        log.info( len( self.__doc__ )*'=' )
        log.info( self.__doc__ )
        log.info( len( self.__doc__ )*'-' )
        # Create a framework for the test
        self.framework = lockss_daemon.Framework( self.daemon_count, self.config_URLs, self.start_UI_port )
        # List of daemon clients
        self.clients = self.framework.clientList
        # Enable clean-up after user interruption
        frameworkList.append( self.framework )

    def tearDown( self ):
        # Dump threads and look for deadlocks (independent of success)
        deadlockLogs = self.framework.checkForDeadlock()
        if deadlockLogs:
            log.error( 'Deadlocks detected!' )
            self.fail( 'Failing due to deadlock detection.  Check the following log file(s): ' + ', '.join( deadlockLogs ) )
        else:
            log.info( 'No deadlocks detected' )

        if self.delayShutdown:
            raw_input( '>>> Delaying shutdown.  Press Enter to continue...' )

        log.info( 'Stopping framework' )
        self.framework.stop()
        self.failIf( self.framework.isRunning, 'Framework did not stop' )

        unittest.TestCase.tearDown( self )


class TinyUiTests( LockssTestCases ):
    """Abstract class"""

    def __init__( self, methodName = 'runTest' ):
        LockssTestCases.__init__( self, methodName )
        self.daemon_count = 1
        self.start_UI_port = 8081

    def setUp( self ):
        LockssTestCases.setUp( self )
        self._start_framework()

        # Block return until all clients are ready to go.
        log.info( 'Waiting for framework to become ready' )
        self.tinyUiClient = self.clients[ 0 ]
        time.sleep( 2 )
        self.tinyUiClient.waitForCanConnectToHost( sleep = 2 )

    def runTest( self ):
        HTML = self.tinyUiClient.getAdminUi().read()
        pattern = 'This LOCKSS box \(.*\) has not started because it is unable to load configuration data'
        self.assert_( re.search( pattern, HTML, re.DOTALL | re.MULTILINE ), 'No match for "%s" in\n%s' % ( pattern, HTML ) )
        pattern = "Shouldn't happen"
        self.assertFalse( re.search( pattern, HTML, re.DOTALL | re.IGNORECASE | re.MULTILINE ), 'Unexpected match for "%s"' % pattern )
        self.assert_( re.search( self.expected_pattern, HTML, re.DOTALL | re.MULTILINE ), 'No match for "%s" in\n%s' % ( self.expected_pattern, HTML ) )
        log.info( 'Found "%s"' % self.expected_pattern )


class TinyUiUnknownHostTestCase( TinyUiTests ):
    """Test that config URL with unknown host name gets Tiny UI"""

    def __init__( self, methodName = 'runTest' ):
        TinyUiTests.__init__( self, methodName )
        self.config_URLs = 'http://unknownhost.lockss.org/',
        self.expected_pattern = 'UnknownHostException.*unknownhost\.lockss\.org'


class TinyUiMalformedUrlTestCase( TinyUiTests ):
    """Test that malformed config URL gets Tiny UI"""

    def __init__( self, methodName = 'runTest' ):
        TinyUiTests.__init__( self, methodName )
        self.config_URLs = 'http://x.y:12:13/',
        self.expected_pattern = 'MalformedURLException'


class TinyUiForbiddenTestCase( TinyUiTests ):
    """Test that a forbidden config fetch gets Tiny UI with the proper hint"""

    def __init__( self, methodName = 'runTest' ):
        TinyUiTests.__init__( self, methodName )
        # Relies on the URL returning a 403 with a specially crafted body containing hint text.
        # See HTTPConfigFile.java and http://props.lockss.org:8001/daemon/README
        self.config_URLs = 'http://props.lockss.org:8001/daemon/forbidden.xml',
        self.expected_pattern = '403: Forbidden.*LOCKSS team.*access list'


class TinyUiRefusedTestCase( TinyUiTests ):
    """Test that a refused config connect gets Tiny UI"""

    def __init__( self, methodName = 'runTest' ):
        TinyUiTests.__init__( self, methodName )
        # XXX should find a guaranteed non-listening port (by binding?)
        self.config_URLs = 'http://127.0.0.1:%i/' % lockss_util.unused_port(),
        self.expected_pattern = 'ConnectException:.*Connection refused'


class TinyUiFileNotFoundTestCase( TinyUiTests ):
    """Test a config file not found gets Tiny UI"""

    def __init__( self, methodName = 'runTest' ):
        TinyUiTests.__init__( self, methodName )
        self.config_URLs = '/no/such/file/or/directory',
        self.expected_pattern = 'FileNotFoundException'


class V3TestCases( LockssTestCases ):

    def __init__( self, methodName = 'runTest' ):
        LockssTestCases.__init__( self, methodName )
        # V3 has a much shorter default timeout of 8 minutes
        self.timeout = int( lockss_util.config.get( 'timeout', 8*60 ) )
        self.daemon_count = 5
        self.offline_peers = []
        self.local_configuration = {}
        self.simulated_AU_parameters = {}

    def _await_V3_poll_agreement( self ):
        # Expect to see a top level content poll called by all peers
        log.info( 'Waiting for a V3 poll by all simulated caches' )
        for client in self.clients:
            self.assert_( client.waitForV3Poller( self.AU ), 'Never called V3 poll' )
            log.info( 'Client on port %s called V3 poll...' % client.port )
        # Expect each client to have won a top-level v3 poll
        log.info( 'Waiting for all peers to win their polls' )
        for client in self.clients:
            self.assert_( client.waitForWonV3Poll( self.AU, self.timeout ), 'Client on port %s did not win V3 poll' % client.port )
            log.info( 'Client on port %s won V3 poll...' % client.port )

    def _setup_AU( self ):
        self.AU = lockss_daemon.Simulated_AU( **self.simulated_AU_parameters )
        log.info( "Creating simulated AU's" )
        for client in self.clients:
            client.createAu( self.AU )
        for client in self.clients:
            client.waitAu( self.AU )
        log.info( "Waiting for simulated AU's to crawl" )
        for client in self.clients:
            self.assert_( client.waitForSuccessfulCrawl( self.AU ), "AU's did not complete initial crawl" )
        log.info( "AU's completed initial crawl" )

    def _content_matches( self, node ):
        return filecmp.cmp( *( client.getAuNode( self.AU, node.url ).filename() for client in ( self.victim, self.nonVictim ) ) )

    def _ensure_victim_node_deleted( self, node ):
        self.assertFalse( os.path.isfile( node.filename() ), 'File was not deleted: %s' % node.url )

    def _verify_damage( self, nodes ):
        for node in nodes:
            self.assertFalse( self._content_matches( node ), 'Failed to damage AU file: %s' % node.url )
        log.info( 'Damaged the following node(s) on client %s:\n\t\t\t%s' % ( self.victim, '\n\t\t\t'.join( str( node ) for node in nodes ) ) )

    def _await_repair( self, nodes ):
        # Just pause until we have better tests; assumes that repair poll has not yet been completed
        self.assert_( self.victim.waitForV3Repair( self.AU, nodes, self.timeout ), 'Timed out while waiting for V3 repair' )

    def _verify_repair( self, nodes ):
        for node in nodes:
            self.assert_( self._content_matches( node ), 'File not repaired: %s' % node.url )

    def setUp( self ):
        LockssTestCases.setUp( self )
        self.victim = self.clients[ 0 ]
        self.nonVictim = self.clients[ 1 ]

        for client in self.clients:
            extraConf = { 'org.lockss.auconfig.allowEditDefaultOnlyParams': True,
                          'org.lockss.id.initialV3PeerList': ';'.join( [ peer.getV3Identity() for peer in self.clients ] + self.offline_peers ),
                          'org.lockss.platform.v3.identity': client.getV3Identity(),
                          'org.lockss.poll.v3.enableV3Poller': client is self.victim,
                          'org.lockss.poll.v3.enableV3Voter': True }
            extraConf.update( self.local_configuration )
            self.framework.appendLocalConfig( extraConf, client )
        self._start_framework()

        # Block return until all clients are ready to go.
        log.info( 'Waiting for framework to become ready' )
        self.framework.waitForFrameworkReady()

    def runTest( self ):
        self._setup_AU()
        # disable polling?
        nodes = self._damage_AU()
        self._verify_damage( nodes )
        # enable polling?

        log.info( 'Waiting for a V3 poll to be called...' )
        self.assert_( self.victim.waitForV3Poller( self.AU ), 'Timed out while waiting for V3 poll' )
        log.info( 'Successfully called a V3 poll' )

        log.info( 'Waiting for V3 repair...' )
        self._await_repair( nodes )
        self._verify_repair( nodes )
        log.info( 'AU successfully repaired' )


class SimpleDamageV3TestCase( V3TestCases ):
    """Test a basic V3 poll"""

    def __init__( self, methodName = 'runTest' ):
        V3TestCases.__init__( self, methodName )
        self.simulated_AU_parameters = { 'numFiles': 15 }
            
    def _damage_AU( self ):
        return [ self.victim.randomDamageSingleNode( self.AU ) ]
                

class RandomDamageV3TestCase( V3TestCases ):
    """Test a V3 Poll with a random number of damaged AUs"""

    def __init__( self, methodName = 'runTest' ):
        V3TestCases.__init__( self, methodName )
        self.simulated_AU_parameters = { 'depth': 1, 'branch': 1, 'numFiles': 30 }
            
    def _damage_AU( self ):
        return self.victim.randomDamageRandomNodes( self.AU, 30, 50 )
                

class DeleteV3Tests( V3TestCases ):
    """Abstract class"""

    def __init__( self, methodName = 'runTest' ):
        V3TestCases.__init__( self, methodName )
        self.simulated_AU_parameters = { 'numFiles': 15 }

    def _verify_damage( self, nodes ):
        for node in nodes:
            self._ensure_victim_node_deleted( node )
        log.info( 'Deleted the following node(s) on client %s:\n\t\t\t%s' % ( self.victim, '\n\t\t\t'.join( str( node ) for node in nodes ) ) )


class SimpleDeleteV3TestCase( DeleteV3Tests ):
    """Test repair of a missing file"""

    def _damage_AU( self ):
        return [ self.victim.randomDelete( self.AU ) ]
        

class LastFileDeleteV3TestCase( DeleteV3Tests ):
    """Ensure that the deletion of the alphabetically last file in the AU can be repaired"""

    def _damage_AU( self ):
        node = self.victim.getAuNode( self.AU, 'http://www.example.com/index.html' )
        self.victim.deleteNode( node )
        return [ node ]
        

class RandomDeleteV3TestCase( DeleteV3Tests ):
    """Test recovery by V3 from randomly deleted nodes in our cache"""

    def __init__( self, methodName = 'runTest' ):
        DeleteV3Tests.__init__( self, methodName )
        self.simulated_AU_parameters.update( { 'depth': 1, 'branch': 1 } )

    def _damage_AU( self ):
        return self.victim.randomDeleteRandomNodes( self.AU, 5, 15 )


class ExtraFilesV3Tests( V3TestCases ):
    """Abstract class"""

    def __init__( self, methodName = 'runTest' ):
        V3TestCases.__init__( self, methodName )
        self.simulated_AU_parameters = { 'numFiles': 20 }

    def _create_AU_nodes( self, minimum, maximum ):
        # Damage the AU by creating extra nodes
        nodes = self.victim.randomCreateRandomNodes( self.AU, minimum, maximum )
        log.info( 'Created the following nodes on client %s:\n\t\t\t%s' % ( self.victim, '\n\t\t\t'.join( str( node ) for node in nodes ) ) )
        return nodes

    def _verify_damage( self, nodes ):
        pass

    def _await_repair( self, nodes ):
        self.assert_( self.victim.waitForV3Repair( self.AU, [], self.timeout ), 'Timed out while waiting for V3 repair' )

    def _verify_repair( self, nodes ):
        for node in nodes:
            self._ensure_victim_node_deleted( node )


class SimpleExtraFileV3TestCase( ExtraFilesV3Tests ):
    """Test recovery by V3 from an extra node in our cache"""

    def _damage_AU( self ):
        # Damage the AU by creating an extra node
        node = self.victim.createNode( self.AU, '000extrafile.txt' )
        log.info( 'Created file %s on client %s' % ( node.url, self.victim ) )
        return [ node ]


class LastFileExtraV3TestCase( ExtraFilesV3Tests ):
    """Test recovery by V3 from an extra last-file node in our cache"""

    def _damage_AU( self ):
        # Damage the AU by creating an extra node that should sort LAST in the list of CachedUrls
        node = self.victim.createNode( self.AU, 'zzzzzzzzzzzzz.txt' )
        log.info( 'Created file %s on client %s' % ( node.url, self.victim ) )
        return [ node ]


class RandomExtraFileV3TestCase( ExtraFilesV3Tests ):
    """Test recovery by V3 from a random number of extra nodes in our cache"""

    def __init__( self, methodName = 'runTest' ):
        ExtraFilesV3Tests.__init__( self, methodName )
        self.simulated_AU_parameters = { 'depth': 1, 'branch': 1, 'numFiles': 20 }

    def _damage_AU( self ):
        return self._create_AU_nodes( 5, 15 )


class OfflinePeersV3Tests( ExtraFilesV3Tests ):
    """Abstract class"""

    def __init__( self, methodName = 'runTest' ):
        ExtraFilesV3Tests.__init__( self, methodName )
        self.offline_peers = [ 'TCP:[127.0.0.1]:65520', 'TCP:[127.0.0.1]:65521', 'TCP:[127.0.0.1]:65522' ]
        self.local_configuration = { 'org.lockss.poll.v3.maxPollSize': 8,
                                     'org.lockss.poll.v3.minPollSize': 8 }
        self.simulated_AU_parameters = { 'depth': 1, 'branch': 1, 'numFiles': 20 }


class VotersDontParticipateV3TestCase( OfflinePeersV3Tests ):
    """Test a V3 poll where some peers do not participate"""

    def __init__( self, methodName = 'runTest' ):
        OfflinePeersV3Tests.__init__( self, methodName )
        self.local_configuration[ 'org.lockss.poll.v3.quorum' ] = 3
    
    def _damage_AU( self ):
        return self._create_AU_nodes( 5, 15 )


class NoQuorumV3TestCase( OfflinePeersV3Tests ):
    """Be sure a V3 poll with too few participants ends in No Quorum"""

    def __init__( self, methodName = 'runTest' ):
        OfflinePeersV3Tests.__init__( self, methodName )
        self.local_configuration[ 'org.lockss.poll.v3.quorum' ] = 6

    def _damage_AU( self ):
        return self._create_AU_nodes( 3, 7 )

    def _await_repair( self, nodes ):
        log.info( 'Waiting for V3 poll to report no quorum...' )
        self.assert_( self.victim.waitForV3NoQuorum( self.AU ), 'Timed out while waiting for no quorum' )
        log.info( 'AU successfully reported No Quorum' )

    def _verify_repair( self, nodes ):
        peer_agreements = self.victim.getAuRepairerInfo( self.AU, 'HighestPercentAgreement' )
        log.debug2( 'Peer agreements: ' + str( peer_agreements ) )
        for client in self.clients:
            if client != self.victim:
                self.assert_( peer_agreements[ client.getV3Identity() ] > 60, 'No agreement recorded for %s' % client )
    

class TotalLossRecoveryV3TestCase( V3TestCases ):
    """Test repairing a cache under V3 that has lost all its contents"""

    def __init__( self, methodName = 'runTest' ):
        V3TestCases.__init__( self, methodName )
        self.local_configuration = { 'org.lockss.poll.v3.enableV3Poller': True }    # Enable polling on all peers
        self.simulated_AU_parameters = { 'depth': 1, 'branch': 1, 'numFiles': 30 }

    def _setup_AU( self ):
        V3TestCases._setup_AU( self )
        # Allow daemons to record their agreeing peers
        self._await_V3_poll_agreement()

    def _damage_AU( self ):
        nodes = self.victim.getAuNodesWithContent( self.AU )
        log.info( 'Backing up cache configuration on victim cache...' )
        self.victim.backupConfiguration()
        log.info( 'Backed up successfully' )

        self.victim.daemon.stop()
        log.info( 'Stopped daemon running on UI port %s' % self.victim.port )
        self.victim.simulateDiskFailure()
        log.info( 'Deleted entire contents of cache on stopped daemon' )

        # Write a TitleDB entry for the simulated AU so it will be marked 'publisher down' when restored.
        self.framework.appendLocalConfig( { 'org.lockss.auconfig.allowEditDefaultOnlyParams': True,
                                            'org.lockss.title.sim1.journalTitle': 'Simulated Content',
                                            'org.lockss.title.sim1.param.1.key': 'root',
                                            'org.lockss.title.sim1.param.1.value': 'simContent',
                                            'org.lockss.title.sim1.param.2.key': 'depth',
                                            'org.lockss.title.sim1.param.2.value': 0,
                                            'org.lockss.title.sim1.param.3.key': 'branch',
                                            'org.lockss.title.sim1.param.3.value': 0,
                                            'org.lockss.title.sim1.param.4.key': 'numFiles',
                                            'org.lockss.title.sim1.param.4.value': 30,
                                            'org.lockss.title.sim1.param.pub_down.key': 'pub_down',
                                            'org.lockss.title.sim1.param.pub_down.value': True,
                                            'org.lockss.title.sim1.plugin': 'org.lockss.plugin.simulated.SimulatedPlugin',
                                            'org.lockss.title.sim1.title': 'Simulated Content: simContent' }, self.victim )
        time.sleep( 5 ) # Settling time

        self.victim.daemon.start()
        # Wait for the client to come up
        self.assert_( self.victim.waitForDaemonReady(), 'Daemon is not ready' )
        log.info( 'Started daemon running on UI port %s' % self.victim.port)

        return nodes

    def _verify_damage( self, nodes ):
        self.assertFalse( self.victim.hasAu( self.AU ), 'AU still intact' )

        # Restore the backup file
        log.info( 'Restoring cache configuration...' )
        self.victim.restoreConfiguration( self.AU )
        log.info( 'Restored successfully' )

        # These should be equal AU IDs, so both should return true
        self.assert_( self.victim.hasAu( self.AU ) )
        self.assert_( self.victim.isPublisherDown( self.AU ) )
        
    def _await_repair( self, nodes ):
        # Expect to see the AU successfully repaired
        log.info( 'Waiting for successful V3 repair of entire AU' )
        self.assert_( self.victim.waitForV3Repair( self.AU, timeout = self.timeout ), 'AU was not repaired by V3' )


class RepairFromPublisherV3TestCase( V3TestCases ):
    """Ensure that repair from publisher works correctly in V3"""

    def __init__( self, methodName = 'runTest' ):
        V3TestCases.__init__( self, methodName )
        self.local_configuration = { "org.lockss.poll.v3.repairFromCachePercent": 0 }    # NEVER repair from a cache
        self.simulated_AU_parameters = { 'depth': 1, 'branch': 1 }

    def _damage_AU( self ):
        return self.victim.randomDamageRandomNodes( self.AU, 15, 20 )

    def _verify_repair( self, nodes ):
        for node in nodes:
            self.assert_( self.victim.isNodeRepairedFromServerByV3( self.AU, node, True ), 'Node %s was not repaired from the publisher!' % node )
        V3TestCases._verify_repair( self, nodes )


class RepairFromPeerV3Tests( V3TestCases ):
    """Abstract class"""

    def __init__( self, methodName = 'runTest' ):
        V3TestCases.__init__( self, methodName )
        self.timeout = 2*60*60
        self.daemon_count += 1
        # ALWAYS repair from a cache; enable polling on all peers.
        self.local_configuration = { 'org.lockss.poll.minPollAttemptInterval': 10,
                                     'org.lockss.poll.v3.enableV3Poller': False,
                                     'org.lockss.poll.v3.repairFromCachePercent': 100,
                                     'org.lockss.poll.v3.toplevelPollInterval': '1d' }
        self.simulated_AU_parameters = { 'depth': 1, 'branch': 1 }    # Reasonably complex AU for testing
        self.toggle_AU_activation = False

    def _setup_AU( self ):
        self.client_without_AU = self.clients[ -1 ]
        self.clients.remove( self.client_without_AU )
        log.debug( 'Peer without AU: %s' % self.client_without_AU )
        
        V3TestCases._setup_AU( self )
        self.framework.appendExtraConfig( { 'org.lockss.poll.v3.enableV3Poller': True } )
        self.framework.reloadAllConfiguration()

        # Agreement is required from all peers before continuing
        self._await_V3_poll_agreement()

        self.victim_first_poll_key = self.victim.getV3PollKey( self.AU )
        log.debug( "Victim's first poll key: " + self.victim_first_poll_key )
        invitees = self.victim.getV3PollInvitedPeers( self.victim_first_poll_key )
        log.debug( 'invitedPeers: %s' % invitees )
        self.assertFalse( self.victim.getV3Identity() in invitees, 'Victim invited itself' )
        self.assert_( self.client_without_AU.getV3Identity() in invitees, 'Peer without AU not invited in first poll' )

        log.debug( 'victim.getNoAuPeers( self.AU ): %s' % self.victim.getNoAuPeers( self.AU ) )
        self.assertEqual( self.victim.getNoAuPeers( self.AU ), [ self.client_without_AU.getV3Identity() ], 'Peer without AU not recorded' )

    def _update_configuration( self ):
        self.framework.appendLocalConfig( { 'org.lockss.poll.v3.toplevelPollInterval': 10 }, self.victim )
        self.victim.reloadConfiguration()
        if self.toggle_AU_activation:
            log.info( 'Deactivating AU' )
            self.victim.deactivateAu( self.AU )
            log.info( 'Reactivating AU' )
            self.victim.reactivateAu( self.AU )

    def _damage_AU( self ):
        nodes = self.victim.randomDamageRandomNodes( self.AU, 15, 20 )
        self._update_configuration()
        return nodes

    def _verify_damage( self, nodes ):
        V3TestCases._verify_damage( self, nodes )
        
        log.debug( 'victim.getNoAuPeers( self.AU ): %s' % self.victim.getNoAuPeers( self.AU ) )
        self.assertEqual( self.victim.getNoAuPeers( self.AU ), [ self.client_without_AU.getV3Identity() ], 'Peer without AU disappeared!' )

        log.info( 'Waiting for a V3 poll to be called...' )
        # Ignores first victim poll
        self.assert_( self.victim.waitForV3Poller( self.AU, [ self.victim_first_poll_key ] ), 'Timed out while waiting for V3 poll' )
        log.info( 'Successfully called a V3 poll' )

        victim_second_poll_key = self.victim.getV3PollKey( self.AU, self.victim_first_poll_key )
        log.debug( "Victim's second poll key: " + victim_second_poll_key )
        invitees = self.victim.getV3PollInvitedPeers( victim_second_poll_key )
        log.debug( 'invitedPeers: %s' % invitees )
        self.assertFalse( self.client_without_AU.getV3Identity() in invitees, 'Peer without AU invited in 2nd poll' )

    def _verify_repair( self, nodes ):
        # Verify that all repairs came from peers
        for node in nodes:
            self.assert_( self.victim.isNodeRepairedFromServerByV3( self.AU, node, False ), 'Node %s was not repaired from a peer!' % node )
        V3TestCases._verify_repair( self, nodes )        


class RepairFromPeerV3TestCase( RepairFromPeerV3Tests ):
    """Ensure that repairing from a V3 peer works correctly"""

    def __init__( self, methodName = 'runTest' ):
        RepairFromPeerV3Tests.__init__( self, methodName )
        self.local_configuration[ 'org.lockss.poll.pollStarterAdditionalDelayBetweenPolls' ] = '20s'    # XXX


class RepairFromPeerWithDeactivateV3TestCase( RepairFromPeerV3Tests ):
    """Ensure that repairing from a V3 peer after AU deactivate/reactivate works correctly"""

    def __init__( self, methodName = 'runTest' ):
        RepairFromPeerV3Tests.__init__( self, methodName )
        self.local_configuration[ 'org.lockss.poll.pollStarterAdditionalDelayBetweenPolls' ] = '20s'    # XXX
        self.toggle_AU_activation = True


class RepairHugeFromPeerV3TestCase( RepairFromPeerV3Tests ):
    """Ensure that repairing a huge file from a V3 peer works correctly."""
    # This test requires about 35 GB of disk space; a fast machine is recommended.  It is not part of any suite.

    def __init__( self, methodName = 'runTest' ):
        RepairFromPeerV3Tests.__init__( self, methodName )
        self.local_configuration.update( { 'org.lockss.metrics.default.hash.speed': 8000,
                                           'org.lockss.poll.v3.tallyDurationMultiplier': 5,
                                           'org.lockss.poll.v3.voteDurationMultiplier': 6,
                                           'org.lockss.scomm.maxMessageSize': 10000000000 } )
        self.simulated_AU_parameters = { 'numFiles': 1, 'binFileSize': 3*1024*1024*1024 - 1 }

    def _damage_AU( self ):
        node = self.victim.getAuNode( self.AU, 'http://www.example.com/001file.bin' )
        self.victim.damageNode( node )
        self._update_configuration()
        return [ node ]


tinyUiTests = unittest.TestSuite( ( TinyUiUnknownHostTestCase(),
                                    TinyUiMalformedUrlTestCase(),
                                    TinyUiForbiddenTestCase(),
                                    TinyUiRefusedTestCase(),
                                    TinyUiFileNotFoundTestCase() ) )

simpleV3Tests = unittest.TestSuite( ( SimpleDamageV3TestCase(),
                                      SimpleDeleteV3TestCase(),
                                      SimpleExtraFileV3TestCase(),
                                      LastFileDeleteV3TestCase(),
                                      LastFileExtraV3TestCase(),
                                      VotersDontParticipateV3TestCase(),
                                      NoQuorumV3TestCase(),
                                      TotalLossRecoveryV3TestCase(),
                                      RepairFromPublisherV3TestCase(),
                                      RepairFromPeerV3TestCase(),
                                      RepairFromPeerWithDeactivateV3TestCase() ) )

randomV3Tests = unittest.TestSuite( ( RandomDamageV3TestCase(),
                                      RandomDeleteV3TestCase(),
                                      RandomExtraFileV3TestCase() ) )

v3Tests = unittest.TestSuite( ( simpleV3Tests, randomV3Tests ) )

# Release-candidate tests
postTagTests = unittest.TestSuite( ( tinyUiTests, v3Tests ) )


# Load configuration
lockss_util.config.load( 'testsuite.props' )
if os.path.isfile( 'testsuite.opt' ):
    lockss_util.config.load( 'testsuite.opt' )

# Module globals
frameworkList = []
deleteAfterSuccess = lockss_util.config.getBoolean( 'deleteAfterSuccess', True )

try:
    unittest.main( argv = sys.argv[ 0 : 1 ] + [ '-q' ] + sys.argv[ 1 : ] )
except ( KeyboardInterrupt, SystemExit ), exception:
    for framework in frameworkList:
        if framework.isRunning:
            log.info( 'Stopping framework' )
            framework.stop()
    if type( exception ) is SystemExit and not exception.code and deleteAfterSuccess:
        for framework in frameworkList:
            framework.clean()
except Exception, exception:
    log.error( exception )

raise
