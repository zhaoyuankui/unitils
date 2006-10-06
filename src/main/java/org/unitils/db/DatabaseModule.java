package org.unitils.db;

import org.apache.commons.configuration.Configuration;
import org.unitils.core.TestListener;
import org.unitils.core.Unitils;
import org.unitils.core.UnitilsException;
import org.unitils.core.UnitilsModule;
import org.unitils.dbmaintainer.config.DataSourceFactory;
import org.unitils.dbmaintainer.handler.StatementHandlerException;
import org.unitils.dbmaintainer.handler.JDBCStatementHandler;
import org.unitils.dbmaintainer.handler.StatementHandler;
import org.unitils.dbmaintainer.maintainer.DBMaintainer;
import org.unitils.dbmaintainer.constraints.ConstraintsCheckDisablingDataSource;
import org.unitils.dbmaintainer.constraints.ConstraintsDisabler;
import org.unitils.util.ReflectionUtils;
import org.unitils.util.UnitilsConfiguration;
import org.unitils.util.AnnotationUtils;
import org.unitils.dbunit.DatabaseTest;
import org.unitils.db.annotations.AfterCreateDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Module that provides basic support for database testing. This module provides the following services to unit tests
 * <ul>
 * <li>Connection pooling: A connection pooled DataSource is created, and supplied to methods annotated with
 * {@link AfterCreateDataSource}</li>
 * <li>A 'current connection' is associated with each thread from which the method #getCurrentConnection is called</li>
 * <li>If the updateDataBaseSchema.enabled property is set to true, the {@link DBMaintainer} is invoked to update the
 * database and prepare it for unit testing (see {@link DBMaintainer} Javadoc)</li>
 */
public class DatabaseModule implements UnitilsModule {

    /* Property keys indicating if the database schema should be updated before performing the tests */
    static final String PROPKEY_UPDATEDATABASESCHEMA_ENABLED = "updateDataBaseSchema.enabled";

    /* Property keys of the datasource factory classname */
    static final String PROPKEY_DATASOURCEFACTORY_CLASSNAME = "dataSourceFactory.className";

    /* Property key indicating if the database constraints should org disabled after updating the database */
    private static final String PROPKEY_DISABLECONSTRAINTS_ENABLED = "dbMaintainer.disableConstraints.enabled";

    /* Property key of the implementation class of {@link ConstraintsDisabler} */
    private static final String PROPKEY_CONSTRAINTSDISABLER_START = "constraintsDisabler.className";

    /* Property key of the SQL dialect of the underlying DBMS implementation */
    private static final String PROPKEY_DATABASE_DIALECT = "database.dialect";

    /* The pooled datasource instance */
    private DataSource dataSource;

    /*
    * Database connection holder: ensures that if the method getCurrentConnection is always used for getting
    * a connection to the database, at most one database connection exists per thread
    */
    private ThreadLocal<Connection> connectionHolder = new ThreadLocal<Connection>();

    /**
     * @param testClass
     * @return True if the test class is a database test, i.e. is annotated with the {@link DatabaseTest} annotation,
     * false otherwise
     */
    protected boolean isDatabaseTest(Class testClass) {
        return testClass.getAnnotation(DatabaseTest.class) != null;
    }

    /**
     * Inializes the database setup. I.e., creates a <code>DataSource</code> and updates the database schema if needed
     * using the {@link DBMaintainer}
     */
    protected void initDatabase(Object testObject) {
        try {
            if (dataSource == null) {
                //create the singleton datasource
                dataSource = createDataSource();
                //call methods annotated with AfterCreateDataSource, if any
                callAfterCreateDataSourceMethods(testObject);
                //create the connection instance
                updateDatabaseSchemaIfNeeded();
            }
        } catch (Exception e) {
            throw new UnitilsException("Error while intializing database connection", e);
        }
    }

    /**
     * Creates a datasource by using the factory that is defined by the dataSourceFactory.className property
     *
     * @return the datasource
     */
    private DataSource createDataSource() {
        DataSourceFactory dataSourceFactory = getDataSourceFactory();
        dataSourceFactory.init();
        DataSource ds = dataSourceFactory.createDataSource();

        // If contstraints disabling is active, a ConstraintsCheckDisablingDataSource is returned that wrappes the
        // DataSource object
        Configuration configuration = UnitilsConfiguration.getInstance();
        boolean disableConstraints = configuration.getBoolean(PROPKEY_DISABLECONSTRAINTS_ENABLED);
        if (disableConstraints) {
            ConstraintsDisabler constraintsDisabler = createConstraintsDisabler(configuration, ds);
            return new ConstraintsCheckDisablingDataSource(ds, constraintsDisabler);
        } else {
            return ds;
        }
    }

    /**
     * Creates the configured instance of the {@link ConstraintsDisabler}
     * @param configuration
     * @param ds
     * @return The configured instance of the {@link ConstraintsDisabler}
     */
    private ConstraintsDisabler createConstraintsDisabler(Configuration configuration, DataSource ds) {
        StatementHandler statementHandler = new JDBCStatementHandler();
        statementHandler.init(ds);
        String databaseDialect = configuration.getString(PROPKEY_DATABASE_DIALECT);
        ConstraintsDisabler constraintsDisabler = ReflectionUtils.createInstanceOfType(configuration.getString(
                PROPKEY_CONSTRAINTSDISABLER_START + "." + databaseDialect));
        constraintsDisabler.init(configuration, ds, statementHandler);
        return constraintsDisabler;
    }

    /**
     * Returns an instance of the configured {@link DataSourceFactory}
     * @return The configured {@link DataSourceFactory}
     */
    protected DataSourceFactory getDataSourceFactory() {
        return ReflectionUtils.createInstanceOfType(
                UnitilsConfiguration.getInstance().getString(PROPKEY_DATASOURCEFACTORY_CLASSNAME));
    }

    /**
     * @return The <code>DataSource</code>
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * @return The database connection that is associated with the current thread.
     */
    public Connection getCurrentConnection() {
        Connection currentConnection = connectionHolder.get();
        if (currentConnection == null) {
            try {
                currentConnection = getDataSource().getConnection();
            } catch (SQLException e) {
                throw new UnitilsException("Error while establishing connection to the database", e);
            }
            connectionHolder.set(currentConnection);
        }
        return currentConnection;
    }

    /**
     * Calls all methods annotated with {@link AfterCreateDataSource}
     * @param testObject
     */
    private void callAfterCreateDataSourceMethods(Object testObject) {
        List<Method> methods = AnnotationUtils.getMethodsAnnotatedWith(testObject.getClass(), AfterCreateDataSource.class);
        for (Method method : methods) {
            try {
                ReflectionUtils.invokeMethod(testObject, method, dataSource);

            } catch (UnitilsException e) {

                throw new UnitilsException("Unable to invoke after create DataSource method. Ensure that this method has " +
                        "following signature: void myMethod(DataSource dataSource, String name)", e);
            }
        }
    }

    /**
     * Determines whether the test database is outdated and, if that is the case, updates the database with the
     * latest changes. See {@link org.unitils.dbmaintainer.maintainer.DBMaintainer} for more information.
     */
    protected void updateDatabaseSchemaIfNeeded() throws StatementHandlerException {
        Configuration configuration = UnitilsConfiguration.getInstance();

        if (configuration.getBoolean(PROPKEY_UPDATEDATABASESCHEMA_ENABLED)) {
            DBMaintainer dbMaintainer = createDbMaintainer();
            dbMaintainer.updateDatabase();
        }
    }

    /**
     * Creates a new instance of the DBMaintainer for the given <code>DataSource</code>
     * @return a new instance of the DBMaintainer
     */
    protected DBMaintainer createDbMaintainer() {
        return new DBMaintainer(dataSource);
    }

    /**
     * @return The {@link TestListener} associated with this module
     */
    public TestListener createTestListener() {
        return new DatabaseTestListener();
    }

    /**
     * DatabaseTestListener that makes callbacks to methods of this module while running tests.
     */
    private class DatabaseTestListener extends TestListener {

        public void beforeTestClass() {
            if (isDatabaseTest(Unitils.getTestContext().getTestClass())) {
                initDatabase(Unitils.getTestContext().getTestObject());
           }
        }

    }
}