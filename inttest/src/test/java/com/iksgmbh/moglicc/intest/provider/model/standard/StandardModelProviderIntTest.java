package com.iksgmbh.moglicc.intest.provider.model.standard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

import com.iksgmbh.moglicc.core.InfrastructureService;
import com.iksgmbh.moglicc.exceptions.MOGLiPluginException;
import com.iksgmbh.moglicc.intest.IntTestParent;
import com.iksgmbh.moglicc.provider.model.standard.StandardModelProviderStarter;
import com.iksgmbh.moglicc.provider.model.standard.metainfo.MetaInfoValidationUtil;
import com.iksgmbh.moglicc.provider.model.standard.metainfo.MetaInfoValidator;
import com.iksgmbh.moglicc.utils.MOGLiFileUtil;
import com.iksgmbh.utils.FileUtil;

public class StandardModelProviderIntTest extends IntTestParent {

	@Test
	public void createsStatisticsFile() throws MOGLiPluginException {
		// call functionality under test
		standardModelProviderStarter.doYourJob();

		// verify test result
		final InfrastructureService infrastructure = standardModelProviderStarter.getMOGLiInfrastructure();
		final File file = new File(infrastructure.getPluginOutputDir(), StandardModelProviderStarter.FILENAME_STATISTICS_FILE);
		assertFileExists(file);
		final File expectedFile = getTestFile("ExpectedModelStatistics.txt");
		assertFileEquals(expectedFile, file);
	}

	@Test
	public void filtersMetaInfoValidatorVendorsByNameOfModel() throws MOGLiPluginException {
		// prepare test
		setMetaInfoValidationFile(velocityClassBasedGeneratorStarter, "MetaInfoValidatorsForDifferentModels.txt");
		setMetaInfoValidationFile(velocityModelBasedInserterStarter, "MetaInfoValidatorsForDifferentModels.txt");
		standardModelProviderStarter.doYourJob();

		// call functionality under test
		final List<MetaInfoValidator> allMetaInfoValidators = standardModelProviderStarter.getAllMetaInfoValidators();

		// verify test result
		assertEquals("Number of MetaInfoValidators", 4, allMetaInfoValidators.size());
		assertEquals("Number of vendors", 2, countVendors(allMetaInfoValidators));
	}

	private int countVendors(final List<MetaInfoValidator> allMetaInfoValidators) {
		final HashSet<String> counter = new HashSet<String>();
		for (final MetaInfoValidator metaInfoValidator : allMetaInfoValidators) {
			counter.add(metaInfoValidator.getVendorPluginId());
		}
		return counter.size();
	}

	@Test
	public void createsStatisticsFileWithMetaInfoNamesThatContainSpaces() throws MOGLiPluginException {
		// prepare test
		setModelFile("ModelFileWithMetaInfosContainingSpacesInNames.txt");
		setMetaInfoValidationFile(velocityClassBasedGeneratorStarter, "MetaInfoValidatoresContainingSpacesInNames.txt");

		// call functionality under test
		standardModelProviderStarter.doYourJob();

		// verify test result
		final InfrastructureService infrastructure = standardModelProviderStarter.getMOGLiInfrastructure();
		final File file = new File(infrastructure.getPluginOutputDir(), StandardModelProviderStarter.FILENAME_STATISTICS_FILE);
		assertFileExists(file);
		final File expectedFile = new File(getProjectTestResourcesDir(),
				                           "ExpectedStatisticsFileWithMetaInfosContainingSpacesInNames.txt");
		assertFileEquals(expectedFile, file);
	}

	@Test
	public void readsModelFileWithCustomizedBraceSymbolForMetaInfoValues() throws MOGLiPluginException {
		// prepare test
		setModelFile("ModelFileWithMetaInfosContainingDoubleQoutesInNames.txt");
		final File modelPropertiesFile = new File(standardModelProviderStarter.getMOGLiInfrastructure().getPluginInputDir(),
				                                  StandardModelProviderStarter.PLUGIN_PROPERTIES_FILE);
		MOGLiFileUtil.createNewFileWithContent(modelPropertiesFile, "BraceSymbolForMetaInfoValues=*");


		// call functionality under test
		standardModelProviderStarter.doYourJob();

		// verify test result
		final InfrastructureService infrastructure = standardModelProviderStarter.getMOGLiInfrastructure();
		final File file = new File(infrastructure.getPluginOutputDir(), StandardModelProviderStarter.FILENAME_STATISTICS_FILE);
		assertFileExists(file);
		final File expectedFile = new File(getProjectTestResourcesDir(),
				                           "ExpectedStatisticsFileWithMetaInfosContainingDoubleQoutesInNames.txt");
		assertFileEquals(expectedFile, file);
	}

	@Test
	public void validatesModelByTheDefaultMetaInfoValidators() throws Exception {
		// call functionality under test
		standardModelProviderStarter.doYourJob();

		// verify test result
		final File statisticsFile = new File(standardModelProviderStarter.getMOGLiInfrastructure().getPluginOutputDir(),
				                                StandardModelProviderStarter.FILENAME_STATISTICS_FILE);
		assertFileExists(statisticsFile);
		assertFileDoesNotContainEntry(statisticsFile, "WARNING:");
	}

	@Test
	public void validatesTestModelByRule_MetaInfo_Nullable_must_exist_if_MetaInfo_MinOccurs_does_not_exist() throws Exception {
		// prepare test
		File modelFile = new File(standardModelProviderStarter.getMOGLiInfrastructure().getPluginInputDir(), StandardModelProviderStarter.FILENAME_STANDARD_MODEL_TEXTFILE);
		MOGLiFileUtil.createNewFileWithContent(modelFile, "model TestModel"
				                                          + FileUtil.getSystemLineSeparator() +
				                                          "class de.Testklasse"
												          + FileUtil.getSystemLineSeparator() +
														  "MetaInfo MinOccurs 0");

		final File conditionFile = new File(velocityClassBasedGeneratorStarter.getMOGLiInfrastructure().getPluginInputDir(), "condition.txt");
		MOGLiFileUtil.createNewFileWithContent(conditionFile, "|if MetaInfo| MinOccurs |does not exist.|" );   // false because MinOccurs does exist

        final File validatorFile = new File(velocityClassBasedGeneratorStarter.getMOGLiInfrastructure().getPluginInputDir(), MetaInfoValidationUtil.FILENAME_VALIDATION);
		MOGLiFileUtil.createNewFileWithContent(validatorFile, "|MetaInfo| Nullable |must occur| 1 |time(s) for| " +
				                                              "classes |in| TestModel |if| condition.txt |is true.|" );  // should fail - Nullable does not exist

		// TEST 1: validation ok - condition is NOT met -> validation of occurrence for Nullable is not performed
		standardModelProviderStarter.doYourJob();

		// verify test result: no exception

		// TEST 2: validation fails due to invalid absence of MetaInfo "Nullable"
		MOGLiFileUtil.createNewFileWithContent(modelFile, "model TestModel"
                                                          + FileUtil.getSystemLineSeparator() +
                                                          "class de.Testklasse");

		try {
			// call functionality under test
			standardModelProviderStarter.doYourJob();
			fail("Expected exception was not thrown!");
		} catch (Exception e) {
			// verify test result
			assertEquals("Error message", "Error validating MetaInfos. For more information see StandardModelProvider.log", e.getMessage());
		}


		// TEST 3: validation ok - condition is met and occurence for Nullable is successful validated
		MOGLiFileUtil.createNewFileWithContent(modelFile, "model TestModel"
				                                          + FileUtil.getSystemLineSeparator() +
				                                          "class de.Testklasse "
												          + FileUtil.getSystemLineSeparator() +
														  "MetaInfo Nullable true ");

		// call functionality under test
		standardModelProviderStarter.doYourJob();

		// verify test result: no exception
	}

	@Test
	public void validatesTestModelByRule_MetaInfo_Nullable_must_exist_if_MetaInfos_DBType_and_DBLength_exist() throws Exception {
		// prepare test
		File modelFile = new File(standardModelProviderStarter.getMOGLiInfrastructure().getPluginInputDir(), StandardModelProviderStarter.FILENAME_STANDARD_MODEL_TEXTFILE);
		MOGLiFileUtil.createNewFileWithContent(modelFile, "model TestModel"
				                                          + FileUtil.getSystemLineSeparator() +
				                                          "class de.Testklasse"
												          + FileUtil.getSystemLineSeparator() +
												          "MetaInfo DBLength 5");

		final File conditionFile = new File(velocityClassBasedGeneratorStarter.getMOGLiInfrastructure().getPluginInputDir(), "condition.txt");
		MOGLiFileUtil.createNewFileWithContent(conditionFile, "|if MetaInfo| DBType |exists.|"
		                                                      + FileUtil.getSystemLineSeparator() +
                                                              "|if MetaInfo| DBLength |exists.|");  // conditions fail - first condition not met

        final File validatorFile = new File(velocityClassBasedGeneratorStarter.getMOGLiInfrastructure().getPluginInputDir(), MetaInfoValidationUtil.FILENAME_VALIDATION);
		MOGLiFileUtil.createNewFileWithContent(validatorFile, "|MetaInfo| Nullable |must occur| 1 |time(s) for| " +
				                                              "classes |in| TestModel |if| condition.txt |is true.|" );  // should fail - Nullable does not exist

		// TEST 1: validation ok - condition is NOT met -> validation of occurrence for Nullable is not performed
		standardModelProviderStarter.doYourJob();  // does not fail because conditions fails and occurence is not validated

		// verify test result: no exception

		// TEST 2: validation fails because Nullable is missing
		MOGLiFileUtil.createNewFileWithContent(modelFile, "model TestModel"
										                  + FileUtil.getSystemLineSeparator() +
										                  "class de.Testklasse "
												          + FileUtil.getSystemLineSeparator() +
												          "MetaInfo DBLength 5 "
												          + FileUtil.getSystemLineSeparator() +
												          "MetaInfo DBType NUMBER");

		try {
			// call functionality under test
			standardModelProviderStarter.doYourJob();
			fail("Expected exception was not thrown!");
		} catch (Exception e) {
			// verify test result
			assertEquals("Error message", "Error validating MetaInfos. For more information see StandardModelProvider.log", e.getMessage());
		}

		// TEST 3: validation successful because occurrence is validated successful
		MOGLiFileUtil.createNewFileWithContent(modelFile, "model TestModel"
										                  + FileUtil.getSystemLineSeparator() +
										                  "class de.Testklasse "
												          + FileUtil.getSystemLineSeparator() +
												          "MetaInfo Nullable true "
												          + FileUtil.getSystemLineSeparator() +
												          "MetaInfo DBType NUMBER "
												          + FileUtil.getSystemLineSeparator() +
												          "MetaInfo DBLength 5 ");

		// call functionality under test
		standardModelProviderStarter.doYourJob();

		// verify test result: no exception
	}

	@Test
	public void validatesTestModelByRule_MetaInfo_Nullable_must_exist_if_MetaInfos_DBType_OR_DBLength_exist() throws Exception {
		// prepare test
		File modelFile = new File(standardModelProviderStarter.getMOGLiInfrastructure().getPluginInputDir(), StandardModelProviderStarter.FILENAME_STANDARD_MODEL_TEXTFILE);
		MOGLiFileUtil.createNewFileWithContent(modelFile, "model TestModel"
				                                          + FileUtil.getSystemLineSeparator() +
				                                          "class de.Testklasse");

		final File conditionFile = new File(velocityClassBasedGeneratorStarter.getMOGLiInfrastructure().getPluginInputDir(), "condition.txt");
		MOGLiFileUtil.createNewFileWithContent(conditionFile, "|if MetaInfo| DBType |exists.|"
				                                              + FileUtil.getSystemLineSeparator() +
				                                              "OR"
				                                              + FileUtil.getSystemLineSeparator() +
		                                                      "|if MetaInfo| DBLength |exists.|");  // conditions fails - both return false

        final File validatorFile = new File(velocityClassBasedGeneratorStarter.getMOGLiInfrastructure().getPluginInputDir(), MetaInfoValidationUtil.FILENAME_VALIDATION);
		MOGLiFileUtil.createNewFileWithContent(validatorFile, "|MetaInfo| Nullable |must occur| 1 |time(s) for| " +
				                                              "classes |in| TestModel |if| condition.txt |is true.|"); // should fail - Nullable does not exist


		// TEST 1: validation ok - condition is NOT met -> validation of occurrence for Nullable is not performed
		standardModelProviderStarter.doYourJob();

		// verify test result: no exception

		// TEST 2: validation fails because Nullable is missing
		MOGLiFileUtil.createNewFileWithContent(modelFile, "model TestModel"
										                  + FileUtil.getSystemLineSeparator() +
										                  "class de.Testklasse "
												          + FileUtil.getSystemLineSeparator() +
												          "MetaInfo DBType NUMBER");

		// call functionality under test
		try {
			// call functionality under test
			standardModelProviderStarter.doYourJob();
			fail("Expected exception was not thrown!");
		} catch (Exception e) {
			// verify test result
			assertEquals("Error message", "Error validating MetaInfos. For more information see StandardModelProvider.log", e.getMessage());
		}

		// TEST 3: validation ok - condition is met and occurence for Nullable is successful validated
		MOGLiFileUtil.createNewFileWithContent(modelFile, "model TestModel"
										                  + FileUtil.getSystemLineSeparator() +
										                  "class de.Testklasse "
												          + FileUtil.getSystemLineSeparator() +
												          "MetaInfo Nullable true "
												          + FileUtil.getSystemLineSeparator() +
												          "MetaInfo DBLength 5 ");

		// call functionality under test
		standardModelProviderStarter.doYourJob();

		// verify test result: no exception
	}

	@Test
	public void validatesTestModelByRule_MetaInfo_Nullable_must_exist_with_value_true_if_MetaInfos_MinOccurs_has_value_0() throws Exception {
		// prepare test
		File modelFile = new File(standardModelProviderStarter.getMOGLiInfrastructure().getPluginInputDir(), StandardModelProviderStarter.FILENAME_STANDARD_MODEL_TEXTFILE);
		MOGLiFileUtil.createNewFileWithContent(modelFile, "model TestModel"
				                                          + FileUtil.getSystemLineSeparator() +
				                                          "class de.Testklasse "
												          + FileUtil.getSystemLineSeparator() +
												          "MetaInfo Nullable false"
												          + FileUtil.getSystemLineSeparator() +
												          "MetaInfo MinOccurs 1");

		final File conditionFile = new File(velocityClassBasedGeneratorStarter.getMOGLiInfrastructure().getPluginInputDir(), "condition.txt");
		MOGLiFileUtil.createNewFileWithContent(conditionFile, "|if MetaInfo| MinOccurs |with value| 0 |exists.|"); // condition fails due to wrong value of MinOccurs

        final File validatorFile = new File(velocityClassBasedGeneratorStarter.getMOGLiInfrastructure().getPluginInputDir(), MetaInfoValidationUtil.FILENAME_VALIDATION);
		MOGLiFileUtil.createNewFileWithContent(validatorFile, "|MetaInfo| Nullable |with value| true |must occur| 1 |time(s) for| " +
				                                              "classes |in| TestModel |if| condition.txt |is true.|");  // should fails due to wrong value of Nullable

		// TEST 1: validation ok - condition is NOT met -> validation of occurrence and value for Nullable is not performed
		standardModelProviderStarter.doYourJob();

		// verify test result: no exception

		// TEST 2: validation fails because Nullable has wrong value
		MOGLiFileUtil.createNewFileWithContent(modelFile, "model TestModel"
				                                          + FileUtil.getSystemLineSeparator() +
				                                          "class de.Testklasse "
												          + FileUtil.getSystemLineSeparator() +
												          "MetaInfo Nullable false"
												          + FileUtil.getSystemLineSeparator() +
												          "MetaInfo MinOccurs 0");

		// call functionality under test
		try {
			// call functionality under test
			standardModelProviderStarter.doYourJob();
			fail("Expected exception was not thrown!");
		} catch (Exception e) {
			// verify test result
			assertEquals("Error message", "Error validating MetaInfos. For more information see StandardModelProvider.log", e.getMessage());
		}

		// TEST 3: validation ok - condition is met and occurence for Nullable is successful validated
		MOGLiFileUtil.createNewFileWithContent(modelFile, "model TestModel"
										                  + FileUtil.getSystemLineSeparator() +
										                  "class de.Testklasse "
												          + FileUtil.getSystemLineSeparator() +
												          "MetaInfo Nullable true"
												          + FileUtil.getSystemLineSeparator() +
												          "MetaInfo MinOccurs 0");

		// call functionality under test
		standardModelProviderStarter.doYourJob();

		// verify test result: no exception
	}

}
