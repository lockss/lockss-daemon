#!/usr/bin/env python

# $Id: delete_aus.py,v 1.1 2011-02-15 20:23:52 barry409 Exp $

# Copyright (c) 2011 Board of Trustees of Leland Stanford Jr. University,
# all rights reserved.
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
# STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
# WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
# IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#
# Except as contained in this notice, the name of Stanford University shall not
# be used in advertising or otherwise to promote the sale, use or other dealings
# in this Software without prior written authorization from Stanford University.

import argparse
import os
import sys
import urllib2
import pprint
import lockss_daemon
import fix_auth_failure

__author__ = "Barry Hayes"
__maintainer__ = "Barry Hayes"
__version__ = "1.0.0"


def _parser():
    """Make a parser for the arguments."""
    parser = argparse.ArgumentParser(
        description='Delete AUs on a LOCKSS daemon')
    parser.add_argument('-u', '--user', default='lockss-u')
    parser.add_argument('-p', '--password', default='lockss-p')
    parser.add_argument('-v', '--verbose', dest='verbose', action='store_true',
                        default=False)
    parser.add_argument('-q', '--quiet', dest='verbose', action='store_false')
    parser.add_argument('-f', '--force', dest='verify', action='store_false',
                        help='ignore auids not present on the daemon, \
                              never prompt')
    parser.add_argument('-i', '--verify', dest='verify', action='store_true',
                        default=False, help='prompt before each delete')
    parser.add_argument('host', default='127.0.0.1')
    parser.add_argument('port', type=int)
    parser.add_argument('auid_files', metavar='auid-file',
                        type=argparse.FileType(), nargs='*',
                        default=[sys.stdin],
                        help='A file containing auids (one per line), or \'-\'')
    return parser


def _aus(auid_files):
    """Read each line of each file and make a list of lockss_daemon.AUs"""
    aus = list()
    for auid_file in auid_files:
        for auid in auid_file.readlines():
            aus.append(lockss_daemon.AU(auid))
    return aus


def main():
    parser = _parser()
    arguments = parser.parse_args()

    fix_auth_failure.fix_auth_failure()
    client = lockss_daemon.Client(arguments.host, arguments.port,
                                  arguments.user, arguments.password)
    aus = _aus(arguments.auid_files)
    has = list()
    missing = list()
    for au in aus:
        if client.hasAu(au):
            has.append(au)
        else:
            missing.append(au)
    
    if arguments.verbose:
        print "Daemon %s" % client
        if missing:
            print "does not have"
            for au in missing:
                print au.auId
        if has:
            print "to be deleted"
            for au in has:
                print au.auId

    for au in has:
        if not arguments.verify or \
                arguments.verify and \
                raw_input('delete %s [n]? ' % au.title).startswith('y'):
            client.deleteAu(au)


if __name__ == '__main__':
    main()
