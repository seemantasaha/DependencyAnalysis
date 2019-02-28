package drivergen;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.commons.io.FileUtils;

/**
 * @author Rody Kersten
 *
 */
public class Template {
	public static final String DELIM_START = "<";
	public static final String DELIM_END = ">";
	
	private String content;
	
	public Template(String file) throws IOException {
		URL url = getClass().getResource(file);
		content = FileUtils.readFileToString(new File(url.getFile()), "UTF-8");
	}
	
	public String getContent() {
		return content;
	}
	
	/**
	 * Replace a tag by a value.
	 * @param tag
	 * @param value
	 */
	public void replace(String tag, String value) {
		String key = DELIM_START+tag+DELIM_END;
		content = content.replaceAll(Matcher.quoteReplacement(key), Matcher.quoteReplacement(value));
	}
	
	/**
	 * Replace all keys in the HashMap by their values
	 * @param tag
	 * @param value
	 */
	public void replaceAll(Map<String,String> tagValuePairs) {
		for (Map.Entry<String,String> e : tagValuePairs.entrySet()) {
			replace(e.getKey(), e.getValue());
		}
	}
}
