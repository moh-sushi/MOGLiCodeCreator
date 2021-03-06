package com.iksgmbh.moglicc.lineinserter.modelbased.velocity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;

import com.iksgmbh.moglicc.data.BuildUpGeneratorResultData;
import com.iksgmbh.moglicc.exceptions.MOGLiPluginException;
import com.iksgmbh.moglicc.generator.classbased.velocity.VelocityGeneratorResultData;
import com.iksgmbh.moglicc.generator.classbased.velocity.VelocityGeneratorResultData.KnownGeneratorPropertyNames;
import com.iksgmbh.moglicc.generator.utils.ArtefactListUtil;
import com.iksgmbh.moglicc.generator.utils.TemplateUtil;
import com.iksgmbh.moglicc.lineinserter.modelbased.velocity.VelocityLineInserterResultData.KnownInserterPropertyNames;
import com.iksgmbh.moglicc.provider.model.standard.metainfo.MetaInfoValidationUtil;
import com.iksgmbh.moglicc.provider.model.standard.metainfo.MetaInfoValidator;
import com.iksgmbh.moglicc.provider.model.standard.metainfo.validator.ConditionalMetaInfoValidator;
import com.iksgmbh.moglicc.utils.MOGLiFileUtil;
import com.iksgmbh.utils.FileUtil;

public class VelocityModelBasedLineInserterUnitTest extends VelocityModelBasedLineInserterTestParent {

	private static final String TARGET_FILE_TXT = "targetFile.txt";
	private static final String ARTEFACT_BEAN_FACTORY = "BeanFactory";

	private File targetFile;
	private File generatorPropertiesFile;

	@Override
	@Before
	public void setup() {
		super.setup();
		applicationTempDir.mkdirs();

		final File sourcefile = new File(getProjectTestResourcesDir(), TARGET_FILE_TXT);
		targetFile = new File(applicationTempDir, TARGET_FILE_TXT);
		FileUtil.copyTextFile(sourcefile, targetFile);

		generatorPropertiesFile = new File(infrastructure.getPluginInputDir(), VelocityModelBasedLineInserterStarter.PLUGIN_PROPERTIES_FILE);
		MOGLiFileUtil.createNewFileWithContent(generatorPropertiesFile, "");
	}

	@Test
	public void findsDefaultArtefactList() throws MOGLiPluginException {
		// call functionality under test
		final List<String> artefactList = velocityModelBasedLineInserter.getArtefactList();

		// verify test result
		assertEquals("artefact number", 1, artefactList.size());
	}

	@Test
	public void throwsExceptionIfMainTemplateIsNotFound() {
		// prepare test
		final File artefactDir = new File(infrastructure.getPluginInputDir(), ARTEFACT_BEAN_FACTORY);
		FileUtil.deleteDirWithContent(artefactDir);
		assertFileDoesNotExist(artefactDir);
		artefactDir.mkdirs();

		// call functionality under test
		try {
			velocityModelBasedLineInserter.findMainTemplates(artefactDir);
		} catch (MOGLiPluginException e) {
			assertStringContains(e.getMessage(), TemplateUtil.NO_MAIN_TEMPLATE_FOUND);

			// cleanup
			FileUtil.deleteDirWithContent(generatorPluginInputDir);

			return;
		}

		// cleanup
		FileUtil.deleteDirWithContent(generatorPluginInputDir);

		fail("Expected exception not thrown!");
	}

	@Test
	public void findsDefaultMainTemplates() throws MOGLiPluginException {
		// prepare test
		final File artefactDir = new File(infrastructure.getPluginInputDir(), ARTEFACT_BEAN_FACTORY);

		// call functionality under test
		 List<String> mainTemplate = velocityModelBasedLineInserter.findMainTemplates(artefactDir);

		// verify test result
		assertEquals("Number of Main Templates", 4, mainTemplate.size());
	}

	@Test
	public void unpacksInputDefaultData() throws MOGLiPluginException {
		// prepare test
		FileUtil.deleteDirWithContent(applicationInputDir);

		// call functionality under test
		velocityModelBasedLineInserter.unpackDefaultInputData();

		// verify test result
		assertFileExists(applicationInputDir);
	}



	@Test
	public void savesGeneratedContentInPluginOutputDir() throws MOGLiPluginException {
		// prepare test
		VelocityLineInserterResultData resultData = buildVelocityLineInserterResultData("Content",
				"temp", TARGET_FILE_TXT, KnownGeneratorPropertyNames.CreateNew.name(), "true");
		velocityEngineProvider.setVelocityGeneratorResultData(resultData);
		final File targetFile = prepareTargetFile(applicationOutputDir, VelocityModelBasedLineInserterStarter.PLUGIN_ID
                                                                        + "/" + ARTEFACT_BEAN_FACTORY
                                                                        + "/" + TARGET_FILE_TXT);
		assertFileDoesNotExist(targetFile);

		// call functionality under test
		velocityModelBasedLineInserter.doYourJob();

		// verify test result
		assertFileExists(targetFile);
		assertFileContainsEntry(targetFile, "Content");
	}

	private VelocityLineInserterResultData buildVelocityLineInserterResultData(final String content,
			final String targetdir, final String targetFileName,
			final String propertyName, final String propertyValue) {

		final BuildUpGeneratorResultData buildUpGeneratorResultData = new BuildUpGeneratorResultData();
		buildUpGeneratorResultData.setGeneratedContent(content);

		final VelocityLineInserterResultData toReturn =
			   new VelocityLineInserterResultData(buildUpGeneratorResultData);

		if (targetFileName != null) {
			toReturn.addProperty(KnownGeneratorPropertyNames.TargetFileName.name(), targetFileName);
		}
		if (targetdir != null) {
			toReturn.addProperty(KnownGeneratorPropertyNames.TargetDir.name(), targetdir);
		}

		if (StringUtils.isNotBlank(propertyName) && StringUtils.isNotBlank(propertyValue)) {
			toReturn.addProperty(propertyName, propertyValue);
		}
		return toReturn;
	}

	@Test
	public void savesGeneratedContentInNewFileInTemplateTargetDir() throws MOGLiPluginException {
		// prepare test
		VelocityLineInserterResultData resultData = buildVelocityLineInserterResultData("Content",
				PROJECT_ROOT_DIR + TEST_SUBDIR + "/temp",
				TARGET_FILE_TXT, KnownGeneratorPropertyNames.CreateNew.name(), "true");
		velocityEngineProvider.setVelocityGeneratorResultData(resultData);
		assertFileExists(targetFile);

		// call functionality under test
		velocityModelBasedLineInserter.doYourJob();

		// verify test result
		assertFileExists(targetFile);
		assertFileContainsEntry(targetFile, "Content");
	}

	private File prepareTargetFile(final File targetDir, final String filename) {
		final File targetFile = new File(targetDir, filename);
		targetFile.delete();
		assertFileDoesNotExist(targetFile);
		return targetFile;
	}

	@Test
	public void throwsExceptionIfTargetDirFromTemplateFileIsNotFound() throws MOGLiPluginException {
		// prepare test
		VelocityLineInserterResultData resultData = buildVelocityLineInserterResultData("ContentToInsert",
				PROJECT_ROOT_DIR + TEST_SUBDIR + "/FOO",
				TARGET_FILE_TXT, KnownInserterPropertyNames.InsertAbove.name(), "-InsertAbove");
		velocityEngineProvider.setVelocityGeneratorResultData(resultData);
		final File targetFile = prepareTargetFile(applicationTempDir, TARGET_FILE_TXT);
		assertFileDoesNotExist(targetFile);

		// call functionality under test
		try {
			velocityModelBasedLineInserter.doYourJob();
		} catch (MOGLiPluginException e) {
			assertStringContains(e.getMessage(), VelocityGeneratorResultData.TEXT_TARGET_DIR_NOT_FOUND);

			// cleanup
			FileUtil.deleteDirWithContent(generatorPluginInputDir);

			return;
		}

		fail("Expected exception not thrown!");
	}

	@Test
	public void throwsExceptionIfTargetFileFromTemplateFileIsNotFound() throws MOGLiPluginException {
		// prepare test
		VelocityLineInserterResultData resultData = buildVelocityLineInserterResultData("ContentToInsert",
				PROJECT_ROOT_DIR + TEST_SUBDIR + "/temp",
				TARGET_FILE_TXT, KnownInserterPropertyNames.InsertAbove.name(), "-InsertAbove");
		velocityEngineProvider.setVelocityGeneratorResultData(resultData);
		targetFile.delete();
		assertFileDoesNotExist(targetFile);

		// call functionality under test
		try {
			velocityModelBasedLineInserter.doYourJob();
		} catch (MOGLiPluginException e) {
			assertStringContains(e.getMessage(), VelocityGeneratorResultData.TEXT_TARGET_FILE_NOT_FOUND);

			// cleanup
			FileUtil.deleteDirWithContent(generatorPluginInputDir);

			return;
		}

		fail("Expected exception not thrown!");
	}

	@Test
	public void insertsGeneratedContentAboveMarkerInTemplateTargetDir() throws MOGLiPluginException {
		// prepare test
		velocityModelBasedLineInserter = new VelocityModelBasedLineInserterStarter();
		velocityModelBasedLineInserter.setInfrastructure(createInfrastructure(new File(getProjectTestResourcesDir(),
				"applicationTestInputDir")));
		final VelocityLineInserterResultData resultData = buildVelocityLineInserterResultData("ContentToInsert",
				PROJECT_ROOT_DIR + TEST_SUBDIR + "/temp",
				TARGET_FILE_TXT, KnownInserterPropertyNames.InsertAbove.name(), "-InsertAbove");
		velocityEngineProvider.setVelocityGeneratorResultData(resultData);
		assertFileExists(targetFile);

		// call functionality under test
		velocityModelBasedLineInserter.doYourJob();

		// verify test result
		final File expectedFile = new File(getProjectTestResourcesDir(), "expectedInsertAboveResultContent.txt");
		assertFileEquals(expectedFile, targetFile);
	}

	@Test
	public void insertsGeneratedContentBelowMarkerInTemplateTargetDir() throws MOGLiPluginException {
		// prepare test
		velocityModelBasedLineInserter = new VelocityModelBasedLineInserterStarter();
		velocityModelBasedLineInserter.setInfrastructure(createInfrastructure(new File(getProjectTestResourcesDir(), "applicationTestInputDir")));
		VelocityLineInserterResultData resultData = buildVelocityLineInserterResultData("ContentToInsert",
				PROJECT_ROOT_DIR + TEST_SUBDIR + "/temp",
				TARGET_FILE_TXT, KnownInserterPropertyNames.InsertBelow.name(), "-InsertBelow");
		velocityEngineProvider.setVelocityGeneratorResultData(resultData);
		assertFileExists(targetFile);

		// call functionality under test
		velocityModelBasedLineInserter.doYourJob();

		// verify test result
		final File expectedFile = new File(getProjectTestResourcesDir(), "expectedInsertBelowResultContent.txt");
		assertFileEquals(expectedFile, targetFile);
	}

	@Test
	public void replacesGeneratedContentInTemplateTargetFile() throws MOGLiPluginException {
		// prepare test
		VelocityLineInserterResultData resultData = buildVelocityLineInserterResultData("ContentToInsert",
				PROJECT_ROOT_DIR + TEST_SUBDIR + "/temp",
				TARGET_FILE_TXT, KnownInserterPropertyNames.ReplaceStart.name(), "-ReplaceStart");
		resultData.addProperty(KnownInserterPropertyNames.ReplaceEnd.name(), "-ReplaceEnd");
		velocityEngineProvider.setVelocityGeneratorResultData(resultData);
		assertFileExists(targetFile);

		// call functionality under test
		velocityModelBasedLineInserter.doYourJob();

		// verify test result
		final File expectedFile = new File(getProjectTestResourcesDir(), "expectedReplaceResultContent.txt");
		assertFileEquals(expectedFile, targetFile);
	}

	@Test
	public void throwsExceptionIfReplaceStartIndicatorNotFound() throws MOGLiPluginException {
		// prepare test
		VelocityLineInserterResultData resultData = buildVelocityLineInserterResultData("ContentToInsert",
				PROJECT_ROOT_DIR + TEST_SUBDIR + "/temp",
				TARGET_FILE_TXT, KnownInserterPropertyNames.ReplaceStart.name(), "-replStart");
		resultData.addProperty(KnownInserterPropertyNames.ReplaceEnd.name(), "-ReplaceEnd");
		velocityEngineProvider.setVelocityGeneratorResultData(resultData);
		assertFileExists(targetFile);

		// call functionality under test
		try {
			velocityModelBasedLineInserter.doYourJob();
		} catch (MOGLiPluginException e) {
			assertStringContains(e.getMessage(), TextConstants.TEXT_START_REPLACE_INDICATOR_NOT_FOUND);
			return;
		}
		fail("Expected exception not thrown!");
	}

	@Test
	public void throwsExceptionIfReplaceEndIndicatorNotFound() throws MOGLiPluginException {
		// prepare test
		VelocityLineInserterResultData resultData = buildVelocityLineInserterResultData("ContentToInsert",
				PROJECT_ROOT_DIR + TEST_SUBDIR + "/temp",
				TARGET_FILE_TXT, KnownInserterPropertyNames.ReplaceStart.name(), "-ReplaceStart");
		resultData.addProperty(KnownInserterPropertyNames.ReplaceEnd.name(), "-replEnd");
		velocityEngineProvider.setVelocityGeneratorResultData(resultData);
		assertFileExists(targetFile);

		// call functionality under test
		try {
			velocityModelBasedLineInserter.doYourJob();
		} catch (MOGLiPluginException e) {
			assertStringContains(e.getMessage(), TextConstants.TEXT_END_REPLACE_INDICATOR_NOT_FOUND);
			return;
		}
		fail("Expected exception not thrown!");
	}

	@Test
	public void throwsExceptionIfInsertBelowIndicatorNotFound() throws MOGLiPluginException {
		// prepare test
		VelocityLineInserterResultData resultData = buildVelocityLineInserterResultData("ContentToInsert",
				PROJECT_ROOT_DIR + TEST_SUBDIR + "/temp",
				TARGET_FILE_TXT, KnownInserterPropertyNames.InsertBelow.name(), "-BI");
		velocityEngineProvider.setVelocityGeneratorResultData(resultData);
		assertFileExists(targetFile);

		// call functionality under test
		try {
			velocityModelBasedLineInserter.doYourJob();
		} catch (MOGLiPluginException e) {
			assertStringContains(e.getMessage(), TextConstants.TEXT_INSERT_BELOW_INDICATOR_NOT_FOUND);
			return;
		}
		fail("Expected exception not thrown!");
	}


	@Test
	public void throwsExceptionIfInsertAboveIndicatorNotFound() throws MOGLiPluginException {
		// prepare test
		VelocityLineInserterResultData resultData = buildVelocityLineInserterResultData("ContentToInsert",
				PROJECT_ROOT_DIR + TEST_SUBDIR + "/temp",
				TARGET_FILE_TXT, KnownInserterPropertyNames.InsertAbove.name(), "-AI");
		velocityEngineProvider.setVelocityGeneratorResultData(resultData);
		assertFileExists(targetFile);

		// call functionality under test
		try {
			velocityModelBasedLineInserter.doYourJob();
		} catch (MOGLiPluginException e) {
			assertStringContains(e.getMessage(), TextConstants.TEXT_INSERT_ABOVE_INDICATOR_NOT_FOUND);
			return;
		}
		fail("Expected exception not thrown!");
	}


	@Test
	public void throwsExceptionIfOutputFileNameIsMissingInTemplateFile() throws MOGLiPluginException {
		// prepare test
		VelocityLineInserterResultData resultData = buildVelocityLineInserterResultData("ContentToInsert",
				PROJECT_ROOT_DIR + TEST_SUBDIR + "/temp",
				null, null, null);
		velocityEngineProvider.setVelocityGeneratorResultData(resultData);
		assertFileExists(targetFile);

		// call functionality under test
		try {
			velocityModelBasedLineInserter.doYourJob();
		} catch (MOGLiPluginException e) {
			assertStringContains(e.getMessage(), VelocityGeneratorResultData.NO_TARGET_FILE_NAME);
			return;
		}
		fail("Expected exception not thrown!");
	}

	@Test
	public void throwsExceptionIfOutputDirIsMissingInTemplateFile() throws MOGLiPluginException {
		// prepare test
		VelocityLineInserterResultData resultData = buildVelocityLineInserterResultData("ContentToInsert",
				null, TARGET_FILE_TXT, null, null);
		velocityEngineProvider.setVelocityGeneratorResultData(resultData);
		assertFileExists(targetFile);

		// call functionality under test
		try {
			velocityModelBasedLineInserter.doYourJob();
		} catch (MOGLiPluginException e) {
			assertStringContains(e.getMessage(), VelocityGeneratorResultData.NO_TARGET_DIR);
			return;
		}
		fail("Expected exception not thrown!");
	}

	@Test
	public void createsNotExistingTargetDirWithCreateNewInstructions() throws MOGLiPluginException {
		// prepare test
		final String targetDir = PROJECT_ROOT_DIR + TEST_SUBDIR + "/example";
		VelocityLineInserterResultData resultData = buildVelocityLineInserterResultData("ContentToInsert",
				targetDir, TARGET_FILE_TXT,
				KnownGeneratorPropertyNames.CreateNew.name(), "true");
		resultData.addProperty(KnownGeneratorPropertyNames.CreateNew.name(), "true");
		velocityEngineProvider.setVelocityGeneratorResultData(resultData);
		final File dir = new File(targetDir);
		FileUtil.deleteDirWithContent(dir);
		assertFileDoesNotExist(dir);

		// call functionality under test
		velocityModelBasedLineInserter.doYourJob();

		// verify test result
		assertFileExists(dir);
	}

	@Test
	public void createsNotExistingTargetFileDirWithoutCreateNewInstruction() throws MOGLiPluginException {
		// prepare test
		final String targetDir = PROJECT_ROOT_DIR + TEST_SUBDIR + "/example";
		VelocityLineInserterResultData resultData = buildVelocityLineInserterResultData("ContentToInsert",
				targetDir, TARGET_FILE_TXT, null, null);
		velocityEngineProvider.setVelocityGeneratorResultData(resultData);
		final File targetDirAsFile = new File(targetDir);
		targetDirAsFile.mkdirs();
		final File targetFile = new File(targetDir, TARGET_FILE_TXT);
		targetFile.delete();
		assertFileDoesNotExist(targetFile);

		// call functionality under test
		velocityModelBasedLineInserter.doYourJob();

		// verify test result
		assertFileExists(targetFile);
	}

	@Test
	public void createsExistingTargetFileNewlyWithCreateNewInstructions() throws Exception {
		// prepare test
		final String targetDir = PROJECT_ROOT_DIR + TEST_SUBDIR + "/example";
		final String content = "ContentToInsert";
		VelocityLineInserterResultData resultData = buildVelocityLineInserterResultData(content,
				targetDir, TARGET_FILE_TXT,
				KnownGeneratorPropertyNames.CreateNew.name(), "true");
		resultData.addProperty(KnownGeneratorPropertyNames.CreateNew.name(), "true");
		velocityEngineProvider.setVelocityGeneratorResultData(resultData);
		targetFile = new File(targetDir, TARGET_FILE_TXT);
		FileUtil.createNewFileWithContent(targetFile, "Test");
		assertFileDoesNotContainEntry(targetFile, "ContentToInsert");

		// call functionality under test
		velocityModelBasedLineInserter.doYourJob();

		// verify test result
		assertFileContainsEntry(targetFile, "ContentToInsert");
	}

	@Test
	public void createsTargetFileWithRootDirDefinedInTemplateFile() throws MOGLiPluginException {
		// prepare test
		final String targetDir = VelocityGeneratorResultData.ROOT_IDENTIFIER + "/example";
		VelocityLineInserterResultData resultData = buildVelocityLineInserterResultData("ContentToInsert",
				targetDir, TARGET_FILE_TXT,
				KnownGeneratorPropertyNames.CreateNew.name(), "true");
		velocityEngineProvider.setVelocityGeneratorResultData(resultData);
		targetFile = new File(targetDir, TARGET_FILE_TXT);
		targetFile.delete();
		assertFileDoesNotExist(targetFile);

		// call functionality under test
		velocityModelBasedLineInserter.doYourJob();

		// verify test result
		final File file = new File(PROJECT_ROOT_DIR + TEST_SUBDIR + "/example", TARGET_FILE_TXT);
		assertFileExists(file);
	}

	@Test
	public void doesNotOverwriteExistingTargetFile() throws Exception {
		// prepare test
		final String targetDir = PROJECT_ROOT_DIR + TEST_SUBDIR + "/example";
		VelocityLineInserterResultData resultData = buildVelocityLineInserterResultData("ContentToInsert",
				targetDir, TARGET_FILE_TXT,
				KnownGeneratorPropertyNames.CreateNew.name(), "false");
		velocityEngineProvider.setVelocityGeneratorResultData(resultData);
		final File targetFile = new File(targetDir, TARGET_FILE_TXT);
		FileUtil.createNewFileWithContent(targetFile, "Test");
		assertFileContainsEntry(targetFile, "Test");

		// call functionality under test
		velocityModelBasedLineInserter.doYourJob();

		// verify test result
		assertFileContainsEntry(targetFile, "Test");
	}

	@Test
	public void throwsExceptionIfSubdirContainsNoMainTemplate() {
		// prepare test
		final File subdir = new File(infrastructure.getPluginInputDir(), ".svn");
		subdir.mkdirs();
		assertFileExists(subdir);

		// call functionality under test
		try {
			velocityModelBasedLineInserter.doYourJob();
		} catch (MOGLiPluginException e) {
			assertStringContains(e.getMessage(), TemplateUtil.NO_MAIN_TEMPLATE_FOUND);
			return;
		}
		fail("Expected exception not thrown!");
	}


	@Test
	public void throwsExceptionIfSubdirContainsNoMainTemplateAndIgnorePropertyIsWrong() {
		// prepare test
		final File subdir = new File(infrastructure.getPluginInputDir(), ".svn");
		subdir.mkdirs();
		assertFileExists(subdir);
		MOGLiFileUtil.createNewFileWithContent(generatorPropertiesFile, ".svn=doNOT" + ArtefactListUtil.IGNORE);

		// call functionality under test
		try {
			velocityModelBasedLineInserter.doYourJob();
		} catch (MOGLiPluginException e) {
			assertStringContains(e.getMessage(), TemplateUtil.NO_MAIN_TEMPLATE_FOUND);
			return;
		}
		fail("Expected exception not thrown!");
	}

	@Test
	public void ignoresSubdirAsArtefactIfDefinedInPluginPropertiesFile() throws MOGLiPluginException {
		// prepare test
		final File subdir = new File(infrastructure.getPluginInputDir(), ".svn");
		subdir.mkdirs();
		assertFileExists(subdir);
		MOGLiFileUtil.createNewFileWithContent(generatorPropertiesFile, ".svn=" + ArtefactListUtil.IGNORE);
		final VelocityLineInserterResultData resultData = buildVelocityLineInserterResultData("ContentToInsert",
				PROJECT_ROOT_DIR + TEST_SUBDIR + "/temp",
				TARGET_FILE_TXT, KnownInserterPropertyNames.InsertAbove.name(), "-InsertAbove");
		velocityEngineProvider.setVelocityGeneratorResultData(resultData);

		// call functionality under test
		velocityModelBasedLineInserter.doYourJob();

		// cleanup
		subdir.delete();
		assertFileDoesNotExist(subdir);

		// verify test result
		final File outputDir = new File(infrastructure.getPluginOutputDir(), ".svn");
		assertFileDoesNotExist(outputDir);
	}


	@Test
	public void returnsMetaInfoValidatorList() throws MOGLiPluginException {
		// prepare test
		final File conditionFile = new File(velocityModelBasedLineInserter.getInfrastructure().getPluginInputDir(), "condition.txt");
		MOGLiFileUtil.createNewFileWithContent(conditionFile, "|if MetaInfo| The \"other\" MetaInfo Name1 |exists.|"
													          + FileUtil.getSystemLineSeparator() +
													          "|if MetaInfo| The \"other\" MetaInfo Name2 |does not exist.|"
													          + FileUtil.getSystemLineSeparator() +
													          "|if MetaInfo| The \"other\" MetaInfo Name3 |with value| my value |does not exist.|" );

        final File validatorFile = new File(velocityModelBasedLineInserter.getInfrastructure().getPluginInputDir(), MetaInfoValidationUtil.FILENAME_VALIDATION);
		MOGLiFileUtil.createNewFileWithContent(validatorFile, "|MetaInfo| MetaInfoTestName1 |is| optional |for| attributes |in| ModelName |.|"
				                                             + FileUtil.getSystemLineSeparator() +
				                                             "|MetaInfo| MetaInfoTestName2 |is| mandatory |for| attributes |in| ModelName |.|"
				                                             + FileUtil.getSystemLineSeparator() +
				                                             "|MetaInfo| MetaInfoTestName3 |is valid to occur| 0-4 |time(s) for| attributes |in| ModelName |.|"
				                                             + FileUtil.getSystemLineSeparator() +
				                                             "|MetaInfo| MetaInfoTestName4 |is valid to occur| 1-2 |time(s) for| attributes |in| ModelName |if| condition.txt |is true.|" );

		// call functionality under test
		final List<MetaInfoValidator> metaInfoValidatorList = velocityModelBasedLineInserter.getMetaInfoValidatorList();

		// verify test result
		assertEquals("validator number", 4, metaInfoValidatorList.size());
		assertEquals("condition number", 3, ((ConditionalMetaInfoValidator)metaInfoValidatorList.get(3)).getTotalNumberOfConditions());
	}

	private void createFakeArtefactDir() throws Exception {
		final File testArtefactDir = new File(infrastructure.getPluginInputDir(), "/TestArtefact");
		testArtefactDir.mkdirs();
		final File testTemplate = new File(testArtefactDir, "Main.tpl");
		FileUtil.createNewFileWithContent(testTemplate, "dummy content");
	}

	@Test
	public void createsReportForNewlyCreatedArtefact() throws Exception {
		// prepare test
		FileUtil.deleteDirWithContent(generatorPluginInputDir);
		createFakeArtefactDir();
		final String targetDir = VelocityGeneratorResultData.ROOT_IDENTIFIER + "/example";
		final VelocityLineInserterResultData resultData = buildVelocityLineInserterResultData("Content",
				targetDir, TARGET_FILE_TXT,
				KnownGeneratorPropertyNames.CreateNew.name(), "true");
		velocityEngineProvider.setVelocityGeneratorResultData(resultData);  // fake result for the mocked engine plugin

		// call functionality under test
		velocityModelBasedLineInserter.doYourJob();

		// clean up
		FileUtil.deleteDirWithContent(generatorPluginInputDir);  // forces setup to rebuild it

		// verify test result
		final String report = velocityModelBasedLineInserter.getGeneratorReport();
		final File expected = new File(getProjectTestResourcesDir(), "expectedReportCreateNew.txt");
		assertEquals("report", FileUtil.getFileContent(expected), report);
	}

	@Test
	public void createsReportForNotOverwrittenArtefact() throws Exception {
		// prepare test
		FileUtil.deleteDirWithContent(generatorPluginInputDir);
		createFakeArtefactDir();
		final String targetDir = VelocityGeneratorResultData.ROOT_IDENTIFIER + "/example";
		new File(targetDir).mkdirs();
		final File file = new File(applicationRootDir + "/example", TARGET_FILE_TXT);
		FileUtil.createNewFileWithContent(file, "content");
		final VelocityLineInserterResultData resultData = buildVelocityLineInserterResultData("ContentToInsert",
				targetDir, TARGET_FILE_TXT,
				KnownGeneratorPropertyNames.CreateNew.name(), "false");
		velocityEngineProvider.setVelocityGeneratorResultData(resultData);  // fake result for the mocked engine plugin

		// call functionality under test
		velocityModelBasedLineInserter.doYourJob();

		// clean up
		FileUtil.deleteDirWithContent(generatorPluginInputDir);  // forces setup to rebuild it

		// verify test result
		final String report = velocityModelBasedLineInserter.getGeneratorReport();
		final File expected = new File(getProjectTestResourcesDir(), "expectedReportNotOverwritten.txt");
		assertEquals("report", FileUtil.getFileContent(expected), report);
	}

	@Test
	public void createsReportForInsertedAboveContent() throws Exception {
		// prepare test
		FileUtil.deleteDirWithContent(generatorPluginInputDir);
		createFakeArtefactDir();
		final String targetDir = VelocityGeneratorResultData.ROOT_IDENTIFIER + "/temp";
		final VelocityLineInserterResultData resultData = buildVelocityLineInserterResultData("contentToInsert",
				targetDir, TARGET_FILE_TXT,
				KnownInserterPropertyNames.InsertAbove.name(), "-InsertAbove");
		velocityEngineProvider.setVelocityGeneratorResultData(resultData);  // fake result for the mocked engine plugin

		// call functionality under test
		velocityModelBasedLineInserter.doYourJob();

		// clean up
		FileUtil.deleteDirWithContent(generatorPluginInputDir);  // forces setup to rebuild it

		// verify test result
		final String report = velocityModelBasedLineInserter.getGeneratorReport();
		final File expected = new File(getProjectTestResourcesDir(), "expectedReportAboveInsertion.txt");
		assertEquals("report", FileUtil.getFileContent(expected), report);
	}

	@Test
	public void createsReportForInsertedBelowContent() throws Exception {
		// prepare test
		FileUtil.deleteDirWithContent(generatorPluginInputDir);
		createFakeArtefactDir();
		final String targetDir = VelocityGeneratorResultData.ROOT_IDENTIFIER + "/temp";
		final VelocityLineInserterResultData resultData = buildVelocityLineInserterResultData("contentToInsert",
				targetDir, TARGET_FILE_TXT,
				KnownInserterPropertyNames.InsertBelow.name(), "-InsertBelow");
		velocityEngineProvider.setVelocityGeneratorResultData(resultData);  // fake result for the mocked engine plugin

		// call functionality under test
		velocityModelBasedLineInserter.doYourJob();

		// clean up
		FileUtil.deleteDirWithContent(generatorPluginInputDir);  // forces setup to rebuild it

		// verify test result
		final String report = velocityModelBasedLineInserter.getGeneratorReport();
		final File expected = new File(getProjectTestResourcesDir(), "expectedReportBelowInsertion.txt");
		assertEquals("report", FileUtil.getFileContent(expected), report);
	}

	@Test
	public void createsReportForReplacedContent() throws Exception {
		// prepare test
		FileUtil.deleteDirWithContent(generatorPluginInputDir);
		createFakeArtefactDir();
		final String targetDir = VelocityGeneratorResultData.ROOT_IDENTIFIER + "/temp";
		final VelocityLineInserterResultData resultData = buildVelocityLineInserterResultData("contentToInsert",
				targetDir, TARGET_FILE_TXT,
				KnownInserterPropertyNames.ReplaceStart.name(), "-ReplaceStart");
		resultData.addProperty(KnownInserterPropertyNames.ReplaceEnd.name(), "-ReplaceEnd");
		velocityEngineProvider.setVelocityGeneratorResultData(resultData);  // fake result for the mocked engine plugin

		// call functionality under test
		velocityModelBasedLineInserter.doYourJob();

		// clean up
		FileUtil.deleteDirWithContent(generatorPluginInputDir);  // forces setup to rebuild it

		// verify test result
		final String report = velocityModelBasedLineInserter.getGeneratorReport();
		final File expected = new File(getProjectTestResourcesDir(), "expectedReportReplaceInsertion.txt");
		assertEquals("report", FileUtil.getFileContent(expected), report);
	}

}
