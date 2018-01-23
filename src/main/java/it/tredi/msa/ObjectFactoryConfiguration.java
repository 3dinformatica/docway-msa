package it.tredi.msa;

import java.util.HashMap;
import java.util.Map;

public class ObjectFactoryConfiguration {
	
	private String className;
	private Map<String, String> params;
	
	public ObjectFactoryConfiguration(String className, String paramsString) throws Exception {
		this.className = className;
		this.params = new HashMap<>();
		String[] paramsArr = paramsString.split(",");
		for (String param:paramsArr) {
			param = param.trim();
			if (!param.isEmpty()) {
				int index = param.indexOf("=");
				if (index == -1)
					throw new Exception("Configuration Syntax Error: " + paramsString);
				String key = param.substring(0, index);
				String value = param.substring(index + 1);
				this.params.put(key, value);
			}
		}
	}
	
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
