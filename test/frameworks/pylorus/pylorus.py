#!/usr/bin/env python
'''$Id: pylorus.py,v 1.2 2008-11-04 04:48:15 mrbax Exp $
Compares AU content hashes between servers, two local and one remote.
Servers are randomly selected from available pools for each AU.
The process is divided into stages to improve parallel efficiency.
Reads configuration from $0.conf, with command-line overrides.'''

import ConfigParser
import cStringIO
import getpass
import logging
import optparse
import os
import random
import subprocess
import sys
import tempfile

program_directory = os.path.dirname( sys.argv[ 0 ] )
sys.path.append( os.path.normpath( os.path.join( program_directory, '../lib' ) ) )
import lockss_daemon


# Constants
PYLORUS = 'Pylorus'
DEFAULT_UI_PORT = 8081
DEFAULT_V3_PORT = 8801
CRAWL_CHECK_TIMEOUT = 10
REMOTE_CRAWL_RETRY_TOTAL = 3
DEFAULT_AGREEMENT_THRESHOLD = 95


class Leaving_Pipeline( Exception ):
    '''Thrown to bypass further processing of an AU'''
    pass


def strip_comments( commented_lines ):
    '''Remove potentially time-varying comments from a hash file'''
    commented_file = cStringIO.StringIO( commented_lines )
    uncommented_file = cStringIO.StringIO()
    for line in commented_file:
        if not line.startswith( '#' ):
            uncommented_file.write( line ) 
    return uncommented_file.getvalue()


def diff( hash_1, hash_2 ):
    '''Returns the difference between two hashes found using diff and grep'''
    hash_1_file = open( tempfile.mkstemp()[ 1 ], 'wb' )
    hash_1_file.write( hash_1 )
    hash_1_file.close()
    hash_2_file = open( tempfile.mkstemp()[ 1 ], 'wb' )
    hash_2_file.write( hash_2 )
    hash_2_file.close()
    diff_output = subprocess.Popen( [ 'diff', '-U0', hash_1_file.name, hash_2_file.name ], stdout = subprocess.PIPE )
    grep_output = subprocess.Popen( [ 'grep', '^[+-][^+-]' ], stdin = diff_output.stdout, stdout = subprocess.PIPE ).communicate()[ 0 ]
    os.remove( hash_1_file.name )
    os.remove( hash_2_file.name )
    return grep_output


def client_ID( client ):
    '''Standardized notation'''
    return client.hostname + ':' + str( client.port )


def self_test_startup():
    '''Sets up a framework for simulated AU testing'''

    global framework
    framework = lockss_daemon.Framework( 5 )

    peer_list = ';'.join( [ client.getPeerId() for client in framework.clientList ] )
    for client in framework.clientList:
        framework.appendLocalConfig( { 'org.lockss.auconfig.allowEditDefaultOnlyParams': 'true',
                                       'org.lockss.id.initialV3PeerList': peer_list,
                                       'org.lockss.localV3Identity': client.getPeerId(),
                                       'org.lockss.poll.v3.enableV3Poller':'true',
                                       'org.lockss.poll.v3.enableV3Voter': 'true' },
                                     client )

    logging.info( 'Starting framework in %s', framework.frameworkDir )
    framework.start()
    assert framework.isRunning, 'Framework failed to start'
    
    AUs = [ lockss_daemon.SimulatedAu( 'simContent%i' % i, depth = 0, branch = 0, numFiles = 10, binFileSize = 1024, fileTypes = [ lockss_daemon.FILE_TYPE_TEXT, lockss_daemon.FILE_TYPE_BIN ], protocolVersion = 3 ) for i in range( 2 ) ]
    #AUs = [ lockss_daemon.AU( 'edu|una|plugin|McDonaldPhotosPlugin&base_url~http%3A%2F%2Fwww2%2Euna%2Eedu%2Fagordon%2Fpermission01%2Ehtml&volume~1' ) ]
    #AUs = [ lockss_daemon.AU( 'org|lockss|plugin|bioone|BioOnePlugin&base_url~http%3A%2F%2Fwww%2Ebioone%2Eorg%2F&journal_id~0071-4739&volume~40' ),
    #        lockss_daemon.AU( 'org|lockss|plugin|bioone|BioOnePlugin&base_url~http%3A%2F%2Fwww%2Ebioone%2Eorg%2F&journal_id~0097-3157&volume~155' ) ]

    #node = framework.clientList[ 0 ].randomDamageSingleNode( AUs[ 0 ] )

    #assert not self.compareNode( node, simAu, victim, self.nonVictim ), "Failed to damage AU"
    #log.info( "Damaged node %s on client %s" % ( node.url, victim ) )

    return AUs


def self_test_shutdown():
    '''Housekeeping'''
    if 'framework' in globals():
        if framework.isRunning:
            logging.info( 'Stopping framework' )
            framework.stop()
        logging.info( 'Cleaning up framework' )
        framework.clean()


class Content():
    '''Content processes itself through the pipeline'''
    
    def __init__( self, AU, local_clients, remote_clients ):
        self.AU = AU
        self.stage = self.check
        self.local_clients = local_clients[ : ]
        self.remote_clients = remote_clients[ : ]
        self.clients = self.local_clients
        self.crawl_failures = []
        self.remote_crawl_retries = REMOTE_CRAWL_RETRY_TOTAL
        self.hashes = []

    def check( self ):
        '''Does the server already have this AU?'''
        self.client = self.clients.pop( random.randrange( len( self.clients ) ) )
        self.server_name = client_ID( self.client )
        logging.info( 'Checking for AU "%s" on server %s' % ( self.AU, self.server_name ) )
        self.pre_existent = self.client.hasAu( self.AU )
        if self.pre_existent:
            logging.debug( 'Found AU "%s" on server %s' % ( self.AU, self.server_name ) )
            self.stage = self.crawl
        else:
            logging.debug( 'Did not find AU "%s" on server %s' % ( self.AU, self.server_name ) )
            self.stage = self.add
        
    def add( self ):
        '''Add this AU to the server'''
        logging.info( 'Adding AU "%s" to server %s' % ( self.AU, self.server_name ) )
        self.client.createAu( self.AU )
        # Handle failure?
        logging.debug( 'Added AU "%s" to server %s' % ( self.AU, self.server_name ) )
        self.stage = self.crawl
        
    def remove( self ):
        '''Remove this AU from the server'''
        if not self.pre_existent:
            logging.info( 'Removing AU "%s" from server %s' % ( self.AU, self.server_name ) )
            self.client.deleteAu( self.AU )
            logging.debug( 'Removed AU "%s" from server %s' % ( self.AU, self.server_name ) )
    
    def crawl( self ):
        '''Crawl this AU on the server'''
        logging.info( 'Waiting for crawl of AU "%s" on server %s' % ( self.AU, self.server_name ) )
        try:
            crawl_succeeded = self.client.waitForSuccessfulCrawl( self.AU, CRAWL_CHECK_TIMEOUT )
        except lockss_daemon.LockssError, exception:
            logging.error( exception )
            logging.info( 'Failed to crawl AU "%s" on server %s' % ( self.AU, self.server_name ) )
            self.crawl_failures.append( self.client )
            if self.clients == self.remote_clients:
                self.remote_crawl_retries -= 1
                if self.remote_clients and self.remote_crawl_retries:
                    self.stage = self.check
                    return
            # Local or repeated remote crawl failures are fatal
            crawl_failures.append( ( self.AU, self.crawl_failures ) )
            self.remove()
            raise Leaving_Pipeline
        if crawl_succeeded:
            logging.debug( 'Completed crawl of AU "%s" on server %s' % ( self.AU, self.server_name ) )
            self.stage = self.hash
    
    def hash( self ):
        '''Hash this AU on the server'''
        logging.info( 'Waiting for hash of AU "%s"' % self.AU )
        hash_file = self.client.getAuHashFile( self.AU )
        self.remove()
        if not hash_file:
            logging.warn( 'Hash failure for AU "%s"' % self.AU )
            hash_failures.append( ( self.AU, client ) )
            raise Leaving_Pipeline # Only if no remote clients left?
        logging.debug( 'Received hash of AU "%s"' % self.AU )
        self.hashes.append( ( self.client, strip_comments( hash_file ) ) )
        if len( self.hashes ) == 1:
            self.stage = self.check
        else:
            self.stage = self.compare

    def compare( self ):
        '''Compare server hashes of this AU'''
        local_comparison = len( self.hashes ) == 2
        location = 'Local' if local_comparison else 'Remote'
        clients, hashes = zip( *self.hashes )
        differences = diff( hashes[ 0 ], hashes[ -1 ] )
        agreement = 100 - 100*differences.count( '\n' )/( hashes[ 0 ].count( '\n' ) + hashes[ -1 ].count( '\n' ) )
        if agreement > options.agreement:
            logging.info( location + ' hash file match for AU "%s"' % self.AU )
            if local_comparison and self.remote_clients:
                self.clients = self.remote_clients
                self.stage = self.check
                return
            else:
                hash_matches.append( ( self.AU, self.hashes, differences, agreement ) )
        else:
            logging.info( location + ' hash file mismatch for AU "%s"' % self.AU )
            hash_mismatches.append( ( self.AU, self.hashes, differences, agreement ) )
        raise Leaving_Pipeline
    
        
option_defaults = { 'configuration': os.path.normpath( os.path.join( program_directory, os.path.splitext( os.path.basename( sys.argv[ 0 ] ) )[ 0 ] + '.conf' ) ), 'username': '', 'password': '', 'local_servers': [], 'remote_servers': [], 'agreement': DEFAULT_AGREEMENT_THRESHOLD, 'AU_IDs': [], 'test': False }

option_parser = optparse.OptionParser( usage = "usage: %prog [options] [AU's]", version = '%prog $Revision: 1.2 $', description = 'LOCKSS content gateway tester' )
option_parser.set_defaults( **option_defaults )
option_parser.add_option( '-c', '--configuration', help = 'read configuration from CONFIGURATION [%default]' )
option_parser.add_option( '-u', '--username', help = 'LOCKSS server username [%default]' )
option_parser.add_option( '-p', '--password', help = 'LOCKSS server password [%default]' )
option_parser.add_option( '-l', '--local', action = 'append', dest = 'local_servers', help = 'use local server LOCAL_SERVER (host:port)', metavar = 'LOCAL_SERVER' )
option_parser.add_option( '-r', '--remote', action = 'append', dest = 'remote_servers', help = 'use remote server REMOTE_SERVER (host:port)', metavar = 'REMOTE_SERVER' )
option_parser.add_option( '-a', '--agreement', help = 'threshold agreement percentage [%default]' )
option_parser.add_option( '-A', '--AU_ID', action = 'append', dest = 'AU_IDs', help = 'test content of AU_ID', metavar = 'AU_ID' )
option_parser.add_option( '-t', '--test', action = 'store_true', help = 'run self test' )
( options, AUs ) = option_parser.parse_args()

configuration = ConfigParser.RawConfigParser()
configuration.read( options.configuration )

# Merge configuration file settings subject to command-line override
for key in option_defaults:
    if configuration.has_option( PYLORUS, key ) and not getattr( options, key ):
        if type( getattr( options, key ) ) is list:
            setattr( options, key, configuration.get( PYLORUS, key ).strip().split( '\n' ) )
        else:
            setattr( options, key, configuration.get( PYLORUS, key ) )

if options.username == '':
    print 'Username:',
    options.username = raw_input()

if options.password == '':
    options.password = getpass.getpass()

logging.basicConfig( datefmt='%T', format='%(asctime)s.%(msecs)03d: %(levelname)s: %(message)s', level = logging.INFO )
 
crawl_failures = []
hash_failures = []
hash_mismatches = []
hash_matches = []

try:
    if options.test:
        AUs = self_test_startup()
        local_clients = framework.clientList[ : 2 ]
        remote_clients = framework.clientList[ 2 : ]
    else:
        local_clients = []
        for server in options.local_servers:
            hostname, UI_port = server.split( ':' ) if ':' in server else ( server, DEFAULT_UI_PORT )
            local_clients.append( lockss_daemon.Client( hostname, UI_port, DEFAULT_V3_PORT, options.username, options.password ) )
        assert len( local_clients ) >= 2, 'Requires at least two local servers'
        remote_clients = []
        for server in options.remote_servers:
            hostname, UI_port = server.split( ':' ) if ':' in server else ( server, DEFAULT_UI_PORT )
            remote_clients.append( lockss_daemon.Client( hostname, UI_port, DEFAULT_V3_PORT, options.username, options.password ) )
        AUs = [ lockss_daemon.AU( AU_ID ) for AU_ID in options.AU_IDs ]

    logging.info( 'Waiting for local servers' )
    for client in local_clients:
        client.waitForDaemonReady()

    logging.info( 'Waiting for remote servers' )
    for client in remote_clients:
        client.waitForDaemonReady()

    pipeline = [ Content( AU, local_clients, remote_clients ) for AU in AUs ]
    initial_length = len( pipeline )
    cycle = 0

    # The main loop
    while pipeline:
        logging.info( 'Cycle %i: pipeline contains %i%% of initial content' % ( cycle, 100*len( pipeline )/initial_length ) )
        next_pipeline = []
        for content in pipeline:
            try:
                content.stage() # It's a kind of magic...
                next_pipeline.append( content )
            except Leaving_Pipeline:
                pass
            except Exception, exception:
                # Unhandled exception
                logging.critical( exception )
                raise
        pipeline = next_pipeline
        cycle += 1

finally:
    if options.test:
        self_test_shutdown()

logging.info( 'Finished' )

if crawl_failures:
    print '\nCRAWL FAILURES\n'
    for AU, clients in sorted( crawl_failures ):
        print AU.auId
        for client in clients:
            print '\t', client_ID( client )

if hash_failures:
    print '\nHASH FAILURES\n'
    for AU, client in sorted( hash_failures ):
        print AU.auId
        print '\t', client_ID( client )

if hash_mismatches:
    print "\nMISMATCHED AU'S\n"
    for AU, hash_records, differences, agreement in sorted( hash_mismatches ):
        print AU.auId
        clients, hashes = zip( *hash_records )
        print '%i%% agreement between %s and %s' % ( agreement, client_ID( clients[ 0 ] ), client_ID( clients[ -1 ] ) )
        print differences

if hash_matches:
    print "\nMATCHED AU'S\n"
    for AU, hash_records, differences, agreement in sorted( hash_matches ):
        print AU.auId
        clients, hashes = zip( *hash_records )
        print '%i%% agreement between %s and %s' % ( agreement, client_ID( clients[ 0 ] ), client_ID( clients[ -1 ] ) )
        print differences
