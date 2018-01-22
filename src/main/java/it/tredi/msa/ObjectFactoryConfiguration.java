package it.tredi.msa;

import java.util.Map;

public class ObjectFactoryConfiguration {
	
	private String className;
	private Map<String, String> params;
	
	public String getClassName() {
		return className;
	}
	
	public void setClassName(String className) {
		this.className = className;
	}
	
	public Map<String, String> getParams() {
		return params;
	}
	
	public void setParams(Map<String, String> params) {
		this.params = params;
	}
	
}
