package com.iksgmbh.moglicc.systemtest;

import static com.iksgmbh.moglicc.MogliSystemConstants.FILENAME_APPLICATION_PROPERTIES;
import static com.iksgmbh.moglicc.MogliSystemConstants.FILENAME_LOG_FILE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.iksgmbh.moglicc.MogliSystemConstants;
import com.iksgmbh.moglicc.build.MogliReleaseBuilder;
import com.iksgmbh.utils.FileUtil;

public class A_TechnicalSystemTest extends _AbstractSystemTest {
	
	private Properties buildProperties;
	
	@Before
	@Override
	public void setup() {
		super.setup();
		prepareTestDir();
		try {
			buildProperties = MogliReleaseBuilder.readBuildPropertiesFile();
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@After
	@Override
	public void teardown() {
		super.teardown();
	}

	// *****************************  test methods  ************************************
	
	@Test
	public void assertInitialFileStructure() {
		assertTrue("TestsDir does not exist!", testDir.exists());
		assertEquals("File number", 2, testDir.listFiles().length);
		
		final File libDir = FileUtil.getSubDir(testDir, "lib");
		assertEquals("File number", getNumberOfJarFilesToExpectInLibDir() + 1, libDir.listFiles().length);
		
		final File pluginsDir = FileUtil.getSubDir(libDir, "plugins");
		assertEquals("File number", getNumberOfJarFilesToExpectPluginsDir(), pluginsDir.listFiles().length);
	}

	private int getNumberOfJarFilesToExpectPluginsDir() {
		return MogliReleaseBuilder.PLUGIN_MODULES.size();
	}

	protected int getNumberOfJarFilesToExpectInLibDir() {
		return MogliReleaseBuilder.THIRD_PARTY_LIBRARIES.size() + 
		       MogliReleaseBuilder.CORE_MODULES.size();
	}	

	@Test
	public void createsDefaultMogliPropertiesFile() {
		// prepare test
		final File propertiesFile = new File(testDir, FILENAME_APPLICATION_PROPERTIES); 
		assertFileDoesNotExist(propertiesFile);
		
		// call functionality under test
		executeMogliApplication();
		
		// verify test result
		assertFileExists(propertiesFile);
	}


	@Test
	public void createsMogliLogfile() {
		// prepare test
		assertFileDoesNotExist(applicationLogDir);
		assertFileDoesNotExist(applicationLogfile);
		
		// call functionality under test
		executeMogliApplication();
		
		// verify test result
		assertFileExists(applicationLogDir);
		assertFileExists(applicationLogfile);
	}	

	@Test
	public void countNumberFoundPluginsInMogliLogfile() {
		// prepare test
		applicationLogfile.delete();
		assertFileDoesNotExist(applicationLogfile);
		
		// call functionality under test
		executeMogliApplication();
		
		// verify test result
		assertFileExists(applicationLogfile);
		assertEquals("Number Plugins", MogliReleaseBuilder.PLUGIN_MODULES.size(), countMatchesInContainedFile(applicationLogfile, "PluginMetaData"));
	}
	

	@Test
	public void logsCurrentVersionString() {
		// prepare test
		applicationLogfile.delete();
		assertFileDoesNotExist(applicationLogfile);
		
		// call functionality under test
		executeMogliApplication();
		
		// verify test result
		assertFileExists(applicationLogfile);
		final String expectedVersionString = buildProperties.getProperty(MogliReleaseBuilder.PROPERTY_RELEASE_VERSION);
		assertFileContainsEntry(applicationLogfile, expectedVersionString);
	}

	@Test
	public void createsNewWorkspaceDirWithRelativePathToSubdir() {
		// prepare test
		initPropertiesWith("workspace=workspaces/demo");
		final File workspaceDir = new File(applicationTestDir, "workspaces/demo");
		assertFileDoesNotExist(workspaceDir.getParentFile());
		
		// call functionality under test
		executeMogliApplication();
		
		// cleanup critical stuff before possible test failures
		applicationPropertiesFile.delete();
		assertFileDoesNotExist(applicationPropertiesFile);

		// verify test result
		assertFileExists(workspaceDir);
		final File workspaceLogDir = new File(workspaceDir, MogliSystemConstants.DIR_LOGS_FILES);
		assertChildrenNumberInDirectory(workspaceLogDir, 5);
		final File workspaceInputDir = new File(workspaceDir, MogliSystemConstants.DIR_INPUT_FILES);
		assertChildrenNumberInDirectory(workspaceInputDir, 3);
		final File workspaceOutputDir = new File(workspaceDir, MogliSystemConstants.DIR_OUTPUT_FILES);
		assertChildrenNumberInDirectory(workspaceOutputDir, 3);
			// TEMP dir ?	

		// cleanup
		FileUtil.deleteDirWithContent(workspaceDir.getParentFile());
	}
	
	@Test
	public void createsEmergencyLogFileIfDefinedWorkspaceDirCannotBeCreated() {
		// prepare test
		initPropertiesWith("workspace=C:/temp/Mogli/workspace");
		final File emergencyLogFile = new File(applicationTestDir, FILENAME_LOG_FILE);
		assertFileDoesNotExist(emergencyLogFile);
		
		// call functionality under test
		executeMogliApplication();
		
		// cleanup critical stuff before possible test failures
		applicationPropertiesFile.delete();
		assertFileDoesNotExist(applicationPropertiesFile);
		
		// verify test result
		assertFileExists(emergencyLogFile);
		assertFileContainsEntry(emergencyLogFile, "C:\\temp\\Mogli\\workspace\\log");
		assertFileContainsEntry(emergencyLogFile, "Could not create ");
	}
	
	@Test
	public void executesAllPluginsSuccessfully() {
		// call functionality under test
		executeMogliApplication();
		
		// cleanup critical stuff before possible test failures
		assertFileContainsEntry(applicationLogfile, "All " + getNumberOfJarFilesToExpectPluginsDir() 
				                                   + " plugins executed successfully");
	}
}