#!/usr/bin/env python
"""This test suite requires at minimum a top-level work directory in which to
build frameworks.  If desired, optional parameters may also be set to change
the default behavior.  See testsuite.props for details."""

# $Id$

__copyright__ = '''\
Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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

import filecmp
import os
import re
import sys
import time
import unittest
import urllib2

sys.path.append( os.path.normpath( os.path.join( os.path.dirname( sys.argv[ 0 ] ), '../lib' ) ) )
import fix_auth_failure
import lockss_daemon
import lockss_util
from lockss_util import log


fix_auth_failure.fix_auth_failure()

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
        errorLogs = self.framework.checkForLogErrors()
        if errorLogs:
            log.error( 'Errors detected in log!' )
            self.framework.stop()
            self.fail( 'Failing due to log errors.  Check the following log file(s): ' + ', '.join( errorLogs ) )
        # Dump threads and look for deadlocks (independent of success)
        deadlockLogs = self.framework.checkForDeadlock()
        if deadlockLogs:
            log.error( 'Deadlocks detected!' )
            self.framework.stop()
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
#         self.expected_pattern = '403: Forbidden.*LOCKSS team.*access list'
        self.expected_pattern = '403: Forbidden'


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

    @staticmethod
    def _expected_agreement( numerator, denominator ):
        """Trying to mimic what the daemon does to calculate
        agreement. See doubleToPercent in ArchivalUnitStatus.java."""
        return '%.02f' % ( int ( 10000.0 * numerator / denominator ) / 100.0 )

    def __init__( self, methodName = 'runTest' ):
        LockssTestCases.__init__( self, methodName )
        # V3 has a much shorter default timeout of 8 minutes
        self.timeout = int( lockss_util.config.get( 'timeout', 8*60 ) )
        self.daemon_count = 5
        self.offline_peers = []
        self.local_configuration = {}
        self.simulated_AU_parameters = {}
        self.expected_agreement = '100.00'
        self.expected_voter_agreement = '0.00'
        self.symmetric = False
        self.pop = False

    def _await_V3_poll_agreement( self ):
        # Expect to see a top level content poll called by all peers
        log.info( 'Waiting for a V3 poll by all simulated caches' )
        for client in self.clients:
            log.debug( 'Checking client %s' % client.port);
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
            self.pre_create_client( client )
            client.createAu( self.AU )
        for client in self.clients:
            client.waitAu( self.AU )
        log.info( "Waiting for simulated AU's to crawl" )
        for client in self.clients:
	    log.info( 'Checking client %s for crawl' % client.port);
            self.assert_( client.waitForSuccessfulCrawl( self.AU ), "AU's did not complete initial crawl" )
        log.info( "AU's completed initial crawl" )

    def pre_create_client( self, client ):
        pass

    def _content_matches( self, node ):
        return filecmp.cmp( *( client.getAuNode( self.AU, node.url ).filename() for client in ( self.victim, self.nonVictim ) ) )

    def _ensure_victim_node_deleted( self, node ):
        self.assertFalse( os.path.isfile( node.filename() ), 'File was not deleted: %s' % node.url )

    def _set_expected_agreement_from_damaged( self, damagedNodes ):
        content = self.victim.getAuNodesWithContent( self.AU )
        numerator = len( content ) - len( damagedNodes )
        denominator = len( content )
        self.expected_agreement = self._expected_agreement( numerator, denominator )

    def _verify_damage( self, nodes ):
        for node in nodes:
            self.assertFalse( self._content_matches( node ), 'Failed to damage AU file: %s' % node.url )
        if nodes:
            log.info( 'Damaged the following node(s) on client %s:\n\t\t\t%s' % ( self.victim, '\n\t\t\t'.join( str( node ) for node in nodes ) ) )
        else:
            log.info( 'No nodes damaged on client %s' %  self.victim )

    def _enableVictimPoller( self ):
        self.framework.appendLocalConfig( { 'org.lockss.poll.v3.enableV3Poller': True }, self.victim )
        self.victim.reloadConfiguration()

    def _await_repair( self, nodes ):
        summary = self.victim.getPollSummaryFromKey( self.poll_key )
        # XXX self.assert_( self.victim.waitForV3Poller( self.AU ), 'Timed out while waiting for V3 poll' )
        if (summary[ 'Type' ] != 'Local' ):
            # Just pause until we have better tests; assumes that repair
            # poll has not yet been completed
            self.assert_( self.victim.waitForV3Repair( self.AU, nodes, self.timeout ), 'Timed out while waiting for V3 repair' )

    def _verify_repair( self, nodes ):
        summary = self.victim.getPollSummaryFromKey( self.poll_key )
        if (summary[ 'Type' ] != 'Local' ):
            for node in nodes:
                log.debug('super verify repair of node %s' % node )
                self.assert_( self._content_matches( node ), 'File not repaired: %s' % node.url )

    def _verify_poll_results( self ):
        summary = self.victim.getPollSummaryFromKey( self.poll_key )
        if (summary[ 'Type' ] != 'Local' ):
            self.assertEqual( self.victim.getPollResults( self.AU ),
                              (u'Complete', u'100.00% Agreement') )

    def _verify_voter_agreements( self ):
        summary = self.victim.getPollSummaryFromKey( self.poll_key )
        if (summary[ 'Type' ] != 'Local' ):
            for client in self.clients:
                if client != self.victim:
                    repairer_info = client.getAuRepairerInfo( self.AU, 'LastPercentAgreement' )
                    repairer_count = len( repairer_info)
                    for ( box, agreement ) in repairer_info.iteritems():
                        self.assertEqual(self.expected_voter_agreement,
                                         agreement,
                                         'Client %s wrong agreement %s with box %s' % ( client, agreement, box))
                        if self.symmetric == True:
                            log.info( 'Symmetric client %s repairers OK' % client )
                        else:
                            log.info( 'Asymmetric client %s repairers OK' % client )

            repairer_info = self.victim.getAuRepairerInfo( self.AU, 'LastPercentAgreement' )
            self.assertEqual( len( self.clients ) - 1, len( repairer_info ) )
            for ( box, agreement ) in repairer_info.iteritems():
                log.debug( "Client %s box %s agree %s" % ( self.victim, box, agreement ) )
                self.assertEqual( agreement, self.expected_agreement,
                           'Voter %s had actual agreement: %s expected: %s' % ( box, agreement, self.expected_agreement ) )


    def _verify_voters_counts( self ):
        summary = self.victim.getPollSummaryFromKey( self.poll_key )
        if (summary[ 'Type' ] != 'Local' ):
            voters_counts = self.victim.getV3PollVotersCounts( self.poll_key )
            # -1 for us, the victim
            self.assertEqual( len( self.clients ) +
                              len ( self.offline_peers ) - 1,
                              len( voters_counts ) )

    def setUp( self ):
        LockssTestCases.setUp( self )
        self.victim = self.clients[ 0 ]
        self.nonVictim = self.clients[ 1 ]
        self.victim2 = self.clients[ 2 ]
        self.victim3 = self.clients[ 3 ]

        for client in self.clients:
            extraConf = { 'org.lockss.auconfig.allowEditDefaultOnlyParams': True,
                          'org.lockss.id.initialV3PeerList': ';'.join( [ peer.getV3Identity() for peer in self.clients ] + self.offline_peers ),
                          'org.lockss.platform.v3.identity': client.getV3Identity(),
                          'org.lockss.dbManager.enabled': False,
                          'org.lockss.poll.v3.enableV3Poller': False,
                          'org.lockss.poll.v3.enableV3Voter': True,
                          'org.lockss.poll.v3.enableLocalPolls': 'true',
                          'org.lockss.poll.v3.enablePoPPolls': 'true',
                          'org.lockss.poll.v3.allLocalPolls': 'false',
                          'org.lockss.poll.v3.allPoPPolls': 'false',
			  'org.lockss.poll.v3.minTimeBetweenAnyPoll': '6s',
                          'org.lockss.poll.v3.recordPeerUrlLists' : 'false',
                          'org.lockss.poll.pollStarterInitialDelay': '30s'
            }
            extraConf.update( self.local_configuration )
            self.framework.appendLocalConfig( extraConf, client )
        self._start_framework()

        # Block return until all clients are ready to go.
        log.info( 'Waiting for framework to become ready' )
        self.framework.waitForFrameworkReady()

    def runTest( self ):
        self._setup_AU()
        nodes = self._damage_AU()
        self._verify_damage( nodes )
        # enable polling
        self._enableVictimPoller()

        log.info( 'Waiting for a V3 poll to be called...' )
        self.assert_( self.victim.waitForV3Poller( self.AU ), 'Timed out while waiting for V3 poll' )
        log.info( 'Successfully called a V3 poll' )
        self.poll_key = self.victim.getV3PollKey( self.AU )
        log.debug( "Victim's poll key: " + self.poll_key )
        self.victim.waitForThisCompletedV3Poll( self.AU, self.poll_key )
        self._check_v3_result( nodes )

    def _check_v3_result( self, nodes, variant = u'Proof of Retrievability'):
        log.info( 'Waiting for V3 repair...' )
        self._await_repair( nodes )
        self._verify_repair( nodes )
        self._verify_poll_type( variant )
        self._verify_poll_results()
        self._verify_voter_agreements()
        self._verify_voters_counts()
        log.info( 'AU successfully repaired' )

    def _verify_poll_type( self, variant ):
        summary = self.victim.getPollSummaryFromKey( self.poll_key )
        self.assertEqual(summary[ 'Type' ], variant )


class FormatExpectedAgreementTestCase( V3TestCases ):
    """Test the expected agreement format."""
    def setUp( self ):
        pass

    def tearDown( self ):
        pass

    def runTest( self ):
        """These tests match those in TestArchivalUnitStatus.java."""
        self.assertEqual( '0.00', self._expected_agreement( 0, 1 ) )
        self.assertEqual( '0.00', self._expected_agreement( 1, 100000 ) )
        self.assertEqual( '0.00', self._expected_agreement( 9, 100000 ) )
        self.assertEqual( '0.01', self._expected_agreement( 10, 100000 ) )
        self.assertEqual( '99.99', self._expected_agreement( 999999999,
                                                             1000000000 ) )
        self.assertEqual( '100.00', self._expected_agreement( 100, 100 ) )
        
        
class SimpleV3TestCase( V3TestCases ):
    """Test a V3 poll with no disagreement"""

    def __init__( self, methodName = 'runTest' ):
        V3TestCases.__init__( self, methodName )
        self.simulated_AU_parameters = { 'numFiles': 3 }

    def _damage_AU( self ):
        return [ ]

class PollPolicyTestCases( V3TestCases ):
    """ Abstract Class """

    def _damage_AU( self ):
        return [ ]

    def _check_v3_result( self, nodes ):
        victim_first_poll_key = self.poll_key
        log.info( 'First poll key: ' + victim_first_poll_key )
        V3TestCases._check_v3_result( self, nodes )
        log.info( 'V3 result checked - was PoR' )
        # Now wait for the next poll
        self.assert_( self.victim.waitForV3Poller( self.AU, [ victim_first_poll_key ] ), 'Timed out while waiting for V3 poll' )
        log.info( 'Successfully called a V3 poll' )
        victim_second_poll_key = self.victim.getV3PollKey( self.AU, victim_first_poll_key )
        log.debug( "Victim's second poll key: " + victim_second_poll_key )
        summary = self.victim.getPollSummaryFromKey( victim_second_poll_key )
        self.assertEqual(summary[ 'Type' ], self.expectedSecondType )
        log.info( 'Second poll was ' + self.expectedSecondType)

class PoRThenLocalV3TestCase( PollPolicyTestCases ):
    """Test that an initial PoR with agreement is followed by a Local"""

    def __init__( self, methodName = 'runTest' ):
        V3TestCases.__init__( self, methodName )
	self.local_configuration = {
            'org.lockss.poll.v3.enablePoPPolls': True,
            'org.lockss.poll.v3.enableLocalPolls': True,
            'org.lockss.poll.v3.modulus': 2,
            'org.lockss.poll.v3.toplevelPollInterval': 1000,
            'org.lockss.poll.v3.maxDelayBetweenPoRMultiplier': 5000,
            'org.lockss.poll.minPollAttemptInterval': 500
	}
        self.simulated_AU_parameters = { 'numFiles': 3 }
        self.expectedSecondType = 'Local'

class PoRThenPoPV3TestCase( PollPolicyTestCases ):
    """Test that an initial PoR lacking repairers is followed by a PoP"""

    def __init__( self, methodName = 'runTest' ):
        V3TestCases.__init__( self, methodName )
	self.local_configuration = {
            'org.lockss.poll.v3.enablePoPPolls': True,
            'org.lockss.poll.v3.enableLocalPolls': True,
            'org.lockss.poll.v3.modulus': 2,
            'org.lockss.poll.v3.toplevelPollInterval': 1000,
            'org.lockss.poll.v3.maxDelayBetweenPoRMultiplier': 5000,
            'org.lockss.poll.v3.repairerThreshold': 100,
            'org.lockss.poll.minPollAttemptInterval': 500
	}
        self.simulated_AU_parameters = { 'numFiles': 3 }
        self.expectedSecondType = 'Proof of Possession'

class AuditDemo1( V3TestCases ):
    """Demo a V3 poll with no disagreement"""

    def __init__( self, methodName = 'runTest' ):
        V3TestCases.__init__( self, methodName )
        self.simulated_AU_parameters = { 'depth': 1, 'branch': 1, 'numFiles': 2 }

    def _damage_AU( self ):
        return [ ]

    def _check_v3_result( self, nodes ):
        log.info( 'Checking V3 poll result...' )
        self._verify_poll_results()
        self._verify_voter_agreements()
        self._verify_voters_counts()
        log.info( 'AU successfully polled' )

class AuditDemo2( V3TestCases ):
    """Demo a basic V3 poll with repair via open access"""

    def __init__( self, methodName = 'runTest' ):
        V3TestCases.__init__( self, methodName )
	self.local_configuration = {
            'org.lockss.poll.v3.repairFromCachePercent': 100,
	    'org.lockss.plugin.simulated.SimulatedContentGenerator.openAccess': 'true'
	}
        self.simulated_AU_parameters = { 'depth': 1, 'branch': 1, 'numFiles': 2 }
            
    def _damage_AU( self ):
        nodes = [ self.victim.randomDamageSingleNode( self.AU ) ]
        self._set_expected_agreement_from_damaged( nodes )
        return nodes

    def _verify_voter_agreements( self ):
        poll_key = self.victim.getV3PollKey( self.AU )
        for client in self.clients:
            if client != self.victim:
                repairer_info = client.getAuRepairerInfo( self.AU, 'LastPercentAgreement' )
                repairer_count = len( repairer_info)
                for ( box, agreement ) in repairer_info.iteritems():
                    self.assertEqual(self.expected_voter_agreement,
                                     agreement,
                                     'Client %s wrong agreement %s with box %s' % ( client, agreement, box))
                    if self.symmetric == True:
                        log.info( 'Symmetric client %s repairers OK' % client )
                    else:
                        log.info( 'Asymmetric client %s repairers OK' % client )



class SimpleV3LocalTestCase( V3TestCases ):
    """Test a V3 local poll with no disagreement"""

    def __init__( self, methodName = 'runTest' ):
        V3TestCases.__init__( self, methodName )
        self.local_configuration = {
            'org.lockss.poll.v3.enableLocalPolls': 'true',
            'org.lockss.poll.v3.allLocalPolls': 'true',
            }
        self.simulated_AU_parameters = { 'numFiles': 3 }

    def _check_v3_result( self , nodes ):
        self._verify_poll_type( 'Local' )
        log.debug( "Victim's poll key: " + self.poll_key )
        self.victim.waitForThisCompletedV3Poll( self.AU, self.poll_key )
        summary = self.victim.getPollSummaryFromKey( self.poll_key )
        log.debug( "Summary[Type]: " + summary[ 'Type' ] )
        log.debug( "Summary[Status]: " + summary[ 'Status' ] )
        self.assertEqual(summary[ 'Status' ], u'Complete' )
        self.assertEqual(summary[ 'Total URLs In Vote' ], u'7' )
        agreeURL = summary[ 'Agreeing URLs' ]
        self.assertEqual(summary[ 'Total URLs In Vote' ],
                         agreeURL[ 'value' ] )

    def _damage_AU( self ):
        return [ ]


class SimpleDamageV3LocalTestCase( V3TestCases ):
    """Test a V3 local poll with one disagreement"""

    def __init__( self, methodName = 'runTest' ):
        V3TestCases.__init__( self, methodName )
        self.local_configuration = {
            'org.lockss.poll.v3.enableLocalPolls': 'true',
            'org.lockss.poll.v3.allLocalPolls': 'true',
            }
        self.simulated_AU_parameters = { 'numFiles': 3 }

    def _check_v3_result( self , nodes ):
        self._verify_poll_type( 'Local' )
        self.poll_key = self.victim.getV3PollKey( self.AU )
        log.debug( "Victim's poll key: " + self.poll_key )
        self.victim.waitForThisCompletedV3Poll( self.AU, self.poll_key )
        summary = self.victim.getPollSummaryFromKey( self.poll_key )
        log.debug( "Summary[Type]: " + summary[ 'Type' ] )
        self.assertEqual(summary[ 'Type' ], u'Local' )
        log.debug( "Summary[Status]: " + summary[ 'Status' ] )
        self.assertEqual(summary[ 'Status' ], u'Complete' )
        self.assertEqual(summary[ 'Total URLs In Vote' ], u'7' )
        agreeURL = summary[ 'Agreeing URLs' ]
        # XXX this should be int(agreeURL['value']-1)
        self.assertEqual( int(summary[ 'Total URLs In Vote' ] ),
                         ( int(agreeURL[ 'value' ])) )

    def _damage_AU( self ):
        nodes = [ self.victim.randomDamageSingleNode( self.AU ) ]
        self._set_expected_agreement_from_damaged( nodes )
        return nodes


class SimpleDamageV3TestCase( V3TestCases ):
    """Test a basic V3 poll"""

    def __init__( self, methodName = 'runTest' ):
        V3TestCases.__init__( self, methodName )
        self.simulated_AU_parameters = { 'numFiles': 15 }
            
    def _damage_AU( self ):
        nodes = [ self.victim.randomDamageSingleNode( self.AU ) ]
        self._set_expected_agreement_from_damaged( nodes )
        return nodes

class SimpleDamageV3PostRepairTestCase( V3TestCases ):
    """Test a basic V3 poll, with post-repair counts"""

    def __init__( self, methodName = 'runTest' ):
        V3TestCases.__init__( self, methodName )
        self.simulated_AU_parameters = { 'numFiles': 15 }

    def setUp( self ):
        V3TestCases.setUp( self )
        self.framework.appendLocalConfig(
            {'org.lockss.poll.v3.recordPeerUrlLists' : 'true' },
            self.victim )

    def _damage_AU( self ):
        nodes = [ self.victim.randomDamageSingleNode( self.AU ) ]
        self.expected_agreement = '100.00'
        return nodes


class UnsuccessfulRepairV3TestCase( V3TestCases ):
    """Test a repair that doesn't match consensus"""

    def __init__( self, methodName = 'runTest' ):
        V3TestCases.__init__( self, methodName )
        
    def setUp( self ):
        V3TestCases.setUp( self )
        self.framework.appendLocalConfig( { 'org.lockss.simulatedAu.badCachedFileLoc': '1,1',
                                            'org.lockss.simulatedAu.badCachedFileLoc': '1' }, self.victim )

    def pre_create_client( self, client ):
        if client == self.victim:
            self.framework.appendLocalConfig( { 'org.lockss.simulatedAu.badCachedFileLoc': '',
                                                'org.lockss.simulatedAu.badCachedFileNum': '3' }, self.victim )
        self.victim.reloadConfiguration()

    def _damage_AU( self ):
        return []

    def _check_v3_result( self, nodes ):
        log.info( 'Waiting for poll to complete...' )
        self._await_complete( nodes )
        self._verify_poll_results()
        log.info( 'AU repair not verified, as expected' )

    def _await_complete( self, nodes ):
        log.info( 'Waiting for V3 poll to complete...' )
        self.assert_( self.victim.waitForCompletedV3Poll( self.AU ), 'Timed out while waiting for poll to complete' )
        log.info( 'Poll successfully completed' )

    def _verify_poll_results( self ):
        self.assertEqual( self.victim.getPollResults( self.AU ),
                          (u'Complete', u'95.23% Agreement') )
        poll = self.victim.findCompletedAuV3Poll( self.AU )
        summary = self.victim.getPollSummary( poll )
        self.assertEqual( int( summary[ 'Agreeing URLs' ][ 'value' ] ), 20 )
        self.assertEqual( int( summary[ 'Disagreeing URLs' ][ 'value' ] ), 1 )
        self.assertEqual( int( summary[ 'Completed Repairs' ][ 'value' ] ), 1 )


class UnsuccessfulRepairV3PostRepairTestCase( UnsuccessfulRepairV3TestCase ):
    """Test a repair that doesn't match consensus, with post-repair counts"""

    def __init__( self, methodName = 'runTest' ):
        V3TestCases.__init__( self, methodName )

    def setUp( self ):
        UnsuccessfulRepairV3TestCase.setUp( self )
        self.framework.appendLocalConfig(
            {'org.lockss.poll.v3.recordPeerUrlLists' : 'true' },
            self.victim )

class TooCloseV3Tests( V3TestCases ):
    """Abstract class"""

    def __init__( self, methodName = 'runTest' ):
        V3TestCases.__init__( self, methodName )
        self.simulated_AU_parameters = { 'numFiles': 15 }
            
    def _damage_AU( self ):

        node = self.victim.getRandomContentNode( self.AU )
        url = node.url
        self.victim.damageNode( node )
        log.debug( 'damaged: ' + node.url )
        node2 = self.victim2.getAuNode( self.AU, url )
        self.victim2.damageNode( node2 )
        log.debug( 'damaged: ' + node2.url )
        node3 = self.victim3.getAuNode( self.AU, url )
        self.victim3.damageNode( node3 )
        log.debug( 'damaged: ' + node3.url )

        nodes = [ node ]
        self._set_expected_agreement_from_damaged( nodes )
        return nodes

    def _check_v3_result( self, nodes ):
        log.info( 'Waiting for poll to complete...' )
        self._await_complete( nodes )
        self._verify_poll_results()
        log.info( 'AU successfully repaired' )

    def _await_complete( self, nodes ):
        log.info( 'Waiting for V3 poll to complete...' )
        self.assert_( self.victim.waitForCompletedV3Poll( self.AU ), 'Timed out while waiting for poll to complete' )
        log.info( 'Poll successfully completed' )

    def _verify_poll_results( self ):
        self.assertEqual( self.victim.getPollResults( self.AU ),
                          (u'Complete', u'96.77% Agreement') )
        poll = self.victim.findCompletedAuV3Poll( self.AU )
        summary = self.victim.getPollSummary( poll )
        agreed = int( summary[ 'Agreeing URLs' ][ 'value' ] )
        too_close = int( summary[ 'Too Close URLs' ][ 'value' ] )
        self.assertEqual( agreed, 30 )
        self.assertEqual( too_close, 1 )

class TooCloseV3TestCase( TooCloseV3Tests ):
    """Test a too-close V3 poll"""

    def __init__( self, methodName = 'runTest' ):
        TooCloseV3Tests.__init__( self, methodName )
        self.local_configuration = { 'org.lockss.poll.v3.repairFromPublisherWhenTooClose': 'false' }
            
    def _verify_poll_results( self ):
        self.assertEqual( self.victim.getPollResults( self.AU ),
                          (u'Complete', u'96.77% Agreement') )
        poll = self.victim.findCompletedAuV3Poll( self.AU )
        summary = self.victim.getPollSummary( poll )
        agreed = int( summary[ 'Agreeing URLs' ][ 'value' ] )
        too_close = int( summary[ 'Too Close URLs' ][ 'value' ] )
        self.assertEqual( agreed, 30 )
        self.assertEqual( too_close, 1 )
        self.assertFalse( 'Completed Repairs' in summary )

class TooCloseWithRepairV3TestCase( TooCloseV3Tests ):
    """Test a too-close V3 poll with repair from publisher"""

    def __init__( self, methodName = 'runTest' ):
        TooCloseV3Tests.__init__( self, methodName )
        self.local_configuration = { 'org.lockss.poll.v3.repairFromPublisherWhenTooClose': 'true' }
            
    def _verify_poll_results( self ):
        self.assertEqual( self.victim.getPollResults( self.AU ),
                          (u'Complete', u'96.77% Agreement') )
        poll = self.victim.findCompletedAuV3Poll( self.AU )
        summary = self.victim.getPollSummary( poll )
        agreed = int( summary[ 'Agreeing URLs' ][ 'value' ] )
        too_close = int( summary[ 'Too Close URLs' ][ 'value' ] )
        self.assertTrue( 'Completed Repairs' in summary )
        completed_repairs = int( summary[ 'Completed Repairs' ][ 'value' ] )
        self.assertEqual( agreed, 30 )
        self.assertEqual( too_close, 1 )
        self.assertEqual( completed_repairs, 1 )

class RandomDamageV3TestCase( V3TestCases ):
    """Test a V3 Poll with a random number of damaged AUs"""

    def __init__( self, methodName = 'runTest' ):
        V3TestCases.__init__( self, methodName )
        self.simulated_AU_parameters = { 'depth': 1, 'branch': 1, 'numFiles': 30 }
            
    def _damage_AU( self ):
        nodes = self.victim.randomDamageRandomNodes( self.AU, 30, 50 )
        self._set_expected_agreement_from_damaged( nodes )
        return nodes
                

class DeleteV3Tests( V3TestCases ):
    """Abstract class"""

    def __init__( self, methodName = 'runTest' ):
        V3TestCases.__init__( self, methodName )
        self.simulated_AU_parameters = { 'numFiles': 15 }

    def _set_expected_agreement_from_deleted( self, deletedNodes ):
        content = self.victim.getAuNodesWithContent( self.AU )
        numerator = len( content )
        denominator = len( content ) + len( deletedNodes )
        self.expected_agreement = self._expected_agreement( numerator, denominator )

    def _verify_damage( self, nodes ):
        for node in nodes:
            self._ensure_victim_node_deleted( node )
        log.info( 'Deleted the following node(s) on client %s:\n\t\t\t%s' % ( self.victim, '\n\t\t\t'.join( str( node ) for node in nodes ) ) )


class SimpleDeleteV3TestCase( DeleteV3Tests ):
    """Test repair of a missing file"""

    def _damage_AU( self ):
        node = self.victim.randomDelete( self.AU )
        self._set_expected_agreement_from_deleted( [ node ] )
        return [ node ]
        
class SimpleDeleteV3PostRepairTestCase( DeleteV3Tests ):
    """Test repair of a missing file, with post-repair counts"""

    def setUp( self ):
        DeleteV3Tests.setUp( self )
        self.framework.appendLocalConfig(
            {'org.lockss.poll.v3.recordPeerUrlLists' : 'true' },
            self.victim )

    def _damage_AU( self ):
        node = self.victim.randomDelete( self.AU )
        return [ node ]


class LastFileDeleteV3TestCase( DeleteV3Tests ):
    """Ensure that the deletion of the alphabetically last file in the AU can be repaired"""

    def _damage_AU( self ):
        node = self.victim.getAuNode( self.AU, 'http://www.example.com/index.html' )
        self.victim.deleteNode( node )
        self._set_expected_agreement_from_deleted( [ node ] )
        return [ node ]
        

class RandomDeleteV3TestCase( DeleteV3Tests ):
    """Test recovery by V3 from randomly deleted nodes in our cache"""

    def __init__( self, methodName = 'runTest' ):
        DeleteV3Tests.__init__( self, methodName )
        self.simulated_AU_parameters.update( { 'depth': 1, 'branch': 1 } )

    def _damage_AU( self ):
        nodes = self.victim.randomDeleteRandomNodes( self.AU, 5, 15 )
        self._set_expected_agreement_from_deleted( nodes )
        return nodes


class ExtraFilesV3Tests( V3TestCases ):
    """Abstract class"""

    def __init__( self, methodName = 'runTest' ):
        V3TestCases.__init__( self, methodName )
        self.simulated_AU_parameters = { 'numFiles': 20 }

    def _set_expected_agreement_from_extra( self, extraNodes ):
        content = self.victim.getAuNodesWithContent( self.AU )
        numerator = len( content ) - len( extraNodes )
        denominator = len( content )
        self.expected_agreement = self._expected_agreement( numerator, denominator )

    def _create_AU_nodes( self, minimum, maximum ):
        # Damage the AU by creating extra nodes
        nodes = self.victim.randomCreateRandomNodes( self.AU, minimum, maximum )
        log.info( 'Created the following nodes on client %s:\n\t\t\t%s' % ( self.victim, '\n\t\t\t'.join( str( node ) for node in nodes ) ) )
        self._set_expected_agreement_from_extra( nodes )
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
        self._set_expected_agreement_from_extra( [ node ] )
        return [ node ]


class LastFileExtraV3TestCase( ExtraFilesV3Tests ):
    """Test recovery by V3 from an extra last-file node in our cache"""

    def _damage_AU( self ):
        # Damage the AU by creating an extra node that should sort LAST in the list of CachedUrls
        node = self.victim.createNode( self.AU, 'zzzzzzzzzzzzz.txt' )
        log.info( 'Created file %s on client %s' % ( node.url, self.victim ) )
        self._set_expected_agreement_from_extra( [ node ] )
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
    # todo(bhayes): Why is this an ExtraFilesV3Tests? That introduces
    # randomness in the damage, which might be good, or maybe just
    # annoying.

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

    def _verify_voters_counts( self ):
        # Since the quorum is set low, sometimes a 4th peer will be
        # invited, sometimes not.
        poll_key = self.victim.getV3PollKey( self.AU )
        voters_counts = self.victim.getV3PollVotersCounts( poll_key )
        # -1 for us, the victim
        max_expected = len( self.clients ) + len ( self.offline_peers ) - 1
        # todo(bhayes): Can this be made more predictable?
        # I believe it's 6 if all 4 active peers are invited in the
        # initial round, and 7 if only 3 are, and the 4th is invited
        # in a second round. 
        min_expected = max_expected - 1
        self.assertTrue( len( voters_counts ) >= min_expected )
        self.assertTrue( len( voters_counts ) <= max_expected )
        # todo(bhayes): Check actual values of voter_counts


class NoQuorumV3TestCase( OfflinePeersV3Tests ):
    """Be sure an Asymmetric V3 poll with too few participants ends in No Quorum"""

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

    def _verify_poll_results( self ):
        self.assertEqual( self.victim.getPollResults( self.AU ),
                          (u'No Quorum', u'Waiting for Poll') )
        

class TotalLossRecoveryV3Tests( V3TestCases ):
    """Test repairing a cache under V3 that has lost all its contents"""

    def __init__( self, methodName = 'runTest' ):
        V3TestCases.__init__( self, methodName )
        self.simulated_AU_parameters = { 'depth': 1, 'branch': 1, 'numFiles': 30 }
        self.expected_agreement = '0.00'
        self.expected_voter_agreement = '100.00'


    def _setup_AU( self ):
        V3TestCases._setup_AU( self )
        # Allow daemons to record their agreeing peers
        self._await_V3_poll_agreement()

    def _damage_AU( self ):
        nodes = self.victim.getAuNodesWithContent( self.AU )
        log.info( 'Setting victim AU to pub_down' )
        self.victim.setPublisherDown( self.AU )
        log.info( 'Backing up cache configuration on victim cache...' )
        self.victim.backupConfiguration()
        log.info( 'Backed up successfully' )

        log.info( 'Stopping victim daemon' )
        self.victim.daemon.stop()
        log.info( 'Stopped daemon running on UI port %s' % self.victim.port )
        self.victim.simulateDiskFailure()
        log.info( 'Deleted entire contents of cache on stopped daemon' )

        log.info( 'Starting victim daemon' )
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


class TotalLossRecoveryV3TestCase( TotalLossRecoveryV3Tests ):
    """Test repairing a cache under V3 that has lost all its contents"""

    def __init__( self, methodName = 'runTest' ):
        TotalLossRecoveryV3Tests.__init__( self, methodName )
        self.local_configuration = { 'org.lockss.poll.v3.enableV3Poller': True }    # Enable polling on all peers

class TotalLossRecoveryV3PostRepairTestCase( TotalLossRecoveryV3Tests ):
    """Test repairing a cache under V3 that has lost all its contents, with post-repair counts"""

    def __init__( self, methodName = 'runTest' ):
        TotalLossRecoveryV3Tests.__init__( self, methodName )
        self.local_configuration = { 'org.lockss.poll.v3.enableV3Poller': True }    # Enable polling on all peers
        self.expected_agreement = '100.00'

    def setUp( self ):
        TotalLossRecoveryV3Tests.setUp( self )
        self.framework.appendLocalConfig(
            {'org.lockss.poll.v3.recordPeerUrlLists' : 'true' },
            self.victim )

class TotalLossRecoverySymmetricV3TestCase( TotalLossRecoveryV3Tests ):
    """Test repairing a cache under V3 that has lost all its contents via a symmetric poll"""

    def __init__( self, methodName = 'runTest' ):
        TotalLossRecoveryV3Tests.__init__( self, methodName )
        self.local_configuration = { "org.lockss.poll.v3.enableSymmetricPolls": True }
        self.symmetric = True
        self.expected_agreement = '100.00'
        self.expected_voter_agreement = '100.00'

    def _setup_AU( self ):
        V3TestCases._setup_AU( self )

    def runTest( self ):
        self._setup_AU()
        self.expected_agreement = '100.00'
        self.expected_voter_agreement = '100.00'
        self._enableVictimPoller()
        # Wait for the first poll to finish
        log.info( 'Waiting for a V3 poll to be called...' )
        self.assert_( self.victim.waitForV3Poller( self.AU ), 'Timed out while waiting for first V3 poll' )
        log.info( 'Successfully called first V3 poll' )
        self.poll_key = self.victim.getV3PollKey( self.AU )
        log.debug( "Victim's poll key: " + self.poll_key )
        self.victim.waitForThisCompletedV3Poll( self.AU, self.poll_key )
        self._verify_poll_results()
        self._verify_voter_agreements()
        self._verify_voters_counts()
        # Destroy the AU
        nodes = self._damage_AU()
        self._verify_damage( nodes )
        log.info( 'Waiting for second V3 poll to be called...' )
        self.assert_( self.victim.waitForV3Poller( self.AU ), 'Timed out while waiting for second V3 poll' )
        log.info( 'Successfully called second V3 poll' )
        self.poll_key = self.victim.getV3PollKey( self.AU )
        log.debug( "Victim's second poll key: " + self.poll_key )
        self.victim.waitForThisCompletedV3Poll( self.AU, self.poll_key )
        # Check the result
        self._await_repair(nodes)
        self._check_v3_result( nodes )

    def _damage_AU( self ):
        nodes = TotalLossRecoveryV3Tests._damage_AU( self )
        self.expected_agreement = '0.00'
        self.expected_voter_agreement = '100.00' # past agreement

        return nodes


class TotalLossRecoveryPoPV3TestCase( TotalLossRecoveryV3Tests ):
    """Test repairing a cache under V3 that has lost all its contents via PoP polls"""

    def __init__( self, methodName = 'runTest' ):
        TotalLossRecoveryV3Tests.__init__( self, methodName )
        self.local_configuration = { 'org.lockss.poll.v3.enableV3Poller': True,
                                     'org.lockss.poll.v3.modulus': 2,
                                     'org.lockss.baseuc.checksumAlgorithm': 'SHA-1',
                                     'org.lockss.blockHasher.enableLocalHash': True,
                                     'org.lockss.blockHasher.localHashAlgorithm': 'SHA-1',
                                     'org.lockss.poll.v3.allPoPPolls': True,
                                     'org.lockss.poll.v3.enablePoPVoting': True,
            			     'org.lockss.poll.v3.toplevelPollInterval': 1000,
            			     'org.lockss.poll.minPollAttemptInterval': 500,
                                     'org.lockss.poll.v3.enablePoPPolls': True}

    def _verify_poll_results( self ):
        poll = self.victim.findCompletedAuV3Poll( self.AU )
        summary = self.victim.getPollSummary( poll )
        self.assertEqual( summary[ 'Type' ], u'Proof of Possession' )
        self.assertEqual( summary[ 'Status' ], u'Complete' )
        self.assertEqual( summary[ 'Agreement' ], u'100.00%' )

    def _verify_repair( self, nodes ):
        # XXX need to identify files not count them
        node_count = 0
        repair_count = 0
        for node in nodes:
            node_count = node_count + 1
            if self.victim.isAuNode( self.AU, node.url ):
                if self._content_matches( node ):
                    repair_count = repair_count + 1
        log.info( '%i nodes %i repaired' % ( node_count, repair_count ) )
        # There is a small chance that the following tests will generate
        # a false negative, about the chance of tossing 120 heads in a row
        self.assert_( repair_count > 0 )
        self.assert_( repair_count < node_count )

    def _check_v3_result( self, nodes, variant = u'Proof of Retrievability'):
        log.info( 'Waiting for V3 repair...' )
        self._await_repair( nodes )
        self._verify_repair( nodes )
        self._verify_poll_type( 'Proof of Possession' )
        self._verify_poll_results()
        self._verify_voter_agreements()
        self._verify_voters_counts()
        log.info( 'AU successfully repaired' )


class RepairFromPublisherV3TestCase( V3TestCases ):
    """Ensure that repair from publisher works correctly in V3"""
    """Hangs if PoP polls enabled"""

    def __init__( self, methodName = 'runTest' ):
        V3TestCases.__init__( self, methodName )
        self.local_configuration = { "org.lockss.poll.v3.repairFromCachePercent": 0 }    # NEVER repair from a cache
        self.simulated_AU_parameters = { 'depth': 1, 'branch': 1 }

    def _damage_AU( self ):
        nodes = self.victim.randomDamageRandomNodes( self.AU, 15, 20 )
        self._set_expected_agreement_from_damaged( nodes )
        return nodes

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
        self.simulated_AU_parameters = { 'depth': 1, 'branch': 1 }    # Reasonably complex AU for testing
        self.toggle_AU_activation = False
        self.expected_voter_agreement = '100.00'

    def _setup_AU( self ):
        self.client_without_AU = self.clients[ -1 ]
        self.clients.remove( self.client_without_AU )
        log.debug( 'Peer without AU: %s' % self.client_without_AU )
        
        V3TestCases._setup_AU( self )
        self.framework.appendExtraConfig( { 'org.lockss.poll.v3.enableV3Poller': True } )
        # self.framework.appendExtraConfig( { 'org.lockss.poll.v3.enableLocalPolls': False } )
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
        self._set_expected_agreement_from_damaged( nodes )
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
            log.debug('verify repair of node %s' % node )
            self.assert_( self.victim.isNodeRepairedFromServerByV3( self.AU, node, False ), 'Node %s was not repaired from a peer!' % node )
        log.debug('Calling V3TestCases._verify_repair')
        V3TestCases._verify_repair( self, nodes )

class RepairFromPeerV3TestCase( RepairFromPeerV3Tests ):
    """Ensure that repairing from a V3 peer works correctly with Local polls disabled"""

    def __init__( self, methodName = 'runTest' ):
        RepairFromPeerV3Tests.__init__( self, methodName )
        self.local_configuration = { 'org.lockss.poll.minPollAttemptInterval': 10,
                                     'org.lockss.poll.v3.enableV3Poller': False,
                                     'org.lockss.poll.v3.repairFromCachePercent': 100,
                                     'org.lockss.poll.v3.enableLocalPolls': False,
                                     'org.lockss.poll.pollStarterAdditionalDelayBetweenPolls': '20s',
                                     'org.lockss.poll.v3.toplevelPollInterval': '1d' }

class RepairFromPeerV3LocalTestCase( RepairFromPeerV3Tests ):
    """Ensure that repairing from a V3 peer works correctly with Local polls enabled"""

    def __init__( self, methodName = 'runTest' ):
        RepairFromPeerV3Tests.__init__( self, methodName )
	self.local_configuration = {
            # 'org.lockss.poll.pollStarterAdditionalDelayBetweenPolls': '20s',
            'org.lockss.poll.v3.enablePoPPolls': True,
            'org.lockss.poll.v3.enableLocalPolls': True,
            'org.lockss.poll.v3.toplevelPollInterval': 5000,
            'org.lockss.poll.v3.maxDelayBetweenPoRMultiplier': 100000,
            'org.lockss.poll.v3.enableV3Poller': False,
            'org.lockss.poll.v3.repairFromCachePercent': 100,
            'org.lockss.poll.minPollAttemptInterval': 500
	}

    def _damage_AU( self ):
        nodes = self.victim.randomDamageRandomNodes( self.AU, 15, 20 )
        self.local_configuration['org.lockss.poll.v3.enableLocalPolls'] = True;
        self._update_configuration()
        return nodes

    def _verify_damage( self, nodes ):
        V3TestCases._verify_damage( self, nodes )
        
        log.debug( 'victim.getNoAuPeers( self.AU ): %s' % self.victim.getNoAuPeers( self.AU ) )
        self.assertEqual( self.victim.getNoAuPeers( self.AU ), [ self.client_without_AU.getV3Identity() ], 'Peer without AU disappeared!' )

        log.info( 'Waiting for second V3 poll to be called...' )
        # Ignores first victim poll
        self.assert_( self.victim.waitForV3Poller( self.AU, [ self.victim_first_poll_key ] ), 'Timed out while waiting for V3 poll' )
        log.info( 'Successfully called second V3 poll' )
        summary = self.victim.getPollSummaryFromKey( self.victim_first_poll_key )
        self.assertEqual(summary[ 'Type' ], 'Proof of Retrievability' )
        log.info( 'First poll was Proof of retrievability')

        self.victim_second_poll_key = self.victim.getV3PollKey( self.AU, self.victim_first_poll_key )
        log.debug( "Victim's second poll key: " + self.victim_second_poll_key )
        log.info( 'Waiting for third V3 poll to be called...' )
        # Ignores second victim poll
        self.assert_( self.victim.waitForV3Poller( self.AU, [ self.victim_first_poll_key, self.victim_second_poll_key ] ), 'Timed out while waiting for V3 poll' )
        log.info( 'Successfully called third V3 poll' )
        summary = self.victim.getPollSummaryFromKey( self.victim_second_poll_key )
        self.assertEqual(summary[ 'Type' ], 'Local' )
        log.info( 'Second poll was Local')

        self.victim_third_poll_key = self.victim.getV3PollKey( self.AU, [ self.victim_first_poll_key, self.victim_second_poll_key ] )
        log.debug( "Victim's third poll key: " + self.victim_third_poll_key )
        invitees = self.victim.getV3PollInvitedPeers( self.victim_third_poll_key )
        log.debug( 'invitedPeers: %s' % invitees )
        self.assertFalse( self.client_without_AU.getV3Identity() in invitees, 'Peer without AU invited in 3rd poll' )

    def _verify_voter_agreements( self ):
        summary = self.victim.getPollSummaryFromKey( self.victim_third_poll_key )
        self.assertEqual(summary[ 'Type' ], 'Proof of Retrievability' )
        log.debug('3rd poll type verified')
        log.info( 'Waiting for fourth poll to be called ...' )
        self.assert_( self.victim.waitForV3Poller( self.AU, [ self.victim_first_poll_key, self.victim_second_poll_key, self.victim_third_poll_key ] ), 'Timed out while waiting for V3 poll' )
        log.info( 'Successfully called fourth V3 poll' )
        victim_fourth_poll_key = self.victim.getV3PollKey( self.AU,
                                                           [ self.victim_first_poll_key,
                                                             self.victim_second_poll_key,
                                                             self.victim_third_poll_key ] )
        log.debug( "Victim's fourth poll key: " + victim_fourth_poll_key )
        summary = self.victim.getPollSummaryFromKey( victim_fourth_poll_key )
        self.assertEqual(summary[ 'Type' ], 'Local' )
        log.info( 'Fourth poll was Local' )

    def _verify_voters_counts( self ):
        log.debug( "Stubbed _verify_voters_counts()" )

class RepairFromPeerWithDeactivateV3TestCase( RepairFromPeerV3Tests ):
    """Ensure that repairing from a V3 peer after AU deactivate/reactivate works correctly"""

    def __init__( self, methodName = 'runTest' ):
        RepairFromPeerV3Tests.__init__( self, methodName )
        self.local_configuration = { 'org.lockss.poll.minPollAttemptInterval': 10,
                                     'org.lockss.poll.v3.enableV3Poller': False,
                                     'org.lockss.poll.v3.repairFromCachePercent': 100,
                                     'org.lockss.poll.v3.enableLocalPolls': False,
                                     'org.lockss.poll.pollStarterAdditionalDelayBetweenPolls': '20s',
                                     'org.lockss.poll.v3.toplevelPollInterval': '1d' }
        self.toggle_AU_activation = True

class RepairFromPeerWhenTooCloseV3TestCase( RepairFromPeerV3Tests ):
    """Ensure that selected voter-only too-close URLs are repaired from peer"""

    def __init__( self, methodName = 'runTest' ):
        RepairFromPeerV3Tests.__init__( self, methodName )
        self.local_configuration = { 'org.lockss.poll.minPollAttemptInterval': 10,
                                     'org.lockss.poll.v3.enableV3Poller': False,
                                     'org.lockss.poll.v3.repairFromCachePercent': 100,
                                     'org.lockss.poll.v3.enableLocalPolls': False,
                                     'org.lockss.poll.pollStarterAdditionalDelayBetweenPolls': '20s',
                                     'org.lockss.poll.v3.toplevelPollInterval': '1d' }
        self.simulated_AU_parameters = { 'depth': 1, 'branch': 1, 'repairFromPeerIfMissingUrlPatterns' : '.*' }    # Reasonably complex AU for testing

    def _damage_AU( self ):
        self.delnode = self.victim.getRandomContentNode( self.AU )
        url = self.delnode.url
        self.delnode2 = self.victim2.getAuNode( self.AU, url )
        self.delnode3 = self.victim3.getAuNode( self.AU, url )
        self.victim.deleteNode( self.delnode )
        self.victim2.deleteNode( self.delnode2 )
        self.victim3.deleteNode( self.delnode3 )
        log.debug( 'deleted: %s on poller and 2 peers' % self.delnode.url )

        nodes = [ self.delnode ]
        self._set_expected_agreement_from_damaged( nodes )
        self._update_configuration()
        return nodes

    def _verify_damage( self, nodes ):
        self._ensure_victim_node_deleted( self.delnode )
        self._ensure_victim_node_deleted( self.delnode2 )
        self._ensure_victim_node_deleted( self.delnode3 )

        log.info( 'Waiting for a V3 poll to be called...' )
        # Ignores first victim poll
        self.assert_( self.victim.waitForV3Poller( self.AU, [ self.victim_first_poll_key ] ), 'Timed out while waiting for V3 poll' )
        log.info( 'Successfully called a V3 poll' )

        self.victim_second_poll_key = self.victim.getV3PollKey( self.AU, self.victim_first_poll_key )
        log.debug( "Victim's second poll key: " + self.victim_second_poll_key )
        invitees = self.victim.getV3PollInvitedPeers( self.victim_second_poll_key )
        log.debug( 'invitedPeers: %s' % invitees )
        self.assertFalse( self.client_without_AU.getV3Identity() in invitees, 'Peer without AU invited in 2nd poll' )

    def _check_v3_result( self, nodes ):
        log.info( 'Waiting for poll to complete...' )
        self.victim.waitForThisCompletedV3Poll( self.AU, self.victim_second_poll_key )
        log.info( 'Poll %s complete' % self.victim_second_poll_key )
        self._verify_poll_results()
        log.info( 'AU repair verified' )

    def _verify_poll_results( self ):
        summary = self.victim.getPollSummaryFromKey( self.victim_second_poll_key )
        self.assertEqual( int( summary[ 'Agreeing URLs' ][ 'value' ] ), 41 )
        self.assertEqual( int( summary[ 'Too Close URLs' ][ 'value' ] ), 1 )
        self.assertEqual( int( summary[ 'Completed Repairs' ][ 'value' ] ), 1 )
        self.assertTrue( os.path.isfile( self.delnode.filename() ), 'File was not repaired: %s' % self.delnode.url )


class AuditDemo3( RepairFromPeerV3Tests ):
    """Demo a basic V3 poll with repair via previous agreement"""

    def __init__( self, methodName = 'runTest' ):
        RepairFromPeerV3Tests.__init__( self, methodName )
        self.local_configuration[ 'org.lockss.poll.pollStarterAdditionalDelayBetweenPolls' ] = '20s'    # XXX
        self.simulated_AU_parameters = { 'depth': 1, 'branch': 1, 'numFiles': 2 }

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


class SimpleV3SymmetricTestCase( V3TestCases ):
    """Test a V3 symmetric poll with no disagreement"""

    def __init__( self, methodName = 'runTest' ):
        V3TestCases.__init__( self, methodName )
        # ALWAYS have voter ask for symmetric poll
        self.local_configuration = { "org.lockss.poll.v3.allSymmetricPolls": True }
        self.simulated_AU_parameters = { 'numFiles': 3 }
        self.symmetric = True

        self.expected_voter_agreement = '100.00'

    def _damage_AU( self ):
        return [ ]

class NoQuorumSymmetricV3TestCase( SimpleV3SymmetricTestCase ):
    """Be sure a Symmetric V3 poll with too few participants ends in No Quorum"""

    def __init__( self, methodName = 'runTest' ):
        SimpleV3SymmetricTestCase.__init__( self, methodName )
        self.local_configuration[ 'org.lockss.poll.v3.quorum' ] = 30
        self.expected_agreement = '100.00'
        # Even though no quorum, voters will tally symmetric vote
        self.expected_voter_agreement = '100.00'

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

    def _verify_poll_results( self ):
        self.assertEqual( self.victim.getPollResults( self.AU ),
                          (u'No Quorum', u'Waiting for Poll') )
        
class SimpleV3PoPTestCase( V3TestCases ):
    """Test a V3 proof of possession poll with no disagreement"""

    def __init__( self, methodName = 'runTest' ):
        V3TestCases.__init__( self, methodName )
        self.local_configuration = { "org.lockss.poll.v3.modulus": 2,
                                     'org.lockss.poll.v3.enablePoPVoting': True}
        self.simulated_AU_parameters = { 'numFiles': 30 }
        # XXX need to confirm poll on ~15 files
        # XXX need to confirm willing repairer status
        
    def _damage_AU( self ):
        return [ ]

class SimpleV3PoPCompatibilityTestCase( V3TestCases ):
    """Test compatibility between PoP poller and non-PoP voters"""

    def __init__( self, methodName = 'runTest' ):
        V3TestCases.__init__( self, methodName )
        self.local_configuration = { "org.lockss.poll.v3.modulus": 2,
                                     'org.lockss.poll.v3.enablePoPVoting': False}
        self.simulated_AU_parameters = { 'numFiles': 30 }
        # XXX need to confirm poll on ~15 files
        # XXX need to confirm willing repairer status

    def _damage_AU( self ):
        return [ ]

# XXX need a test that triggers the hang mentioned in
# RepairFromPublisherV3TestCase

# Load configuration (*before* creating test instances)
lockss_util.config.load( 'testsuite.props' )

if os.path.isfile( 'testsuite.opt' ):
    lockss_util.config.load( 'testsuite.opt' )


tinyUiTests = unittest.TestSuite( ( TinyUiUnknownHostTestCase(),
                                    TinyUiMalformedUrlTestCase(),
                                    TinyUiForbiddenTestCase(),
                                    TinyUiRefusedTestCase(),
                                    TinyUiFileNotFoundTestCase() ) )

simpleV3Tests = unittest.TestSuite( ( FormatExpectedAgreementTestCase(),
                                      SimpleV3TestCase(),
                                      SimpleDamageV3TestCase(),
                                      SimpleDamageV3PostRepairTestCase(),
                                      UnsuccessfulRepairV3TestCase(),
                                      UnsuccessfulRepairV3PostRepairTestCase(),
                                      SimpleDeleteV3TestCase(),
                                      SimpleDeleteV3PostRepairTestCase(),
                                      SimpleExtraFileV3TestCase(),
                                      LastFileDeleteV3TestCase(),
                                      LastFileExtraV3TestCase(),
                                      TooCloseV3TestCase(),
                                      TooCloseWithRepairV3TestCase(),
                                      VotersDontParticipateV3TestCase(),
                                      NoQuorumV3TestCase(),
                                      TotalLossRecoveryV3TestCase(),
                                      TotalLossRecoveryV3PostRepairTestCase(),
                                      RepairFromPublisherV3TestCase(),
                                      RepairFromPeerV3TestCase(),
                                      RepairFromPeerV3LocalTestCase(),
                                      RepairFromPeerWithDeactivateV3TestCase(),
                                      RepairFromPeerWhenTooCloseV3TestCase() ) )

symmetricV3Tests = unittest.TestSuite( ( SimpleV3SymmetricTestCase(),
                                         NoQuorumSymmetricV3TestCase(),
                                         TotalLossRecoverySymmetricV3TestCase()
                                         ) )

popV3Tests = unittest.TestSuite( ( SimpleV3PoPTestCase(),
                                   SimpleV3PoPCompatibilityTestCase() ) )

pollPolicyV3Tests = unittest.TestSuite( ( PoRThenLocalV3TestCase(),
                                           PoRThenPoPV3TestCase() ) )

randomV3Tests = unittest.TestSuite( ( RandomDamageV3TestCase(),
                                      RandomDeleteV3TestCase(),
                                      RandomExtraFileV3TestCase() ) )

v3Tests = unittest.TestSuite( ( simpleV3Tests,
                                symmetricV3Tests,
                                popV3Tests,
                                pollPolicyV3Tests,
                                randomV3Tests ) )

# Release-candidate tests
postTagTests = unittest.TestSuite( ( tinyUiTests, v3Tests ) )


# Module globals
frameworkList = []
deleteAfterSuccess = lockss_util.config.getBoolean( 'deleteAfterSuccess', True )

if __name__ == '__main__':
    try:
        unittest.main( defaultTest = 'v3Tests',
                       argv = sys.argv[ 0 : 1 ] + [ '-q' ] + sys.argv[ 1 : ] )
    except ( KeyboardInterrupt, SystemExit ), exception:
        for framework in frameworkList:
            if framework.isRunning:
                log.info( 'Stopping framework' )
                framework.stop()

        if type( exception ) is KeyboardInterrupt:
            sys.exit(2)
        if type( exception ) is SystemExit:
            if not exception.code and deleteAfterSuccess:
                for framework in frameworkList:
                    framework.clean()
            raise
    except Exception, exception:
        log.error( exception )
        raise
