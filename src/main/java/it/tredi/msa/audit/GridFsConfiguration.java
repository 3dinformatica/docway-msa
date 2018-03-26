package it.tredi.msa.audit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import com.mongodb.MongoClient;

@Configuration
public class GridFsConfiguration extends AbstractMongoConfiguration {
	
	@Value("${spring.data.mongodb.host}")
	private String mongoHost;

	@Value("${spring.data.mongodb.port}")
	private String mongoPort;
	
	@Value("${spring.data.mongodb.database}")
	private String mongoDatabase;

	@Bean
	public GridFsTemplate gridFsTemplate() throws Exception {
		return new GridFsTemplate(mongoDbFactory(), mappingMongoConverter());
	}

	@Override
	public MongoClient mongoClient() {
		return new MongoClient(mongoHost + ":" + mongoPort);
	}

	@Override
	protected String getDatabaseName() {
		return mongoDatabase;
	}

}
