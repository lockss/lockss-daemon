#!/usr/bin/env python3

'''A library and a command line tool to interact with the LOCKSS daemon AU control
service via its Web Services API.'''

__copyright__ = '''\
Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.
'''

__license__ = '''\
Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
'''

__version__ = '0.2'

import argparse
import os.path
import sys
import time
from threading import Thread

try:
    import requests
except ImportError:
    sys.exit('The Python Requests module must be installed (or on the PYTHONPATH)')
import requests.auth

try:
    import zeep
except ImportError:
    sys.exit('The Python Zeep module must be installed (or on the PYTHONPATH)')
import zeep.helpers
import zeep.transports
import zeep.exceptions

from wsutil import requests_basic_auth


def request_deep_crawl_by_id(host, username, password, auid, refetch_depth, priority, force):
    client = _make_client(host, requests_basic_auth(username, password))
    return zeep.helpers.serialize_object(
        client.service.requestDeepCrawlById(auId=auid, refetchDepth=refetch_depth, priority=priority, force=force))


def request_deep_crawl_by_id_list(host, username, password, auids, refetch_depth, priority, force):
    client = _make_client(host, requests_basic_auth(username, password))
    return client.service.requestDeepCrawlByIdList(auIds=auids, refetchDepth=refetch_depth, priority=priority,
                                                   force=force)

def _do_request_deep_crawl_by_id(options):
    return request_deep_crawl_by_id(options.host, options.username, options.password, options.auid,
                                    options.refetch_depth, options.priority, options.force)


def _do_request_deep_crawl_by_id_list(options):
    return request_deep_crawl_by_id_list(options.host, options.username, options.password, options.auids,
                                         options.refetch_depth, options.priority, options.force)


#
# Command line tool
#

class _AuControlServiceOptions(object):

    @staticmethod
    def make_parser():
        usage = '%(prog)s {--host=HOST}... [OPTIONS]'
        parser = argparse.ArgumentParser(description=__doc__, usage=usage)
        parser.add_argument('--version', '-V', action='version', version=__version__)
        # Hosts
        group = parser.add_argument_group('Target hosts')
        group.add_argument('--host',  help='add host:port pair to list of target hosts')
        group.add_argument('--password', metavar='PASS', help='UI password (default: interactive prompt)')
        group.add_argument('--username', metavar='USER', help='UI username (default: interactive prompt)')

        # AUIDs
        group = parser.add_argument_group('Target AUIDs')
        group.add_argument('--auid', help='add AUID to list of target AUIDs')
        group.add_argument('--auids', action='append', default=list(), metavar='AFILE',
                           help='add AUIDs in AFILE to list of target AUIDs')
        # AUID operations
        group = parser.add_argument_group('AU operations')
        group.add_argument('--request-deep-crawl-by-id', action='store_true', help='perform deep crawl on a single AU')
        group.add_argument('--request-deep-crawl-by-id-list', action='store_true', help='perform a deep crawl on multiple AUs')

        # Other options
        group = parser.add_argument_group('Other options')
        group.add_argument('--refetch-depth', default=123, type=int, metavar='DEPTH', help='crawl depth (default: %(default)s)')
        group.add_argument('--priority', default=10, type=int, help='priority for crawl (default: %(default)s)' )
        group.add_argument('--force', action="store_true", help='force crawl outside of crawl window')
        return parser

    def __init__(self, parser, args):
        super(_AuControlServiceOptions, self).__init__()
        if len(list(filter(None, [args.request_deep_crawl_by_id, args.request_deep_crawl_by_id_list]))) != 1:
            parser.error('exactly one of --request-deep-crawl-by-id --request-deep-crawl-by-id-list is required')
        self.auid=args.auid
        self.auids = args.auid[:]
        for f in args.auids: self.auids.extend(_file_lines(f))
        # request_deep_crawl_by_id/request_deep_crawl_by_id_list
        self.request_deep_crawl_by_id=args.request_deep_crawl_by_id
        self.request_deep_crawl_by_id_list=args.request_deep_crawl_by_id_list
        self.host=args.host
        self.password=args.password
        self.username=args.username
        self.force = args.force
        self.priority = args.priority
        self.refetch_depth = args.refetch_depth

def _file_lines(fstr):
    with open(os.path.expanduser(fstr)) as f: ret = list(filter(lambda y: len(y) > 0, [x.partition('#')[0].strip() for x in f]))
    if len(ret) == 0: sys.exit(f'Error: {fstr} contains no meaningful lines')
    return ret


def _dispatch(options):
    if options.request_deep_crawl_by_id:
        _do_request_deep_crawl_by_id(options)
    elif options.request_deep_crawl_by_id_list:
        _do_request_deep_crawl_by_id_list(options)
    else:
        raise RuntimeError('Unreachable')


def _make_client(host, auth):
    session = requests.Session()
    session.auth = auth
    transport = zeep.transports.Transport(session=session)
    wsdl = 'http://{}/ws/AuControlService?wsdl'.format(host)
    client = zeep.Client(wsdl, transport=transport)
    return client

def _main():
    '''Main method.'''
    # Parse command line
    parser = _AuControlServiceOptions.make_parser()
    args = parser.parse_args()
    options = _AuControlServiceOptions(parser, args)
    # Dispatch
    t = Thread(target=_dispatch, args=(options,))
    t.daemon = True
    t.start()
    while True:
        t.join(1.5)
        if not t.is_alive(): break

if __name__ == '__main__': _main()
