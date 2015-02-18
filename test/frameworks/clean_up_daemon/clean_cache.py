#!/usr/bin/env python

# $Id$

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
__version__ = "1.0.2"


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
    parser.add_option('-c', '--commands', action='store_true', default=False,
                        help='print mv commands, but do not move files')
    parser.add_option('-d', '--directory', default='.',
                        help='the daemon directory where ./cache is '
                        '(default: \'%default\')')
    parser.add_option('--dest', default='deleted',
                        help='where under the daemon directory the cache '
                        'entries are moved to (default: \'%default\')')
    return parser


def _process_args():
    parser = _parser()
    (options, arguments) = parser.parse_args()
    if arguments != []:
        parser.error('There should be no arguments. Try --help')
    return options

def _auid(cache_dir):
    """Return the AUID for the given cache dir."""
    # If the #au_id_file isn't present, or doesn't contain an au.id
    # entry, the daemon doesn't list the directory in the table, so no
    # need to check either condition. 
    path = os.path.join(cache_dir, '#au_id_file')
    config = ConfigParser.ConfigParser()
    f = open(os.path.join(path))
    try:
        config.readfp(_SectionAdder('foo', f))
        auid = config.get('foo', 'au.id')
        # If this fails, something very odd is going on, and a human
        # should check.
        assert auid
    finally:
        f.close()
    return auid


def main():
    options = _process_args()
    src = options.directory
    local_txt = os.path.join(src, 'local.txt')
    if not os.path.isdir(os.path.join(src, 'cache')):
        raise Exception('%s doesn\'t look like a daemon directory. '
                        'Try --directory.' % src)

    if 'LOCKSS_IPADDR' in os.environ: ipAddr = os.environ['LOCKSS_IPADDR']
    else: ipAddr = '127.0.0.1'

    if 'LOCKSS_UI_PORT' in os.environ:
        port = os.environ['LOCKSS_UI_PORT']
    else:
        if not os.path.isfile(local_txt):
          raise Exception('LOCKSS_UI_PORT is not set but there is no'
                          '%s' % (local_txt,))
        config = ConfigParser.ConfigParser()
        local_config = open(local_txt)
        try:
            config.readfp(_SectionAdder('foo', local_config))
            port = config.get('foo', 'org.lockss.ui.port')
        finally:
            local_config.close()

    fix_auth_failure.fix_auth_failure()
    client = lockss_daemon.Client(ipAddr, port,
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
            if os.path.isabs(dir):
              if not dir.startswith(options.directory): print 'Absolute/relative path mismatch: %s' % (dir,)
              dst_r = os.path.join(dst, dir[len(options.directory)+1:])
            else: dst_r = os.path.join(dst, dir)
            if options.commands:
                print "mv %s %s # %s" % (src_r, dst_r, r['auid'])
            else:
                os.renames(src_r, dst_r)


if __name__ == '__main__':
    main()
