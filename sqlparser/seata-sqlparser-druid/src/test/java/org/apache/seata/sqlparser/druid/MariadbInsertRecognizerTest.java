/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.sqlparser.druid;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.seata.sqlparser.druid.mariadb.MariadbInsertRecognizer;
import org.apache.seata.sqlparser.druid.mysql.MySQLInsertRecognizer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.expr.MySqlOrderingExpr;

import org.apache.seata.sqlparser.SQLParsingException;
import org.apache.seata.sqlparser.SQLType;
import org.apache.seata.sqlparser.util.JdbcConstants;

/**
 * The type Mariadb insert recognizer test.
 *
 */
public class MariadbInsertRecognizerTest extends AbstractRecognizerTest {

    private final int pkIndex = 0;

    /**
     * Insert recognizer test 0.
     */
    @Test
    public void insertRecognizerTest_0() {

        String sql = "INSERT INTO t1 (name) VALUES ('name1')";

        SQLStatement statement = getSQLStatement(sql);

        MariadbInsertRecognizer insertRecognizer = new MariadbInsertRecognizer(sql, statement);

        Assertions.assertEquals(sql, insertRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", insertRecognizer.getTableName());
        Assertions.assertEquals(Collections.singletonList("name"), insertRecognizer.getInsertColumns());
        Assertions.assertEquals(1, insertRecognizer.getInsertRows(Collections.singletonList(pkIndex)).size());
        Assertions.assertEquals(Collections.singletonList("name1"), insertRecognizer.getInsertRows(Collections.singletonList(pkIndex)).get(0));
    }

    /**
     * Insert recognizer test 1.
     */
    @Test
    public void insertRecognizerTest_1() {

        String sql = "INSERT INTO t1 (name1, name2) VALUES ('name1', 'name2')";

        SQLStatement statement = getSQLStatement(sql);

        MariadbInsertRecognizer insertRecognizer = new MariadbInsertRecognizer(sql, statement);

        Assertions.assertEquals(sql, insertRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", insertRecognizer.getTableName());
        Assertions.assertEquals(Arrays.asList("name1", "name2"), insertRecognizer.getInsertColumns());
        Assertions.assertEquals(1, insertRecognizer.getInsertRows(Collections.singletonList(pkIndex)).size());
        Assertions.assertEquals(Arrays.asList("name1", "name2"), insertRecognizer.getInsertRows(Collections.singletonList(pkIndex)).get(0));
    }

    /**
     * Insert recognizer test 3.
     */
    @Test
    public void insertRecognizerTest_3() {

        String sql = "INSERT INTO t1 (name1, name2) VALUES ('name1', 'name2'), ('name3', 'name4'), ('name5', 'name6')";

        SQLStatement statement = getSQLStatement(sql);

        MariadbInsertRecognizer insertRecognizer = new MariadbInsertRecognizer(sql, statement);

        Assertions.assertEquals(sql, insertRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", insertRecognizer.getTableName());
        Assertions.assertEquals(Arrays.asList("name1", "name2"), insertRecognizer.getInsertColumns());
        Assertions.assertEquals(3, insertRecognizer.getInsertRows(Collections.singletonList(pkIndex)).size());
        Assertions.assertEquals(Arrays.asList("name1", "name2"), insertRecognizer.getInsertRows(Collections.singletonList(pkIndex)).get(0));
        Assertions.assertEquals(Arrays.asList("name3", "name4"), insertRecognizer.getInsertRows(Collections.singletonList(pkIndex)).get(1));
        Assertions.assertEquals(Arrays.asList("name5", "name6"), insertRecognizer.getInsertRows(Collections.singletonList(pkIndex)).get(2));
    }

    @Test
    public void testGetSqlType() {
        String sql = "insert into t(id) values (?)";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MARIADB);

        MariadbInsertRecognizer recognizer = new MariadbInsertRecognizer(sql, asts.get(0));
        Assertions.assertEquals(recognizer.getSQLType(), SQLType.INSERT);
    }

    @Test
    public void testGetTableAlias() {
        String sql = "insert into t(id) values (?)";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MARIADB);

        MariadbInsertRecognizer recognizer = new MariadbInsertRecognizer(sql, asts.get(0));
        Assertions.assertNull(recognizer.getTableAlias());
    }

    @Test
    public void testGetInsertColumns() {

        //test for no column
        String sql = "insert into t values (?)";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MARIADB);

        MariadbInsertRecognizer recognizer = new MariadbInsertRecognizer(sql, asts.get(0));
        List<String> insertColumns = recognizer.getInsertColumns();
        Assertions.assertNull(insertColumns);

        //test for normal
        sql = "insert into t(a) values (?)";
        asts = SQLUtils.parseStatements(sql, JdbcConstants.MARIADB);

        recognizer = new MariadbInsertRecognizer(sql, asts.get(0));
        insertColumns = recognizer.getInsertColumns();
        Assertions.assertEquals(1, insertColumns.size());

        //test for exception
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String s = "insert into t(a) values (?)";
            List<SQLStatement> sqlStatements = SQLUtils.parseStatements(s, JdbcConstants.MARIADB);
            SQLInsertStatement sqlInsertStatement = (SQLInsertStatement)sqlStatements.get(0);
            sqlInsertStatement.getColumns().add(new MySqlOrderingExpr());

            MariadbInsertRecognizer oracleInsertRecognizer = new MariadbInsertRecognizer(s, sqlInsertStatement);
            oracleInsertRecognizer.getInsertColumns();
        });
    }

    @Test
    public void testGetInsertRows() {
        //test for null value
        String sql = "insert into t(id, no, name, age, time) values (1, null, 'a', ?, now())";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MARIADB);

        MariadbInsertRecognizer recognizer = new MariadbInsertRecognizer(sql, asts.get(0));
        List<List<Object>> insertRows = recognizer.getInsertRows(Collections.singletonList(pkIndex));
        Assertions.assertEquals(1, insertRows.size());

        //test for exception
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String s = "insert into t(a) values (?)";
            List<SQLStatement> sqlStatements = SQLUtils.parseStatements(s, JdbcConstants.MARIADB);
            SQLInsertStatement sqlInsertStatement = (SQLInsertStatement)sqlStatements.get(0);
            sqlInsertStatement.getValuesList().get(0).getValues().set(pkIndex, new MySqlOrderingExpr());

            MariadbInsertRecognizer insertRecognizer = new MariadbInsertRecognizer(s, sqlInsertStatement);
            insertRecognizer.getInsertRows(Collections.singletonList(pkIndex));
        });
    }

    @Override
    public String getDbType() {
        return JdbcConstants.MARIADB;
    }

    @Test
    public void testGetInsertColumns_2() {
        String sql = "insert into t(`id`, `no`, `name`, `age`) values (1, 'no001', 'aaa', '20')";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MARIADB);
        MariadbInsertRecognizer recognizer = new MariadbInsertRecognizer(sql, asts.get(0));
        List<String> insertColumns = recognizer.getInsertColumns();
        for (String insertColumn : insertColumns) {
            Assertions.assertTrue(insertColumn.contains("`"));
        }
    }

}