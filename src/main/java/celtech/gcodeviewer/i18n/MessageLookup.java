package celtech.gcodeviewer.i18n;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import libertysystems.stenographer.LogLevel;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Tony Aldhous
 */
public class MessageLookup
{

    private static final Stenographer steno = StenographerFactory.getStenographer(
            MessageLookup.class.getName());

    private static ResourceBundle i18nbundle = null;
    private static Locale applicationLocale = null;
    private static String applicationInstallDirectory = null;

    public static ResourceBundle getLanguageBundle()
    {
        return i18nbundle;
    }

    public static String i18n(String stringId)
    {
        String langString = null;
        try
        {
           langString = i18nbundle.getString(stringId);
        }
        catch (MissingResourceException ex)
        {
            langString = stringId;
        }
        langString = substituteTemplates(langString);
        return langString;
    }

    /**
     * Strings containing templates (eg *T14) should be substituted with the
     * correct text.
     *
     * @param langString
     * @return
     */
    public static String substituteTemplates(String langString)
    {
        String patternString = ".*\\*T(\\d\\d).*";
        Pattern pattern = Pattern.compile(patternString);
        while (true)
        {
            Matcher matcher = pattern.matcher(langString);
            if (matcher.find())
            {
                String template = "*T" + matcher.group(1);
                String templatePattern = "\\*T" + matcher.group(1);
                langString = langString.replaceAll(templatePattern, i18n(template));
            } else
            {
                break;
            }
        }
        return langString;
    }

    public static Locale getDefaultApplicationLocale(String languageTag)
    {
        Locale appLocale;
        if (languageTag == null || languageTag.length() == 0)
        {
            appLocale = Locale.getDefault();
        } else
        {
            String[] languageElements = languageTag.split("-");
            switch (languageElements.length)
            {
                case 1:
                    appLocale = new Locale(languageElements[0]);
                    break;
                case 2:
                    appLocale = new Locale(languageElements[0], languageElements[1]);
                    break;
                case 3:
                    appLocale = new Locale(languageElements[0], languageElements[1],
                            languageElements[2]);
                    break;
                default:
                    appLocale = Locale.getDefault();
                    break;
            }
        }
        
        return appLocale;
    }

    public static Locale getApplicationLocale()
    {
        return applicationLocale;
    }

    public static void setApplicationLocale(Locale locale)
    {
        applicationLocale = locale;
    }

    public static String getApplicationInstallDirectory()
    {
        return applicationInstallDirectory;
    }

    public static void setApplicationInstallDirectory(String directory)
    {
        applicationInstallDirectory = directory;
    }

    public static void loadMessages(String installDirectory, Locale appLocale)
    {
        applicationInstallDirectory = installDirectory;
        
        applicationLocale = appLocale;
        steno.info("Loading resources for locale " + applicationLocale);
        i18nbundle = null;
        try
        {
            i18nbundle = ResourceBundle.getBundle("celtech.gcodeviewer.i18n.languagedata.LanguageData", applicationLocale);
        }
        catch (Exception ex)
        {
            steno.error("Failed to load language resources: " + ex.getMessage());
            i18nbundle = null;
        }

        if (i18nbundle == null)
        {
            applicationLocale = Locale.ENGLISH;
            steno.debug("Loading resources for fallback locale " + applicationLocale);

            i18nbundle = ResourceBundle.getBundle("celtech.gcodeviewer.i18n.languagedata.LanguageData", applicationLocale);
        }
    }
}
