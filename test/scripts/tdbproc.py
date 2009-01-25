#!/usr/bin/python

# $Id: tdbproc.py,v 1.5 2009-01-25 01:34:35 thib_gc Exp $
#
# Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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

from tdbconst import *
import tdblint

TDBPROC_VERSION = '0.1.2'

OPTION_LONG        = '--'
OPTION_SHORT       = '-'

TDB_OPTION_LEVEL_SHORT = 'l'
TDB_OPTION_STYLE_SHORT = 's'

def _make_command_line_parser():
    '''Builds a new command line option parser'''

    from optparse import OptionGroup, OptionParser
    parser = OptionParser(version=TDBPROC_VERSION)

    parser.add_option(OPTION_SHORT + TDB_OPTION_LEVEL_SHORT,
                      OPTION_LONG + TDB_OPTION_LEVEL,
                      dest=TDB_OPTION_LEVEL,
                      action='append',
                      help='output level, comma-separated (%default)')
    parser.add_option(OPTION_SHORT + TDB_OPTION_STYLE_SHORT,
                      OPTION_LONG + TDB_OPTION_STYLE,
                      dest=TDB_OPTION_STYLE,
                      action='store',
                      choices=TDB_STYLES,
                      default=TDB_STYLE_DEFAULT,
                      help='output style (default: %default)')

## @begin tdblint
    tdblint.__option_parser__(parser)
## @end tdblint

    return parser

def _dispatch(tdb, options):
## @begin tdblint
    if tdblint.__dispatch__(options): tdblint.tdblint(tdb, options)
## @end tdblint
## @begin tdbxml
    elif options.style in [ TDB_STYLE_XML, TDB_STYLE_XML_ENTRIES, TDB_STYLE_XML_LEGACY ]:
        from tdbxml import tdb_to_xml
        tdb_to_xml(tdb, options)
## @end tdbxml
    else:
        if options.style is not TDB_STYLE_NONE:
            print '(no output)'

def _reprocess_levels(options):
    if options.level is None or len(options.level) == 0:
        options.level = TDB_LEVEL_DEFAULT
    levels = []
    input = []
    for str in options.level: input.extend(str.split(','))
    for level in input:
        add = []
        if level == TDB_LEVEL_CONTENT_TESTING: add.extend(TDB_LEVEL_CONTENT_TESTING_STATUSES)
        elif level == TDB_LEVEL_EVERYTHING: add.extend(TDB_LEVEL_EVERYTHING_STATUSES)
        elif level == TDB_LEVEL_PRODUCTION: add.extend(TDB_LEVEL_PRODUCTION_STATUSES)
        elif level != '': add.append(level)
        for lev in add:
            if lev not in levels: levels.append(lev)
    options.level = levels
        
if __name__ == '__main__':
    from tdbparse import TdbScanner, TdbParser
    from sys import stdin, argv
    parser = _make_command_line_parser()
    (options, argv[1:]) = parser.parse_args(values=parser.get_default_values())
    _reprocess_levels(options)
    scanner = TdbScanner(stdin)
    parser = TdbParser(scanner)
    tdb = parser.parse()
    _dispatch(tdb, options)
