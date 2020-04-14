package org.lockss.plugin.clockss.intechopen;

import org.lockss.plugin.clockss.Onix3BooksSchemaHelper;
import org.lockss.util.Logger;

public class IntechOpenOnix3BooksSchemaHelper extends Onix3BooksSchemaHelper {

    static Logger log = Logger.getLogger(IntechOpenOnix3BooksSchemaHelper.class);

    /*
        Each book will have 3 "<product>" element in the file, each represents PDF, hardback, and as a digital online release
        And it is guaranteed the second one is always the hardback (the one we want)
        It's x mod 3 == 2 because in XPath position() is one-based
     */
    static private final String ONIX_articleNode = "//Product[position() mod 3 = 2]|//product";

    /**
     * Return the article node path
     */
    @Override
    public String getArticleNode() {
        return ONIX_articleNode;
    }

}

