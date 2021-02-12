#!/usr/bin/env python3

'''A library and a command line tool to interact with the LOCKSS daemon hasher
service via its legacy SOAP Web Services API.'''

__copyright__ = '''\
Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University.
All rights reserved.'''

__license__ = '''\
Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.'''

__version__ = '0.4.1'

import argparse
import getpass
from multiprocessing.dummy import Pool as ThreadPool
import os.path
import sys
import time
from threading import Thread

try:
    import requests
    import requests.auth
except ImportError:
    sys.exit('The Python Requests module must be installed (or on the PYTHONPATH)')

try:
    import zeep
    import zeep.transports
    import zeep.exceptions
except ImportError:
    sys.exit('The Python Zeep module must be installed (or on the PYTHONPATH)')

from wsutil import datems, datetimems, durationms, requests_basic_auth

###FIXME see https://docs.python-zeep.org/en/master/transport.html?highlight=debug#debugging
# import logging.config
# logging.config.dictConfig({
#     'version': 1,
#     'formatters': {
#         'verbose': {
#             'format': '%(name)s: %(message)s'
#         }
#     },
#     'handlers': {
#         'console': {
#             'level': 'DEBUG',
#             'class': 'logging.StreamHandler',
#             'formatter': 'verbose',
#         },
#     },
#     'loggers': {
#         'zeep.transports': {
#             'level': 'DEBUG',
#             'propagate': True,
#             'handlers': ['console'],
#         },
#     }
# })

#
# Library
#

def hash_au(host, auth, auid, include_weight=False):
    """Performs a hash operation on an AU on the given host and returns
    a hasherWsResult record with these fields:
    - blockFileDataHandler (base64Binary)
    - blockFileName (string)
    - bytesHashed (long)
    - elapsedTime (long)
    - errorMessage (string)
    - filesHashed (int)
    - hashResult (base64Binary)
    - recordFileDataHandler (base64Binary)
    - recordFileName (string)
    - startTime (long)
    - status (string)

    Parameters:
    :param host: a host:port pair (string)
    :param auth: an authentication object (requests.auth.AuthBase object)
    :param auid: an auid to hash (string)
    :param include_weight: a boolean indicating whether to include weight (boolean)
    :return:
    """
    hasher_params = {
        "auId": auid,
        "includeWeight": include_weight
    }
    return _hash(host, auth, hasher_params)


def hash_au_url(host, auth, auid, url):
    """Performs a hash operation on a URL in an AU on the given host and returns
    a hasherWsResult record with these fields:
    - blockFileDataHandler (base64Binary)
    - blockFileName (string)
    - bytesHashed (long)
    - elapsedTime (long)
    - errorMessage (string)
    - filesHashed (int)
    - hashResult (base64Binary)
    - recordFileDataHandler (base64Binary)
    - recordFileName (string)
    - startTime (long)
    - status (string)

    Parameters:
    :param host: a host:port pair (string)
    :param auth: an authentication object (requests.auth.AuthBase object)
    :param auid: an auid (string)
    :param url: a url (string)
    :return:
    """
    hasher_params = {
        "auId": auid,
        "url": url,
        "hashType": "V3File",
        "recordFilteredStream": True
    }
    return _hash(host, auth, hasher_params)


def _hash(host, auth, hasher_params):
    """Performs a hash operation on the given host with the given hasher parameters (hasherWsParams) and returns
    a hasherWsResult record with these fields:
    - blockFileDataHandler (base64Binary)
    - blockFileName (string)
    - bytesHashed (long)
    - elapsedTime (long)
    - errorMessage (string)
    - filesHashed (int)
    - hashResult (base64Binary)
    - recordFileDataHandler (base64Binary)
    - recordFileName (string)
    - startTime (long)
    - status (string)

    Parameters:
    :param host: a host:port pair (string)
    :param auth: an authentication object (requests.auth.AuthBase object)
    :param hasher_params: hasherWsParams
    :return:
    """
    client = _make_client(host, auth)
    return client.service.hash(hasherParams=hasher_params)


def hash_asynchronously_au(host, auth, auid, include_weight=False):
    """Performs a hashAsynchronously operation on the given host and returns
    a hasherWsAsynchronousResult record with these fields:
    - requestId (string)
    - requestTime (long)
    - blockFileDataHandler (base64Binary)
    - blockFileName (string)
    - bytesHashed (long)
    - elapsedTime (long)
    - errorMessage (string)
    - filesHashed (int)
    - hashResult (base64Binary)
    - recordFileDataHandler (base64Binary)
    - recordFileName (string)
    - startTime (long)
    - status (string)

    Parameters:
    :param host: a host:port pair (string)
    :param auth: an authentication object (requests.auth.AuthBase object)
    :param auid: an auid (string)
    :param include_weight:
    :return:
    """
    hasher_params = {
        "auId": auid,
        "includeWeight": include_weight
    }
    return _hash_asynchronously(host, auth, hasher_params)


def hash_asynchronously_au_url(host, auth, auid, url):
    """Performs a hashAsynchronously operation on the given host and returns
    a hasherWsAsynchronousResult record with these fields:
    - requestId (string)
    - requestTime (long)
    - blockFileDataHandler (base64Binary)
    - blockFileName (string)
    - bytesHashed (long)
    - elapsedTime (long)
    - errorMessage (string)
    - filesHashed (int)
    - hashResult (base64Binary)
    - recordFileDataHandler (base64Binary)
    - recordFileName (string)
    - startTime (long)
    - status (string)

    Parameters:
    :param host: a host:port pair (string)
    :param auth: an authentication object (requests.auth.AuthBase object)
    :param auid: an auid (string)
    :param url: a url (string)
    :return:
    """
    hasher_params = {
        "auId": auid,
        "url": url,
        "hashType": "V3File",
        "recordFilteredStream": True
    }
    return _hash_asynchronously(host, auth, hasher_params)


def _hash_asynchronously(host, auth, hasher_params):
    """Performs a hashAsynchronously operation on the given host with the given hasher parameters and returns
    a hasherWsAsynchronousResult record with these fields:
    - requestId (string)
    - requestTime (long)
    - blockFileDataHandler (base64Binary)
    - blockFileName (string)
    - bytesHashed (long)
    - elapsedTime (long)
    - errorMessage (string)
    - filesHashed (int)
    - hashResult (base64Binary)
    - recordFileDataHandler (base64Binary)
    - recordFileName (string)
    - startTime (long)
    - status (string)

    Parameters:
    :param host: a host:port pair (string)
    :param auth: an authentication object (requests.auth.AuthBase object)
    :param hasher_params: hasherWsParams
    :return:
    """
    client = _make_client(host, auth)
    return client.service.hashAsynchronously(hasherParams=hasher_params)


def get_all_asynchronous_hash_results(host, auth):
    """Performs a getAllAsynchronousHashResults operation on the givne host and returns
    an array of hasherWsAsynchronousResult with these fields:
    - requestId (string)
    - requestTime (long)
    - blockFileDataHandler (base64Binary)
    - blockFileName (string)
    - bytesHashed (long)
    - elapsedTime (long)
    - errorMessage (string)
    - filesHashed (int)
    - hashResult (base64Binary)
    - recordFileDataHandler (base64Binary)
    - recordFileName (string)
    - startTime (long)
    - status (string)

    Parameters:
    :param host: a host:port pair (string)
    :param auth: an authentication object (requests.auth.AuthBase object)
    :return:
    """
    client = _make_client(host, auth)
    return client.service.getAllAsynchronousHashResults()


def get_asynchronous_hash_result(host, auth, request_id):
    """Performs a getAsynchronousHashResult operation on the given host and returns
    a hasherWsAsynchronousResult record with these fields:
    - requestId (string)
    - requestTime (long)
    - blockFileDataHandler (base64Binary)
    - blockFileName (string)
    - bytesHashed (long)
    - elapsedTime (long)
    - errorMessage (string)
    - filesHashed (int)
    - hashResult (base64Binary)
    - recordFileDataHandler (base64Binary)
    - recordFileName (string)
    - startTime (long)
    - status (string)

    Parameters:
    :param host: a host:port pair (string)
    :param auth: an authentication object (requests.auth.AuthBase object)
    :param request_id: a request ID (string)
    :return:
    """
    client = _make_client(host, auth)
    return client.service.getAsynchronousHashResult(requestId=request_id)


def remove_asynchronous_hash_request(host, auth, request_id):
    """Performs a removeAsynchronousHashRequest operation on the given host and returns
    a hasherWsAsynchronousResult record with these fields:
    - requestId (string)
    - requestTime (long)
    - blockFileDataHandler (base64Binary)
    - blockFileName (string)
    - bytesHashed (long)
    - elapsedTime (long)
    - errorMessage (string)
    - filesHashed (int)
    - hashResult (base64Binary)
    - recordFileDataHandler (base64Binary)
    - recordFileName (string)
    - startTime (long)
    - status (string)

    Parameters:
    :param host: a host:port pair (string)
    :param auth: an authentication object (requests.auth.AuthBase object)
    :param request_id: a request id (string)
    :return:
    """
    client = _make_client(host, auth)
    return client.service.removeAsynchronousHashRequest(requestId=request_id)


#
# Command line tool
#

class _HasherServiceOptions(object):

    @staticmethod
    def make_parser():
        usage = '%(prog)s [--host=HOST|--hosts=HFILE]... --auid=AUID [--url=URL] [--output-directory=OUTDIR] --output-prefix=PREFIX [OPTIONS]'
        parser = argparse.ArgumentParser(description=__doc__, usage=usage)

        # Hosts
        group = parser.add_argument_group('Target hosts')
        group.add_argument('--host', action='append', default=list(), help='add host:port pair to list of target hosts')
        group.add_argument('--hosts', action='append', default=list(), metavar='HFILE', help='add host:port pairs in HFILE to list of target hosts')
        group.add_argument('--password', metavar='PASS', help='UI password (default: interactive prompt)')
        group.add_argument('--username', metavar='USER', help='UI username (default: interactive prompt)')

        # AUID and URL
        group = parser.add_argument_group('Target AUID and URL')
        group.add_argument('--auid', help='target AUID')
        group.add_argument('--url', help='target URL (optional)')

        # Output
        group = parser.add_argument_group('Output')
        group.add_argument('--output-directory', metavar='OUTDIR', default='.', help='output directory (default: current directory)')
        group.add_argument('--output-prefix', metavar='PREFIX', default='hasherservice', help='prefix for output file names (default: "hasherservice")')

        # Other options
        group = parser.add_argument_group('Other options')
        group.add_argument('--long-html-line', action='store_true', help='add a newline before each "<" character')
        group.add_argument('--long-text-line', action='store_true', help='replace each space with a newline')
        group.add_argument('--threads', type=int, help='maximum number of parallel jobs allowed (default: no limit)')
        group.add_argument('--wait', type=int, help='seconds to wait between asynchronous checks (default: 10 with --url, 30 without)')
        group.add_argument('--include-weight', action='store_true', help='include hash weights in full tree hash')

        return parser

    def __init__(self, parser, args):
        super(_HasherServiceOptions, self).__init__()
# FIXME        if len(args) != 0: parser.error('extraneous arguments: %s' % (' '.join(args)))
        # hosts
        self.hosts = args.host[:]
        for f in args.hosts: self.hosts.extend(_file_lines(f))
        if len(self.hosts) == 0: parser.error('at least one target host is required')
        # auid/url
        self.auid = args.auid
        self.url = args.url
        # output_directory/output_prefix
        self.output_directory = os.path.expanduser(args.output_directory)
        if not os.path.isdir(self.output_directory):
            parser.error('no such directory: %s' % (self.output_directory,))
        if args.output_prefix is None: parser.error('--output-prefix is required')
        if '/' in args.output_prefix: parser.error('output prefix cannot contain a slash')
        self.output_prefix = args.output_prefix
        # long_html_line/long_text_line/wait/threads
        if any([args.long_html_line, args.long_text_line]) and self.url is None:
            parser.error('--long-html-line, --long-text-line only apply to --url')
        if args.long_html_line and args.long_text_line:
            parser.error('--long-html-line, --long-text-line are incompatible')
        if args.include_weight and self.url:
            parser.error('--include-weight not compatible with --url')
        self.include_weight = args.include_weight
        self.long_html_line = args.long_html_line
        self.long_text_line = args.long_text_line
        if args.wait is None:
            self.wait = 30 if self.url is None else 10
        else:
            self.wait = args.wait
        # threads
        self.threads = args.threads or len(self.hosts)
        # auth
        u = args.username or getpass.getpass('UI username: ')
        p = args.password or getpass.getpass('UI password: ')
        self.auth = requests_basic_auth(u, p)


def _do_hash(options, host):
    if options.url is None:
        wsResult = hash_asynchronously_au(host, options.auth, options.auid, options.include_weight)
    else:
        wsResult = hash_asynchronously_au_url(host, options.auth, options.auid, options.url)
    if wsResult is None: return host, False
    reqid = wsResult.requestId
    while True:
        time.sleep(options.wait)
        res = get_asynchronous_hash_result(host, options.auth, reqid)
        if res.status == 'Done': break
    if options.url is None:
        source = res.blockFileDataHandler
        fstr = '%s.%s.hash' % (options.output_prefix, host)
    else:
        source = res.recordFileDataHandler
        fstr = '%s.%s.filtered' % (options.output_prefix, host)
    if source is not None:
        if options.long_html_line: source = source.replace(b'<', b'\n<')
        if options.long_text_line: source = source.replace(b' ', b'\n')
        with open(os.path.join(options.output_directory, fstr), 'wb') as f:
            f.write(source)
    res = remove_asynchronous_hash_request(host, options.auth, reqid)
    return host, source is not None


def _do_hashes(options):
    for host, result in ThreadPool(options.threads).imap_unordered( \
            lambda _host: _do_hash(options, _host), \
            options.hosts):
        if result is False:
            sys.stderr.write('Warning: not found on %s\n' % (host,))


# Last modified 2015-08-31
def _file_lines(fstr):
    with open(os.path.expanduser(fstr)) as f: ret = filter(lambda y: len(y) > 0,
                                                           [x.partition('#')[0].strip() for x in f])
    if len(ret) == 0: sys.exit('Error: %s contains no meaningful lines' % (fstr,))
    return ret


def _make_client(host, auth):
    session = requests.Session()
    session.auth = auth
    transport = zeep.transports.Transport(session=session)
    wsdl = f'http://{host}/ws/HasherService?wsdl'
    client = zeep.Client(wsdl, transport=transport)
    return client


def _main():
    '''Main method.'''
    parser = _HasherServiceOptions.make_parser()
    args = parser.parse_args()
    options = _HasherServiceOptions(parser, args)
    t = Thread(target=_do_hashes, args=(options,))
    t.daemon = True
    t.start()
    while True:
        t.join(1.5)
        if not t.is_alive(): break


# Main entry point
if __name__ == '__main__': _main()
