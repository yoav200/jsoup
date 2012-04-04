package org.jsoup.nodes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.CharsetEncoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTML entities, and escape routines. Source: <a href="http://www.w3.org/TR/html5/named-character-references.html#named-character-references">W3C
 * HTML named character references</a>.
 */
public class Entities {
    public enum EscapeMode {
	/** Restricted entities suitable for XHTML output: lt, gt, amp, apos, and quot only. */
	xhtml(xhtmlByVal),
	/** Default HTML output entities. */
	base(baseByVal),
	/** Complete HTML entities. */
	extended(fullByVal);

	private Map<Character, String> map;

	EscapeMode(Map<Character, String> map) {
	    this.map = map;
	}

	public Map<Character, String> getMap() {
	    return map;
	}
    }

    private static final Map<String, Character> full;
    private static final Map<Character, String> xhtmlByVal;
    private static final Map<Character, String> baseByVal;
    private static final Map<Character, String> fullByVal;
    private static final Pattern unescapePattern = Pattern.compile("&(#(x|X)?([0-9a-fA-F]+)|[a-zA-Z]+\\d*);?");
    private static final Pattern strictUnescapePattern = Pattern.compile("&(#(x|X)?([0-9a-fA-F]+)|[a-zA-Z]+\\d*);");

    private Entities() {
    }

    /**
     * Check if the input is a known named entity
     * 
     * @param name
     *            the possible entity name (e.g. "lt" or "amp"
     * @return true if a known named entity
     */
    public static boolean isNamedEntity(String name) {
	return full.containsKey(name);
    }

/**
     * Get the Character value of the named entity
     * @param name named entity (e.g. "lt" or "amp")
     * @return the Character value of the named entity (e.g. '<' or '&')
     */
    public static Character getCharacterByName(String name) {
	return full.get(name);
    }

    static String escape(String string, Document.OutputSettings out) {
	return escape(string, out.encoder(), out.escapeMode());
    }

    static String escape(String string, CharsetEncoder encoder, EscapeMode escapeMode) {
	StringBuilder accum = new StringBuilder(string.length() * 2);
	Map<Character, String> map = escapeMode.getMap();

	for (int pos = 0; pos < string.length(); pos++) {
	    Character c = string.charAt(pos);
	    if (map.containsKey(c))
		accum.append('&').append(map.get(c)).append(';');
	    else if (encoder.canEncode(c))
		accum.append(c.charValue());
	    else
		accum.append("&#").append((int) c).append(';');
	}

	return accum.toString();
    }

    static String unescape(String string) {
	return unescape(string, false);
    }

    /**
     * Unescape the input string.
     * 
     * @param string
     * @param strict
     *            if "strict" (that is, requires trailing ';' char, otherwise that's optional)
     * @return
     */
    static String unescape(String string, boolean strict) {
	// todo: change this method to use Tokeniser.consumeCharacterReference
	if (!string.contains("&"))
	    return string;

	Matcher m = strict ? strictUnescapePattern.matcher(string) : unescapePattern.matcher(string); // &(#(x|X)?([0-9a-fA-F]+)|[a-zA-Z]\\d*);?
	StringBuffer accum = new StringBuffer(string.length()); // pity matcher can't use stringbuilder, avoid syncs
	// todo: replace m.appendReplacement with own impl, so StringBuilder and quoteReplacement not required

	while (m.find()) {
	    int charval = -1;
	    String num = m.group(3);
	    if (num != null) {
		try {
		    int base = m.group(2) != null ? 16 : 10; // 2 is hex indicator
		    charval = Integer.valueOf(num, base);
		} catch (NumberFormatException e) {
		} // skip
	    } else {
		String name = m.group(1);
		if (full.containsKey(name))
		    charval = full.get(name);
	    }

	    if (charval != -1 || charval > 0xFFFF) { // out of range
		String c = Character.toString((char) charval);
		m.appendReplacement(accum, Matcher.quoteReplacement(c));
	    } else {
		m.appendReplacement(accum, Matcher.quoteReplacement(m.group(0))); // replace with original string
	    }
	}
	m.appendTail(accum);
	return accum.toString();
    }

    // xhtml has restricted entities
    private static final Object[][] xhtmlArray = { { "quot", 0x00022 }, { "amp", 0x00026 }, { "apos", 0x00027 }, { "lt", 0x0003C }, { "gt", 0x0003E } };

    // most common, base entities can be unescaped without trailing ;
    // e.g. &amp
    private static final Object[][] baseArray = { { "AElig", 0x000C6 }, { "AMP", 0x00026 }, { "Aacute", 0x000C1 }, { "Acirc", 0x000C2 },
	    { "Agrave", 0x000C0 }, { "Aring", 0x000C5 }, { "Atilde", 0x000C3 }, { "Auml", 0x000C4 }, { "COPY", 0x000A9 }, { "Ccedil", 0x000C7 },
	    { "ETH", 0x000D0 }, { "Eacute", 0x000C9 }, { "Ecirc", 0x000CA }, { "Egrave", 0x000C8 }, { "Euml", 0x000CB }, { "GT", 0x0003E },
	    { "Iacute", 0x000CD }, { "Icirc", 0x000CE }, { "Igrave", 0x000CC }, { "Iuml", 0x000CF }, { "LT", 0x0003C }, { "Ntilde", 0x000D1 },
	    { "Oacute", 0x000D3 }, { "Ocirc", 0x000D4 }, { "Ograve", 0x000D2 }, { "Oslash", 0x000D8 }, { "Otilde", 0x000D5 }, { "Ouml", 0x000D6 },
	    { "QUOT", 0x00022 }, { "REG", 0x000AE }, { "THORN", 0x000DE }, { "Uacute", 0x000DA }, { "Ucirc", 0x000DB }, { "Ugrave", 0x000D9 },
	    { "Uuml", 0x000DC }, { "Yacute", 0x000DD }, { "aacute", 0x000E1 }, { "acirc", 0x000E2 }, { "acute", 0x000B4 }, { "aelig", 0x000E6 },
	    { "agrave", 0x000E0 }, { "amp", 0x00026 }, { "aring", 0x000E5 }, { "atilde", 0x000E3 }, { "auml", 0x000E4 }, { "brvbar", 0x000A6 },
	    { "ccedil", 0x000E7 }, { "cedil", 0x000B8 }, { "cent", 0x000A2 }, { "copy", 0x000A9 }, { "curren", 0x000A4 }, { "deg", 0x000B0 },
	    { "divide", 0x000F7 }, { "eacute", 0x000E9 }, { "ecirc", 0x000EA }, { "egrave", 0x000E8 }, { "eth", 0x000F0 }, { "euml", 0x000EB },
	    { "frac12", 0x000BD }, { "frac14", 0x000BC }, { "frac34", 0x000BE }, { "gt", 0x0003E }, { "iacute", 0x000ED }, { "icirc", 0x000EE },
	    { "iexcl", 0x000A1 }, { "igrave", 0x000EC }, { "iquest", 0x000BF }, { "iuml", 0x000EF }, { "laquo", 0x000AB }, { "lt", 0x0003C },
	    { "macr", 0x000AF }, { "micro", 0x000B5 }, { "middot", 0x000B7 }, { "nbsp", 0x000A0 }, { "not", 0x000AC }, { "ntilde", 0x000F1 },
	    { "oacute", 0x000F3 }, { "ocirc", 0x000F4 }, { "ograve", 0x000F2 }, { "ordf", 0x000AA }, { "ordm", 0x000BA }, { "oslash", 0x000F8 },
	    { "otilde", 0x000F5 }, { "ouml", 0x000F6 }, { "para", 0x000B6 }, { "plusmn", 0x000B1 }, { "pound", 0x000A3 }, { "quot", 0x00022 },
	    { "raquo", 0x000BB }, { "reg", 0x000AE }, { "sect", 0x000A7 }, { "shy", 0x000AD }, { "sup1", 0x000B9 }, { "sup2", 0x000B2 },
	    { "sup3", 0x000B3 }, { "szlig", 0x000DF }, { "thorn", 0x000FE }, { "times", 0x000D7 }, { "uacute", 0x000FA }, { "ucirc", 0x000FB },
	    { "ugrave", 0x000F9 }, { "uml", 0x000A8 }, { "uuml", 0x000FC }, { "yacute", 0x000FD }, { "yen", 0x000A5 }, { "yuml", 0x000FF } };

    private static Object[][] fullArray = loadArray("full-array");

    static {
	
	full = new HashMap<String, Character>(fullArray.length);
	xhtmlByVal = new HashMap<Character, String>(xhtmlArray.length);
	baseByVal = new HashMap<Character, String>(baseArray.length);
	fullByVal = new HashMap<Character, String>(fullArray.length);

	for (Object[] entity : xhtmlArray) {
	    Character c = Character.valueOf((char) ((Integer) entity[1]).intValue());
	    xhtmlByVal.put(c, ((String) entity[0]));
	}
	for (Object[] entity : baseArray) {
	    Character c = Character.valueOf((char) ((Integer) entity[1]).intValue());
	    baseByVal.put(c, ((String) entity[0]));
	}
	for (Object[] entity : fullArray) {
	    Character c = Character.valueOf((char) ((Integer) entity[1]).intValue());
	    full.put((String) entity[0], c);
	    fullByVal.put(c, ((String) entity[0]));
	}
    }

    private static Object[][] loadArray(String string) {
	Object[][] array = null;
	Properties prop = new Properties();

	try {
	    InputStream in = Entities.class.getResourceAsStream(string + ".properties");
	    prop.load(in);
	    in.close();
	} catch (IOException e) {
	    throw new RuntimeException(e);
	}

	if (prop.size() > 0) {
	    array = new Object[prop.size()][];
	    Enumeration<?> e = prop.propertyNames();
	    int index = 0;
	    while (e.hasMoreElements()) {
		String key = (String) e.nextElement();
		String value = prop.getProperty(key);
		Object[] arr = { key, Integer.parseInt(value, 34) };
		array[index++] = arr;
	    }
	}
	return array;
    }
}
