package org.unitils.dbunit.config;

import java.util.Properties;

import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConfig.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * org.unitils.dbunit.config.DbUnitConfig
 * Configure the DbUnit module.
 * @author Kevin(zhaoyk2010@163.com)
 */
public final class DbUnitConfig {
	public static final DbUnitConfig getInstance() {
		if (null == instance) {
			instance = new DbUnitConfig();
		}
		return instance;
	}
	
	/**
	 * Use the properties in the Properties instance unitilsConfig of unitils 
	 * to configure the DatabaseConfig instance dbUnitConfig. If a property in
	 * unitilsConfig already configured in dbUnitConfig, it will be replaced.
	 * If none nullable property doesn't set, it will thow RuntimeException.<br/>
	 * The property key in unitilsConfig maps to the DbUnit configuration like:
	 * dbUnit.properties.statementFactory -> http://www.dbunit.org/properties/statementFactory
	 * dbUnit.features.datatypeWarning -> http://www.dbunit.org/features/datatypeWarning
	 * @author Kevin(zhaoyk2010@163.com)
	 * @param unitilsConfig  The unitils configuration properties.
	 * @param dbUnitConfig  The DbUnit configuration instance.
	 */
	public void setDbUnitConfig(Properties unitilsConfig, DatabaseConfig dbUnitConfig) {
		for (ConfigProperty configItem : DatabaseConfig.ALL_PROPERTIES) {
			String configKey = configItem.getProperty();
			Class<?> valueType = configItem.getPropertyType();
			Boolean isNullable = configItem.isNullable();
			Object value = getConfigValue(unitilsConfig, configKey, valueType);
			if (!isNullable && null == value
					&& null == dbUnitConfig.getProperty(configKey)) {
				logger.error("Required config '{}' missing.", configKey);
				throw new RuntimeException("Required config '" + configKey + "' missing.");
			}
			if (null != value) {
				dbUnitConfig.setProperty(configKey, value);
			}
		}
		logger.debug("DbUnit config details: {}", dbUnitConfig);
	}
	
	/**
	 * Parse the config value in property files.
	 * @author Kevin(zhaoyk2010@163.com)
	 * @param unitilsConfig  The Properties instance.
	 * @param configKey  The DbUnit configuration key.
	 * @param valueType  The value type of the configuration.
	 *                   Supports meta type of Integer, String, String[] and Boolean.
	 *                   Others are parsed as a Class type.
	 * @return  The value object. If the valueType is a meta type, then return the
	 *          parsed value, or else return the class object of a class.
	 */
	private Object getConfigValue(Properties unitilsConfig, String configKey, Class<?> valueType) {
		String unitilsConfKey = getUnitilsConfKey(configKey);
		if (!unitilsConfig.containsKey(unitilsConfKey)) {
			return null;
		}
		String value = unitilsConfig.getProperty(unitilsConfKey, "");
		if (value.isEmpty()) {
			return null;
		}
		if (String.class.equals(valueType)) {
			return value;
		}
		if (String[].class.equals(valueType)) {
			return value.split(",");
		}
		if (Integer.class.equals(valueType)) {
			return Integer.parseInt(value);
		}
		if (Boolean.class.equals(valueType)) {
			return value.equalsIgnoreCase("true");
		}
		try {
			Class<?> clazz = Class.forName(value);
			return clazz;
		} catch (ClassNotFoundException e) {
			logger.error("Load class '{}' failed. Trace:\n", value, e);
		}
		return null;
	}

	private String getUnitilsConfKey(String configKey) {
		return UNITILS_DBUNIT_CONFIG_PREFIX 
				+ configKey.replace(DBUNIT_CONFIG_PREFIX, "").replace("/", ".");
	}

	/**
	 * Private constructor. Single instance.
	 */
	private DbUnitConfig() {}
	
	// Single instance.
	private static DbUnitConfig instance = null;
	private static final Logger logger = LoggerFactory.getLogger(DbUnitConfig.class);
	private static final Class<?>[] metaTypes = new Class<?>[] {
		String.class,
		String[].class,
		Integer.class,
		Integer[].class,
		Boolean.class,
		Boolean[].class,
		Long.class,
		Long[].class,
		Double.class,
		Double[].class
	};
	private static final String DBUNIT_CONFIG_PREFIX = "http://www.dbunit.org/";
	private static final String UNITILS_DBUNIT_CONFIG_PREFIX = "dbunit.";
}
