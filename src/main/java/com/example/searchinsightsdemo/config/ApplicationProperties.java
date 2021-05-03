package com.example.searchinsightsdemo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;

import lombok.Getter;

/**
 * Properties specific to the insights application.
 *
 * <p>
 * Properties are configured in the application.yml file.
 * </p>
 */
@ConfigurationProperties(prefix = "insights", ignoreUnknownFields = false)
@Component
@Getter
public class ApplicationProperties {

	@NestedConfigurationProperty
	private final StorageConfiguration storageConfiguration = new StorageConfiguration();
}
