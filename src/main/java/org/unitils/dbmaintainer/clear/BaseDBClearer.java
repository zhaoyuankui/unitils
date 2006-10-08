package org.unitils.dbmaintainer.clear;

import org.unitils.dbmaintainer.handler.StatementHandler;
import org.unitils.dbmaintainer.handler.StatementHandlerException;
import org.unitils.util.UnitilsConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.dbutils.DbUtils;

import javax.sql.DataSource;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.DatabaseMetaData;

/**
 */
abstract public class BaseDBClearer implements DBClearer {

    /**
     * Property keys of the database schema name
     */
    public static final String PROPKEY_DATABASE_SCHEMANAME = "dataSource.schemaName";

    /**
     *  Property key for the tables that should not be deleted
     */
    public static final String PROPKEY_TABLESTOPRESERVE = "dbMaintainer.tablesToPreserve";

    /**
     * The key of the property that specifies the name of the datase table in which the
     * DB version is stored. This table should not be deleted
     */
    public static final String PROPKEY_VERSION_TABLE_NAME = "dbMaintainer.dbVersionSource.tableName";

    /**
     * The DataSource
     */
    protected DataSource dataSource;

    /**
     * The StatementHandler
     */
    protected StatementHandler statementHandler;

    /* The name of the database schema */
    protected String schemaName;

    /* The tables that should not be cleaned */
    protected Set<String> tablesToPreserve;

    public void init(DataSource dataSource, StatementHandler statementHandler) {
        this.dataSource = dataSource;
        this.statementHandler = statementHandler;

        Configuration configuration = UnitilsConfiguration.getInstance();
        schemaName = configuration.getString(PROPKEY_DATABASE_SCHEMANAME);

        tablesToPreserve = new HashSet<String>();
        tablesToPreserve.add(configuration.getString(PROPKEY_VERSION_TABLE_NAME));
        tablesToPreserve.addAll(toUpperCaseList(Arrays.asList(configuration.getStringArray(PROPKEY_TABLESTOPRESERVE))));
    }

    private List<String> toUpperCaseList(List<String> strings) {
        List<String> toUpperCaseList = new ArrayList<String>();
        for (String string : strings) {
            toUpperCaseList.add(string.toUpperCase());
        }
        return toUpperCaseList;
    }

    public void clearDatabase() throws StatementHandlerException {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            Statement st = conn.createStatement();

            dropViews(conn);
            dropTables(conn);
            dropSequences(st);
        } catch (SQLException e) {
            throw new RuntimeException("Error while clearing database", e);
        } finally {
            DbUtils.closeQuietly(conn);
        }
    }

    private void dropViews(Connection conn) throws SQLException, StatementHandlerException {
        List<String> viewNames = getViewNames(conn);
        for (String viewName : viewNames) {
            if (!tablesToPreserve.contains(viewName)) {
                dropView(viewName);
            }
        }
    }

    private void dropTables(Connection conn) throws SQLException, StatementHandlerException {
        List<String> tableNames = getTableNames(conn);
        for (String tableName : tableNames) {
            if (!tablesToPreserve.contains(tableName)) {
                dropTable(tableName);
            }
        }
    }

    abstract protected void dropView(String viewName) throws SQLException, StatementHandlerException;

    abstract protected void dropTable(String tableName) throws SQLException, StatementHandlerException;

    private List<String> getViewNames(Connection conn) throws SQLException {
        ResultSet rset = null;
        try {
            List<String> tableNames = new ArrayList<String>();
            DatabaseMetaData databaseMetadata = conn.getMetaData();
            rset = databaseMetadata.getTables(null, schemaName.toUpperCase(), null, new String[] {"VIEW"});
            while (rset.next()) {
                String tableName = rset.getString("TABLE_NAME");
                tableNames.add(tableName);
            }
            return tableNames;
        } finally {
            DbUtils.closeQuietly(rset);
        }
    }

    private List<String> getTableNames(Connection conn) throws SQLException {
        ResultSet rset = null;
        try {
            List<String> tableNames = new ArrayList<String>();
            DatabaseMetaData databaseMetadata = conn.getMetaData();
            rset = databaseMetadata.getTables(null, schemaName.toUpperCase(), null, new String[] {"TABLE"});
            while (rset.next()) {
                String tableName = rset.getString("TABLE_NAME");
                tableNames.add(tableName);
            }
            return tableNames;
        } finally {
            DbUtils.closeQuietly(rset);
        }
    }

    abstract protected void dropSequences(Statement st) throws SQLException, StatementHandlerException;
}
