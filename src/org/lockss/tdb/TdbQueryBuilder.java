/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/

package org.lockss.tdb;

import java.util.*;
import java.util.regex.Pattern;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.cli.*;
import org.lockss.tdb.AntlrUtil.NamedAntlrInputStream;
import org.lockss.tdb.Predicates.*;
import org.lockss.tdb.TdbQueryParser.*;

/**
 * <p>
 * A module to parse TDB queries, based on the ANTLR-generated
 * <code>TdbQueryLexer.g4</code> and <code>TdbQueryParser.g4</code> grammars.
 * </p>
 * <p>
 * If options created by {@link #addOptions(Options)} are requested on the
 * command line processed by
 * {@link #processCommandLine(Map, CommandLineAccessor)},
 * {@link #getAuPredicate(Map)} will return a predicate that selects AUs based
 * on criteria supplied by the client. If no options relevant to this module are
 * requested, the returned predicate will always return <code>true</code> (i.e.
 * the return value will never be <code>null</code>).
 * </p>
 * <p>
 * <i>This class does not depend on class files found in the daemon's JAR
 * libraries or on source files found in the daemon's code base, but on source
 * files generated into <code>generated/src/</code> by the ANTLR tool. Run
 * <code>ant generate-parsers</code> to cause the necessary source files to be
 * generated.</i>
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.67
 */
public class TdbQueryBuilder extends TdbQueryParserBaseListener {

  /**
   * <p>
   * Key for the all option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_ALL = "all";
  
  /**
   * <p>
   * Single letter for the all option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_ALL = 'A';
  
  /**
   * <p>
   * Unmodifiable list of statuses associated with the all option.
   * </p>
   * 
   * @since 1.67
   * @see Au#STATUS_MANIFEST
   * @see Au#STATUS_WANTED
   * @see Au#STATUS_TESTING
   * @see Au#STATUS_NOT_READY
   * @see Au#STATUS_READY
   * @see Au#STATUS_READY_SOURCE
   * @see Au#STATUS_CRAWLING
   * @see Au#STATUS_DEEP_CRAWL
   * @see Au#STATUS_FROZEN
   * @see Au#STATUS_ING_NOT_READY
   * @see Au#STATUS_RELEASING
   * @see Au#STATUS_FINISHED
   * @see Au#STATUS_RELEASED
   * @see Au#STATUS_DOWN
   * @see Au#STATUS_SUPERSEDED
   */
  public static final List<String> ALL_STATUSES =
      AppUtil.ul(Au.STATUS_MANIFEST,
                 Au.STATUS_WANTED,
                 Au.STATUS_TESTING,
                 Au.STATUS_NOT_READY,
                 Au.STATUS_READY,
                 Au.STATUS_READY_SOURCE,
                 Au.STATUS_CRAWLING,
                 Au.STATUS_DEEP_CRAWL,
                 Au.STATUS_FROZEN,
                 Au.STATUS_ING_NOT_READY,
                 Au.STATUS_RELEASING,
                 Au.STATUS_FINISHED,
                 Au.STATUS_RELEASED,
                 Au.STATUS_DOWN,
                 Au.STATUS_SUPERSEDED);
  
  /**
   * <p>
   * The all option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_ALL =
      OptionBuilder.withLongOpt(KEY_ALL)
                   .withDescription(String.format("include all testable (pre-production and production) statuses in secondary query %s", ALL_STATUSES))
                   .create(LETTER_ALL);
  
  /**
   * <p>
   * Key for the alliance option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_ALLIANCE = "alliance";
  
  /**
   * <p>
   * The alliance option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_ALLIANCE =
      OptionBuilder.withLongOpt(KEY_ALLIANCE)
                   .withDescription(String.format("include only AUs whose plugin is not in the non-Alliance set"))
                   .create();
  
  /**
   * <p>
   * Key for the clockss-preserved option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_CLOCKSS_PRESERVED = "clockss-preserved";

  /**
   * <p>
   * Single letter for the clockss-preserved option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_CLOCKSS_PRESERVED = 'K';
  
  /**
   * <p>
   * Unmodifiable list of statuses associated with the clockss-preserved option.
   * </p>
   * 
   * @since 1.67
   * @see Au#STATUS_FINISHED
   * @see Au#STATUS_DOWN
   * @see Au#STATUS_SUPERSEDED
   */
  public static final List<String> CLOCKSS_PRESERVED_STATUSES =
      AppUtil.ul(Au.STATUS_FINISHED,
                 Au.STATUS_DOWN,
                 Au.STATUS_SUPERSEDED);
  
  /**
   * <p>
   * The clockss-preserved option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_CLOCKSS_PRESERVED =
      OptionBuilder.withLongOpt(KEY_CLOCKSS_PRESERVED)
                   .withDescription(String.format("include all CLOCKSS production statuses fit for 'Preserved' Keepers label in secondary query %s", CLOCKSS_PRESERVED_STATUSES))
                   .create(LETTER_CLOCKSS_PRESERVED);
  
  /**
   * <p>
   * Key for the clockss-production option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_CLOCKSS_PRODUCTION = "clockss-production";
  
  /**
   * <p>
   * Single letter for the clockss-production option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_CLOCKSS_PRODUCTION = 'J';
  
  /**
   * <p>
   * Unmodifiable list of statuses associated with the clockss-production option.
   * </p>
   * 
   * @since 1.67
   * @see Au#STATUS_CRAWLING
   * @see Au#STATUS_TESTING
   * @see Au#STATUS_NOT_READY
   * @see Au#STATUS_FROZEN
   * @see Au#STATUS_FINISHED
   * @see Au#STATUS_DOWN
   * @see Au#STATUS_SUPERSEDED
   */  
  public static final List<String> CLOCKSS_PRODUCTION_STATUSES =
      AppUtil.ul(Au.STATUS_CRAWLING,
                 Au.STATUS_TESTING,
                 Au.STATUS_NOT_READY,
                 Au.STATUS_FROZEN,
                 Au.STATUS_FINISHED,
                 Au.STATUS_DOWN,
                 Au.STATUS_SUPERSEDED);

  /**
   * <p>
   * The clockss-production option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_CLOCKSS_PRODUCTION =
      OptionBuilder.withLongOpt(KEY_CLOCKSS_PRODUCTION)
                   .withDescription(String.format("include all CLOCKSS production statuses in secondary query %s", CLOCKSS_PRODUCTION_STATUSES))
                   .create(LETTER_CLOCKSS_PRODUCTION);
  
  /**
   * <p>
   * Key for the crawling option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_CRAWLING = Au.STATUS_CRAWLING;

  /**
   * <p>
   * Single letter for the crawling option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_CRAWLING = 'C';
  
  /**
   * <p>
   * The crawling option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_CRAWLING =
      OptionBuilder.withLongOpt(KEY_CRAWLING)
                   .withDescription(String.format("include status '%s' in secondary query", Au.STATUS_CRAWLING))
                   .create(LETTER_CRAWLING);

  /**
   * <p>
   * Key for the deepCrawl option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_DEEP_CRAWL = Au.STATUS_DEEP_CRAWL;

  /**
   * <p>
   * Single letter for the deepCrawl option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_DEEP_CRAWL = 'L';
  
  /**
   * <p>
   * The deepCrawl option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_DEEP_CRAWL =
      OptionBuilder.withLongOpt(KEY_DEEP_CRAWL)
                   .withDescription(String.format("include status '%s' in secondary query", Au.STATUS_DEEP_CRAWL))
                   .create(LETTER_DEEP_CRAWL);

  /**
   * <p>
   * Key for the down option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_DOWN = Au.STATUS_DOWN;

  /**
   * <p>
   * Single letter for the down option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_DOWN = 'D';
  
  /**
   * <p>
   * The down option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_DOWN =
      OptionBuilder.withLongOpt(KEY_DOWN)
                   .withDescription(String.format("include status '%s' in secondary query", Au.STATUS_DOWN))
                   .create(LETTER_DOWN);

  /**
   * <p>
   * Key for the exists option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_EXISTS = Au.STATUS_EXISTS;

  /**
   * <p>
   * Single letter for the exists option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_EXISTS = 'E';
  
  /**
   * <p>
   * The exists option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_EXISTS =
      OptionBuilder.withLongOpt(KEY_EXISTS)
                   .withDescription(String.format("include status '%s' in secondary query", Au.STATUS_EXISTS))
                   .create(LETTER_EXISTS);

  /**
   * <p>
   * Key for the expected option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_EXPECTED = Au.STATUS_EXPECTED;

  /**
   * <p>
   * Single letter for the expected option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_EXPECTED = 'X';
  
  /**
   * <p>
   * The expected option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_EXPECTED =
      OptionBuilder.withLongOpt(KEY_EXPECTED)
                   .withDescription(String.format("include status '%s' in secondary query", Au.STATUS_EXPECTED))
                   .create(LETTER_EXPECTED);

  /**
   * <p>
   * Key for the finished option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_FINISHED = Au.STATUS_FINISHED;

  /**
   * <p>
   * Single letter for the finished option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_FINISHED = 'F';
  
  /**
   * <p>
   * The finished option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_FINISHED =
      OptionBuilder.withLongOpt(KEY_FINISHED)
                   .withDescription(String.format("include status '%s' in secondary query", Au.STATUS_FINISHED))
                   .create(LETTER_FINISHED);

  /**
   * <p>
   * Key for the frozen option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_FROZEN = Au.STATUS_FROZEN;

  /**
   * <p>
   * Single letter for the frozen option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_FROZEN = 'Z';
  
  /**
   * <p>
   * The frozen option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_FROZEN =
      OptionBuilder.withLongOpt(KEY_FROZEN)
                   .withDescription(String.format("include status '%s' in secondary query", Au.STATUS_FROZEN))
                   .create(LETTER_FROZEN);

  /**
   * <p>
   * Key for the ingNotReady option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_ING_NOT_READY = Au.STATUS_ING_NOT_READY;

  /**
   * <p>
   * Single letter for the ingNotReady option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_ING_NOT_READY = 'I';
  
  /**
   * <p>
   * The ingNotReady option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_ING_NOT_READY =
      OptionBuilder.withLongOpt(KEY_ING_NOT_READY)
                   .withDescription(String.format("include status '%s' in secondary query", Au.STATUS_ING_NOT_READY))
                   .create(LETTER_ING_NOT_READY);

  /**
   * <p>
   * Key for the manifest option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_MANIFEST = Au.STATUS_MANIFEST;

  /**
   * <p>
   * Single letter for the manifest option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_MANIFEST = 'M';
  
  /**
   * <p>
   * The manifest option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_MANIFEST =
      OptionBuilder.withLongOpt(KEY_MANIFEST)
                   .withDescription(String.format("include status '%s' in secondary query", Au.STATUS_MANIFEST))
                   .create(LETTER_MANIFEST);

  /**
   * <p>
   * Key for the non-alliance option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_NON_ALLIANCE = "non-alliance";
  
  /**
   * <p>
   * Unmodifiable list of plugins associated with the non-alliance option.
   * </p>
   * 
   * @since 1.67
   */
  public static final List<String> NON_ALLIANCE_PLUGINS =
      AppUtil.ul("edu.columbia.plugin.JiwsPlugin",
                 "edu.cornell.library.epr.EPRPlugin2001",
                 "edu.cornell.library.epr.EPRPlugin2002On",
                 "edu.cornell.library.jbe.JBEPlugin",
                 "edu.fcla.plugin.arkivoc.ArkivocPlugin",
                 "edu.harvard.plugin.AppliedSemiotics.AppliedSemioticsPlugin",
                 "edu.harvard.plugin.jrs.JRSPlugin",
                 "edu.harvard.plugin.WorldHaikuReview.WorldHaikuReviewPlugin",
                 "edu.indiana.lib.plugin.jcjpc.JcjpcPlugin",
                 "edu.indiana.lib.plugin.mto.MTOPlugin",
                 "edu.jhu.library.plugin.jrf.JournalOfReligionAndFilmPlugin",
                 "edu.jhu.library.plugin.MedievalForumPlugin",
                 "edu.nyu.plugin.bonefolder.BonefolderPlugin",
                 "edu.nyu.plugin.ejce.EJCEPlugin",
                 "edu.nyu.plugin.ejcjs.EJCJSPlugin",
                 "edu.nyu.plugin.heplwebzine.HEPLwebzine",
                 "edu.nyu.plugin.journalofglobalbuddhism.JournalOfGlobalBuddhismPlugin",
                 "edu.nyu.plugin.LeedsICSPlugin",
                 "edu.princeton.plugin.bmcr.BMCRPlugin",
                 "edu.princeton.plugin.ncaw.19thCenturyArtWorldwidePlugin",
                 "edu.stanford.plugin.exquisitecorpse.ExquisiteCorpsePlugin",
                 "edu.upenn.library.plugin.annualofurdustudies.AnnualOfUrduStudiesPlugin",
                 "edu.upenn.library.plugin.clcweb.CLCWebPlugin",
                 "edu.wisc.library.plugin.BigBridgePlugin",
                 "edu.wisc.library.plugin.BigBridgeVol1Plugin",
                 "edu.wisc.library.plugin.CortlandReviewPlugin",
                 "edu.wisc.library.plugin.CortlandReview00Plugin",
                 "edu.wisc.library.plugin.CortlandReview98Plugin",
                 "edu.wisc.library.plugin.CortlandReview99Plugin",
                 "edu.yale.library.lockss.plugin.intermarium.IntermariumPlugin",
                 "edu.yale.library.lockss.plugin.mitejmes.MITEJMESPlugin",
                 "gov.loc.plugin.CJPentecostalCharismaticResearchPlugin",
                 "gov.loc.plugin.TESLEJPlugin",
                 "nz.ac.otago.plugin.scholia.ScholiaPlugin",
                 "org.lockss.plugin.absinthe.AbsinthePlugin",
                 "org.lockss.plugin.bepress.BePressPlugin",
                 "org.lockss.plugin.bioone.BioOnePlugin",
                 "org.lockss.plugin.blackbird.BlackbirdPlugin",
                 "org.lockss.plugin.clogic.CulturalLogicPlugin",
                 "org.lockss.plugin.disputatio.DisputatioPlugin",
                 "org.lockss.plugin.emc.EarlyModernCulturePlugin",
                 "org.lockss.plugin.emls.EmlsPlugin",
                 "org.lockss.plugin.evergreenreview.EvergreenReviewPlugin",
                 "org.lockss.plugin.GendersPlugin",
                 "org.lockss.plugin.histcoop.HistoryCooperativePlugin",
                 "org.lockss.plugin.invisibleculture.InvisibleCulturePlugin",
                 "org.lockss.plugin.jackmagazine.JackMagazinePlugin",
                 "org.lockss.plugin.jscm.JSCMPlugin",
                 "org.lockss.plugin.lapetitezine.LaPetiteZinePlugin",
                 "org.lockss.plugin.locksscard.LockssCardPlugin",
                 "org.lockss.plugin.madhattersreview.MadHattersReviewPlugin",
                 "org.lockss.plugin.minerva.MinervaPlugin",
                 "org.lockss.plugin.msr.MSRPlugin",
                 "org.lockss.plugin.ojs.OJSPlugin",
                 "org.lockss.plugin.othervoices.OtherVoicesPlugin",
                 "org.lockss.plugin.projmuse.ProjectMusePlugin",
                 "org.lockss.plugin.prok.ProkPlugin",
                 "org.molvis.plugin.MolVisPlugin",
                 "org.lockss.plugin.sfpoetrybroadside.SantaFePoetryBroadsidePlugin",
                 "org.nypl.plugin.failbetter.FailbetterPlugin",
                 "org.nypl.plugin.PoetryBayPlugin",
                 "org.nypl.plugin.shampoo.ShampooPlugin",
                 "org.nypl.plugin.WordsWithoutBordersPlugin",
                 "za.ac.nlsa.lockss.plugin.WaterSAPlugin");
  
  /**
   * <p>
   * The alliance option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_NON_ALLIANCE =
      OptionBuilder.withLongOpt(KEY_NON_ALLIANCE)
                   .withDescription(String.format("include only AUs whose plugin is in the non-Alliance set"))
                   .create();
  
  /**
   * <p>
   * Key for the notReady option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_NOT_READY = Au.STATUS_NOT_READY;

  /**
   * <p>
   * Single letter for the notReady option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_NOT_READY = 'N';
  
  /**
   * <p>
   * The notReady option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_NOT_READY =
      OptionBuilder.withLongOpt(KEY_NOT_READY)
                   .withDescription(String.format("include status '%s' in secondary query", Au.STATUS_NOT_READY))
                   .create(LETTER_NOT_READY);

  /**
   * <p>
   * Key for the production option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_PRODUCTION = "production";

  /**
   * <p>
   * Single letter for the production option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_PRODUCTION = 'P';

  /**
   * <p>
   * Unmodifiable list of statuses associated with the production option.
   * </p>
   * 
   * @since 1.67
   * @see Au#STATUS_FINISHED
   * @see Au#STATUS_RELEASED
   * @see Au#STATUS_DOWN
   * @see Au#STATUS_SUPERSEDED
   */
  public static final List<String> PRODUCTION_STATUSES =
      AppUtil.ul(Au.STATUS_FINISHED,
                 Au.STATUS_RELEASED,
                 Au.STATUS_DOWN,
                 Au.STATUS_SUPERSEDED);
  
  /**
   * <p>
   * The production option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_PRODUCTION =
      OptionBuilder.withLongOpt(KEY_PRODUCTION)
                   .withDescription(String.format("include all production statuses in secondary query %s", PRODUCTION_STATUSES))
                   .create(LETTER_PRODUCTION);

  /**
   * <p>
   * Key for the query option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_QUERY = "query";

  /**
   * <p>
   * Single letter for the query option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_QUERY = 'Q';
  
  /**
   * <p>
   * The query option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_QUERY =
      OptionBuilder.withLongOpt(KEY_QUERY)
                   .hasArg()
                   .withArgName(KEY_QUERY.toUpperCase())
                   .withDescription(String.format("use principal query %s", KEY_QUERY.toUpperCase()))
                   .create(LETTER_QUERY);

  /**
   * <p>
   * Key for the ready option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_READY = Au.STATUS_READY;

  /**
   * <p>
   * Single letter for the ready option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_READY = 'Y';
  
  /**
   * <p>
   * The ready option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_READY =
      OptionBuilder.withLongOpt(KEY_READY)
                   .withDescription(String.format("include status '%s' in secondary query", Au.STATUS_READY))
                   .create(LETTER_READY);

  /**
   * <p>
   * Key for the readySource option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_READY_SOURCE = Au.STATUS_READY_SOURCE;

  /**
   * <p>
   * Single letter for the readySource option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_READY_SOURCE = 'O';
  
  /**
   * <p>
   * The readySource option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_READY_SOURCE =
      OptionBuilder.withLongOpt(KEY_READY_SOURCE)
                   .withDescription(String.format("include status '%s' in secondary query", Au.STATUS_READY_SOURCE))
                   .create(LETTER_READY_SOURCE);

  /**
   * <p>
   * Key for the released option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_RELEASED = Au.STATUS_RELEASED;

  /**
   * <p>
   * Single letter for the released option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_RELEASED = 'R';
  
  /**
   * <p>
   * The released option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_RELEASED =
      OptionBuilder.withLongOpt(KEY_RELEASED)
                   .withDescription(String.format("include status '%s' in secondary query", Au.STATUS_RELEASED))
                   .create(LETTER_RELEASED);

  /**
   * <p>
   * Key for the releasing option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_RELEASING = Au.STATUS_RELEASING;

  /**
   * <p>
   * Single letter for the releasing option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_RELEASING = 'G';
  
  /**
   * <p>
   * The releasing option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_RELEASING =
      OptionBuilder.withLongOpt(KEY_RELEASING)
                   .withDescription(String.format("include status '%s' in secondary query", Au.STATUS_RELEASING))
                   .create(LETTER_RELEASING);

  /**
   * <p>
   * Key for the status2 option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_STATUS2 = "status2";

  /**
   * <p>
   * Single letter for the status2 option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_STATUS2 = '2';
  
  /**
   * <p>
   * The status2 option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_STATUS2 =
      OptionBuilder.withLongOpt(KEY_STATUS2)
                   .withDescription(String.format("use status2 instead of status in secondary query"))
                   .create(LETTER_STATUS2);

  /**
   * <p>
   * Key for the superseded option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_SUPERSEDED = Au.STATUS_SUPERSEDED;

  /**
   * <p>
   * Single letter for the superseded option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_SUPERSEDED = 'S';
  
  /**
   * <p>
   * The superseded option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_SUPERSEDED =
      OptionBuilder.withLongOpt(KEY_SUPERSEDED)
                   .withDescription(String.format("include status '%s' in secondary query", Au.STATUS_SUPERSEDED))
                   .create(LETTER_SUPERSEDED);

  /**
   * <p>
   * Key for the testing option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_TESTING = Au.STATUS_TESTING;

  /**
   * <p>
   * Single letter for the testing option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_TESTING = 'T';
  
  /**
   * <p>
   * The testing option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_TESTING =
      OptionBuilder.withLongOpt(KEY_TESTING)
                   .withDescription(String.format("include status '%s' in secondary query", Au.STATUS_TESTING))
                   .create(LETTER_TESTING);

  /**
   * <p>
   * Key for the unreleased option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_UNRELEASED = "unreleased";

  /**
   * <p>
   * Single letter for the unreleased option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_UNRELEASED = 'U';

  /**
   * <p>
   * Unmodifiable list of statuses associated with the all option.
   * </p>
   * 
   * @since 1.67
   * @see Au#STATUS_MANIFEST
   * @see Au#STATUS_WANTED
   * @see Au#STATUS_TESTING
   * @see Au#STATUS_NOT_READY
   * @see Au#STATUS_READY
   * @see Au#STATUS_READY_SOURCE
   * @see Au#STATUS_CRAWLING
   * @see Au#STATUS_DEEP_CRAWL
   * @see Au#STATUS_FROZEN
   * @see Au#STATUS_ING_NOT_READY
   * @see Au#STATUS_RELEASING
   */
  public static final List<String> UNRELEASED_STATUSES =
      AppUtil.ul(Au.STATUS_MANIFEST,
                 Au.STATUS_WANTED,
                 Au.STATUS_TESTING,
                 Au.STATUS_NOT_READY,
                 Au.STATUS_READY,
                 Au.STATUS_READY_SOURCE,
                 Au.STATUS_CRAWLING,
                 Au.STATUS_DEEP_CRAWL,
                 Au.STATUS_FROZEN,
                 Au.STATUS_ING_NOT_READY,
                 Au.STATUS_RELEASING);
  
  /**
   * <p>
   * The unreleased option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_UNRELEASED =
      OptionBuilder.withLongOpt(KEY_UNRELEASED)
                   .withDescription(String.format("include all unreleased (pre-production) statuses in secondary query %s", UNRELEASED_STATUSES))
                   .create(LETTER_UNRELEASED);

  /**
   * <p>
   * Key for the wanted option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_WANTED = Au.STATUS_WANTED;

  /**
   * <p>
   * Single letter for the wanted option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_WANTED = 'W';
  
  /**
   * <p>
   * The wanted option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_WANTED =
      OptionBuilder.withLongOpt(KEY_WANTED)
                   .withDescription(String.format("include status '%s' in secondary query", Au.STATUS_WANTED))
                   .create(LETTER_WANTED);

  /**
   * <p>
   * Key for the zapped option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_ZAPPED = Au.STATUS_ZAPPED;

  /**
   * <p>
   * Single letter for the zapped option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_ZAPPED = 'B';
  
  /**
   * <p>
   * The zapped option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_ZAPPED =
      OptionBuilder.withLongOpt(KEY_ZAPPED)
                   .withDescription(String.format("include status '%s' in secondary query", Au.STATUS_ZAPPED))
                   .create(LETTER_ZAPPED);

  /**
   * <p>
   * A parse-time stack of predicates.
   * </p>
   * 
   * @since 1.67
   */
  protected Stack<Predicate<Au>> predicateStack;

  /**
   * <p>
   * Makes a new TDB query builder.
   * </p>
   * 
   * @since 1.67
   */
  public TdbQueryBuilder() {
    this.predicateStack = new Stack<Predicate<Au>>();
  }

  /**
   * <p>
   * Add this module's options to a Commons CLI {@link Options} instance.
   * </p>
   * 
   * @param options
   *          A Commons CLI {@link Options} instance.
   * @since 1.67
   */
  public void addOptions(Options options) {
    options.addOption(OPTION_ALL);
    options.addOption(OPTION_ALLIANCE);
    options.addOption(OPTION_CLOCKSS_PRESERVED);
    options.addOption(OPTION_CLOCKSS_PRODUCTION);
    options.addOption(OPTION_CRAWLING);
    options.addOption(OPTION_DEEP_CRAWL);
    options.addOption(OPTION_DOWN);
    options.addOption(OPTION_EXISTS);
    options.addOption(OPTION_EXPECTED);
    options.addOption(OPTION_FINISHED);
    options.addOption(OPTION_FROZEN);
    options.addOption(OPTION_ING_NOT_READY);
    options.addOption(OPTION_MANIFEST);
    options.addOption(OPTION_NON_ALLIANCE);
    options.addOption(OPTION_NOT_READY);
    options.addOption(OPTION_PRODUCTION);
    options.addOption(OPTION_QUERY);
    options.addOption(OPTION_READY);
    options.addOption(OPTION_READY_SOURCE);
    options.addOption(OPTION_RELEASED);
    options.addOption(OPTION_RELEASING);
    options.addOption(OPTION_STATUS2);
    options.addOption(OPTION_SUPERSEDED);
    options.addOption(OPTION_TESTING);
    options.addOption(OPTION_UNRELEASED);
    options.addOption(OPTION_WANTED);
    options.addOption(OPTION_ZAPPED);
  }
  
  /**
   * <p>
   * Processes a {@link CommandLineAccessor} instance and stores appropriate
   * information in the given options map.
   * </p>
   * 
   * @param options
   *          An options map.
   * @param cmd
   *          A {@link CommandLineAccessor} instance.
   * @since 1.67
   */
  public void processCommandLine(Map<String, Object> options, CommandLineAccessor cmd) {
    // Parse primary query into predicate
    Predicate<Au> queryPredicate = null;
    if (cmd.hasOption(KEY_QUERY)) {
      String query = cmd.getOptionValue(KEY_QUERY);
      CharStream charStream = new NamedAntlrInputStream("<query>", query);
      TdbQueryLexer lexer = new TdbQueryLexer(charStream);
      AntlrUtil.setEmacsErrorListener(lexer);
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      TdbQueryParser parser = new TdbQueryParser(tokens);
      AntlrUtil.setEmacsErrorListener(parser);
      ParserRuleContext tree = parser.query();
      ParseTreeWalker walker = new ParseTreeWalker();
      walker.walk(this, tree);
      if (predicateStack.size() != 1) {
        AppUtil.error("Internal error: predicate stack size is %d", predicateStack.size());
      }
      queryPredicate = predicateStack.pop();
    }
    
    // Build status predicate
    Predicate<Au> statusPredicate = null;
    final Set<String> secondarySet = new HashSet<String>();
    if (cmd.hasOption(KEY_ALL)) {
      secondarySet.addAll(ALL_STATUSES);
    }
    if (cmd.hasOption(KEY_CLOCKSS_PRESERVED)) {
      secondarySet.addAll(CLOCKSS_PRESERVED_STATUSES);
    }
    if (cmd.hasOption(KEY_CLOCKSS_PRODUCTION)) {
      secondarySet.addAll(CLOCKSS_PRODUCTION_STATUSES);
    }
    if (cmd.hasOption(KEY_CRAWLING)) {
      secondarySet.add(Au.STATUS_CRAWLING);
    }
    if (cmd.hasOption(KEY_DEEP_CRAWL)) {
      secondarySet.add(Au.STATUS_DEEP_CRAWL);
    }
    if (cmd.hasOption(KEY_DOWN)) {
      secondarySet.add(Au.STATUS_DOWN);
    }
    if (cmd.hasOption(KEY_EXISTS)) {
      secondarySet.add(Au.STATUS_EXISTS);
    }
    if (cmd.hasOption(KEY_EXPECTED)) {
      secondarySet.add(Au.STATUS_EXPECTED);
    }
    if (cmd.hasOption(KEY_FINISHED)) {
      secondarySet.add(Au.STATUS_FINISHED);
    }
    if (cmd.hasOption(KEY_FROZEN)) {
      secondarySet.add(Au.STATUS_FROZEN);
    }
    if (cmd.hasOption(KEY_ING_NOT_READY)) {
      secondarySet.add(Au.STATUS_ING_NOT_READY);
    }
    if (cmd.hasOption(KEY_MANIFEST)) {
      secondarySet.add(Au.STATUS_MANIFEST);
    }
    if (cmd.hasOption(KEY_NOT_READY)) {
      secondarySet.add(Au.STATUS_NOT_READY);
    }
    if (cmd.hasOption(KEY_PRODUCTION)) {
      secondarySet.addAll(PRODUCTION_STATUSES);
    }
    if (cmd.hasOption(KEY_READY)) {
      secondarySet.add(Au.STATUS_READY);
    }
    if (cmd.hasOption(KEY_READY_SOURCE)) {
      secondarySet.add(Au.STATUS_READY_SOURCE);
    }
    if (cmd.hasOption(KEY_RELEASED)) {
      secondarySet.add(Au.STATUS_RELEASED);
    }
    if (cmd.hasOption(KEY_RELEASING)) {
      secondarySet.add(Au.STATUS_RELEASING);
    }
    if (cmd.hasOption(KEY_SUPERSEDED)) {
      secondarySet.add(Au.STATUS_SUPERSEDED);
    }
    if (cmd.hasOption(KEY_TESTING)) {
      secondarySet.add(Au.STATUS_TESTING);
    }
    if (cmd.hasOption(KEY_UNRELEASED)) {
      secondarySet.addAll(UNRELEASED_STATUSES);
    }
    if (cmd.hasOption(KEY_WANTED)) {
      secondarySet.add(Au.STATUS_WANTED);
    }
    if (cmd.hasOption(KEY_ZAPPED)) {
      secondarySet.add(Au.STATUS_ZAPPED);
    }
    if (secondarySet.size() > 0) {
      if (cmd.hasOption(KEY_STATUS2)) {
        statusPredicate = new Predicate<Au>() {
          @Override
          public boolean test(Au a) {
            return secondarySet.contains(a.getStatus2());
          }
        };
      }
      else {
        statusPredicate = new Predicate<Au>() {
          @Override
          public boolean test(Au a) {
            return secondarySet.contains(a.getStatus());
          }
        };
      }
    }
    
    // Add non-Alliance predicate
    Predicate<Au> nonAlliancePredicate = null;
    final Set<String> nonAllianceSet = new HashSet<String>(NON_ALLIANCE_PLUGINS);
    if (cmd.hasOption(KEY_NON_ALLIANCE)) {
      nonAlliancePredicate = new Predicate<Au>() {
        @Override
        public boolean test(Au a) {
          return nonAllianceSet.contains(a.getPlugin());
        }
      };
    }
    else if (cmd.hasOption(KEY_ALLIANCE)) {
      nonAlliancePredicate = new Predicate<Au>() {
        @Override
        public boolean test(Au a) {
          return !nonAllianceSet.contains(a.getPlugin());
        }
      };
    }
    
    // Join predicates
    Predicate<Au> predicate = queryPredicate;
    if (statusPredicate != null) {
      predicate = (predicate == null ? statusPredicate : new AndPredicate<Au>(statusPredicate, predicate));
    }
    if (nonAlliancePredicate != null) {
      predicate = (predicate == null ? nonAlliancePredicate : new AndPredicate<Au>(nonAlliancePredicate, predicate));
    }
    if (predicate == null) {
      predicate = new TruePredicate<Au>();
    }
    options.put(KEY_QUERY, predicate);
  }
  
  /**
   * <p>
   * Retrieves from the options map the current AU predicate, which will never
   * be <code>null</code> (it will be an instance of
   * {@link Predicates.TruePredicate} if needed).
   * </p>
   * 
   * @param options
   *          The options map.
   * @return The current AU predicate or an instance of
   *         {@link Predicates.TruePredicate}.
   * @since 1.67
   */
  public Predicate<Au> getAuPredicate(Map<String, Object> options) {
    return (Predicate<Au>)options.get(KEY_QUERY);
  }
  
  /**
   * <p>
   * If processing an abstract syntax tree of type Or2, pops the two predicates
   * waiting on top of the stack, merges them into an
   * {@link Predicates.OrPredicate} instance, and pushes the result onto the
   * stack.
   * </p>
   * 
   * @param o2ctx
   *          Context supplied by ANTLR.
   * @since 1.67
   */
  @Override
  public void exitOr2(@NotNull Or2Context o2ctx) {
    Predicate<Au> op2 = predicateStack.pop();
    Predicate<Au> op1 = predicateStack.pop();
    predicateStack.push(new OrPredicate<Au>(op1, op2));
  }
  
  /**
   * <p>
   * If processing an abstract syntax tree of type And2, pops the two predicates
   * waiting on top of the stack, merges them into an
   * {@link Predicates.OrPredicate} instance, and pushes the result onto the
   * stack.
   * </p>
   * 
   * @param a2ctx
   *          Context supplied by ANTLR.
   * @since 1.67
   */
  @Override
  public void exitAnd2(@NotNull And2Context a2ctx) {
    Predicate<Au> op2 = predicateStack.pop();
    Predicate<Au> op1 = predicateStack.pop();
    predicateStack.push(new AndPredicate<Au>(op1, op2));
  }
  
  /**
   * <p>
   * If processing an abstract syntax tree of type ExprRegex, parses the regular
   * expression and pushes an appropriate predicate onto the stack.
   * </p>
   * 
   * @param erctx
   *          Context supplied by ANTLR.
   * @since 1.67
   */
  @Override
  public void exitExprRegex(@NotNull ExprRegexContext erctx) {
    final Functor<Au, String> traitFunctor = Au.traitFunctor(erctx.IDENTIFIER().getText());
    final Pattern pat = Pattern.compile(erctx.STRING().getText());
    Predicate<Au> ret = new Predicate<Au>() {
      @Override
      public boolean test(Au a) {
        String trait = traitFunctor.apply(a);
        return pat.matcher(trait == null ? "" : trait).find();
      }
    };
    if (erctx.DOES_NOT_MATCH() != null) {
      ret = new NotPredicate<Au>(ret);
    }
    predicateStack.push(ret);
  }
  
  /**
   * <p>
   * If processing an abstract syntax tree of type ExprString, pushes an
   * appropriate predicate onto the stack.
   * </p>
   * 
   * @param esctx
   *          Context supplied by ANTLR.
   * @since 1.67
   */
  @Override
  public void exitExprString(@NotNull ExprStringContext esctx) {
    final Functor<Au, String> traitFunctor = Au.traitFunctor(esctx.IDENTIFIER().getText());
    final String str = esctx.STRING().getText();
    Predicate<Au> ret = new Predicate<Au>() {
      @Override
      public boolean test(Au a) {
        return str.equals(traitFunctor.apply(a));
      }
    };
    if (esctx.NOT() != null || esctx.NOT_EQUALS() != null) {
      ret = new NotPredicate<Au>(ret);
    }
    predicateStack.push(ret);
  }
  
  /**
   * <p>
   * If processing an abstract syntax tree of type ExprSet, push an appropriate
   * predicate onto the stack.
   * </p>
   * 
   * @param esctx
   *          Context supplied by ANTLR.
   * @since 1.67
   */
  @Override
  public void exitExprSet(@NotNull ExprSetContext esctx) {
    final Functor<Au, String> traitFunctor = Au.traitFunctor(esctx.IDENTIFIER().getText());
    Predicate<Au> ret = new Predicate<Au>() {
        @Override
        public boolean test(Au a) {
          return traitFunctor.apply(a) != null;
        }
    };
    if (esctx.NOT() != null) {
      ret = new NotPredicate<Au>(ret);
    }
    predicateStack.push(ret);
  }

}
