#!/usr/bin/env python

# $Id: tdbgln.py,v 1.4.8.1 2012-06-20 00:02:55 nchondros Exp $

# Copyright (c) 2000-2012 Bsoard of Trustees of Leland Stanford Jr. University,
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

from optparse import OptionGroup, OptionParser
import tdbout
import tdbparse
import tdbxml
import sys

class TdbglnConstants:
    '''Constants associated with the tdbgln module.'''
    
    VERSION = '''0.1.3'''
    
    DESCRIPTION = '''Invokes the tdbout or tdbxml module with
extensions and functionality specific to the Global LOCKSS Network
(GLN). See each module's help section to learn more about their
features and options.'''

    OPTION_ALLIANCE = 'alliance'
    OPTION_ALLIANCE_HELP = 'keep GLN AUs earmarked for the LOCKSS Alliance'

    OPTION_NON_ALLIANCE = 'non-alliance'
    OPTION_NON_ALLIANCE_HELP = 'keep GLN AUs not earmarked for the LOCKSS Alliance'

    OPTION_TDBOUT = 'tdbout'
    OPTION_TDBOUT_HELP = 'invoke the tdbout module with GLN extensions (must be placed first)'
    
    OPTION_TDBXML = 'tdbxml'
    OPTION_TDBXML_HELP = 'invoke the tdbxml module with GLN extensions (must be placed first)'
    
    MODES = [OPTION_TDBOUT,
             OPTION_TDBXML]

def __preliminary_reprocess_options__(parser, options):
    if options.tdbout or options.tdbxml: return
    parser.error('missing mode: one of %s' % ', '.join([str(parser.get_option('--' + op)) for op in TdbglnConstants.MODES]))
    
def __option_parser__(parser=None, prelim_opt=None):
    if parser is None: parser = OptionParser(description=TdbglnConstants.DESCRIPTION,
                                             version=TdbglnConstants.VERSION)
    if prelim_opt:
        if prelim_opt.tdbout: parser = tdbout.__option_parser__(parser)
        elif prelim_opt.tdbxml: parser = tdbxml.__option_parser__(parser)
        else: parser.error('internal error')
    tdbgln_group = OptionGroup(parser, 'tdbgln module (%s)' % (TdbglnConstants.VERSION,))
    tdbgln_group.add_option('--' + TdbglnConstants.OPTION_TDBOUT,
                            action='store_true',
                            default=False,
                            help=TdbglnConstants.OPTION_TDBOUT_HELP)
    tdbgln_group.add_option('--' + TdbglnConstants.OPTION_TDBXML,
                            action='store_true',
                            default=False,
                            help=TdbglnConstants.OPTION_TDBXML_HELP)
    tdbgln_group.add_option('--' + TdbglnConstants.OPTION_ALLIANCE,
                            action='store_true',
                            default=False,
                            help=TdbglnConstants.OPTION_ALLIANCE_HELP)
    tdbgln_group.add_option('--' + TdbglnConstants.OPTION_NON_ALLIANCE,
                            action='store_true',
                            default=False,
                            help=TdbglnConstants.OPTION_NON_ALLIANCE_HELP)
    parser.add_option_group(tdbgln_group)
    return parser

def __alliance__(is_alliance):
    non_alliance_plugins = ['edu.columbia.plugin.JiwsPlugin',
                            'edu.cornell.library.epr.EPRPlugin2001',
                            'edu.cornell.library.epr.EPRPlugin2002On',
                            'edu.cornell.library.jbe.JBEPlugin',
                            'edu.fcla.plugin.arkivoc.ArkivocPlugin',
                            'edu.harvard.plugin.AppliedSemiotics.AppliedSemioticsPlugin',
                            'edu.harvard.plugin.jrs.JRSPlugin',
                            'edu.harvard.plugin.WorldHaikuReview.WorldHaikuReviewPlugin',
                            'edu.indiana.lib.plugin.jcjpc.JcjpcPlugin',
                            'edu.indiana.lib.plugin.mto.MTOPlugin',
                            'edu.jhu.library.plugin.jrf.JournalOfReligionAndFilmPlugin',
                            'edu.jhu.library.plugin.MedievalForumPlugin',
                            'edu.nyu.plugin.bonefolder.BonefolderPlugin',
                            'edu.nyu.plugin.ejce.EJCEPlugin',
                            'edu.nyu.plugin.ejcjs.EJCJSPlugin',
                            'edu.nyu.plugin.heplwebzine.HEPLwebzine',
                            'edu.nyu.plugin.journalofglobalbuddhism.JournalOfGlobalBuddhismPlugin',
                            'edu.nyu.plugin.LeedsICSPlugin',
                            'edu.princeton.plugin.bmcr.BMCRPlugin',
                            'edu.princeton.plugin.ncaw.19thCenturyArtWorldwidePlugin',
                            'edu.stanford.plugin.exquisitecorpse.ExquisiteCorpsePlugin',
                            'edu.upenn.library.plugin.annualofurdustudies.AnnualOfUrduStudiesPlugin',
                            'edu.upenn.library.plugin.clcweb.CLCWebPlugin',
                            'edu.wisc.library.plugin.BigBridgePlugin',
                            'edu.wisc.library.plugin.BigBridgeVol1Plugin',
                            'edu.wisc.library.plugin.CortlandReviewPlugin',
                            'edu.wisc.library.plugin.CortlandReview00Plugin',
                            'edu.wisc.library.plugin.CortlandReview98Plugin',
                            'edu.wisc.library.plugin.CortlandReview99Plugin',
                            'edu.yale.library.lockss.plugin.intermarium.IntermariumPlugin',
                            'edu.yale.library.lockss.plugin.mitejmes.MITEJMESPlugin',
                            'gov.loc.plugin.CJPentecostalCharismaticResearchPlugin',
                            'gov.loc.plugin.TESLEJPlugin',
                            'nz.ac.otago.plugin.scholia.ScholiaPlugin',
                            'org.lockss.plugin.absinthe.AbsinthePlugin',
                            'org.lockss.plugin.bepress.BePressPlugin',
                            'org.lockss.plugin.bioone.BioOnePlugin',
                            'org.lockss.plugin.blackbird.BlackbirdPlugin',
                            'org.lockss.plugin.clogic.CulturalLogicPlugin',
                            'org.lockss.plugin.disputatio.DisputatioPlugin',
                            'org.lockss.plugin.emc.EarlyModernCulturePlugin',
                            'org.lockss.plugin.emls.EmlsPlugin',
                            'org.lockss.plugin.evergreenreview.EvergreenReviewPlugin',
                            'org.lockss.plugin.GendersPlugin',
                            'org.lockss.plugin.histcoop.HistoryCooperativePlugin',
                            'org.lockss.plugin.invisibleculture.InvisibleCulturePlugin',
                            'org.lockss.plugin.jackmagazine.JackMagazinePlugin',
                            'org.lockss.plugin.jscm.JSCMPlugin',
                            'org.lockss.plugin.lapetitezine.LaPetiteZinePlugin',
                            'org.lockss.plugin.locksscard.LockssCardPlugin',
                            'org.lockss.plugin.madhattersreview.MadHattersReviewPlugin',
                            'org.lockss.plugin.minerva.MinervaPlugin',
                            'org.lockss.plugin.msr.MSRPlugin',
                            'org.lockss.plugin.ojs.OJSPlugin',
                            'org.lockss.plugin.othervoices.OtherVoicesPlugin',
                            'org.lockss.plugin.projmuse.ProjectMusePlugin',
                            'org.lockss.plugin.prok.ProkPlugin',
                            'org.molvis.plugin.MolVisPlugin',
                            'org.lockss.plugin.sfpoetrybroadside.SantaFePoetryBroadsidePlugin',
                            'org.nypl.plugin.failbetter.FailbetterPlugin',
                            'org.nypl.plugin.PoetryBayPlugin',
                            'org.nypl.plugin.shampoo.ShampooPlugin',
                            'org.nypl.plugin.WordsWithoutBordersPlugin',
                            'za.ac.nlsa.lockss.plugin.WaterSAPlugin']
    if is_alliance: return ' and '.join(['plugin is not "%s"' % (str,) for str in non_alliance_plugins])
    else: return ' or '.join(['plugin is "%s"' % (str,) for str in non_alliance_plugins])

def __reprocess_options__(parser, options):
    if options.alliance or options.non_alliance:
        if options.query: options.query = '(%s) and (%s)' % (__alliance__(options.alliance), options.query)
        else: options.query = __alliance__(options.alliance)
    if options.tdbout: tdbout.__reprocess_options__(parser, options)
    elif options.tdbxml: tdbxml.__reprocess_options__(parser, options)
    else: parser.error('internal error')

def process_tdb(tdb, options):
    if options.tdbout: tdbout.process_tdbout(tdb, options)
    elif options.tdbxml: tdbxml.tdb_to_xml(tdb, options)
    else: raise RuntimeError, 'internal error'

if __name__ == '__main__':
    parser = __option_parser__(None, None)
    (options, args) = parser.parse_args(args=sys.argv[1:2], values=parser.get_default_values())
    __preliminary_reprocess_options__(parser, options)
    parser = __option_parser__(None, options)
    (options, args) = parser.parse_args(args=sys.argv[1:], values=parser.get_default_values())
    __reprocess_options__(parser, options)
    try:
        tdb = tdbparse.tdbparse(sys.stdin, options)
    except tdbparse.TdbparseSyntaxError, e:
        print >>sys.stderr, e
        exit(1)
    process_tdb(tdb, options)
