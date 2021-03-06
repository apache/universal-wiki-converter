package com.atlassian.uwc.converters.moinmoin;

import java.io.File;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.atlassian.uwc.ui.FileUtils;
import org.apache.log4j.Logger;

import com.atlassian.uwc.converters.BaseConverter;
import com.atlassian.uwc.ui.Page;

public class MoinInfoConverter extends BaseConverter {

	final Logger log = Logger.getLogger(this.getClass());
	
	final Pattern date = Pattern.compile("\\{timestamp:([^}]*)\\}\\n", Pattern.MULTILINE);
	final Pattern username = Pattern.compile("\\{userid:([^}]*)\\}\\n", Pattern.MULTILINE);
	// Use reluctant quantifier for '.' in the revcomment, to be able to handle revcomments with curly braces inside
	final Pattern revInfoPattern = Pattern.compile("\\{revcomment:(.*?)\\}\\n", Pattern.MULTILINE);
	// Pattern for parsing the origauthor (injected by the MoinmoinExporter), so it can be added to the page metadata
	final Pattern origAuthorPattern = Pattern.compile("\\{origauthor:([^}]*)\\}\\n", Pattern.MULTILINE);
	final Pattern catPattern = Pattern.compile("\\[{2}CategoryRoot\\/([^\\]]*)\\]{2}", Pattern.MULTILINE);

	public MoinInfoConverter() {
		log.debug("Loaded MoinInfoConverter" + this.toString());
	}
	
	
	
	public void convert(Page page) {
		log.debug("Adding User and Date metadata - Starting");
		String input = page.getOriginalText();
		String username = getUser(input);
		Date timestamp = getDate(input);
		String revInfo = getRevinfo(input);
		String origAuthor = getOrigAuthor(input);
		String converted = cleanUserDate(input);
		
		addKeywords(input, page);
	
		log.debug(String.format("Page: %s Author: %s timestamp: %s revinfo: %s ", page.getName(), username, timestamp, revInfo));
				
		if (username != null) page.setAuthor(username);
		if (timestamp != null) page.setTimestamp(timestamp);
		if (revInfo != null) page.setVersionComment(revInfo);
		if (origAuthor != null) page.setOrigAuthor(origAuthor);

		page.setConvertedText(converted);
		log.debug("Adding User and Date metadata - Complete");
	}
	
	
	private void addKeywords(String input, Page page) {
		Matcher m = catPattern.matcher(input);
		if(m.find()){
			String x = m.group(1);
			log.debug(String.format("Adding to Page %s labels: %s ", page.getName(), x));
			for(String s : x.split("\\/")){
				page.addLabel(s);
			}
		}
	}


	private String getRevinfo(String input) {
		Matcher revFinder = revInfoPattern.matcher(input);
		if (revFinder.find()){
			return revFinder.group(1);
		}
		return null;
	}

	private String getOrigAuthor(String input) {
		Matcher origAuthorFinder = origAuthorPattern.matcher(input);
		if (origAuthorFinder.find()){
			return origAuthorFinder.group(1);
		}
		return null;
	}


	protected String getUser(String input) {
		Matcher userFinder = username.matcher(input);
		if (userFinder.find()) {
			return userFinder.group(1);
		}
		return null;
	}

	
	protected Date getDate(String input) {
		Matcher dateFinder = date.matcher(input);
		if (dateFinder.find()) {
			String timestamp = dateFinder.group(1);
			DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
			try {
				return dateFormat.parse(timestamp);
			} catch (ParseException e) {
				log.error("Couldn't format date: " + timestamp);
				e.printStackTrace();
				return null;
			}
		}
		return null;
	}

	protected String cleanUserDate(String input) {
		Matcher userFinder = username.matcher(input);
		if (userFinder.find()) {
			input = userFinder.replaceFirst("");
		}
		Matcher dateFinder = date.matcher(input);
		if (dateFinder.find()) {
			input = dateFinder.replaceFirst("");
		}
		Matcher x  = revInfoPattern.matcher(input);
		if(x.find()){
			input = x.replaceFirst("");
		}
		Matcher origAuthorFinder = origAuthorPattern.matcher(input);
		if (origAuthorFinder.find()) {
			input = origAuthorFinder.replaceFirst("");
		}
		return input;
	}

}
