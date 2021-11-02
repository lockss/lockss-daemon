/*

Copyright (c) 2000-2020, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin;

import java.util.*;
import java.util.regex.*;

import org.lockss.extractor.MetadataTarget;
import org.lockss.util.Logger;

/**
 * <p>
 * This utility class can programmatically assemble an article iterator
 * based on the {@link SubTreeArticleIterator} class, using building
 * blocks called aspects. Though the builder can only express certain categories
 * of article iterators, it can create subtree article iterators for many
 * scenarios involving pattern-matching among the URLs found in the archival
 * unit (AU).
 * </p>
 * <p>
 * Article iterators returned by this builder follow the following general work
 * flow.
 * </p>
 * <ul>
 * <li>
 * First potential URLs of interest are enumerated by means of a subtree article
 * iterator ({@link SubTreeArticleIterator}) and its specification (
 * {@link SubTreeArticleIterator.Spec}) and passed to a custom implementation of
 * {@link SubTreeArticleIterator#createArticleFiles(CachedUrl)}.
 * </li>
 * <li>
 * Then the buildable iterator considers a sequence of article aspects in the
 * order they were declared by the client. The first aspect that recognizes the
 * CU using one or more patterns ({@link Pattern}), which produces a matcher 
 * ({@link Matcher}), is selected for further consideration.
 * </li>
 * <li>
 * The aspect queries prior aspects in the sequence to see if the AU contains a
 * CU that they recognize as corresponding to this article. They do this by
 * applying one or more replacement strings to the matcher, yielding new URL
 * strings that can be used to test the presence or absence of a CU in the AU.
 * If a prior aspect indicates that it recognizes a CU of its own, the aspect
 * currently under consideration defers to the prior aspect and does not emit an
 * {@link ArticleFiles} instance for this article.
 * </li>
 * <li>
 * If there are no prior aspects recognizing this article, the aspect under
 * consideration has the responsibility of emitting an {@link ArticleFiles}
 * instance for it. It sets the current CU as the full text CU for the article,
 * and gives it any other roles the client defined it should set. Then it
 * queries later aspects in the sequence to see if they recognize CUs in the AU
 * corresponding to this article too. Those that do are asked to set these CUs
 * as the values for additional roles as defined by the client. During this
 * process, sequence order prevails; the value of a role, if already set, is not
 * overwritten.
 * </li>
 * <li>
 * Finally, for those roles whose value is set to that of existing roles in an
 * order other than aspect order, the client can define such custom orders,
 * which are applied at the end. This can even apply to the CU designated as the
 * article's full text CU (in which case that capability is not exercised by the
 * designated aspect).
 * </li>
 * </ul>
 * <p>
 * In the jargon of this builder, aspects that define one or more patterns to
 * match CUs directly from the subtree article iterator are referred to as
 * "major" and aspects that only recognize additional URLs by matcher
 * replacement are referred to as "minor".
 * </p>
 * <p>
 * A typical buildable iterator defines one or more major aspects followed by
 * zero or more minor aspects. The major aspects are intended to pinpoint the
 * full text CU and the minor aspects ancillary views or components of the
 * article. The order of major aspects matters.
 * </p>
 * <p>
 * The patterns defined by major aspects and the matcher replacements defined by
 * all aspects need to be "compatible". That is, they need to be able to agree
 * on what groups are matched by the various patterns so that the various
 * matcher replacements can generate the necessary substitute URLs.
 * </p>
 * <p>
 * Examples follow.
 * </p>
 * <p>
 * Site A uses a plugin that defines a <code>base_url</code>, a
 * <code>journal_id</code> and a <code>volume_name</code> as parameters. Volume
 * 123 of the Journal of ABC might have the following full text HTML URLs:
 * </p>
 * <pre>
http://www.example.com/content/jabc/v123/i1/95.html
http://www.example.com/content/jabc/v123/i1/98.html
http://www.example.com/content/jabc/v123/i1/101.html
...
</pre>
 * <p>
 * It might also have the following full text PDF URLs:
 * </p>
 * <pre>
http://www.example.com/cgi-bin/download?jid=jabc&vol=123&iss=1&page=95
http://www.example.com/cgi-bin/download?jid=jabc&vol=123&iss=1&page=101
http://www.example.com/cgi-bin/download?jid=jabc&vol=123&iss=1&page=104
...
</pre>
 * <p>
 * An article can have either or both types of full text URLs. If it has both,
 * HTML is preferred over PDF to be the designated full text CU. A buildable
 * article iterator for this plugin might look like this:
 * </p>
 * <pre>
    class ArticleIteratorFactoryA implements ArticleIteratorFactory {
      public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {
        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
        builder.setSpec(target,
                        Arrays.asList("\"%s%s/v%s/\", base_url, journal_id, volume_name",
                                      "\"%scgi-bin/\", base_url"),
                        "\"^%s(%s/v%s/i[^/]+/\\d+\\.html|cgi-bin/download\\?jid=%s&vol=%s&iss=[^&]+&page=\\d+)$\", base_url, journal_id, volume_name, base_url, journal_id, volume_name",
                        Pattern.CASE_INSENSITIVE);
        builder.addAspect(Pattern.compile("/([^/]+)/v([^/]+)/i([^/]+)/([^/]+)\\.html$", Pattern.CASE_INSENSITIVE),
                          "/$1/v$2/i$3/$4.html",
                          ArticleFiles.ROLE_FULL_TEXT_HTML);
        builder.addAspect(Pattern.compile("/cgi-bin/download?jid=([^&]+)&vol=([^&]+)&iss=([^&]+)&page=([^&]+)$", Pattern.CASE_INSENSITIVE),
                          "/cgi-bin/download?jid=$1&vol=$2&iss=$3&page=$4",
                          ArticleFiles.ROLE_FULL_TEXT_PDF);
        return builder.getSubTreeArticleIterator();
      }
    }
</pre>
 * <p>
 * Site B is just like site A, except in Site B it is guaranteed that all
 * articles have a full text PDF URL. The buildable article iterator for Site A
 * would work fine, but one could also do this:
 * </p>
 * <pre>
    class ArticleIteratorFactoryB implements ArticleIteratorFactory {
      public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {
        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
        builder.setSpec(target,
                        "\"%scgi-bin/\", base_url",
                        "\"^%scgi-bin/download\\?jid=%s&vol=%s&iss=[^&]+&page=\\d+$\", base_url, journal_id, volume_name",
                        Pattern.CASE_INSENSITIVE);
        builder.addAspect(Pattern.compile("/cgi-bin/download?jid=([^&]+)&vol=([^&]+)&iss=([^&]+)&page=([^&]+)$", Pattern.CASE_INSENSITIVE),
                          "/cgi-bin/download?jid=$1&vol=$2&iss=$3&page=$4",
                          ArticleFiles.ROLE_FULL_TEXT_PDF);
        builder.addAspect("/$1/v$2/i$3/$4.html",
                          ArticleFiles.ROLE_FULL_TEXT_HTML);
        builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_HTML,
                                     ArticleFiles.ROLE_FULL_TEXT_PDF);
        return builder.getSubTreeArticleIterator();
      }
    }
</pre>
 * <p>
 * Site C is like Site A, except articles may also have an optional abstract
 * page, at URLs like these:
 * </p>
 * <pre>
http://www.example.com/content/jabc/v123/i1/95-abstract.html
http://www.example.com/content/jabc/v123/i1/98-abstract.html
http://www.example.com/content/jabc/v123/i1/104-abstract.html
...
</pre>
 * <p>
 * If either the full text HTML URL or the abstract URL is present, one would
 * like them to be the value of {@link ArticleFiles#ROLE_ARTICLE_METADATA},
 * because both contain metadata for the article. One way to do this is:
 * </p>
 * <pre>
    class ArticleIteratorFactoryC1 implements ArticleIteratorFactory {
      public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {
        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
        builder.setSpec(target,
                        Arrays.asList("\"%s%s/v%s/\", base_url, journal_id, volume_name",
                                      "\"%scgi-bin/\", base_url"),
                        "\"^%s(%s/v%s/i[^/]+/\\d+\\.html|cgi-bin/download\\?jid=%s&vol=%s&iss=[^&]+&page=\\d+)$\", base_url, journal_id, volume_name, base_url, journal_id, volume_name");
        builder.addAspect(Pattern.compile("/([^/]+)/v([^/]+)/i([^/]+)/([^/]+)\\.html$", Pattern.CASE_INSENSITIVE),
                          "/$1/v$2/i$3/$4.html",
                          ArticleFiles.ROLE_FULL_TEXT_HTML,
                          ArticleFiles.ROLE_ARTICLE_METADATA);
        builder.addAspect(Pattern.compile("/cgi-bin/download?jid=([^&]+)&vol=([^&]+)&iss=([^&]+)&page=([^&]+)$", Pattern.CASE_INSENSITIVE),
                          "/cgi-bin/download?jid=$1&vol=$2&iss=$3&page=$4",
                          ArticleFiles.ROLE_FULL_TEXT_PDF);
        builder.addAspect("/$1/v$2/i$3/$4-abstract.html",
                          ArticleFiles.ROLE_ABSTRACT,
                          ArticleFiles.ROLE_ARTICLE_METADATA);
        return builder.getSubTreeArticleIterator();
      }
    }
</pre>
 * <p>
 * Extracting metadata requires parsing the HTML document. If one would favor
 * the abstract URL over the full text HTML URL for this because abstracts are
 * much shorter HTML documents, one can define an order for
 * {@link ArticleFiles#ROLE_ARTICLE_METADATA}:
 * </p>
 * <pre>
    class ArticleIteratorFactoryC2 implements ArticleIteratorFactory {
      public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {
        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
        builder.setSpec(target,
                        Arrays.asList("\"%s%s/v%s/\", base_url, journal_id, volume_name",
                                      "\"%scgi-bin/\", base_url"),
                        "\"^%s(%s/v%s/i[^/]+/\\d+\\.html|cgi-bin/download\\?jid=%s&vol=%s&iss=[^&]+&page=\\d+)$\", base_url, journal_id, volume_name, base_url, journal_id, volume_name",
                        Pattern.CASE_INSENSITIVE);
        builder.addAspect(Pattern.compile("/([^/]+)/v([^/]+)/i([^/]+)/([^/]+)\\.html$", Pattern.CASE_INSENSITIVE),
                          "/$1/v$2/i$3/$4.html",
                          ArticleFiles.ROLE_FULL_TEXT_HTML);
        builder.addAspect(Pattern.compile("/cgi-bin/download?jid=([^&]+)&vol=([^&]+)&iss=([^&]+)&page=([^&]+)$", Pattern.CASE_INSENSITIVE),
                          "/cgi-bin/download?jid=$1&vol=$2&iss=$3&page=$4",
                          ArticleFiles.ROLE_FULL_TEXT_PDF);
        builder.addAspect("/$1/v$2/i$3/$4-abstract.html",
                          ArticleFiles.ROLE_ABSTRACT);
        builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA,
                                      ArticleFiles.ROLE_ABSTRACT,
                                      ArticleFiles.ROLE_FULL_TEXT_HTML);
        return builder.getSubTreeArticleIterator();
      }
    }
</pre>
 *
 * @author Thib Guicherd-Callin
 * @since 1.60
 */
public class SubTreeArticleIteratorBuilder {

  /**
   * <p>
   * A programmatically buildable subtree article iterator.
   * </p>
   * <p>
   * Currently this class can only be exposed by its enclosing class
   * {@link SubTreeArticleIteratorBuilder}, but this may change in the future.
   * </p>
   * 
   * @author Thib Guicherd-Callin
   * @since 1.60
   */
  protected static class BuildableSubTreeArticleIterator extends SubTreeArticleIterator {

    /**
     * <p>
     * Describes an aspect of an article, that is, one of its renditions, or
     * components, or display structures.
     * </p>
     * <p>
     * Currently this class is a non-static inner class of its enclosing class
     * {@link BuildableSubTreeArticleIterator}, but this may change in the
     * future.
     * </p>
     * 
     * @author Thib Guicherd-Callin
     * @since 1.60
     */
    protected class Aspect {
      
      /**
       * This aspect's ordered matcher replacement strings.
       * @since 1.60
       */
      protected List<String> matcherReplacements;
      
      /**
       * This aspect's ordered patterns.
       * @since 1.60
       */
      protected List<Pattern> patterns;
      
      /**
       * This aspect's roles, set if it recognizes a URL.
       * @since 1.60
       */
      protected List<String> roles;
      
      /**
       * <p>
       * Builds a new aspect.
       * </p>
       * @since 1.60
       */
      public Aspect() {
        this.patterns = new ArrayList<Pattern>();
        this.matcherReplacements = new ArrayList<String>();
        this.roles = new ArrayList<String>();
      }

      /**
       * <p>
       * Determines if the given URL matches any of this aspect's patterns.
       * </p>
       * <p>
       * Matching is adjudicated by {@link Matcher#find()}.
       * </p>
       * 
       * @param url
       *          A URL string.
       * @return A {@link Matcher} corresponding to the {@link Pattern} that
       *         matched the given URL, or <code>null</code> if none did.
       * @since 1.60
       */
      public Matcher findCuAmongPatterns(String url) {
        for (Pattern pat : patterns) {
          Matcher mat = pat.matcher(url);
          if (mat.find()) {
            return mat;
          }
        }
        return null;
      }
      
      /**
       * <p>
       * Determines if the AU contains a URL corresponding to this aspect, by
       * applying its replacement strings to the given matcher.
       * </p>
       * 
       * @param matcher
       *          A matcher for a given URL (having matched another aspect).
       * @return A {@link CachedUrl} corresponding to a URL for a successful
       *         matcher replacement, or <code>null</code> if no such URL exists
       *         in the AU.
       * @since 1.60
       */
      public CachedUrl findCuByPatternReplacement(Matcher matcher) {
        for (String matcherReplacement : matcherReplacements) {
          CachedUrl replacedCu = au.makeCachedUrl(matcher.replaceFirst(matcherReplacement));
          if (replacedCu.hasContent()) {
            return replacedCu;
          }
        }
        return null;
      }

      /**
       * <p>
       * Store the given cached URL as the value of this aspect's roles in the
       * given {@link ArticleFiles} instance, for any roles not already set.
       * </p>
       * 
       * @param af
       *          An {@link ArticleFiles} instance.
       * @param cu
       *          A cached URL.
       * @since 1.60
       */
      public void processRoles(ArticleFiles af, CachedUrl cu) {
        for (String role : roles) {
          if (af.getRoleCu(role) == null) {
            af.setRoleCu(role, cu);
          }
        }
      }
      
      public String toString() {
        return "[Aspect: roles: " + roles +
          ", pats: " + patterns +
          ", reps: " + matcherReplacements + "]";
      }
    }
    
    /**
     * <p>
     * This buildable iterator's aspects.
     * </p>
     * 
     * @since 1.60
     */
    protected List<Aspect> aspects;
    
    /**
     * <p>
     * This aspect's ordered list of roles to be used to choose the full text
     * CU.
     * </p>
     * 
     * @since 1.60
     */
    protected List<String> rolesForFullText;
    
    /**
     * <p>
     * A map from a role to the ordered list of roles to be used to choose the
     * CU associated with that role.
     * </p>
     * 
     * @since 1.60
     */
    protected Map<String, List<String>> rolesFromOtherRoles;
    
    /**
     * <p>
     * Makes a new buildable iterator for the given AU using the given subtree
     * article iterator specification.
     * </p>
     * 
     * @param au
     *          An archival unit.
     * @param spec
     *          A subtree article iterator specification.
     * @since 1.60
     */
    public BuildableSubTreeArticleIterator(ArchivalUnit au, SubTreeArticleIterator.Spec spec) {
      super(au, spec);
      this.aspects = new ArrayList<Aspect>();
      this.rolesForFullText = new ArrayList<String>();
      this.rolesFromOtherRoles = new LinkedHashMap<String, List<String>>();
      if (logger.isDebug2()) {
        logger.debug2(String.format("Processing AU: %s", au.getName()));
      }
    }
    
    /**
     * <p>
     * A custom implementation of
     * {@link SubTreeArticleIterator#createArticleFiles(CachedUrl)} performing
     * the aspect-based logic of this buildable iterator.
     * </p>
     * 
     * @param cu
     *          The cached URL currently under consideration.
     * 
     * @since 1.60
     */
    
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      boolean isDebug2 = logger.isDebug2();
      
      // Process this CU
      String url = cu.getUrl();
      if (isDebug2) {
        logger.debug2(String.format("Processing: %s", url));
      }
      
      // Try each aspect
      for (int ci = 0 ; ci < aspects.size() ; ++ci) {
        Aspect aspect = aspects.get(ci);
        Matcher matcher = aspect.findCuAmongPatterns(url);
        if (matcher != null) {
          // Does this aspect defer to a higher aspect?
          for (int cj = 0 ; cj < ci ; ++cj) {
            Aspect higherAspect = aspects.get(cj);
            CachedUrl higherCu = higherAspect.findCuByPatternReplacement(matcher);
            if (higherCu != null) {
              // Defer
              if (isDebug2) {
                logger.debug2(String.format("Deferring to: %s", higherCu.getUrl()));
              }
              AuUtil.safeRelease(higherCu);
              return null;
            }
          }
          // Process this aspect; full text CU (might be overridden later)
          ArticleFiles af = instantiateArticleFiles();
          af.setFullTextCu(cu);
          aspect.processRoles(af, cu);
          if (isDebug2) {
            logger.debug2(String.format("Full text CU set to: %s", url));
          }
          // Process lower aspects (unless only counting articles)
          if (spec.getTarget() != null && !spec.getTarget().isArticle()) {
            if (isDebug2) {
              logger.debug2("Processing additional aspects");
            }
            for (int cj = ci + 1; cj < aspects.size(); ++cj) {
              Aspect lowerAspect = aspects.get(cj);
              CachedUrl lowerCu = lowerAspect.findCuByPatternReplacement(matcher);
              log.debug3("Aspect: " + lowerAspect + ", cu: " + lowerCu);
              if (lowerCu != null) {
                lowerAspect.processRoles(af, lowerCu);
              }
            }
          }
          else {
            if (isDebug2) {
              logger.debug2("Skipping additional aspects");
            }
          }
          // Set roles from other roles if orders specified (unless only counting articles)
          if (spec.getTarget() != null && !spec.getTarget().isArticle()) {
            for (Map.Entry<String, List<String>> entry : rolesFromOtherRoles.entrySet()) {
              String role = entry.getKey();
              for (String otherRole : entry.getValue()) {
                CachedUrl foundCu = af.getRoleCu(otherRole); 
                if (foundCu != null) {
                  if (isDebug2) {
                    logger.debug2(String.format("CU for %s set to: %s", otherRole, foundCu.getUrl()));
                  }
                  af.setRoleCu(role, foundCu);
                  break;
                }
              }
            }
          }
          // Override full text CU if order specified
          if (rolesForFullText.size() > 0) {
            if (isDebug2) {
              logger.debug2("Overriding full text CU");
            }
            af.setFullTextCu(null);
            for (String role : rolesForFullText) {
              CachedUrl foundCu = af.getRoleCu(role);
              if (foundCu != null) {
                if (isDebug2) {
                  logger.debug2(String.format("Full text CU reset to: %s", foundCu.getUrl()));
                }
                af.setFullTextCu(foundCu);
                break;
              }
            }
          }
          // Callers should call emitArticleFiles(af);
          return af;
        }
      }
      logger.debug(String.format("%s in %s did not match any expected patterns", url, au.getName()));
      return null;
    }

    /**
     * <p>
     * Instantiates a new {@link ArticleFiles} instance. Override this method if
     * you need to instantiate a subclass of {@link ArticleFiles} with custom
     * functionality.
     * </p>
     * 
     * @return An {@link ArticleFiles} instance.
     * @since 1.67
     */
    protected ArticleFiles instantiateArticleFiles() {
      return new ArticleFiles();
    }
    
  }

  /**
   * <p>
   * A logger for use by this class and nested classes.
   * </p>
   * 
   * @since 1.60
   */
  private static final Logger logger = Logger.getLogger(SubTreeArticleIteratorBuilder.class);
  
  /**
   * <p>
   * The AU associated with this builder.
   * </p>
   * 
   * @since 1.60
   */
  protected ArchivalUnit au;
  
  /**
   * <p>
   * The most recently created aspect.
   * </p>
   * 
   * @since 1.60
   */
  protected BuildableSubTreeArticleIterator.Aspect currentAspect;
  
  /**
   * <p>
   * The current buildable iterator.
   * </p>
   * 
   * @since 1.60
   */
  protected BuildableSubTreeArticleIterator iterator;

  /**
   * <p>
   * The subtree article iterator specification associated with this builder.
   * </p>
   * 
   * @since 1.60
   */
  protected SubTreeArticleIterator.Spec spec;
  
  /**
   * <p>
   * Makes a new builder.
   * </p>
   * 
   * @since 1.60
   */
  public SubTreeArticleIteratorBuilder() {
    // nothing
  }
  
  /**
   * <p>
   * Convenience method equivalent to calling
   * {@link #SubTreeArticleIteratorBuilder()} then
   * {@link #setArchivalUnit(ArchivalUnit)} with the given AU.
   * </p>
   * 
   * @param au
   *          An archival unit.
   * @since 1.60
   */
  public SubTreeArticleIteratorBuilder(ArchivalUnit au) {
    this();
    setArchivalUnit(au);
  }
  
  /**
   * <p>
   * Instructs the builder to define a new aspect. Subsequent aspect calls (e.g.
   * {@link #addPatterns(Pattern...)}) apply to this newly-defined aspect.
   * </p>
   * 
   * @throws IllegalStateException
   *           if the AU and spec are unset.
   * @since 1.60
   */
  public void addAspect() {
    if (iterator == null) {
      throw new IllegalStateException("Cannot create an aspect until the AU and the spec are set");
    }
    currentAspect = iterator.new Aspect();
    iterator.aspects.add(currentAspect);
  }
  
  /**
   * <p>
   * Convenience method equivalent to calling {@link #addAspect()}, then
   * {@link #addPatterns(List)} with the given list of patterns, then
   * {@link #addMatcherReplacements(List)} with the given list of matcher
   * replacement strings, then {@link #addRoles(String...)} with the given
   * sequence of roles.
   * </p>
   * 
   * @param patterns
   *          A list of patterns for the new aspect.
   * @param matcherReplacements
   *          A list of matcher replacement strings for the new aspect.
   * @param roles
   *          A sequence of roles for the new aspect.
   * @since 1.60
   */
  public void addAspect(List<Pattern> patterns, List<String> matcherReplacements, String... roles) {
    addAspect();
    addPatterns(patterns);
    addMatcherReplacements(matcherReplacements);
    addRoles(roles);
  }
  
  /**
   * <p>
   * Convenience method equivalent to calling {@link #addAspect()}, then
   * {@link #addPatterns(List)} with the given list of patterns, then
   * {@link #addMatcherReplacements(List)} with a singleton list containing the
   * given matcher replacement string, then {@link #addRoles(String...)} with
   * the given sequence of roles.
   * </p>
   * 
   * @param patterns
   *          A list of patterns for the new aspect.
   * @param matcherReplacement
   *          A matcher replcamanet string for the new aspect.
   * @param roles
   *          A sequence of roles for the new aspect.
   * @since 1.60
   */
  public void addAspect(List<Pattern> patterns, String matcherReplacement, String... roles) {
    addAspect(patterns, Arrays.asList(matcherReplacement), roles);
  }
  
  /**
   * <p>
   * Convenience method equivalent to calling {@link #addAspect()}, then
   * {@link #addMatcherReplacements(List)} with the given list of matcher
   * replacement strings, then {@link #addRoles(String...)} with the given
   * sequence of roles.
   * </p>
   * 
   * @param matcherReplacements
   *          A list of matcher replacement strings for the new aspect.
   * @param roles
   *          A sequence of roles for the new aspect.
   * @since 1.60
   */
  public void addAspect(List<String> matcherReplacements, String... roles) {
    addAspect();
    addMatcherReplacements(matcherReplacements);
    addRoles(roles);
  }
  
  /**
   * <p>
   * Convenience method equivalent to calling {@link #addAspect()}, then
   * {@link #addPatterns(List)} with a singleton list containing the given
   * pattern, then {@link #addMatcherReplacements(List)} with the given list of
   * matcher replacement strings, then {@link #addRoles(String...)} with the
   * given sequence of roles.
   * </p>
   * 
   * @param pattern
   *          A pattern for the new aspect.
   * @param matcherReplacements
   *          A list of matcher replacement strings for the new aspect.
   * @param roles
   *          A sequence of roles for the new aspect.
   * @since 1.60
   */
  public void addAspect(Pattern pattern, List<String> matcherReplacements, String... roles) {
    addAspect(Arrays.asList(pattern), matcherReplacements, roles);
  }
  
  /**
   * <p>
   * Convenience method equivalent to calling {@link #addAspect()}, then
   * {@link #addPatterns(List)} with a singleton list containing the given
   * pattern, then {@link #addMatcherReplacements(List)} with a singleton list containing the given 
   * matcher replacement string, then {@link #addRoles(String...)} with the
   * given sequence of roles.
   * </p>
   * 
   * @param pattern
   *          A pattern for the new aspect.
   * @param matcherReplacement
   *          A matcher replacement string for the new aspect.
   * @param roles
   *          A sequence of roles for the new aspect.
   * @since 1.60
   */
  public void addAspect(Pattern pattern, String matcherReplacement, String... roles) {
    addAspect(Arrays.asList(pattern), Arrays.asList(matcherReplacement), roles);
  }
  
  /**
   * <p>
   * Convenience method equivalent to calling {@link #addAspect()}, then
   * {@link #addMatcherReplacements(List)} with a singleton list containing the
   * given matcher replacement string, then {@link #addRoles(String...)} with
   * the given sequence of roles.
   * </p>
   * 
   * @param matcherReplacement
   *          A matcher replacement string for the new aspect.
   * @param roles
   *          A sequence of roles for the new aspect.
   * @since 1.60
   */
  public void addAspect(String matcherReplacement, String... roles) {
    addAspect(Arrays.asList(matcherReplacement), roles);
  }
  
  /**
   * <p>
   * Appends matcher replacement strings to the list known by the most currently
   * created aspect.
   * </p>
   * 
   * @param matcherReplacements
   *          A list of matcher replacement strings.
   * @throws IllegalStateException
   *           if no aspect has been created or the list is empty.
   * @since 1.60
   */
  public void addMatcherReplacements(List<String> matcherReplacements) {
    if (currentAspect == null) {
      throw new IllegalStateException("No aspect has been created");
    }
    if (matcherReplacements.size() < 1) {
      throw new IllegalArgumentException("Must define at least one matcher replacement");
    }
    currentAspect.matcherReplacements.addAll(matcherReplacements);
  }
  
  /**
   * <p>
   * Convenience method that calls {@link #addMatcherReplacements(List)} by
   * turning the given sequence of matcher replacement strings into a list.
   * </p>
   * 
   * @param matcherReplacements
   *          A sequence of matcher replacement strings.
   * @since 1.60
   */
  public void addMatcherReplacements(String... matcherReplacements) {
    addMatcherReplacements(Arrays.asList(matcherReplacements));
  }
  
  /**
   * <p>
   * Appends patterns to the list known by the most currently created aspect.
   * </p>
   * 
   * @param patterns
   *          A list of patterns.
   * @throws IllegalStateException
   *           if no aspect has been created or the list is empty.
   * @since 1.60
   */
  public void addPatterns(List<Pattern> patterns) {
    if (currentAspect == null) {
      throw new IllegalStateException("No aspect has been created");
    }
    if (patterns.size() < 1) {
      throw new IllegalArgumentException("Must define at least one pattern");
    }
    currentAspect.patterns.addAll(patterns);
  }
  
  /**
   * <p>
   * Convenience method that calls {@link #addPatterns(List)} by turning the
   * given sequence of patterns into a list.
   * </p>
   * 
   * @param patterns
   *          A sequence of patterns.
   * @since 1.60
   */
  public void addPatterns(Pattern... patterns) {
    addPatterns(Arrays.asList(patterns));
  }
  
  /**
   * <p>
   * Appends roles to the list known by the most currently created aspect.
   * </p>
   * 
   * @param roles
   *          A list of roles.
   * @throws IllegalStateException
   *           if no aspect has been created or the list is empty.
   * @since 1.60
   */
  public void addRoles(List<String> roles) {
    if (currentAspect == null) {
      throw new IllegalStateException("No aspect has been created");
    }
    if (roles.size() < 1) {
      throw new IllegalArgumentException("Must define at least one role");
    }
    currentAspect.roles.addAll(roles);
  }
  
  /**
   * <p>
   * Convenience method that calls {@link #addRoles(List)} by turning the given
   * sequence of roles into a list.
   * </p>
   * 
   * @param roles
   *          A sequence of roles.
   * @since 1.60
   */
  public void addRoles(String... roles) {
    addRoles(Arrays.asList(roles));
  }
  
  /**
   * <p>
   * Returns the buildable subtree article iterator as it currently is.
   * </p>
   * 
   * @return The currently built subtree article iterator.
   * @since 1.60
   */
  public SubTreeArticleIterator getSubTreeArticleIterator() {
    if (iterator == null) {
      throw new IllegalStateException("Cannot create a subtree article iterator until the AU and the spec are set");
    }
    return iterator;
  }

  /**
   * <p>
   * Convenience method to obtain a new subtree article iterator specficiation
   * object.
   * </p>
   * 
   * @return A new spec.
   * @since 1.60
   */
  public SubTreeArticleIterator.Spec newSpec() {
    return new SubTreeArticleIterator.Spec();
  }
  
  /**
   * <p>
   * Sets the AU associated with this builder.
   * </p>
   * 
   * @param au
   *          An archival unit.
   * @throws IllegalStateException
   *           if the AU is already set
   * @since 1.60
   */
  public void setArchivalUnit(ArchivalUnit au) {
    if (this.au != null) {
      throw new IllegalStateException("AU is already set to " + au.getName());
    }
    this.au = au;
    maybeMakeSubTreeArticleIterator();
  }
  
  /**
   * <p>
   * Declares an order list of roles from which to pick the cached URL that will
   * be set as the full text CU after all aspects have been considered for an
   * article.
   * </p>
   * 
   * @param roles
   *          An ordered list of roles.
   * @throws IllegalStateException
   *           if the AU or spec are not defined or if the list is empty.
   * @since 1.60
   */
  public void setFullTextFromRoles(List<String> roles) {
    if (iterator == null) {
      throw new IllegalStateException("Cannot create a subtree article iterator until the AU and the spec are set");
    }
    if (roles.size() < 1) {
      throw new IllegalArgumentException("Full text role order must contain at least one role");
    }
    iterator.rolesForFullText.addAll(roles);
  }

  /**
   * <p>
   * Convenience method that calls {@link #setFullTextFromRoles(List)} by
   * turning the given sequence of roles into a list.
   * </p>
   * 
   * @param roles
   *          A sequence of roles.
   * @since 1.60
   */
  public void setFullTextFromRoles(String... roles) {
    setFullTextFromRoles(Arrays.asList(roles));
  }

  /**
   * @since 1.60
   */
  public void setRoleFromOtherRoles(String newRole, List<String> otherRoles) {
    if (iterator == null) {
      throw new IllegalStateException("Cannot create a subtree article iterator until the AU and the spec are set");
    }
    if (otherRoles.size() < 1) {
      throw new IllegalArgumentException("Other role order must contain at least one role");
    }
    iterator.rolesFromOtherRoles.put(newRole, otherRoles);
  }

  /**
   * @since 1.60
   */
  public void setRoleFromOtherRoles(String newRole, String... otherRoles) {
    setRoleFromOtherRoles(newRole, Arrays.asList(otherRoles));
  }

  /**
   * @since 1.60
   */
  public void setSpec(MetadataTarget target,
                      List<String> rootTemplates,
                      String patternTemplate) {
    setSpec(target, rootTemplates, patternTemplate, 0);
  }
  
  /**
   * @since 1.60
   */
  public void setSpec(MetadataTarget target,
                      List<String> rootTemplates,
                      String patternTemplate,
                      int patternTemplateFlags) {
    SubTreeArticleIterator.Spec spec = newSpec();
    spec.setTarget(target);
    spec.setRootTemplates(rootTemplates);
    spec.setPatternTemplate(patternTemplate, patternTemplateFlags);
    setSpec(spec);
  }

  /**
   * @since 1.60
   */
  public void setSpec(MetadataTarget target,
                      String rootTemplate,
                      String patternTemplate) {
    setSpec(target, Arrays.asList(rootTemplate), patternTemplate, 0);
  }

  /**
   * @since 1.60
   */
  public void setSpec(MetadataTarget target,
                      String rootTemplate,
                      String patternTemplate,
                      int patternTemplateFlags) {
    setSpec(target, Arrays.asList(rootTemplate), patternTemplate, patternTemplateFlags);
  }

  /**
   * @since 1.60
   */
  public void setSpec(SubTreeArticleIterator.Spec spec) {
    if (this.spec != null) {
      throw new IllegalStateException("Spec is already set");
    }
    this.spec = spec;
    maybeMakeSubTreeArticleIterator();
  }
  
  /**
   * <p>
   * Instantiates a {@link BuildableSubTreeArticleIterator}. If you override
   * this class, you may also need to override
   * {@link BuildableSubTreeArticleIterator} accordingly, as the two
   * collaborate.
   * </p>
   * 
   * @return An instance of {@link BuildableSubTreeArticleIterator}.
   * @since 1.63
   */
  protected BuildableSubTreeArticleIterator instantiateBuildableIterator() {
    return new BuildableSubTreeArticleIterator(au, spec);
  }
  
  /**
   * <p>
   * Instantiates a {@link BuildableSubTreeArticleIterator} if necessary. If you
   * need to use a custom {@link BuildableSubTreeArticleIterator} class, the
   * method you want to override is probably
   * {@link #instantiateBuildableIterator()}, not this method.
   * </p>
   * 
   * @since 1.60
   */
  protected void maybeMakeSubTreeArticleIterator() {
    if (au != null && spec != null && iterator == null) {
      this.iterator = instantiateBuildableIterator();
    }
  }

}
