package celtech.gcodeviewer.i18n.languagedata;

import celtech.gcodeviewer.i18n.MessageLookup;
import celtech.gcodeviewer.i18n.LanguagePropertiesResourceBundle;
import java.util.Locale;

/**
 *
 * @author ianhudson
 */
public class LanguageData extends LanguagePropertiesResourceBundle
{
    public LanguageData()
    {
        super(MessageLookup.getApplicationInstallDirectory(), "Language", "LanguageData");
    }
}
