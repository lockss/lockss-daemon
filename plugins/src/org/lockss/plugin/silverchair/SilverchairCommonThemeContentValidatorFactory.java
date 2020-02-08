package org.lockss.plugin.silverchair;

import org.lockss.plugin.ContentValidator;

public class SilverchairCommonThemeContentValidatorFactory extends BaseScContentValidatorFactory {

    public static class SilverchairCommonThemeContentValidator extends ScTextTypeValidator {

        private static final String RESTRICTED_ACCESS_STRING = "article-top-info-user-restricted-options";
        // the pattern is essentially long enough to contain 2 patterns to ensure the match does not give a false positive
        private static final String EXPIRES_PAT_STRING = "[?]Expires=(?!2147483647).......................";

        @Override
        public String getInvalidString() {
            return RESTRICTED_ACCESS_STRING;
        }

        @Override
        public String getPatternString() {
            return EXPIRES_PAT_STRING;
        }
    }

    public ContentValidator getTextTypeValidator() {
        return new SilverchairCommonThemeContentValidator();
    }
}

