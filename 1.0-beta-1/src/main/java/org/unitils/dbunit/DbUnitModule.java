/*
 * Copyright 2006 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.unitils.dbunit;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.dbunit.Assertion;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.dataset.*;
import org.dbunit.dataset.datatype.IDataTypeFactory;
import org.dbunit.dataset.filter.DefaultColumnFilter;
import org.dbunit.operation.DatabaseOperation;
import org.unitils.core.Module;
import org.unitils.core.TestListener;
import org.unitils.core.Unitils;
import org.unitils.core.UnitilsException;
import org.unitils.database.DatabaseModule;
import org.unitils.dbunit.annotation.DataSet;
import org.unitils.dbunit.annotation.ExpectedDataSet;
import org.unitils.dbunit.util.TablePerRowXmlDataSet;
import org.unitils.dbunit.util.DbUnitDatabaseConnection;
import org.unitils.util.ConfigUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.sql.SQLException;

/**
 * Module that provides support for managing database test data using DBUnit.
 * <p/>
 * This module depends on the {@link DatabaseModule} for database connection management and identificatiof database test
 * classes.
 * <p/>
 * This module only provides services to unit test classes that are annotated with an annotation that identifies it as
 * a database test (see {@link DatabaseModule} for more information)
 * <p/>
 * For each database test method, this module will try to find a test dataset file in the classpath and, if found, insert
 * this dataset into the test database. DbUnits <code>DatabaseTask.CLEAN_INSERT</code> is used for inserting this
 * dataset. This means that the tables specified in the dataset are cleared before inserting the test records. As dataset
 * file format, DbUnit's <code>FlatXmlDataSet</code> is used.
 * <p/>
 * For each database test method, the dataset file is found as follows:
 * <ol><li>If the test method is annotated with {@link DataSet}, the file with the name specified by the fileName
 * property of this annotation is used</li>
 * <li>If a file can be found in the classpath in the same package as the testclass, with the name
 * 'classname without packagename'.'test method name'.xml, this file is used</li>
 * <li>If the test class is annotated with {@link DataSet}, the file with the name specified by the fileName
 * property of this annotation is used</li>
 * <li>If a file can be found in the classpath in the same package as the testclass, with the name
 * 'classname without packagename'.xml, this file is used</li>
 * <p/>
 * Using the method {@link #assertDBContentAsExpected(Object,String)}, the contents of the database can be compared with
 * the contents of a dataset. The expected dataset file should be located in the classpath in the same package as the
 * testclass, with the name 'classname without packagename'.'test method name'-result.xml.
 *
 * @author Filip Neven
 * @author Tim Ducheyne
 */
public class DbUnitModule implements Module {

    /* Property key of the name of the database schema */
    private static final String PROPKEY_SCHEMA_NAME = "dataSource.schemaName";

    /* Property key of the SQL dialect of the underlying DBMS implementation */
    private static final String PROPKEY_DATABASE_DIALECT = "database.dialect";

    /**
     * Object that DbUnit uses to connect to the database and to cache some database metadata. Since DBUnit's data
     * caching is time-consuming, this object is created only once and used througout the entire test run. The
     * underlying JDBC Connection however is 'closed' (returned to the pool) after every database operation.
     */
    private DbUnitDatabaseConnection dbUnitDatabaseConnection;

    /* Name of the database schema, needed to configure DBUnit */
    private String schemaName;

    /* Instance of DbUnits IDataTypeFactory, that handles dbms specific data type issues */
    private IDataTypeFactory dataTypeFactory;

    /**
     * Initializes the DbUnitModule using the given Configuration
     *
     * @param configuration the config, not null
     */
    public void init(Configuration configuration) {

        schemaName = configuration.getString(PROPKEY_SCHEMA_NAME).toUpperCase();
        String databaseDialect = configuration.getString(PROPKEY_DATABASE_DIALECT);
        dataTypeFactory = ConfigUtils.getConfiguredInstance(IDataTypeFactory.class, configuration, databaseDialect);
    }

    /**
     * Checks whether the given test instance is a database test.
     *
     * @param testClass the test class, not null
     * @return true if the test class is a database test false otherwise
     * @see DatabaseModule#isDatabaseTest(Class<?>)
     */
    protected boolean isDatabaseTest(Class<?> testClass) {

        return getDatabaseModule().isDatabaseTest(testClass);
    }

    /**
     * Creates a new instance of dbUnit's <code>IDatabaseConnection</code>
     *
     * @return a new instance of dbUnit's <code>IDatabaseConnection</code>
     */
    protected DbUnitDatabaseConnection createDbUnitConnection() {

        // Create connection
        DbUnitDatabaseConnection connection = new DbUnitDatabaseConnection(getDatabaseModule().getDataSource(), schemaName);

        // Make sure correct dbms specific data types are used
        DatabaseConfig config = connection.getConfig();
        config.setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, dataTypeFactory);
        return connection;
    }

    /**
     * @return The DbUnit connection
     */
    public DbUnitDatabaseConnection getDbUnitDatabaseConnection() {

        if (dbUnitDatabaseConnection == null) {
            dbUnitDatabaseConnection = createDbUnitConnection();
        }
        return dbUnitDatabaseConnection;
    }

    /**
     * This method will first try to load a method level defined dataset.
     * If no such file exists, a class level defined dataset will be loaded.
     * If neither of these files exist, nothing is done.
     * The name of the test data file at both method level and class level can be overridden using the {@link
     * DataSet} annotation. If specified using this annotation but not found, a {@link UnitilsException} is
     * thrown.
     *
     * @param testClass  the test class, not null
     * @param testMethod the method, not null
     */
    protected void insertTestData(Class testClass, Method testMethod) {

        try {
            IDataSet dataSet = getTestDataSet(testClass, testMethod);
            if (dataSet != null) { // Dataset is null when there is no data xml file.
                getInsertDatabaseOperation().execute(getDbUnitDatabaseConnection(), dataSet);
            }
        } catch (Exception e) {
            throw new UnitilsException("Error while trying to insert test data from DbUnit xml file", e);
        } finally {
            closeJdbcConnection();
        }
    }

    /**
     * @return The DbUnit <code>DatabaseTask</code> that is used for loading the data file
     */
    protected DatabaseOperation getInsertDatabaseOperation() {

        return DatabaseOperation.CLEAN_INSERT;
    }

    /**
     * This method will first try to return a method level defined dataset.
     * If no such file exists, a class level defined dataset will be returned.
     * If neither of these files exist, null is returned.
     * The name of the test data file at both method level and class level can be overridden using the {@link
     * DataSet} annotation. If specified using this annotation but not found, a {@link UnitilsException} is
     * thrown.
     *
     * @param testClass the test class, not null
     * @param method    the test method, not null
     * @return the dataset, can be null if the files were not found
     */
    protected IDataSet getTestDataSet(Class testClass, Method method) {

        //load the test specific dataset
        IDataSet dataSet = getMethodLevelTestDataSet(testClass, method);
        if (dataSet == null) {
            //load the default dataset
            dataSet = getClassLevelTestDataSet(testClass);
        }
        return dataSet;
    }

    /**
     * Returns the DbUnit dataSet that has been defined at method level, if it exists. By default, this dataSet is the
     * file located in the same package as the test class, with as name className + '.' + methodName + '.xml'. This default
     * name can be overridden using the {@link DataSet} annotation at method level. If the dataSet filename is
     * explicitly set but not found, a {@link UnitilsException} is thrown.
     *
     * @param testClass the test class, not null
     * @param method    the test method, not null
     * @return The class level DbUnit DataSet
     */
    protected IDataSet getMethodLevelTestDataSet(Class testClass, Method method) {
        DataSet dataSetAnnotation = method.getAnnotation(DataSet.class);
        if (dataSetAnnotation != null) {
            IDataSet dataSet = getDataSet(testClass, dataSetAnnotation.fileName());
            if (dataSet == null) {
                throw new UnitilsException("Could not find DbUnit dataset with name " + dataSetAnnotation.fileName());
            }
            return dataSet;
        } else {
            return getDataSet(testClass, getMethodLevelDefaultTestDataSetFileName(testClass, method));
        }
    }

    /**
     * Gets the name of the default testdata file at method level
     * The default name is constructed as follows: classname + '.' + methodName + '.xml'
     *
     * @param testClass the test class, not null
     * @param method    the test method, not null
     * @return the default filename
     */
    protected String getMethodLevelDefaultTestDataSetFileName(Class<?> testClass, Method method) {
        String className = testClass.getName();
        return className.substring(className.lastIndexOf(".") + 1) + "." + method.getName() + ".xml";
    }

    /**
     * Returns the DbUnit dataSet that has been defined at class level. By default, this dataSet is the file located
     * in the same package as the test class, with as name name of class + '.xml'. This default name can be
     * overridden using the {@link DataSet} annotation at class level. If the dataSet filename is explicitly set
     * but not found, a {@link UnitilsException} is thrown.
     *
     * @param testClass the test class, not null
     * @return The class level DbUnit DataSet
     */
    protected IDataSet getClassLevelTestDataSet(Class<?> testClass) {
        DataSet dataSetAnnotation = testClass.getAnnotation(DataSet.class);
        if (dataSetAnnotation != null) {
            IDataSet dataSet = getDataSet(testClass, dataSetAnnotation.fileName());
            if (dataSet == null) {
                throw new UnitilsException("Could not find DbUnit dataset with name " + dataSetAnnotation.fileName());
            }
            return dataSet;
        } else {
            return getDataSet(testClass, getClassLevelDefaultTestDataSetFileName(testClass));
        }
    }


    /**
     * Gets the name of the default testdata file at class level
     * The default name is constructed as follows: 'classname without packagename'.xml
     *
     * @param testClass the test class, not null
     * @return the default filename
     */
    protected String getClassLevelDefaultTestDataSetFileName(Class<?> testClass) {

        String className = testClass.getName();
        return className.substring(className.lastIndexOf(".") + 1) + ".xml";
    }

    /**
     * Returns the dataset from the file in the classpath with the given name.
     * Filenames that start with '/' are treated absolute. Filenames that do not start with '/', are relative
     * to the current class.
     *
     * @param testClass       the test class, not null
     * @param dataSetFilename the name, (start with '/' for absolute names)
     * @return the data set, or null if the file did not exist
     */
    protected IDataSet getDataSet(Class testClass, String dataSetFilename) {

        try {
            if (dataSetFilename == null) {
                return null;
            }

            InputStream in = null;
            try {
                in = testClass.getResourceAsStream(dataSetFilename);
                if (in == null) {
                    return null;
                }

                IDataSet dataSet = createDbUnitDataSet(in);
                // todo make configurable
                ReplacementDataSet replacementDataSet = new ReplacementDataSet(dataSet);
                replacementDataSet.addReplacementObject("[null]", null);
                return replacementDataSet;

            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } catch (IOException e) {
            throw new UnitilsException("Error while loading DbUnit dataset", e);
        }
    }

    /**
     * Constructs a DbUnit DataSet object from the given InputStream
     *
     * @param in the xml content stream, not null
     * @return a DbUnit DataSet object
     */
    protected IDataSet createDbUnitDataSet(InputStream in) {

        try {
            return new TablePerRowXmlDataSet(in);

        } catch (Exception e) {
            throw new UnitilsException("Error while reading DbUnit dataset", e);
        }
    }


    //todo javadoc
    protected void assertDbContentsAsExpectedIfAnnotated(Object testObject, Method testMethod) {

        ExpectedDataSet expectedDataSetAnnotation = testMethod.getAnnotation(ExpectedDataSet.class);
        if (expectedDataSetAnnotation != null) {
            String expectedDataSetFileName = expectedDataSetAnnotation.fileName();
            if (StringUtils.isEmpty(expectedDataSetFileName)) {
                expectedDataSetFileName = getDefaultExpectedDataSetFileName(testObject.getClass(), testMethod.getName());
            }
            assertDBContentAsExpected(testObject, expectedDataSetFileName);
        }
    }

    /**
     * Compares the contents of the expected DbUnitDataSet with the contents of the database. Only the tables and columns
     * that occur in the expected DbUnitDataSet are compared with the database contents.
     *
     * @param testObject              the test instance, not null
     * @param expectedDataSetFileName the file name, not null
     */
    public void assertDBContentAsExpected(Object testObject, String expectedDataSetFileName) {

        try {
            IDataSet expectedDataSet = getDataSet(testObject.getClass(), expectedDataSetFileName);
            IDataSet actualDataSet = getDbUnitDatabaseConnection().createDataSet(expectedDataSet.getTableNames());
            ITableIterator tables = expectedDataSet.iterator();

            while (tables.next()) {
                ITable expectedTable = tables.getTable();
                ITableMetaData metaData = expectedTable.getTableMetaData();
                ITable actualTable = actualDataSet.getTable(expectedTable.getTableMetaData().getTableName());
                ITable filteredActualTable = DefaultColumnFilter.includedColumnsTable(actualTable, metaData.getColumns());

                Assertion.assertEquals(new SortedTable(expectedTable), new SortedTable(filteredActualTable,
                        expectedTable.getTableMetaData()));
            }
        } catch (Exception e) {
            throw new UnitilsException("Error while verifying db contents", e);
        } finally {
            closeJdbcConnection();
        }
    }

    /**
     * Closes (i.e. return to the pool) the JDBC Connection that is currently in use by the DbUnitDatabaseConnection
     */
    protected void closeJdbcConnection() {
        try {
            getDbUnitDatabaseConnection().closeJdbcConnection();

        } catch (SQLException e) {
            throw new UnitilsException("Error while closing connection", e);
        }
    }

    /**
     * Gets the name of the expected dataset file.
     * The default name of this file is constructed as follows: 'classname without packagename'.'testname'-result.xml.
     * This default name can be overridden by annotating the test method with the {@link ExpectedDataSet}
     * annotation.
     * <p/>
     * todo method name or Method object?
     *
     * @param testClass  the test class, not null
     * @param methodName the test method name, not null
     * @return the expected dataset filename
     */
    protected static String getDefaultExpectedDataSetFileName(Class<?> testClass, String methodName) {

        ExpectedDataSet expectedDataSetAnnotation = testClass.getAnnotation(ExpectedDataSet.class);
        if (expectedDataSetAnnotation != null) {
            return expectedDataSetAnnotation.fileName();
        } else {
            String className = testClass.getName();
            return className.substring(className.lastIndexOf(".") + 1) + "." + methodName + "-result.xml";
        }
    }

    /**
     * @return Implementation of DatabaseModule, on which this module is dependent
     */
    protected DatabaseModule getDatabaseModule() {

        //todo throw exception when module not found
        Unitils unitils = Unitils.getInstance();
        return unitils.getModulesRepository().getModuleOfType(DatabaseModule.class);
    }


    /**
     * @return The TestListener object that implements Unitils' DbUnit support
     */
    public TestListener createTestListener() {

        return new DbUnitListener();
    }


    /**
     * Test listener that is called while the test framework is running tests
     */
    private class DbUnitListener extends TestListener {

        @Override
        public void beforeAll() {
            if (getDatabaseModule() == null) {
                throw new UnitilsException("Invalid configuration: When the DbUnitModule is enabled, the DatabaseModule " +
                        "should also be enabled and the DbUnitModule should be configured to run after the DatabaseModule");
            }
        }

        @Override
        public void beforeTestMethod(Object testObject, Method testMethod) {
            if (isDatabaseTest(testObject.getClass())) {
                insertTestData(testObject.getClass(), testMethod);
            }
        }

        @Override
        public void afterTestMethod(Object testObject, Method testMethod) {
            if (isDatabaseTest(testObject.getClass())) {
                assertDbContentsAsExpectedIfAnnotated(testObject, testMethod);
            }
        }

    }

}