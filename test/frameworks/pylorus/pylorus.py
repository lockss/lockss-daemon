#!/usr/bin/env python
'''Pylorus content validation and ingestion gateway
Michael R Bax, 2008-2009
$Id$'''


import ConfigParser
import cPickle
import httplib
import logging
import optparse
import os
import random
import sys
import tempfile
import threading
import time
import urllib2
import urlparse

sys.path.append( os.path.realpath( os.path.join( os.path.dirname( sys.argv[ 0 ] ), '../lib' ) ) )
import lockss_daemon
import fix_auth_failure


fix_auth_failure.fix_auth_failure()

# Constants
PROGRAM = os.path.splitext( os.path.basename( sys.argv[ 0 ] ) )[ 0 ].title()
REVISION = '$Revision$'.split()[ 1 ]
MAGIC_NUMBER = 'PLRS' + ''.join( number.rjust( 2, '0' ) for number in REVISION.split( '.' ) )
DEFAULT_UI_PORT = 8081
SERVER_READY_TIMEOUT = 600
MINIMUM_SLEEP_DURATION = 60
MAXIMUM_SLEEP_DURATION = 300
REMOTE_CRAWL_RETRY_TOTAL = 3
POLL_FAILURE_RETRY_TOTAL = 6
POLL_MISMATCH_RETRY_TOTAL = 6
MAXIMUM_URLError = SERVER_READY_TIMEOUT / MINIMUM_SLEEP_DURATION
CONFIGURATION_DEFAULTS = { 
    'configuration':    '',
    'local_servers':    'validate://localhost:8081\nvalidate://localhost:8082\ningest://localhost:8081\ningest://localhost:8082',
    'remote_servers':   '',
    'username':         'lockss-u',
    'password':         'lockss-p',
    'agreement':        100,
    'fix':              0,
    'expiration':       168,
    'verbosity':        1,
    'delete':           'no',
    'batch':            'no',
    'test':             'no',
    'snapshot':         '',
    'content_list':     ''
}


class Structure:
    pass


class Leaving_Pipeline( Exception ):
    '''Thrown to bypass further processing of an AU'''
    pass


class Sleeper:
    '''Sleep for progressively longer periods until reset'''

    def __init__( self ):
        self.duration = MINIMUM_SLEEP_DURATION

    def sleep( self, progress ):
        if progress:
            self.duration = MINIMUM_SLEEP_DURATION
        else:
            logging.info( time.strftime( 'Sleeping for %T', time.gmtime( self.duration ) ) )
            time.sleep( self.duration )
            self.duration = min( self.duration + 120, MAXIMUM_SLEEP_DURATION )


class Content:
    '''Content processes itself through the pipeline'''

    class State:
        CREATE = 'create'
        CHECK = 'check'
        ADD = 'add'
        CRAWL = 'crawl'
        HASH = 'hash'
        COMPARE = 'compare'
        POLL = 'poll'
        ADJUDICATE = 'adjudicate'
        REPAIR = 'repair'
        CREATION_FAILURE = 'CREATION FAILURE'
        ADD_FAILURE = 'ADD FAILURE'
        CRAWL_FAILURE = 'CRAWL FAILURE'
        HASH_FAILURE = 'HASH FAILURE'
        HASH_MISMATCH = 'HASH MISMATCH'
        HASH_MATCH = 'HASH MATCH'
        POLL_FAILURE = 'POLL FAILURE'
        POLL_MISMATCH = 'POLL MISMATCH'
        POLL_MATCH = 'POLL MATCH'
        TIME_OUT = 'TIME-OUT'
        display_sequence = ( CREATION_FAILURE, ADD_FAILURE, CRAWL_FAILURE, HASH_FAILURE, HASH_MISMATCH, HASH_MATCH, POLL_FAILURE, POLL_MISMATCH, POLL_MATCH, TIME_OUT )

    class Action:
        VALIDATE = 'validate'
        INGEST = 'ingest'
        values = ( VALIDATE, INGEST )

    def __init__( self, description ):
        self.description = description
        self.remote_crawl_retries = REMOTE_CRAWL_RETRY_TOTAL
        self.poll_failure_retries = POLL_FAILURE_RETRY_TOTAL
        self.poll_mismatch_retries = POLL_MISMATCH_RETRY_TOTAL
        self.crawl_failures = []
        self.crawl_successes = []
        self.hash_records = []
        self.thread = None
        self.update_time = time.time()
        self.state = Content.State.CREATE
    
    def status_message( self, template ):
        return template % ( 'AU "%s"' % self.AU, 'server %s' % self.client )

    def create( self ):
        '''Parses the content description and creates an AU'''
        logging.info( 'Creating AU from "%s"' % self.description )
        try:
            self.action, AU_ID = self.description.split( ':', 1 )
            self.local_clients = local_clients[ self.action ][ : ]
            self.remote_clients = remote_clients[ self.action ][ : ]
            self.AU = simulated_AU_cache.get( AU_ID, lockss_daemon.AU( AU_ID ) )
        except ( ValueError, KeyError, lockss_daemon.LockssError ), exception:
            logging.error( exception )
            logging.warn( 'Failed to create AU from "%s"' % self.description )
            self.state = Content.State.CREATION_FAILURE
            raise Leaving_Pipeline
        logging.debug( 'Created AU from "%s"' % self.description )
        self.clients = self.local_clients[ : ]
        self.state = Content.State.CHECK

    def check( self ):
        '''Does the server already have this AU?'''
        if len( self.crawl_successes ) == 2:
            self.clients = self.remote_clients[ : ]
        self.client = self.clients.pop( random.randrange( len( self.clients ) ) )
        logging.info( self.status_message( 'Checking for %s on %s' ) )
        self.pre_existent = self.client.hasAu( self.AU )
        if self.pre_existent:
            logging.debug( self.status_message( 'Found %s on %s' ) )
            self.state = Content.State.CRAWL
        else:
            logging.debug( self.status_message( 'Did not find %s on %s' ) )
            self.state = Content.State.ADD
        
    def add( self ):
        '''Add this AU to the server'''
        logging.info( self.status_message( 'Adding %s to %s' ) )
        try:
            self.client.createAu( self.AU )
        except lockss_daemon.LockssError, exception:
            logging.error( exception )
            logging.warn( self.status_message( 'Failed to add %s to %s' ) )
            self.state = Content.State.ADD_FAILURE
            raise Leaving_Pipeline
        logging.debug( self.status_message( 'Added %s to %s' ) )
        self.state = Content.State.CRAWL
        
    def crawl( self ):
        '''Crawl this AU on the server'''
        logging.info( self.status_message( 'Waiting for crawl of %s on %s' ) )
        num_URLError = 0
        while True:
            try:
                if isinstance( self.AU, lockss_daemon.Simulated_AU ):
                    crawl_succeeded = self.client.waitForSuccessfulCrawl( self.AU, True, 0 )
                else:
                    crawl_succeeded = self.client.waitForSuccessfulCrawl( self.AU, False, 0 )
            except lockss_daemon.LockssError, exception:
                logging.error( exception )
                logging.warn( self.status_message( 'Failed to crawl %s on %s' ) )
                self.crawl_failures.append( self.client )
                if self.clients == self.remote_clients:
                    self.remote_crawl_retries -= 1
                    if self.remote_clients and self.remote_crawl_retries:
                        self.state = Content.State.CHECK
                        return
                # Local or repeated remote crawl failures are fatal
                self.state = Content.State.CRAWL_FAILURE
                raise Leaving_Pipeline
            except (urllib2.URLError, httplib.BadStatusLine):
                num_URLError += 1
                if num_URLError < MAXIMUM_URLError:
                    logging.warn( self.status_message( 'URL error while attempting to crawl %s on %s.  Continuing.' ))
                    continue
                else:
                    raise
            break
        if crawl_succeeded:
            logging.debug( self.status_message( 'Completed crawl of %s on %s' ) )
            if self.client.hasNoSubstance( self.AU ):
                # Successful crawl with no substance is fatal
                logging.warn( self.status_message( 'No substance for crawl %s on %s.' ))
                self.state = Content.State.CRAWL_FAILURE
                raise Leaving_Pipeline
            self.crawl_successes.append( self.client )
            if self.action == self.Action.VALIDATE:
                self.state = Content.State.HASH
            elif len( self.crawl_successes ) < 2 + min( 2, len( self.local_clients ) ):
                self.state = Content.State.CHECK
            else:
                self.poll_clients = self.crawl_successes[ : 2 ]
                self.state = Content.State.POLL
    
    def remove( self ):
        '''Remove this AU from the server'''
        if not self.pre_existent:
            logging.info( self.status_message( 'Removing %s from %s' ) )
            self.client.deleteAu( self.AU )
            logging.debug( self.status_message( 'Removed %s from %s' ) )
    
    def hash( self ):
        '''Hash this AU on the server'''

        def hash_thread( content ):
            try:
                content.hash_file = content.client.getAuHashFile( content.AU )
            except lockss_daemon.LockssError, exception:
                logging.error( exception )
                content.hash_file = ''

        logging.info( self.status_message( 'Waiting for hash of %s on %s' ) )
        if self.thread:
            if self.thread.isAlive():
                return
            self.thread = None
            if not self.hash_file.startswith( '# Block hashes from ' ):
                logging.warn( self.status_message( 'Hash failure for %s on %s' ) )
                self.state = Content.State.HASH_FAILURE
                raise Leaving_Pipeline # Only if no remote clients left?
            if configuration.getboolean( PROGRAM, 'delete' ):
                self.remove()
            logging.debug( self.status_message( 'Received hash of %s from %s' ) )
            self.hash_records.append( [ self.client, self.hash_file.strip() ] )
            if len( self.hash_records ) == 1:
                self.state = Content.State.CHECK
            else:
                self.state = Content.State.COMPARE
        else:
            self.thread = threading.Thread( target = hash_thread, args = ( self, ) )
            self.thread.start()

    def compare( self ):
        '''Compare server hashes of this AU'''

        def diff( hash_1, hash_2 ):
            '''Returns the differences and agreement between two hashes'''
            hash_1_entries, hash_2_entries = ( [ entry.rsplit( None, 1 ) for entry in hash.splitlines() if entry and not entry.startswith( '#' ) ] for hash in ( hash_1, hash_2 ) )
            differences = []
            difference_count = 0
            offset_1 = 0
            offset_2 = 0
            while True:
                try:
                    hash_1, filename_1 = hash_1_entries[ offset_1 ]
                    hash_2, filename_2 = hash_2_entries[ offset_2 ]
                except IndexError:
                    differences.extend( '< ' + filename for hash, filename in hash_1_entries[ offset_1 : ] )
                    differences.extend( '> ' + filename for hash, filename in hash_2_entries[ offset_2 : ] )
                    entry_total = len( hash_1_entries ) + len( hash_2_entries )
                    difference_count += entry_total - offset_1 - offset_2
                    return differences, 100*( entry_total - difference_count )//entry_total
                if filename_1 == filename_2:
                    if hash_1 == 'Hash error (see log)':
                        differences.append( 'X ' + filename_1 )
                        difference_count += 2
                    elif hash_1 != hash_2:
                        differences.append( '! ' + filename_1 )
                        difference_count += 2
                    offset_1 += 1
                    offset_2 += 1
                elif filename_1 < filename_2:
                    differences.append( '< ' + filename_1 )
                    difference_count += 1
                    offset_1 += 1
                else:
                    differences.append( '> ' + filename_2 )
                    difference_count += 1
                    offset_2 += 1

        local_comparison = len( self.hash_records ) == 2
        location = 'Local' if local_comparison else 'Remote'
        logging.debug( self.status_message( 'Comparing hashes of %s on ' + str( self.hash_records[ 0 ][ 0 ] ) + ' to %s' ) )
        logging.debug2( 'First hash file:\n' + self.hash_records[ 0 ][ 1 ] )
        logging.debug2( 'Second hash file:\n' + self.hash_records[ -1 ][ 1 ] )
        difference, agreement = diff( self.hash_records[ 0 ][ 1 ], self.hash_records[ -1 ][ 1 ] )
        self.hash_records[ -1 ] += difference, agreement
        if agreement >= configuration.getint( PROGRAM, 'agreement' ):
            logging.info( location + ' hash file match for AU "%s"' % self.AU )
            if local_comparison and self.remote_clients:
                self.state = Content.State.CHECK
                return
            else:
                self.state = Content.State.HASH_MATCH
        else:
            logging.warn( location + ' hash file mismatch for AU "%s"' % self.AU )
            self.state = Content.State.HASH_MISMATCH
        raise Leaving_Pipeline
    
    def poll( self ):
        '''Run a poll for this AU on the server'''
        self.client = random.choice( self.poll_clients )
        logging.info( self.status_message( 'Starting poll of %s on %s' ) )
        try:
            self.past_poll_keys = self.client.getV3PollKeys( self.AU )
            self.client.startV3Poll( self.AU )
        except lockss_daemon.LockssError, exception:
            logging.error( exception )
            logging.warn( self.status_message( 'Failed to start poll of %s on %s' ) )
            self.state = Content.State.POLL_FAILURE
            raise Leaving_Pipeline
        self.state = Content.State.ADJUDICATE

    def adjudicate( self ):
        '''Judge poll results for this AU on the server'''
        logging.info( self.status_message( 'Waiting for result of poll of %s on %s' ) )
        if not self.client.waitForCompletedV3Poll( self.AU, self.past_poll_keys, 0 ):
            return
        logging.debug( self.status_message( 'Completed poll of %s on %s' ) )
        key = self.client.getV3PollKey(self.AU, self.past_poll_keys)
        summary, table = self.client.getV3PollerDetail(key)
        if summary['Status'] == 'Complete':
            # Overall poll result is meaningless; can be 100% if the poller
            # agreed with enough (but not all) voters for each URL. Compute an
            # average.
            complete = [float(row['agreement'].rstrip('%')) for row in table if row['peerStatus'] == 'Complete']
            self.poll_agreement = int(sum(complete) / len(complete))
            if self.poll_agreement >= configuration.getint(PROGRAM, 'agreement'):
                logging.info(self.status_message('Poll match for %s on %s'))
                self.state = Content.State.POLL_MATCH
            else:
                logging.warn( self.status_message('Poll mismatch for %s on %s'))
                self.poll_mismatch_retries -= 1
                if self.poll_mismatch_retries:
                    self.state = Content.State.REPAIR
                    return
                self.state = Content.State.POLL_MISMATCH
        else:
            logging.warn( self.status_message( 'Poll failed (' + summary['Status'] + ') for %s on %s' ) )
            self.poll_failure_retries -= 1
            if self.poll_failure_retries:
                self.state = Content.State.POLL
                return
            self.state = Content.State.POLL_FAILURE
        raise Leaving_Pipeline

    def repair( self ):
        '''Restrict the next poll to the most divergent candidates if poll disagreement varies too much'''
        key = self.client.getV3PollKey(self.AU, self.past_poll_keys)
        summary, table = self.client.getV3PollerDetail(key)
        complete = [row for row in table if row['peerStatus'] == 'Complete']
        for index, row in enumerate(complete):
            peer = self.client.valueOfRef(row['identity'])
            if peer not in V3_clients_lookup:
                logging.warn('No known client corresponding to V3 identity %s' % peer)
                del complete[index]
        agreement_min = min([float(row['agreement'].rstrip('%')) for row in complete])
        agreement_max = max([float(row['agreement'].rstrip('%')) for row in complete])
        logging.debug('Poll agreement range: %.02f%% to %.02f%%' % (agreement_min, agreement_max))
        if agreement_max - agreement_min >= configuration.getint(PROGRAM, 'fix'):
            self.poll_clients = [V3_clients_lookup[self.client.valueOfRef(row['identity'])] for row in complete if float(row['agreement'].rstrip('%')) == agreement_min]
        else:
            self.poll_clients = self.crawl_successes[:2]
        self.state = Content.State.POLL

    def process( self ):
        '''Process the content through its current pipeline stage'''
        previous_state = self.state
        getattr( self, self.state )() # It's a kind of magic...
        if self.state == previous_state:
            if time.time() - self.update_time > configuration.getint( PROGRAM, 'expiration' )*3600:
                logging.warn( self.status_message( 'Time-out expiration of %s on %s' ) )
                self.state = Content.State.TIME_OUT
                raise Leaving_Pipeline
            return False
        self.update_time = time.time()
        return True

    def dump( self, pickler ):
        '''Serialize content'''
        self.thread, thread = None, self.thread
        pickler.dump( self )
        self.thread = thread

    def output( self, heading = True, singular = True ):
        '''Format and display user-friendly status report'''

        def plural( singular ):
            '''Pluralises the end of the argument'''
            if singular.endswith( 'H' ):
                return singular + 'ES'
            elif singular.endswith( 'Y' ):
                return singular[ : -1 ] + 'IES'
            return singular + 'S'

        if heading:
            print '\n' + ( self.state if singular else '\n' + plural( self.state ) ) + ':'
        print
        print self.description if self.state == Content.State.CREATION_FAILURE else self.AU.auId
        if self.state in ( Content.State.ADD_FAILURE, Content.State.HASH_FAILURE ):
            print '\t', self.client
        elif self.state == Content.State.CRAWL_FAILURE:
            for client in self.crawl_failures:
                print '\t', client
        elif self.state in ( Content.State.HASH_MISMATCH, Content.State.HASH_MATCH ):
            reference_client = self.hash_records[ 0 ][ 0 ]
            for client, dummy, difference, agreement in self.hash_records[ 1 : ]:
                print '\t%i%% agreement between %s and %s' % ( agreement, reference_client, client )
                for entry in difference:
                    print '\t\t' + entry
        elif self.state in ( Content.State.POLL_MISMATCH, Content.State.POLL_MATCH ):
            print '\t%i%% agreement in poll on %s' % ( self.poll_agreement, self.client )
        if heading and singular:
            print


def read_configuration( filename ):
    '''Process configuration file and handles nesting if necessary'''
    while filename not in read_configurations:
        configuration.read( filename )
        read_configurations.add( filename )
        filename = configuration.get( PROGRAM, 'configuration' )        
    

def self_test_startup():
    '''Sets up a framework for simulated AU testing'''

    global framework
    framework = lockss_daemon.Framework( 4 )

    peer_list = ';'.join( client.getV3Identity() for client in framework.clientList )
    for client in framework.clientList:
        framework.appendLocalConfig( { 'org.lockss.id.initialV3PeerList': peer_list, # Redundant?
                                       'org.lockss.platform.v3.identity': client.getV3Identity(),
                                       'org.lockss.poll.v3.enableV3Poller': False,
                                       'org.lockss.ui.access.ip.include':'171.66.236.62;171.65.29.194;128.12.137.0/24;192.168.10.0/24',
                                       'org.lockss.poll.v3.quorum': 2 },
                                     client )

    logging.info( 'Starting framework in %s', framework.frameworkDir )
    framework.start()
    assert framework.isRunning, 'Framework failed to start'

    logging.info( 'Waiting for framework to become ready' )
    framework.waitForFrameworkReady()

    simulated_AU_roots = [ 'Uncrawlable', 'Major_hash_differences', 'Minor_hash_differences', 'Identical_hashes', 'Poll_identical' ]
    simulated_AUs = [ lockss_daemon.Simulated_AU( simulated_AU_root ) for simulated_AU_root in simulated_AU_roots ]

    global simulated_AU_cache
    simulated_AU_cache = dict( ( AU.auId, AU ) for AU in simulated_AUs )

    client = framework.clientList[ 0 ]
    crawl_failure_AU, badly_damaged_AU, slightly_damaged_AU = simulated_AUs[ : 3 ]

    logging.info( 'Pre-loading AU with a missing URL on server %s' % client )
    client.createAu( crawl_failure_AU )
    client.waitAu( crawl_failure_AU )
    crawl_failure_AU.numFiles += 1

    logging.info( 'Pre-loading AU with major differences on server %s' % client )
    client.createAu( badly_damaged_AU )
    client.waitAu( badly_damaged_AU )
    client.waitForSuccessfulCrawl( badly_damaged_AU, False )
    client.randomDamageRandomNodes( badly_damaged_AU, 2, 2 )

    logging.info( 'Pre-loading AU with minor differences on server %s' % client )
    client.createAu( slightly_damaged_AU )
    client.waitAu( slightly_damaged_AU )
    client.waitForSuccessfulCrawl( slightly_damaged_AU, False )
    client.randomDamageSingleNode( slightly_damaged_AU )

    return [ 'invalid_description', 'validate:Invalid_AU_ID', 'unknown_action:org|lockss|plugin|simulated|SimulatedPlugin&root~Test', 'validate:a|particularly|InvalidPlugin&base_url~http%3A%2F%2Fwww%2Eexample%2Ecom%2F' ] + [ 'validate:' + AU.auId for AU in simulated_AUs[ : -1 ] ] + [ 'ingest:' + simulated_AUs[ -1 ].auId ], { Content.Action.VALIDATE: framework.clientList[ : 2 ], Content.Action.INGEST: framework.clientList[ : 2 ] }, { Content.Action.VALIDATE: framework.clientList[ 2 : ], Content.Action.INGEST: framework.clientList[ 2 : ] }


def self_test_shutdown():
    '''Housekeeping'''
    if 'framework' in globals():
        if framework.isRunning:
            logging.info( 'Stopping framework' )
            framework.stop()
        logging.info( 'Cleaning up framework' )
        framework.clean()


def wait_for_clients( clients ):
    """Wait until the clients are up and running"""
    for action_clients in clients.itervalues():
        for client in action_clients:
            try:
                assert client.waitForDaemonReady( SERVER_READY_TIMEOUT )
            except ( AssertionError, lockss_daemon.LockssError ):
                raise Exception( 'Unavailable server %s' % client )


base_name = os.path.splitext( os.path.basename( sys.argv[ 0 ] ) )[ 0 ]
configuration = ConfigParser.RawConfigParser( CONFIGURATION_DEFAULTS )
configuration.add_section( PROGRAM )
read_configurations = set( [ '' ] )
read_configuration( '/etc/' + base_name + '.conf' )
read_configuration( os.path.expanduser( '~/.' + base_name + 'rc' ) )

action_options = '(' + '|'.join( Content.Action.values ) + ')'
option_parser = optparse.OptionParser( usage = 'usage: %prog [options] [' + action_options + ':(AU_ID|@AU_ID_list)...]', version = '%prog ' + REVISION, description = '%s %s content-testing and ingestion gateway' % ( PROGRAM.title(), REVISION ) )
configuration_dictionary = dict( configuration.items( PROGRAM ) )
for key in 'local_servers', 'remote_servers':
    configuration_dictionary[ key ] = configuration_dictionary[ key ].strip().replace( '\n', ',' )
option_parser.set_defaults( **configuration_dictionary )
option_parser.add_option( '-c', '--configuration', action = 'callback', type = 'string', callback = lambda *parameters: read_configuration( parameters[ 2 ] ), help = 'read configuration from CONFIGURATION [%default]' )
option_parser.add_option( '-l', '--local', dest = 'local_servers', help = 'local servers [%default]', metavar = action_options + '://HOST[:PORT][,...]' )
option_parser.add_option( '-r', '--remote', dest = 'remote_servers', help = 'remote servers [%default]', metavar = action_options + '://HOST[:PORT][,...]' )
option_parser.add_option( '-u', '--username', help = 'LOCKSS server username [%default]' )
option_parser.add_option( '-p', '--password', help = 'LOCKSS server password [%default]' )
option_parser.add_option( '-a', '--agreement', help = 'threshold agreement percentage [%default]' )
option_parser.add_option( '-f', '--fix', help = 'poll disagreement threshold to trigger a repair poll [%default]' )
option_parser.add_option( '-e', '--expiration', help = 'time-out expiration in hours [%default]' )
option_parser.add_option( '-v', '--verbosity', help = 'verbosity level, 1-7 [%default]' )
option_parser.add_option( '-d', '--delete', action = 'store_const', const = 'true', help = "delete added AU's from servers after validation [%default]" )
option_parser.add_option( '-b', '--batch', action = 'store_const', const = 'true', help = 'accumulate results until the end [%default]' )
option_parser.add_option( '-t', '--test', action = 'store_const', const = 'true', help = 'run self test [%default]' )
option_parser.add_option( '-s', '--snapshot', help = 'restore and save status in snapshot FILE [%default]', metavar = 'FILE' )
( options, content_list ) = option_parser.parse_args( values = Structure() )
for key, value in vars( options ).iteritems():
    configuration.set( PROGRAM, key, value )

for key in ( 'local_servers', 'remote_servers', 'content' ):
    if configuration.has_option( PROGRAM, key ):
        configuration.set( PROGRAM, key, configuration.get( PROGRAM, key ).strip().replace( ',', '\n' ).split() )
if not content_list and configuration.has_option( PROGRAM, 'content' ):
    content_list = configuration.get( PROGRAM, 'content' )

log_level = 10*( 6 - configuration.getint( PROGRAM, 'verbosity' ) )
if log_level < logging.DEBUG:
    log_level = logging.DEBUG + log_level//10 - 1
assert logging.DEBUG - 2 <= log_level <= logging.CRITICAL, 'Invalid log level'
logging = logging.getLogger( 'LOCKSS' )
logging.setLevel( log_level )

urlparse.uses_netloc += Content.Action.values
sleeper = Sleeper()
snapshot = configuration.get( PROGRAM, 'snapshot' )
delete_snapshot = False

try:

    if configuration.getboolean( PROGRAM, 'test' ):
        content_list, local_clients, remote_clients = self_test_startup()
    else:

        local_clients = dict( zip( Content.Action.values, ( [] for value in Content.Action.values ) ) )
        for server in configuration.get( PROGRAM, 'local_servers' ):
            url_components = urlparse.urlparse( server )
            assert url_components.scheme in Content.Action.values, 'Unknown local server scheme: "%s"' % url_components.scheme
            local_clients[ url_components.scheme ].append( lockss_daemon.Client( url_components.hostname, url_components.port if url_components.port else DEFAULT_UI_PORT, configuration.get( PROGRAM, 'username' ), configuration.get( PROGRAM, 'password' ) ) )

        remote_clients = dict( zip( Content.Action.values, ( [] for value in Content.Action.values ) ) )
        for server in configuration.get( PROGRAM, 'remote_servers' ):
            url_components = urlparse.urlparse( server )
            assert url_components.scheme in Content.Action.values, 'Unknown remote server scheme: "%s"' % url_components.scheme
            remote_clients[ url_components.scheme ].append( lockss_daemon.Client( url_components.hostname, url_components.port if url_components.port else DEFAULT_UI_PORT, configuration.get( PROGRAM, 'username' ), configuration.get( PROGRAM, 'password' ) ) )

        simulated_AU_cache = {}

    pipeline = []
    processed_content = []
    descriptions = set()
    if snapshot and os.access( snapshot, os.F_OK ) and not configuration.getboolean( PROGRAM, 'test' ):
        logging.info( 'Loading snapshot file "%s"' % snapshot )
        snapshot_file = open( snapshot, 'rb' )
        assert snapshot_file.read( 8 )[ : 4 ] == MAGIC_NUMBER[ : 4 ], 'Unrecognized file'
        unpickler = cPickle.Unpickler( snapshot_file )
        for index in range( unpickler.load() ):
            content = unpickler.load()
            descriptions.add( content.description )
            pipeline.append( content )
        for index in range( unpickler.load() ):
            content = unpickler.load()
            descriptions.add( content.description )
            processed_content.append( content )
        snapshot_file.close()
    for description in content_list: 
        if description not in descriptions:
            descriptions.add( description )
            # ignore the '@@@NONDEF@@@', a separater for nondef parameters
            if ('@' in description) and not('@@@NONDEF@@@' in description) :
                action, filename = description.split( '@', 1 )  
                content_list += ( action + included_description.strip() for included_description in open( filename ) )
            else:
                pipeline.append( Content( description ) )

    logging.info( 'Waiting for local servers' )
    wait_for_clients( local_clients )

    if any( remote_clients.values() ):
        logging.info( 'Waiting for remote servers' )
        wait_for_clients( remote_clients )

    
    V3_clients_lookup = dict( ( client.getV3Identity(), client ) for client in set( sum( local_clients.values() + remote_clients.values(), [] ) ) )
    initial_length = len( pipeline )
    cycle = 0

    # The main loop
    while pipeline:

        logging.info( 'Cycle %i: pipeline is %i%% of its original size' % ( cycle, 100*len( pipeline )//initial_length ) )
        progress = False
        next_pipeline = []
        for content in pipeline:
            try:
                if content.process():
                    progress = True
                next_pipeline.append( content )
            except Leaving_Pipeline:
                progress = True
                if configuration.getboolean( PROGRAM, 'batch' ):
                    processed_content.append( content )
                else:
                    content.output()

        if progress and snapshot and not configuration.getboolean( PROGRAM, 'test' ):
            logging.info( 'Saving snapshot file "%s"' % snapshot )
            file_descriptor, filename = tempfile.mkstemp( '.tmp', os.path.basename( snapshot ) + '.', os.path.dirname( snapshot ) )
            snapshot_file = os.fdopen( file_descriptor, 'wb' ) # Use tempfile.NamedTemporaryFile instead with Python 2.6+
            snapshot_file.write( MAGIC_NUMBER )
            pickler = cPickle.Pickler( snapshot_file, cPickle.HIGHEST_PROTOCOL )
            pickler.dump( len( next_pipeline ) )
            for content in next_pipeline:
                content.dump( pickler )
            pickler.dump( len( processed_content ) )
            for content in processed_content:
                content.dump( pickler )
            snapshot_file.close()
            os.rename( filename, snapshot ) # Windows will raise OSError rather than overwrite

        if next_pipeline:
            sleeper.sleep( progress )

        pipeline = next_pipeline
        cycle += 1

except Exception, exception:
    # Unhandled exception
    logging.critical( exception )
    logging.info( 'Finished with Exception' )
    raise
else:
    delete_snapshot = True
finally:
    if configuration.getboolean( PROGRAM, 'test' ):
        self_test_shutdown()

logging.info( 'Finished' )

if processed_content:

    results = {}
    for content in processed_content:
        results.setdefault( content.state, [] ).append( content )

    for state in Content.State.display_sequence:
        if state in results:
            header = True
            for AU in results[ state ]:
                AU.output( header, False )
                header = False

if delete_snapshot and snapshot and os.access( snapshot, os.F_OK ):
    os.remove( snapshot )
