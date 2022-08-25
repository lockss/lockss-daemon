package org.lockss.plugin.clockss.edituraase;

import org.lockss.plugin.clockss.CrossRefSchemaHelper;

public class EdituraASEPublishingHelper extends CrossRefSchemaHelper {

    @Override
    public String getFilenameXPathKey() {
        return art_resource;
    }
}
