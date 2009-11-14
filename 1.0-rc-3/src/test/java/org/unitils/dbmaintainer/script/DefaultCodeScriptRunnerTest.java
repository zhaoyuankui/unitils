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
package org.unitils.dbmaintainer.script;

import org.unitils.UnitilsJUnit3;
import org.unitils.dbmaintainer.script.impl.SQLCodeScriptRunner;
import static org.unitils.reflectionassert.ReflectionAssert.assertLenEquals;

import java.util.Arrays;
import java.util.List;

/**
 * todo javadoc
 *
 * @author Filip Neven
 * @author Tim Ducheyne
 */
public class DefaultCodeScriptRunnerTest extends UnitilsJUnit3 {

    /* Tested instance  */
    private SQLCodeScriptRunner sqlCodeScriptRunner;

    /* Normal script, containing 2 statements and a blank line */
    private static final String NORMAL_SCRIPT =
            "PROCEDURE TEST1 (param1 VARCHAR, param2 BLOB) IS\n" +
                    "BEGIN\n" +
                    "statement1;\n" +
                    "statement2;\n" +
                    "END\n" +
                    "/  \n" +
                    "PROCEDURE TEST2 (param1 VARCHAR, param2 BLOB) IS\n" +
                    "BEGIN\n" +
                    "statement1;\n" +
                    "statement2;\n" +
                    "END;\n" +
                    "/";

    private static final String SCRIPT_WITH_MULTILINE_COMMENT =
            "/* multiline\n" +
                    " comment */" +
                    "PROCEDURE TEST1 (param1 VARCHAR, param2 BLOB) IS\n" +
                    "BEGIN\n" +
                    "statement1;\n" +
                    "statement2;\n" +
                    "END;\n" +
                    "/";

    private static final String SCRIPT_WITH_LINE_COMMENT =
            "-- line comment /\n" +
                    "PROCEDURE TEST1 (param1 VARCHAR, param2 BLOB) IS\n" +
                    "BEGIN\n" +
                    "statement1;\n" +
                    "statement2;\n" +
                    "END;\n" +
                    "/";

    private static final String SCRIPT_WITH_SLASH_IN_CODE =
            "PROCEDURE TEST1 (param1 VARCHAR, param2 BLOB) IS\n" +
                    "BEGIN\n" +
                    "/statement1/;\n" +
                    "statement2;\n" +
                    "END;\n" +
                    "/\n" +
                    "PROCEDURE TEST2 (param1 VARCHAR, param2 BLOB) IS\n" +
                    "BEGIN\n" +
                    "/statement1/\n" +
                    "statement2;\n" +
                    "END\n" +
                    "/";


    /**
     * Initialize test fixture
     */
    protected void setUp() throws Exception {
        super.setUp();
        sqlCodeScriptRunner = new SQLCodeScriptRunner();
    }


    public void testExecute() throws Exception {
        List<String> result = sqlCodeScriptRunner.parseStatements(NORMAL_SCRIPT);
        assertLenEquals(Arrays.asList("PROCEDURE TEST1 (param1 VARCHAR, param2 BLOB) IS\nBEGIN\nstatement1;\nstatement2;\nEND", "PROCEDURE TEST2 (param1 VARCHAR, param2 BLOB) IS\nBEGIN\nstatement1;\nstatement2;\nEND;"), result);
    }


    public void testExecute_multilineComment() throws Exception {
        List<String> result = sqlCodeScriptRunner.parseStatements(SCRIPT_WITH_MULTILINE_COMMENT);
        assertLenEquals(Arrays.asList("/* multiline\n comment */PROCEDURE TEST1 (param1 VARCHAR, param2 BLOB) IS\nBEGIN\nstatement1;\nstatement2;\nEND;"), result);
    }


    public void testExecute_lineComment() throws Exception {
        List<String> result = sqlCodeScriptRunner.parseStatements(SCRIPT_WITH_LINE_COMMENT);
        assertLenEquals(Arrays.asList("-- line comment /\nPROCEDURE TEST1 (param1 VARCHAR, param2 BLOB) IS\nBEGIN\nstatement1;\nstatement2;\nEND;"), result);
    }


    public void testExecute_scriptWithSlashInCode() throws Exception {
        List<String> result = sqlCodeScriptRunner.parseStatements(SCRIPT_WITH_SLASH_IN_CODE);
        assertLenEquals(Arrays.asList("PROCEDURE TEST1 (param1 VARCHAR, param2 BLOB) IS\nBEGIN\n/statement1/;\nstatement2;\nEND;", "PROCEDURE TEST2 (param1 VARCHAR, param2 BLOB) IS\nBEGIN\n/statement1/\nstatement2;\nEND"), result);
    }

}