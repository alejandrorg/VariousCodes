package com.alejandrorg.wikihsdn.logic;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

/**
 * This class has been made to "fix" the freebase RDF content.
 * 
 * It has been created for my necessities so it is possible that it could work for everyone. I will
 * update it based on the new fixes that I will need to make to the RDF brokes that I will eventually found.
 * 
 * The fix is based on removing all the special characters that are making the RDF content not readable by parsers
 * such as the ones in Jena libraries.
 * 
 * Current version of the fix implies a content loss. Please check and modify the code adapted to your needs.
 * @author Alejandro Rodríguez González [http://www.alejandrorg.com]
 *
 */
public class FreebaseFixer {

	/**
	 * Same as the other method but converts the String into an InputStream needed by some libraries.
	 * @param url Receives the URL.
	 * @return Returns the input stream.
	 * @throws Exception It can throws exceptions.
	 */
	public static InputStream getFreebaseRDFContentAsInputStream(String url) throws Exception {
		return new ByteArrayInputStream(getFreebaseRDFContent(url).getBytes());
	}

	/**
	 * Method to get the freebase RDF content from a given URL as a String.
	 * It is needed to get the URL content as String to process it and fix it.
	 * @param url Receives the URL as parameter.
	 * @return Return the content in a String.
	 * @throws Exception It can throws exceptions.
	 */
	public static String getFreebaseRDFContent(String url) throws Exception {
		String urlContent = getURLContentAsString(url);

		String urlContentParts[] = urlContent.split("\n");
		String fixedWebContent = "";
		for (int i = 0; i < urlContentParts.length; i++) {
			String part = urlContentParts[i];
			/*
			 * In this case I'm removing unicode representations.. other options can be adaptaed.
			 */
			Pattern classPattern = Pattern.compile("\\\\u[0-9a-fA-F]{4}");
			Matcher classMatcher = classPattern.matcher(part);
			if (!classMatcher.find()) {
				part = part.replaceAll("\\\\x[0-9a-fA-F]{4}", "");
				part = part.replaceAll("\\\\x[0-9a-fA-F]", "");
				/*
				 * This is a fix for some special characters.
				 */
				part = part.replaceAll("[\u0024]+", "");
				/*
				 * This fix is for those cases where we have a string like that:
				 * 
				 * "Influenza is also known as "blabla" and blabla because "ble ble" and whatever"@en;
				 * The quotes inside the quotes break the the content for the parser.
				 */
				if (part.split(Character.toString('"')).length > 2) {
					part = fixStringQuotations(part);
				}
				fixedWebContent += part + "\r\n";
				// for debugging: allows to print line number
				// System.out.println(i + " - " + part);
			}
		}
		return fixedWebContent;
	}

	/**
	 * We use Apache HTTP libraries to get the content.
	 * @param urlToRead URL of the RDF data.
	 * @return Return the string.
	 * @throws Exception It can throws exceptions.
	 */
	private static String getURLContentAsString(String urlToRead)
			throws Exception {
		HttpClient client = new DefaultHttpClient();
		HttpGet request = new HttpGet(urlToRead);
		HttpResponse response = client.execute(request);
		HttpEntity entity = response.getEntity();
		String responseString = EntityUtils.toString(entity, "UTF-8");
		return responseString;
	}

	/**
	 * This method is in charge of fixing the problem of multiple quotes.
	 * @param str Receive the original string.
	 * @return Return the fixed string.
	 */
	private static String fixStringQuotations(String str) {
		String fixed = "";
		int numberQuotes = str.split(Character.toString('"')).length;
		int quotesFound = 0;
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) != '"') {
				fixed += str.charAt(i);
			} else {
				quotesFound++;
				if ((quotesFound == 1) || (quotesFound == numberQuotes - 1)) {
					fixed += '"';
				} else {
					fixed += "'";
				}
			}
		}
		return fixed;
	}
}
