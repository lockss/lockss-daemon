/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University,
All rights reserved.

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
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.tdb;

import java.util.*;
import java.util.regex.Pattern;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.cli.*;
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
   * A version string for the TdbQueryBuilder module ({@value}).
   * </p>
   * 
   * @since 1.68
   */
  public static final String VERSION = "[TdbQueryBuilder:0.3.0]";
  
  /**
   * <p>
   * Key for the all option ({@value}).
   * </p>
   * 
   * @since 1.67
   * @deprecated Use {@link #KEY_VIABLE} instead
   */
  @Deprecated
  protected static final String KEY_ALL = "all";
  
  /**
   * <p>
   * Single letter for the all option ({@value}).
   * </p>
   * 
   * @since 1.67
   * @deprecated Use {@link #LETTER_VIABLE} instead
   */
  @Deprecated
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
   * @see Au#STATUS_FROZEN
   * @see Au#STATUS_DEEP_CRAWL
   * @see Au#STATUS_ING_NOT_READY
   * @see Au#STATUS_RELEASING
   * @see Au#STATUS_FINISHED
   * @see Au#STATUS_RELEASED
   * @see Au#STATUS_DOWN
   * @see Au#STATUS_SUPERSEDED
   * @deprecated Use {@link #VIABLE_STATUSES} instead
   */
  @Deprecated
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
   * @deprecated Use {@link #OPTION_VIABLE} instead
   */
  @Deprecated
  protected static final Option OPTION_ALL =
      Option.builder(Character.toString(LETTER_ALL))
            .longOpt(KEY_ALL)
            .desc(String.format("(deprecated; use --viable/-V instead) include all viable (pre-production and production) statuses in secondary query %s", ALL_STATUSES))
            .build();

  /**
   * <p>
   * Key for the any-and-all option ({@value}).
   * </p>
   * 
   * @since 1.70
   */
  protected static final String KEY_ANY_AND_ALL = "any-and-all";
  
  /**
   * <p>
   * The any-and-all option.
   * </p>
   * 
   * @since 1.70
   */
  protected static final Option OPTION_ANY_AND_ALL =
      Option.builder()
            .longOpt(KEY_ANY_AND_ALL)
            .desc("include any and all statuses (viable or not) in secondary query, i.e. ignore secondary query (on by default)")
            .build();

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
      Option.builder()
            .longOpt(KEY_ALLIANCE)
            .desc(String.format("include only AUs whose plugin is not in the non-Alliance set"))
            .build();
  
  /**
   * <p>
   * Key for the clockss-preserved option ({@value}).
   * </p>
   * 
   * @since 1.70
   */
  protected static final String KEY_CLOCKSS_INGEST = "clockss-ingest";

  /**
   * <p>
   * Single letter for the clockss-ingest option ({@value}).
   * </p>
   * 
   * @since 1.70
   */
  protected static final char LETTER_CLOCKSS_INGEST = 'H';
  
  /**
   * <p>
   * Unmodifiable list of statuses associated with the clockss-ingest option.
   * </p>
   * 
   * @since 1.70
   * @see Au#STATUS_CRAWLING
   * @see Au#STATUS_DEEP_CRAWL
   * @see Au#STATUS_FROZEN
   * @see Au#STATUS_ING_NOT_READY
   * @see Au#STATUS_FINISHED
   * @see Au#STATUS_DOWN
   * @see Au#STATUS_SUPERSEDED
   */
  public static final List<String> CLOCKSS_INGEST_STATUSES =
      AppUtil.ul(Au.STATUS_CRAWLING,
                 Au.STATUS_DEEP_CRAWL,
                 Au.STATUS_FROZEN,
                 Au.STATUS_ING_NOT_READY,
                 Au.STATUS_FINISHED,
                 Au.STATUS_DOWN,
                 Au.STATUS_SUPERSEDED);
  
  /**
   * <p>
   * The clockss-ingest option.
   * </p>
   * 
   * @since 1.70
   */
  protected static final Option OPTION_CLOCKSS_INGEST =
      Option.builder(Character.toString(LETTER_CLOCKSS_INGEST))
            .longOpt(KEY_CLOCKSS_INGEST)
            .desc(String.format("include all active CLOCKSS ingest statuses in secondary query %s", CLOCKSS_INGEST_STATUSES))
            .build();
  
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
      Option.builder(Character.toString(LETTER_CLOCKSS_PRESERVED))
            .longOpt(KEY_CLOCKSS_PRESERVED)
            .desc(String.format("include all CLOCKSS production statuses fit for 'Preserved' Keepers label in secondary query %s", CLOCKSS_PRESERVED_STATUSES))
            .build();
  
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
   * @see Au#STATUS_DEEP_CRAWL
   * @see Au#STATUS_FINISHED
   * @see Au#STATUS_DOWN
   * @see Au#STATUS_SUPERSEDED
   */  
  public static final List<String> CLOCKSS_PRODUCTION_STATUSES =
      AppUtil.ul(Au.STATUS_CRAWLING,
                 Au.STATUS_TESTING,
                 Au.STATUS_NOT_READY,
                 Au.STATUS_FROZEN,
                 Au.STATUS_DEEP_CRAWL,
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
      Option.builder(Character.toString(LETTER_CLOCKSS_PRODUCTION))
            .longOpt(KEY_CLOCKSS_PRODUCTION)
            .desc(String.format("include all active CLOCKSS production statuses in secondary query %s", CLOCKSS_PRODUCTION_STATUSES))
            .build();
  
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
      Option.builder(Character.toString(LETTER_CRAWLING))
            .longOpt(KEY_CRAWLING)
            .desc(String.format("include status '%s' in secondary query", Au.STATUS_CRAWLING))
            .build();

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
      Option.builder(Character.toString(LETTER_DEEP_CRAWL))
            .longOpt(KEY_DEEP_CRAWL)
            .desc(String.format("include status '%s' in secondary query", Au.STATUS_DEEP_CRAWL))
            .build();

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
      Option.builder(Character.toString(LETTER_DOWN))
            .longOpt(KEY_DOWN)
            .desc(String.format("include status '%s' in secondary query", Au.STATUS_DOWN))
            .build();

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
      Option.builder(Character.toString(LETTER_EXISTS))
            .longOpt(KEY_EXISTS)
            .desc(String.format("include status '%s' in secondary query", Au.STATUS_EXISTS))
            .build();

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
      Option.builder(Character.toString(LETTER_EXPECTED))
            .longOpt(KEY_EXPECTED)
            .desc(String.format("include status '%s' in secondary query", Au.STATUS_EXPECTED))
            .build();

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
      Option.builder(Character.toString(LETTER_FINISHED))
            .longOpt(KEY_FINISHED)
            .desc(String.format("include status '%s' in secondary query", Au.STATUS_FINISHED))
            .build();

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
      Option.builder(Character.toString(LETTER_FROZEN))
            .longOpt(KEY_FROZEN)
            .desc(String.format("include status '%s' in secondary query", Au.STATUS_FROZEN))
            .build();

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
      Option.builder(Character.toString(LETTER_ING_NOT_READY))
            .longOpt(KEY_ING_NOT_READY)
            .desc(String.format("include status '%s' in secondary query", Au.STATUS_ING_NOT_READY))
            .build();

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
      Option.builder(Character.toString(LETTER_MANIFEST))
            .longOpt(KEY_MANIFEST)
            .desc(String.format("include status '%s' in secondary query", Au.STATUS_MANIFEST))
            .build();

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
                 "gov.gpo.access.permanent.plugin.amberwaves.Amber_WavesPlugin", // gpo-pilot
                 "gov.gpo.access.permanent.plugin.Environmental_Health_PerspectivesPlugin", // gpo-pilot
                 "gov.gpo.access.permanent.plugin.fbilawenforcementbulletin.FBI_Law_Enforcement_BulletinPlugin", // gpo-pilot
                 "gov.gpo.access.permanent.plugin.humanities.HumanitiesPlugin", // gpo-pilot
                 "gov.gpo.access.permanent.plugin.monthlyenergyreview.MonthlyEnergyReviewPlugin", // gpo-pilot
                 "gov.gpo.access.permanent.plugin.monthlylaborreview.MonthlyLaborReviewPlugin", // gpo-pilot
                 "gov.gpo.access.permanent.plugin.nistjournalofresearch.NISTJournalOfResearchPlugin", // gpo-pilot
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
                 "org.lockss.plugin.minerva.Minerva2020Plugin",
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
      Option.builder()
            .longOpt(KEY_NON_ALLIANCE)
            .desc(String.format("include only AUs whose plugin is in the non-Alliance set"))
            .build();
  
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
      Option.builder(Character.toString(LETTER_NOT_READY))
            .longOpt(KEY_NOT_READY)
            .desc(String.format("include status '%s' in secondary query", Au.STATUS_NOT_READY))
            .build();

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
      Option.builder(Character.toString(LETTER_PRODUCTION))
            .longOpt(KEY_PRODUCTION)
            .desc(String.format("include all production statuses in secondary query %s", PRODUCTION_STATUSES))
            .build();

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
      Option.builder(Character.toString(LETTER_QUERY))
            .longOpt(KEY_QUERY)
            .hasArg()
            .argName(KEY_QUERY.toUpperCase())
            .desc(String.format("use principal query %s", KEY_QUERY.toUpperCase()))
            .build();

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
      Option.builder(Character.toString(LETTER_READY))
            .longOpt(KEY_READY)
            .desc(String.format("include status '%s' in secondary query", Au.STATUS_READY))
            .build();

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
      Option.builder(Character.toString(LETTER_READY_SOURCE))
            .longOpt(KEY_READY_SOURCE)
            .desc(String.format("include status '%s' in secondary query", Au.STATUS_READY_SOURCE))
            .build();

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
      Option.builder(Character.toString(LETTER_RELEASED))
            .longOpt(KEY_RELEASED)
            .desc(String.format("include status '%s' in secondary query", Au.STATUS_RELEASED))
            .build();

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
      Option.builder(Character.toString(LETTER_RELEASING))
            .longOpt(KEY_RELEASING)
            .desc(String.format("include status '%s' in secondary query", Au.STATUS_RELEASING))
            .build();

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
      Option.builder(Character.toString(LETTER_STATUS2))
            .longOpt(KEY_STATUS2)
            .desc(String.format("use status2 instead of status in secondary query"))
            .build();

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
      Option.builder(Character.toString(LETTER_SUPERSEDED))
            .longOpt(KEY_SUPERSEDED)
            .desc(String.format("include status '%s' in secondary query", Au.STATUS_SUPERSEDED))
            .build();

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
      Option.builder(Character.toString(LETTER_TESTING))
            .longOpt(KEY_TESTING)
            .desc(String.format("include status '%s' in secondary query", Au.STATUS_TESTING))
            .build();

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
   * @see Au#STATUS_FROZEN
   * @see Au#STATUS_DEEP_CRAWL
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
                 Au.STATUS_FROZEN,
                 Au.STATUS_DEEP_CRAWL,
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
      Option.builder(Character.toString(LETTER_UNRELEASED))
            .longOpt(KEY_UNRELEASED)
            .desc(String.format("include all unreleased (pre-production) statuses in secondary query %s", UNRELEASED_STATUSES))
            .build();

  /**
   * <p>
   * Key for the viable option ({@value}).
   * </p>
   * 
   * @since 1.70
   */
  protected static final String KEY_VIABLE = "viable";
  
  /**
   * <p>
   * Single letter for the viable option ({@value}).
   * </p>
   * 
   * @since 1.70
   */
  protected static final char LETTER_VIABLE = 'V';
  
  /**
   * <p>
   * Unmodifiable list of statuses associated with the all option.
   * </p>
   * 
   * @since 1.70
   * @see Au#STATUS_MANIFEST
   * @see Au#STATUS_WANTED
   * @see Au#STATUS_TESTING
   * @see Au#STATUS_NOT_READY
   * @see Au#STATUS_READY
   * @see Au#STATUS_READY_SOURCE
   * @see Au#STATUS_CRAWLING
   * @see Au#STATUS_FROZEN
   * @see Au#STATUS_DEEP_CRAWL
   * @see Au#STATUS_ING_NOT_READY
   * @see Au#STATUS_RELEASING
   * @see Au#STATUS_FINISHED
   * @see Au#STATUS_RELEASED
   * @see Au#STATUS_DOWN
   * @see Au#STATUS_SUPERSEDED
   */
  public static final List<String> VIABLE_STATUSES =
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
   * The viable option.
   * </p>
   * 
   * @since 1.70
   */
  protected static final Option OPTION_VIABLE =
      Option.builder(Character.toString(LETTER_VIABLE))
            .longOpt(KEY_VIABLE)
            .desc(String.format("include all viable (pre-production and production) statuses in secondary query %s", VIABLE_STATUSES))
            .build();
  
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
      Option.builder(Character.toString(LETTER_WANTED))
            .longOpt(KEY_WANTED)
            .desc(String.format("include status '%s' in secondary query", Au.STATUS_WANTED))
            .build();

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
      Option.builder(Character.toString(LETTER_ZAPPED))
            .longOpt(KEY_ZAPPED)
            .desc(String.format("include status '%s' in secondary query", Au.STATUS_ZAPPED))
            .build();

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
    options.addOption(OPTION_ANY_AND_ALL);
    options.addOption(OPTION_CLOCKSS_INGEST);
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
    options.addOption(OPTION_VIABLE);
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
      CharStream charStream = CharStreams.fromString(query, "<query>");
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
    if (cmd.hasOption(KEY_CLOCKSS_INGEST)) {
      secondarySet.addAll(CLOCKSS_INGEST_STATUSES);
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
    if (cmd.hasOption(KEY_VIABLE)) {
      secondarySet.addAll(VIABLE_STATUSES);
    }
    if (cmd.hasOption(KEY_WANTED)) {
      secondarySet.add(Au.STATUS_WANTED);
    }
    if (cmd.hasOption(KEY_ZAPPED)) {
      secondarySet.add(Au.STATUS_ZAPPED);
    }
    // Must be last
    if (cmd.hasOption(KEY_ANY_AND_ALL)) {
      secondarySet.clear();
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
          return nonAllianceSet.contains(a.getComputedPlugin());
        }
      };
    }
    else if (cmd.hasOption(KEY_ALLIANCE)) {
      nonAlliancePredicate = new Predicate<Au>() {
        @Override
        public boolean test(Au a) {
          return !nonAllianceSet.contains(a.getComputedPlugin());
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
