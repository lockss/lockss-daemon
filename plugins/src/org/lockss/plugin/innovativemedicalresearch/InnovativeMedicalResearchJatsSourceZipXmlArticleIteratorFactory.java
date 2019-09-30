package org.lockss.plugin.innovativemedicalresearch;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.lexer.InputStreamSource;
import org.htmlparser.lexer.Lexer;
import org.htmlparser.lexer.Page;
import org.htmlparser.lexer.Stream;
import org.htmlparser.tags.MetaTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.clockss.JatsPublishingSchemaHelper;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.plugin.clockss.XPathXmlMetadataParser;
import org.lockss.util.Constants;
import org.lockss.util.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InnovativeMedicalResearchJatsSourceZipXmlArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

    protected static Logger log = Logger.getLogger(InnovativeMedicalResearchJatsSourceZipXmlArticleIteratorFactory.class);

    //http://content5.lockss.org/sourcefiles/imrpress-released/2019/JMCM2018-Volume%201%20Issue%202.zip!/JMCM2018-Volume 1 Issue 2/2617-5282-2018-2/2617-5282-1-2-107.xml
    //http://content5.lockss.org/sourcefiles/imrpress-released/2019/JMCM2018-Volume%201%20Issue%202.zip!/JMCM2018-Volume 1 Issue 2/2617-5282-2018-2/1545826577946-771105881.pdf
    //Their folder name combines uppercase and lowercase
    protected static final String ALL_ZIP_XML_PATTERN_TEMPLATE =
            "\"%s%s/[^/]+\\.zip!/[^/]+/[^/]+/[^/]+$\", base_url, directory";

    // Be sure to exclude all nested archives in case supplemental data is provided this way
    protected static final Pattern SUB_NESTED_ARCHIVE_PATTERN =
            Pattern.compile(".*/[^/]+\\.zip!/.+\\.(zip|tar|gz|tgz|tar\\.gz)$",
                    Pattern.CASE_INSENSITIVE);


    protected static final String ROOT_TEMPLATE_STRING =  "\"%s%s\", base_url, directory";
    protected static final Pattern ROOT_TEMPLATE = Pattern.compile(ROOT_TEMPLATE_STRING);

    protected static final String PATTERN_TEMPLATE_STRING =  "\"%s%s/[^/]+\\.zip!/.+\\.xml\", base_url, directory";
    protected static final Pattern PATTERN_TEMPLATE = Pattern.compile(PATTERN_TEMPLATE_STRING);

    protected static final String XML_PATTERN_STRING = "/([^/]+\\.xml)";
    protected static final Pattern XML_PATTERN = Pattern.compile(XML_PATTERN_STRING);


    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, final MetadataTarget target) throws PluginException {
        return new SubTreeArticleIterator(au,
                new SubTreeArticleIterator.Spec()
                        .setTarget(target)
                        .setRootTemplate(ROOT_TEMPLATE_STRING)
                        .setPatternTemplate(PATTERN_TEMPLATE_STRING)
                        .setVisitArchiveMembers(true)) {

            @Override
            protected ArticleFiles createArticleFiles(CachedUrl cu) {
                String url = cu.getUrl();
                ArticleFiles af = new ArticleFiles();

                Matcher xmlMatch = XML_PATTERN.matcher(url);
                if (xmlMatch.find()) {
                    CachedUrl xmlCu = au.makeCachedUrl(url.toString());
                    af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, xmlCu);

                    // .xml and .pdf are one-to-one relationship.
                    // Extract pdf from .xml "related-article" xpath and build PDF aspect manually
                    XPathXmlMetadataParser xmlParser = new XPathXmlMetadataParser();
                    try {
                        SourceXmlSchemaHelper schemaHelper = new JatsPublishingSchemaHelper();

                        xmlParser.setXmlParsingSchema(schemaHelper.getGlobalMetaMap(),
                                schemaHelper.getArticleNode(),
                                schemaHelper.getArticleMetaMap());

                        List<ArticleMetadata> amList = xmlParser.extractMetadataFromCu(target, cu);

                        if (amList != null) {
                            for (ArticleMetadata oneAM : amList) {
                                String filenameValue = oneAM.getRaw(JatsPublishingSchemaHelper.JATS_article_related_pdf);
                                if (filenameValue != null) {
                                    String cuBase = FilenameUtils.getFullPath(url);
                                    String pdfPath = cuBase + filenameValue;
                                    CachedUrl pdfCu = au.makeCachedUrl(pdfPath);
                                    af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
                                    af.setFullTextCu(pdfCu);
                                }
                            }
                        }
                    }  catch (SAXException e) {
                        e.printStackTrace();
                    } catch (XPathExpressionException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return af;
            }
        };
    }

    protected Pattern getExcludeSubTreePattern() {
        return SUB_NESTED_ARCHIVE_PATTERN;
    }

    protected String getIncludePatternTemplate() {
        return ALL_ZIP_XML_PATTERN_TEMPLATE;
    }

    // NOTE - for a child to create their own version of this
    // indicates if the iterator should descend in to archives (for tar/zip deliveries)
    protected boolean getIsArchive() {
        return true;
    }

    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
            throws PluginException {
        return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
    }

}