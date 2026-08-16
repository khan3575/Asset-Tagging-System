package com.sil.asset_tagging_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class AssetTaggingSystemApplication extends SpringBootServletInitializer {
	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return builder.sources(AssetTaggingSystemApplication.class);
	}
	public static void main(String[] args) {
		SpringApplication.run(AssetTaggingSystemApplication.class, args);
	}

}
