/*
 * Copyright Unitils.org
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
package org.unitils.dataset.database;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.unitils.UnitilsJUnit4;
import org.unitils.database.annotations.TestDataSource;

import javax.sql.DataSource;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.unitils.database.SQLUnitils.executeUpdate;
import static org.unitils.database.SQLUnitils.executeUpdateQuietly;
import static org.unitils.dataset.util.DatabaseTestUtils.createDatabaseMetaData;
import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;

/**
 * Tests for getting the primary key column names of a table.
 *
 * @author Tim Ducheyne
 * @author Filip Neven
 */
public class DatabaseMetaDataGetPrimaryKeyColumnNamesTest extends UnitilsJUnit4 {

    /* Tested object */
    private DatabaseMetaData databaseMetaData;

    @TestDataSource
    protected DataSource dataSource;


    @Before
    public void initialize() throws Exception {
        databaseMetaData = createDatabaseMetaData();
    }

    @Before
    public void createTestTables() {
        dropTestTables();
        executeUpdate("create table test (col1 varchar(100) not null, col2 integer not null, col3 timestamp, primary key (col1, col2))", dataSource);
        executeUpdate("create table \"TestCase\" (\"Col1\" varchar(100) not null, \"col2\" integer not null, col3 timestamp, primary key (\"Col1\", \"col2\"))", dataSource);
        executeUpdate("create table noPrimaryKeys (col1 integer)", dataSource);
    }

    @After
    public void dropTestTables() {
        executeUpdateQuietly("drop table test", dataSource);
        executeUpdateQuietly("drop table \"TestCase\"", dataSource);
        executeUpdateQuietly("drop table noPrimaryKeys", dataSource);
    }


    @Test
    public void getPrimaryKeyColumnNames() throws Exception {
        Set<String> result = databaseMetaData.getPrimaryKeyColumnNames("PUBLIC.TEST");
        assertReflectionEquals(asList("COL1", "COL2"), result);
    }

    @Test
    public void noPrimaryKeys() throws Exception {
        Set<String> result = databaseMetaData.getPrimaryKeyColumnNames("PUBLIC.NOPRIMARYKEYS");
        assertTrue(result.isEmpty());
    }

    @Test
    public void caseSensitive() throws Exception {
        Set<String> result = databaseMetaData.getPrimaryKeyColumnNames("\"PUBLIC\".\"TestCase\"");
        assertReflectionEquals(asList("Col1", "col2"), result);
    }

    @Test
    public void primaryKeySetCached() throws Exception {
        Set<String> result1 = databaseMetaData.getPrimaryKeyColumnNames("PUBLIC.TEST");
        Set<String> result2 = databaseMetaData.getPrimaryKeyColumnNames("PUBLIC.TEST");
        assertSame(result1, result2);
    }

    @Test
    public void onlyCachedForIdenticalSchemaAndTableName() throws Exception {
        Set<String> result1 = databaseMetaData.getPrimaryKeyColumnNames("PUBLIC.TEST");
        Set<String> result2 = databaseMetaData.getPrimaryKeyColumnNames("\"PUBLIC\".\"TEST\"");
        assertNotSame(result1, result2);
    }

    @Test
    public void tableNotFound() throws Exception {
        Set<String> result = databaseMetaData.getPrimaryKeyColumnNames("xxxx.xxxx");
        assertTrue(result.isEmpty());
    }
}