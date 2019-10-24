package org.lockss.plugin.clockss.casalini;

import org.apache.commons.io.FilenameUtils;
import org.apache.xerces.xs.StringList;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.clockss.JatsPublishingSchemaHelper;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.plugin.clockss.XPathXmlMetadataParser;
import org.lockss.util.Logger;
import org.xml.sax.SAXException;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.MAX_VALUE;

public class CasaliniLibriMarcXmlSourceArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

    protected static Logger log = Logger.getLogger(CasaliniLibriMarcXmlSourceArticleIteratorFactory.class);

    protected static final String ROOT_TEMPLATE = "\"%s%d\",base_url,year";
    private static final String PATTERN_TEMPLATE = "\"%s%d.*/[^/]+\",base_url,year";


    protected static final Pattern PDF_PATTERN = Pattern.compile("/([^/]+)\\.pdf$");
    protected static final String PDF_REPLACEMENT = "/$1.pdf";

    protected static final Pattern XML_PATTERN = Pattern.compile("/([^/]+)\\.xml$");
    protected static final String XML_REPLACEMENT = "/$1.xml";

    /*
    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                        MetadataTarget target)
            throws PluginException {
        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

        builder.setSpec(target,
                ROOT_TEMPLATE,
                PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);

        builder.addAspect(PDF_PATTERN,
                PDF_REPLACEMENT,
                ArticleFiles.ROLE_FULL_TEXT_PDF);

        builder.addAspect(XML_PATTERN,
                XML_REPLACEMENT,
                ArticleFiles.ROLE_ARTICLE_METADATA);

        builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_PDF);

        return builder.getSubTreeArticleIterator();
    }
     */

    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, final MetadataTarget target) throws PluginException {
        return new SubTreeArticleIterator(au,
                new SubTreeArticleIterator.Spec()
                        .setTarget(target)
                        .setRootTemplate(ROOT_TEMPLATE)
                        .setPatternTemplate(PATTERN_TEMPLATE)
                        .setVisitArchiveMembers(true)) {

            @Override
            protected ArticleFiles createArticleFiles(CachedUrl cu) {
                String url = cu.getUrl();
                ArticleFiles af = new ArticleFiles();

                Matcher xmlMatch = XML_PATTERN.matcher(url);
                if (xmlMatch.find()) {
                    CachedUrl xmlCu = au.makeCachedUrl(url.toString());
                    af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, xmlCu);

                    XPathXmlMetadataParser xmlParser = new XPathXmlMetadataParser();
                    try {
                        SourceXmlSchemaHelper schemaHelper = new CasaliniMarcXmlSchemaHelper();

                        xmlParser.setXmlParsingSchema(schemaHelper.getGlobalMetaMap(),
                                schemaHelper.getArticleNode(),
                                schemaHelper.getArticleMetaMap()
                                );

                        List<ArticleMetadata> amList;
                        amList = xmlParser.extractMetadataFromCu(target, cu);

                        if (amList != null) {

                            for (ArticleMetadata oneAM : amList) {

                                String start_page = "0";
                                String end_page = "0";
                                String yearNum = "0";
                                String volumeNum = "0";
                                String pdfFilePath = "";
                                Boolean volumeNumFound = false;

                                if (oneAM.getRaw(CasaliniMarcXmlSchemaHelper.MARC_start_page) != null) {
                                    String pages = oneAM.getRaw(CasaliniMarcXmlSchemaHelper.MARC_start_page);
                                    // It might in different formats
                                    // P. [1-20] [20]
                                    // 370-370 p.
                                    String page_pattern = "[^\\d]*?(\\d+)\\s*?\\-\\s*?(\\d+)[^\\d]*?";

                                    Pattern pattern = Pattern.compile(page_pattern, Pattern.CASE_INSENSITIVE);
                                    Matcher matcher = pattern.matcher(pages);

                                    while (matcher.find()) {
                                        start_page = matcher.group(1);
                                        end_page = matcher.group(2);
                                    }

                                } else {
                                    // there is not page information, we use a random generated number here
                                    Random rand = new Random();

                                    start_page = Integer.toString(rand.nextInt(Integer.MAX_VALUE));
                                    end_page = Integer.toString(rand.nextInt(Integer.MAX_VALUE));
                                }

                                if (oneAM.getRaw(CasaliniMarcXmlSchemaHelper.PDF_FILE_YEAR) != null) {
                                    yearNum = oneAM.getRaw(CasaliniMarcXmlSchemaHelper.PDF_FILE_YEAR).replace(".", "");
                                } else {
                                    log.debug3("yearNum is empty");
                                }

                                String fileNum = oneAM.getRaw(CasaliniMarcXmlSchemaHelper.MARC_file);
                                String cuBase = FilenameUtils.getFullPath(url);

                                if (oneAM.getRaw(CasaliniMarcXmlSchemaHelper.PDF_FILE_VOLUME) != null) {
                                    String volumeString = oneAM.getRaw(CasaliniMarcXmlSchemaHelper.PDF_FILE_VOLUME);
                                    int lastComma = volumeString.lastIndexOf(",");
                                    if (lastComma > -1) {
                                        volumeNum = volumeString.substring((lastComma - 1), lastComma);
                                        volumeNumFound = true;
                                        log.debug3("Building PDF file with Real volume: startpage = " + start_page + ", endpage = " + end_page + ", filename =" + fileNum + ", volume = " + volumeNum + ", year = " + yearNum);
                                    }
                                }

                                if (!volumeNumFound) {
                                    Random rand = new Random();
                                    volumeNum = Integer.toString(rand.nextInt(Integer.MAX_VALUE));

                                    log.debug3("Building PDF file with Random volume: startpage = " + start_page + ", endpage = " + end_page + ", filename =" + fileNum + ", volume = " + volumeNum + ", year = " + yearNum);
                                }

                                pdfFilePath = cuBase + yearNum + "_" + volumeNum + "_" + fileNum + ".pdf?startpage=" + start_page + "&endpage=" + end_page;
                                log.debug3(" Building PDF file with startpage and endpage: " + pdfFilePath);
                                CachedUrl pdfCu = au.makeCachedUrl(pdfFilePath);
                                af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
                                af.setFullTextCu(pdfCu);
                            }
                        }
                    }  catch (SAXException e) {
                        log.warning("ignoring sax error - " + e.getMessage());
                    } catch (XPathExpressionException e) {
                        log.warning("ignoring xpath error - " + e.getMessage());
                    } catch (IOException e) {
                        log.warning("ignoring io error - " + e.getMessage());
                    }
                }
                return af;
            }
        };
    }

    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
            throws PluginException {
        return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
    }

}