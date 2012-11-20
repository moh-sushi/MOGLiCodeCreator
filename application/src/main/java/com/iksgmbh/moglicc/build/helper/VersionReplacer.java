package com.iksgmbh.moglicc.build.helper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.iksgmbh.moglicc.build.MogliReleaseBuilder;
import com.iksgmbh.moglicc.build.MogliReleaseBuilder.VERSION_TYPE;
import com.iksgmbh.moglicc.exceptions.MogliCoreException;
import com.iksgmbh.moglicc.utils.MogliFileUtil;
import com.iksgmbh.utils.FileUtil;

public class VersionReplacer {
	
	private static final int MAX_LINE_NUMBER_TO_SEARCH_FOR_VERSION_STRING = 12;
	private static final String APPLICATION_START_FILE = "MogliCodeCreator.java";
	private static final String PATH_TO_APPLICATION_START_FILE = "core/src/main/java/com/iksgmbh/moglicc/" + APPLICATION_START_FILE;
	private static final File applicationStartFile = new File(MogliReleaseBuilder.WORKSPACE, PATH_TO_APPLICATION_START_FILE);
	public static final String JAVA_VERSION_IDENTIFIER = "static final String VERSION";
	
	private String oldVersion;
	private String newVersion;

	public VersionReplacer(String oldVersion, String newVersion) {
		this.oldVersion = oldVersion;
		this.newVersion = newVersion;
	}

	public static void doYourJob(final String oldVersion, final String newVersion, final String... poms) {
		final VersionReplacer versionReplacer = new VersionReplacer(oldVersion, newVersion);
		System.out.println("");
		System.out.println("--------------------------------------------------------------");
		if (oldVersion == null) {
			System.out.println("Overwriting current version string with " + newVersion + " in all " + poms.length + " pomfiles...");
			for (int i = 0; i < poms.length; i++) {
				versionReplacer.overwriteVersionInPomToNewVersion(poms[i]);
			}
			replaceInJavaFile(applicationStartFile, null, newVersion);
		} else {
			System.out.println("Replacing " + oldVersion + " to " + newVersion + " in all " + poms.length + " pomfiles...");
			for (int i = 0; i < poms.length; i++) {
				versionReplacer.replaceVersionInPom(poms[i]);
			}
			replaceInJavaFile(applicationStartFile, oldVersion, newVersion);
		}
		System.out.println("--------------------------------------------------------------");
		System.out.println("");
	}

	private static void replaceInJavaFile(final File file, final String oldVersion, 
			                                               final String newVersion) {
		final List<String> fileContentAsList = MogliFileUtil.getFileContentAsList(file);
		final List<String> newContent = new ArrayList<String>();
		int matches = 0;
		for (final String line : fileContentAsList) {
			if (line.contains(JAVA_VERSION_IDENTIFIER)) {
				matches++;
				final int pos = line.indexOf(JAVA_VERSION_IDENTIFIER);
				String newLine = line.substring(0, pos + JAVA_VERSION_IDENTIFIER.length()); 
				if (oldVersion == null) {
					System.out.println("Overwriting current version string with " + newVersion + " in " + APPLICATION_START_FILE + "...");
					newLine += " = \"" + newVersion + "\";";
				} else {
					System.out.println("Replacing " + oldVersion + " to " + newVersion + " in " + APPLICATION_START_FILE + "...");
					newLine = line.replace(oldVersion, newVersion);
				}
				newContent.add(newLine);
			} else {
				newContent.add(line);
			}
		}
		
		if (oldVersion != null && matches != 1) {
			throw new MogliCoreException("\nUnexpected number of version matches:\n" 
					+ "Expected matches of <" + JAVA_VERSION_IDENTIFIER + "> lines: 1\n" 
					+ "Actual matches: "+ matches);
		}
		
		try {
			FileUtil.createNewFileWithContent(file, newContent);
		} catch (Exception e) {
			throw new MogliCoreException("Error creating file\n" + file.getAbsolutePath());
		}
	}

	private void overwriteVersionInPomToNewVersion(final String pomFile) {
		final List<String> fileContent = readFileContent(pomFile);
		System.out.println("Replacing version in " + pomFile + "...");
		final String overwrittenFileContent = overwriteInLines(fileContent);
		try {
			FileUtil.createNewFileWithContent(new File(pomFile), overwrittenFileContent);
		} catch (Exception e) {
			throw new MogliCoreException(e);
		}
	}

	private List<String> readFileContent(final String pomFile) {
		final List<String> fileContent;
		final File file = new File(pomFile);
		try {
			fileContent = FileUtil.getFileContentAsList(file);
		} catch (IOException e) {
			throw new MogliCoreException(e);
		}
		return fileContent;
	}
	
	void replaceVersionInPom(final String pomFile) {
		final List<String> fileContent = readFileContent(pomFile);
		
		System.out.println("Replacing version in " + pomFile);
		final String replacedFileContent = replaceInLines(fileContent);
		
		try {
			FileUtil.createNewFileWithContent(new File(pomFile), replacedFileContent);
		} catch (Exception e) {
			throw new MogliCoreException(e);
		}
	}

	String overwriteInLines(final List<String> fileContent) {
		final StringBuffer replacedFileContent = new StringBuffer();
		int matches = 0;
		for (int i = 0; i < fileContent.size(); i++) {
			String line = fileContent.get(i).trim();
			if (i <= MAX_LINE_NUMBER_TO_SEARCH_FOR_VERSION_STRING) {
				// expecting the version within the first lines of the pom file
				if (line.startsWith("<version>")) {
					matches++;
					line = "<version>" + newVersion + "</version>";
				}
			}
			replacedFileContent.append(line);
			replacedFileContent.append(FileUtil.getSystemLineSeparator());
		}
		if (matches != 1) {
			throw new MogliCoreException("\nUnexpected number of version matches:\n" 
					+ "Expected matches of <version> setting lines: 1\n" 
					+ "Actual matches: "+ matches);
		}
		return replacedFileContent.toString().trim();
	}

	String replaceInLines(final List<String> fileContent) {
		final StringBuffer replacedFileContent = new StringBuffer();
		int matches = 0;
		for (int i = 0; i < fileContent.size(); i++) {
			String line = fileContent.get(i);
			if (i <= MAX_LINE_NUMBER_TO_SEARCH_FOR_VERSION_STRING) {
				// expecting the version within the first lines of the pom file
				if (line.contains(oldVersion)) {
					matches++;
					line = line.replace(oldVersion, newVersion);
				}
			}
			replacedFileContent.append(line);
			replacedFileContent.append(FileUtil.getSystemLineSeparator());
		}
		if (matches != 1) {
			throw new MogliCoreException("\nUnexpected number of version matches:\n" 
					+ "Expected matches of <" + oldVersion + ">: 1\n" 
					+ "Actual matches: "+ matches);
		}
		return replacedFileContent.toString().trim();
	}

	public static void setVersionInPomsBackToOldValue() {
		System.out.println("");
		System.out.println("############################################################################");
		final MogliReleaseBuilder releaseBuilder = new MogliReleaseBuilder();
		final String newVersion = releaseBuilder.getVersion(VERSION_TYPE.Current);  // version before release build
		final String oldVersion1 = releaseBuilder.getVersion(VERSION_TYPE.Next);    // version after release build
		final String oldVersion2 = releaseBuilder.getVersion(VERSION_TYPE.Release); // version during release build
		try {
			replaceVersionStrings(releaseBuilder, newVersion, oldVersion1);
		} catch (Exception e) {
			System.out.println("");
			System.out.println(oldVersion1 + " failed, try " + oldVersion2);
			// oldVersion1 not found, try oldVersion2
			replaceVersionStrings(releaseBuilder, newVersion, oldVersion2);
		}
		System.out.println("Done!");
		System.out.println("############################################################################");
	}

	private static void replaceVersionStrings(final MogliReleaseBuilder releaseBuilder,
			                                  final String newVersion, final String oldVersion) {
		System.out.println("Setting back version " + oldVersion + " to " + newVersion);
		VersionReplacer.doYourJob(oldVersion, newVersion, releaseBuilder.getPomFiles());
	}
	
	public static void main(String[] args) {
		//setVersionInPomsBackToOldValue();
		final MogliReleaseBuilder releaseBuilder = new MogliReleaseBuilder();
		VersionReplacer.doYourJob(null, "0.1.0-SNAPSHOT", releaseBuilder.getPomFiles());
	}
}