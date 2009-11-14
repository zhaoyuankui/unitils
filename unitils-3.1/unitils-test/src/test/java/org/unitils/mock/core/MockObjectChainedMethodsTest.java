/*
 * Copyright 2006-2007,  Unitils.org
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
package org.unitils.mock.core;

import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests chaining of methods when defining behavior and assertions (UNI-153).
 *
 * @author Tim Ducheyne
 * @author Filip Neven
 */
public class MockObjectChainedMethodsTest {

    /* Class under test */
    private MockObject<TestClass> mockObject;


    @Before
    public void setUp() {
        mockObject = new MockObject<TestClass>("testMock", TestClass.class, this);
    }


    @Test
    public void chainedBehavior() {
        mockObject.returns("value").getTestClass().getValue();

        String result = mockObject.getMock().getTestClass().getValue();
        assertEquals("value", result);
    }


    @Test
    public void doubleChainedBehavior() {
        mockObject.returns("value").getTestClass().getTestClass().getValue();

        String result = mockObject.getMock().getTestClass().getTestClass().getValue();
        assertEquals("value", result);
    }


    @Test
    public void chainedAssertInvoked() {
        mockObject.returns("value").getTestClass().getValue();

        mockObject.getMock().getTestClass().getValue();
        mockObject.assertInvoked().getTestClass().getValue();
    }


    @Test
    public void multipleAssertInvoked() {
        mockObject.getMock().getTestClass();
        mockObject.getMock().getTestClass();
        mockObject.assertInvoked().getTestClass();
        mockObject.assertInvoked().getTestClass();
    }


    @Test
    public void chainedAssertNotInvoked() {
        mockObject.assertNotInvoked().getTestClass().getValue();
    }


    @Test(expected = AssertionError.class)
    public void chainedAssertNotInvokedButInvoked() {
        mockObject.returns("value").getTestClass().getValue();

        mockObject.getMock().getTestClass().getValue();
        mockObject.assertNotInvoked().getTestClass().getValue();
    }


    @Test
    public void chainedAssertInvokedInSequence() {
        mockObject.returns("value").getTestClass().getValue();

        mockObject.getMock().getTestClass().getValue();
        mockObject.getMock().getTestClass().getValue();
        mockObject.assertInvokedInSequence().getTestClass().getValue();
        mockObject.assertInvokedInSequence().getTestClass().getValue();
    }


    @Test(expected = AssertionError.class)
    public void chainedAssertInvokedInSequenceButNotInvoked() {
        mockObject.returns("value").getTestClass().getValue();

        mockObject.getMock().getTestClass().getValue();
        mockObject.assertInvokedInSequence().getTestClass().getValue();
        mockObject.assertInvokedInSequence().getTestClass().getValue();
    }


    /**
     * Interface that is mocked during the tests
     */
    private static interface TestClass {

        public String getValue();

        public TestClass getTestClass();

    }

}