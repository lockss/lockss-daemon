#!/usr/bin/python

# $Id: tdbout.py,v 1.1 2010-04-02 11:15:16 thib_gc Exp $
#
# Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

import csv
from optparse import OptionGroup
import tdbq
import sys

class TdboutConstants:
    
    VERSION = '0.1.0'
    OPTION_LONG = '--'
    OPTION_SHORT = '-'
    OPTION_CSV = 'csv'
    OPTION_CSV_SHORT = 'c'
    OPTION_CSV_META = 'CSFIELDS'
    OPTION_CSV_HELP = 'display the comma-separated fields as comma-separated values'
    OPTION_TSV = 'tsv'
    OPTION_TSV_SHORT = 't'
    OPTION_TSV_META = 'CSFIELDS'
    OPTION_TSV_HELP = 'display the comma-separated fields as tab-separated values'

def __dispatch__(tdb, options):
    if not (options.csv or options.tsv): return False
    lambdas = list()
    if options.tsv: str = options.tsv
    else: str = options.csv
    for word in str.split(','):
        fn = tdbq.str_to_lambda_au(word)
        if fn is None: raise RuntimeError, 'invalid field: %s' % (word,)
        else: lambdas.append(fn)
    rows = [[f(au) or '' for f in lambdas] for au in tdb.aus()]
    if options.tsv:
        for row in rows: print '\t'.join(row)
    else:
        writer = csv.writer(sys.stdout, dialect='excel')
        for row in rows: writer.writerow(row)
    return True

def __reprocess_options__(parser, options):
    if options.csv and options.tsv:
        parser.error('%(short)s%(csvshort)s/%(long)s%(csv)s and %(short)s%(tsvshort)s/%(long)s%(tsv)s are mutually exclusive' % {'long': TdboutConstants.OPTION_LONG,
                                                                                                                                 'short': TdboutConstants.OPTION_SHORT,
                                                                                                                                 'csv': TdboutConstants.OPTION_CSV,
                                                                                                                                 'csvshort': TdboutConstants.OPTION_CSV_SHORT,
                                                                                                                                 'tsv': TdboutConstants.OPTION_TSV,
                                                                                                                                 'tsvshort': TdboutConstants.OPTION_TSV_SHORT})

def __option_parser__(parser):
    tdbout_group = OptionGroup(parser, 'tdbout module (%s)' % (TdboutConstants.VERSION,))
    tdbout_group.add_option(TdboutConstants.OPTION_SHORT + TdboutConstants.OPTION_CSV_SHORT,
                            TdboutConstants.OPTION_LONG + TdboutConstants.OPTION_CSV,
                            metavar=TdboutConstants.OPTION_CSV_META,
                            dest=TdboutConstants.OPTION_CSV,
                            help=TdboutConstants.OPTION_CSV_HELP)
    tdbout_group.add_option(TdboutConstants.OPTION_SHORT + TdboutConstants.OPTION_TSV_SHORT,
                            TdboutConstants.OPTION_LONG + TdboutConstants.OPTION_TSV,
                            metavar=TdboutConstants.OPTION_TSV_META,
                            dest=TdboutConstants.OPTION_TSV,
                            help=TdboutConstants.OPTION_TSV_HELP)
    parser.add_option_group(tdbout_group)
