/*
 * Copyright 2008,  Unitils.org
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
package org.unitils.mock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.unitils.core.Module;
import org.unitils.core.TestListener;
import org.unitils.core.UnitilsException;
import org.unitils.easymock.EasyMockModule;
import org.unitils.mock.annotation.AfterCreateMock;
import org.unitils.mock.core.MockObject;
import org.unitils.mock.core.Scenario;
import static org.unitils.util.AnnotationUtils.getMethodsAnnotatedWith;
import static org.unitils.util.ReflectionUtils.*;

import java.lang.reflect.*;
import java.util.Properties;
import java.util.Set;

/**
 * Module for testing with mock objects.
 *
 * @author Filip Neven
 * @author Tim Ducheyne
 * @author Kenny Claes
 */
public class MockModule implements Module {

    /* The logger instance for this class */
    private static Log logger = LogFactory.getLog(MockModule.class);


    private Scenario scenario;


    /** No initialization needed for this module */
    public void init(Properties configuration) {
        scenario = createScenario();
    }


    /** No after initialization needed for this module */
    public void afterInit() {
    }


    public Scenario getScenario() {
        return scenario;
    }


    public void assertNoMoreInvocations() {
        scenario.assertNoMoreInvocations();
    }


    public void logExecutionScenario() {
        String report = scenario.createReport();
        logger.info(report);
    }


    protected Scenario createScenario() {
        return new Scenario();
    }


    @SuppressWarnings({"unchecked"})
    protected Mock<?> createMock(Field field, boolean partial) {
        return new MockObject(field.getName(), getMockedClass(field), partial, getScenario());
    }


    protected Class<?> getMockedClass(Field field) {
        Type type = field.getGenericType();
        if (type instanceof ParameterizedType) {
            Type[] argumentTypes = ((ParameterizedType) type).getActualTypeArguments();
            if (argumentTypes.length == 1 && argumentTypes[0] instanceof Class<?>) {
                return (Class<?>) argumentTypes[0];
            }
        }
        throw new UnitilsException("Unable to determine type of mock. A mock should be declared using the generic Mock<YourTypeToMock> and PartialMock<YourTypeToMock> types. Used type; " + type);
    }


    protected void createAndInjectMocksIntoTest(Object testObject) {
        Set<Field> mockFields = getFieldsOfType(testObject.getClass(), Mock.class, false);
        for (Field field : mockFields) {
            if (getFieldValue(testObject, field) == null) {
                Mock<?> mock = createMock(field, false);
                setFieldValue(testObject, field, mock);
                callAfterCreateMockMethods(testObject, mock, field.getName(), field.getType());
            }
        }

        Set<Field> partialMockFields = getFieldsOfType(testObject.getClass(), PartialMock.class, false);
        for (Field field : partialMockFields) {
            if (getFieldValue(testObject, field) == null) {
                Mock<?> mock = createMock(field, true);
                setFieldValue(testObject, field, mock);
                callAfterCreateMockMethods(testObject, mock, field.getName(), field.getType());
            }
        }
    }


    /**
     * Calls all {@link AfterCreateMock} annotated methods on the test, passing the given mock.
     * These annotated methods must have following signature <code>void myMethod(Object mock, String name, Class type)</code>.
     * If this is not the case, a runtime exception is called.
     *
     * @param testObject the test, not null
     * @param mockObject the mock, not null
     * @param name       the field(=mock) name, not null
     * @param type       the field(=mock) type
     */
    // todo should we inject the mock or the proxy??
    protected void callAfterCreateMockMethods(Object testObject, Object mockObject, String name, Class<?> type) {
        Set<Method> methods = getMethodsAnnotatedWith(testObject.getClass(), AfterCreateMock.class);
        for (Method method : methods) {
            try {
                invokeMethod(testObject, method, mockObject, name, type);

            } catch (InvocationTargetException e) {
                throw new UnitilsException("An exception occurred while invoking an after create mock method.", e);
            } catch (Exception e) {
                throw new UnitilsException("Unable to invoke after create mock method. Ensure that this method has following signature: void myMethod(Object mock, String name, Class type)", e);
            }
        }
    }


    /**
     * Creates the listener for plugging in the behavior of this module into the test runs.
     *
     * @return the listener
     */
    public TestListener getTestListener() {
        return new MockTestListener();
    }


    /** Test listener that handles the mock creation and injection. */
    protected class MockTestListener extends TestListener {

        /**
         * Before the test is executed this calls {@link EasyMockModule#createAndInjectRegularMocksIntoTest(Object)} to
         * create and inject all mocks on the class.
         */
        @Override
        public void beforeTestSetUp(Object testObject, Method testMethod) {
            scenario.reset();
            createAndInjectMocksIntoTest(testObject);
        }

    }


}