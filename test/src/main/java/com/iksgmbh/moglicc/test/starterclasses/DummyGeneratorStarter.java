package com.iksgmbh.moglicc.test.starterclasses;

import java.util.ArrayList;
import java.util.List;

import com.iksgmbh.moglicc.core.InfrastructureService;
import com.iksgmbh.moglicc.exceptions.MOGLiPluginException;
import com.iksgmbh.moglicc.plugin.MOGLiPlugin;
import com.iksgmbh.moglicc.plugin.subtypes.GeneratorPlugin;
import com.iksgmbh.moglicc.provider.model.standard.metainfo.MetaInfoValidator;
import com.iksgmbh.moglicc.provider.model.standard.metainfo.MetaInfoValidatorVendor;

public class DummyGeneratorStarter implements GeneratorPlugin, MetaInfoValidatorVendor {

	public static final String PLUGIN_ID = "DummyGenerator";
	private InfrastructureService infrastructure;
	private List<MetaInfoValidator> metaInfoValidatorList;

	public String getId() {
		return PLUGIN_ID;
	}

	@Override
	public PluginType getPluginType() {
		return MOGLiPlugin.PluginType.GENERATOR;
	}

	@Override
	public void doYourJob() {
		infrastructure.getPluginLogger().logInfo(PLUGIN_ID);
	}

	@Override
	public void setInfrastructure(InfrastructureService infrastructure) {
		this.infrastructure = infrastructure;
	}

	@Override
	public List<String> getDependencies() {
		List<String> toReturn = new ArrayList<String>();
		toReturn.add("DummyDataProvider");
		toReturn.add("StandardModelProvider");
		toReturn.add("VelocityEngineProvider");
		return toReturn;
	}

	@Override
	public boolean unpackDefaultInputData() throws MOGLiPluginException {
		return false;
	}

	@Override
	public List<MetaInfoValidator> getMetaInfoValidatorList() throws MOGLiPluginException {
		return metaInfoValidatorList;
	}

	public void setMetaInfoValidatorList(final List<MetaInfoValidator> metaInfoValidatorList)  {
		this.metaInfoValidatorList = metaInfoValidatorList;
	}

	@Override
	public InfrastructureService getInfrastructure() {
		return infrastructure;
	}

	@Override
	public boolean unpackPluginHelpFiles() throws MOGLiPluginException {
		return false;
	}

	@Override
	public String getGeneratorReport() {
		return null;
	}

	@Override
	public int getNumberOfGenerations() {
		return 0;
	}

	@Override
	public int getNumberOfGeneratedArtefacts() {
		return 0;
	}

	@Override
	public String getShortReport()
	{
		return "";
	}

	@Override
	public int getSuggestedPositionInExecutionOrder()
	{
		return 0;
	}

}
