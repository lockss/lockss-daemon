#!/usr/bin/python

# $Id: tdbsel.py,v 1.1 2009-01-26 00:02:07 thib_gc Exp $
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

from tdb import *

TDBSEL_VERSION = '0.1.1'

OPTION_SELECT_NAME = ( 'selectName', None, 'NAME', 'select AUs whose name is %s' )
OPTION_SELECT_NAME_BEGINS = ( 'selectNameBegins', None, 'NAME', 'select AUs whose name begins with %s' )
OPTION_SELECT_NAME_CONTAINS = ( 'selectNameContains', None, 'NAME', 'select AUs whose name contains %s' )
OPTION_SELECT_NAME_ENDS = ( 'selectNameEnds', None, 'NAME', 'select AUs whose name ends with %s' )

OPTION_SELECT_PUBLISHER = ( 'selectPublisher', None, 'NAME', 'select AUs whose publisher name is %s' )
OPTION_SELECT_PUBLISHER_BEGINS = ( 'selectPublisherBegins', None, 'NAME', 'select AUs whose publisher name begins with %s' )
OPTION_SELECT_PUBLISHER_CONTAINS = ( 'selectPublisherContains', None, 'NAME', 'select AUs whose publisher name contains %s' )
OPTION_SELECT_PUBLISHER_ENDS = ( 'selectPublisherEnds', None, 'NAME', 'select AUs whose publisher name ends with %s' )

OPTION_SELECT_STATUS = ( 'selectStatus', 'S', 'STATUS', 'select AUs whose status is %s' )

OPTION_SELECT_TITLE = ( 'selectTitle', None, 'NAME', 'select AUs whose title name is %s' )
OPTION_SELECT_TITLE_BEGINS = ( 'selectTitleBegins', None, 'NAME', 'select AUs whose title name begins with %s' )
OPTION_SELECT_TITLE_CONTAINS = ( 'selectTitleContains', None, 'NAME', 'select AUs whose title name contains %s' )
OPTION_SELECT_TITLE_ENDS = ( 'selectTitleEnds', None, 'NAME', 'select AUs whose title name ends with %s' )

__TUPLES = ( OPTION_SELECT_NAME, OPTION_SELECT_NAME_BEGINS,
             OPTION_SELECT_NAME_CONTAINS, OPTION_SELECT_NAME_ENDS,
             OPTION_SELECT_PUBLISHER, OPTION_SELECT_PUBLISHER_BEGINS,
             OPTION_SELECT_PUBLISHER_CONTAINS, OPTION_SELECT_PUBLISHER_ENDS,
             OPTION_SELECT_STATUS,
             OPTION_SELECT_TITLE, OPTION_SELECT_TITLE_BEGINS,
             OPTION_SELECT_TITLE_CONTAINS, OPTION_SELECT_TITLE_ENDS )

def __option_parser__(parser):
    from optparse import OptionGroup
    tdbsel_group = OptionGroup(parser, 'tdbsel module (%s)' % ( TDBSEL_VERSION, ))
    for tup in __TUPLES:
        tupopt, tupsho, tupmet, tupstr = tup
        if tupsho: tdbsel_group.add_option('-' + tupsho,
                                           '--' + tupopt,
                                           dest=tupopt,
                                           metavar=tupmet,
                                           help=tupstr % ( tupmet, ) )
        else: tdbsel_group.add_option('--' + tupopt,
                                      dest=tupopt,
                                      metavar=tupmet,
                                      help=tupstr % ( tupmet, ) )
    parser.add_option_group(tdbsel_group)

def __dispatch__(options):
    for tup in __TUPLES:
        if getattr(options, tup[0]): return True
    return False

def __select_aus(tdb, test):
    ret = Tdb()
    for au in tdb.aus():
        if test(au): ret.add_au(au)
    return ret

def __tdb_coalesce(tdb):
    publisher, title = None, None
    for au in tdb.aus():
        if au.title().publisher() is not publisher:
            publisher = au.title().publisher()
            tdb.add_publisher(publisher)
        if au.title() is not title:
            title = au.title()
            tdb.add_title()

def tdb_select(tdb, options):
    if getattr(options, OPTION_SELECT_NAME[0]):
        tdb = __select_aus(tdb, lambda au: au.name() == getattr(options, OPTION_SELECT_NAME[0]))
    if getattr(options, OPTION_SELECT_NAME_BEGINS[0]):
        tdb = __select_aus(tdb, lambda au: au.name().startswith(getattr(options, OPTION_SELECT_NAME_BEGINS[0])))
    if getattr(options, OPTION_SELECT_NAME_CONTAINS[0]):
        tdb = __select_aus(tdb, lambda au: au.name().find(getattr(options, OPTION_SELECT_NAME_CONTAINS[0])) >= 0)
    if getattr(options, OPTION_SELECT_NAME_ENDS[0]):
        tdb = __select_aus(tdb, lambda au: au.name().endswith(getattr(options, OPTION_SELECT_NAME_ENDS[0])))
    if getattr(options, OPTION_SELECT_PUBLISHER[0]):
        tdb = __select_aus(tdb, lambda au: au.title().publisher().name() == getattr(options, OPTION_SELECT_PUBLISHER[0]))
    if getattr(options, OPTION_SELECT_PUBLISHER_BEGINS[0]):
        tdb = __select_aus(tdb, lambda au: au.title().publisher().name().startswith(getattr(options, OPTION_SELECT_PUBLISHER_BEGINS[0])))
    if getattr(options, OPTION_SELECT_PUBLISHER_CONTAINS[0]):
        tdb = __select_aus(tdb, lambda au: au.title().publisher().name().find(getattr(options, OPTION_SELECT_PUBLISHER_CONTAINS[0])) >= 0)
    if getattr(options, OPTION_SELECT_PUBLISHER_ENDS[0]):
        tdb = __select_aus(tdb, lambda au: au.title().publisher().name().endswith(getattr(options, OPTION_SELECT_PUBLISHER_ENDS[0])))
    if getattr(options, OPTION_SELECT_STATUS[0]):
        tdb = __select_aus(tdb, lambda au: au.status() == getattr(options, OPTION_SELECT_STATUS[0]))
    if getattr(options, OPTION_SELECT_TITLE[0]):
        tdb = __select_aus(tdb, lambda au: au.title().name() == getattr(options, OPTION_SELECT_TITLE[0]))
    if getattr(options, OPTION_SELECT_TITLE_BEGINS[0]):
        tdb = __select_aus(tdb, lambda au: au.title().name().startswith(getattr(options, OPTION_SELECT_TITLE_BEGINS[0])))
    if getattr(options, OPTION_SELECT_TITLE_CONTAINS[0]):
        tdb = __select_aus(tdb, lambda au: au.title().name().find(getattr(options, OPTION_SELECT_TITLE_CONTAINS[0])) >= 0)
    if getattr(options, OPTION_SELECT_TITLE_ENDS[0]):
        tdb = __select_aus(tdb, lambda au: au.title().name().endswith(getattr(options, OPTION_SELECT_TITLE_ENDS[0])))
    __coalesce_tdb(tdb)
    return tdb
