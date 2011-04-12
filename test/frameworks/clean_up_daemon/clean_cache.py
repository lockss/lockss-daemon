#!/usr/bin/env python

# $Id: clean_cache.py,v 1.6 2011-04-12 19:02:09 barry409 Exp $

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

import optparse
import ConfigParser
import os
import sys
import urllib2
import fix_auth_failure
import lockss_daemon

__author__ = "Barry Hayes"
__maintainer__ = "Barry Hayes"
__version__ = "1.0.1"


class _SectionAdder(object):
    """Wrap a python configuration section around a file that doesn't
    have one."""
    def __init__(self, section, fp):
        self.section_done = False
        self.section = section
        self.fp = fp

    def readline(self):
        if not self.section_done:
            self.section_done = True
            return '[%s]' % self.section
        else:
            return self.fp.readline()


class IdFileException(Exception):
    pass


def _parser():
    """Make a parser for the arguments."""
    parser = optparse.OptionParser(
        description='Move cache directories on a LOCKSS daemon')
    parser.add_option('-u', '--user', default='lockss-u')
    parser.add_option('-p', '--password', default='lockss-p')
    parser.add_option('-v', '--verbose', dest='verbose', action='store_true',
                        default=False)
    parser.add_option('-q', '--quiet', dest='verbose', action='store_false')
    parser.add_option('-f', '--force', dest='verify', action='store_false',
                        help='ignore auids not present on the daemon, '
                        'never prompt')
    parser.add_option('-i', '--verify', dest='verify', action='store_true',
                        default=False, help='prompt before each move')
    parser.add_option('--commands', action='store_true', default=False,
                        help='print mv commands, but do not move files')
    parser.add_option('-d', '--directory', default='.',
                        help='the daemon directory where ./cache is '
                        '(default: \'.\')')
    parser.add_option('--dest', default='deleted',
                        help='where under the daemon directory the cache '
                        'entries are moved to (default: \'deleted\')')
    return parser


def _process_args():
    parser = _parser()
    (options, arguments) = parser.parse_args()
    if arguments != []:
        parser.error('There should be no arguments. Try --help')
    return options

def _auid(cache_dir):
    """Return the AUID for the given cache dir."""
    # If the #au_id_file isn't present, the daemon doesn't list the
    # directory in the table, so no need to check if the file exists.
    path = os.path.join(cache_dir, '#au_id_file')
    f = open(os.path.join(path))
    try:
        auid = None
        for line in f.readlines():
            line = line.strip()
            if line and line[0] != '#':
                if auid is None:
                    auid = line
                else:
                    raise IdFileException('%s contains more than one line.'
                                          % path)
        if auid is None:
            raise IdFileException('%s contains no AUID.' % path)
    finally:
        f.close()
    return auid


def main():
    options = _process_args()
    src = options.directory
    local_txt = os.path.join(src, 'local.txt')
    if (not os.path.isdir(os.path.join(src, 'cache'))
        or not os.path.isfile(local_txt)):
        raise Exception('%s doesn\'t look like a daemon directory. '
                        'Try --directory.' % src)

    config = ConfigParser.ConfigParser()
    local_config = open(local_txt)
    config.readfp(_SectionAdder('foo', local_config))
    port = config.get('foo', 'org.lockss.ui.port')

    fix_auth_failure.fix_auth_failure()
    client = lockss_daemon.Client('127.0.0.1', port,
                                  options.user, options.password)
    repos = client._getStatusTable( 'RepositoryTable' )[ 1 ]

    no_auid = [r for r in repos if r['status'] == 'No AUID']
    if no_auid:
        print 'Warning: These cache directories have no AUID:'
        for r in no_auid:
            print r['dir']
        print

    deleted = [r for r in repos if r['status'] == 'Deleted']
    for r in deleted:
        r['auid'] = _auid(os.path.join(src, r['dir']))
    deleted.sort(key=lambda r: r['auid'])

    move_all = False
    if options.verbose:
        if deleted:
            print 'These AUs have been deleted on the daemon:'
            for r in deleted:
                print r['auid']
            if options.verify:
                move_all = raw_input('move all [y]? ').startswith('y')
        else:
            print 'No deleted AUs.'

    verify_each = options.verify and not move_all
    dst = os.path.join(options.directory, options.dest)
    for r in deleted:
        dir = r['dir']
        if not verify_each or \
                verify_each and \
                raw_input('move %s [n]? ' % r['auid']).startswith('y'):
            src_r = os.path.join(src, dir)
            dst_r = os.path.join(dst, dir)
            if options.commands:
                print "mv %s %s # %s" % (src_r, dst_r, r['auid'])
            else:
                os.renames(src_r, dst_r)


if __name__ == '__main__':
    main()
