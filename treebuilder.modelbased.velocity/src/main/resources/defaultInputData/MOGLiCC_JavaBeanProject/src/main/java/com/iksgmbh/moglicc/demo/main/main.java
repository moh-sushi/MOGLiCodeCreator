package com.iksgmbh.moglicc.demo.main;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class Main {

	public static void main(String[] args) {

		private static HashMap<File, List<File>> content = new HashMap<File, List<File>>();

		public static void main(String[] args) {
			final File userDir = new File(System.getProperty("user.dir"));
			analyseContent(userDir);
			System.out.println("Source content of project directory '" + userDir.getName() + "':");
			System.out.println();
			
			final Set<File> keySet = content.keySet();
			for (final File dir : keySet) {
				if (! dir.getAbsolutePath().contains("target\\classes")
					&& ! dir.getAbsolutePath().contains("target\\test-classes")) 
				{
					final List<File> list = content.get(dir);
					if (list.size() > 0) {				
						System.out.println("Content of " + dir + ":");
						for (final File file : list) {				
							System.out.println("    " + file.getName());
						}
					}
				}
			}
		}

		
		private static void analyseContent(final File dir) {
			final List<File> subFolders = new ArrayList<File>();
			final List<File> files = new ArrayList<File>();
			
			final File[] listFiles = dir.listFiles();
			for (final File file : listFiles) {
				if (file.isDirectory()) {
					subFolders.add(file);
				} else {
					if (! file.getName().startsWith(".")) {  // ignore config files
						files.add(file);
					}
				}
			}
			
			if (! dir.getName().startsWith(".")) { // ignore config dirs			
				content.put(dir, files);
			}
			
			for (final File subFolder : subFolders) {
				analyseContent(subFolder);
			}		
		}
	}
}
