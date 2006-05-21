/*
 * Copyright (C) 2006, Ordina
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package be.ordina.unitils.testing.util;

import junit.framework.TestCase;

import java.util.Date;


/**
 * Test class for {@link ReflectionEquals}.
 * Contains tests for ignore defaults and lenient dates.
 */
public class ReflectionEqualsBuilderLenientTest extends TestCase {

    /* Test object with no java defaults */
    private Element elementNoDefaultsA;

    /* Same as A but different instance */
    private Element elementNoDefaultsB;

    /* Test object with only defaults */
    private Element elementAllDefaults;

    /* Same as A but with null date */
    private Element elementNoDefaultsNullDateA;

    /* Same as null date  A but different instance */
    private Element elementNoDefaultsNullDateB;

    /* Same as A but different date */
    private Element elementNoDefaultsDifferentDate;


    /**
     * Initializes the test fixture.
     */
    protected void setUp() throws Exception {
        super.setUp();

        Date date = new Date();
        elementNoDefaultsA = new Element(true, 'c', (byte) 1, (short) 2, 3, 4l, 5.0f, 6.0, date, "object");
        elementNoDefaultsB = new Element(true, 'c', (byte) 1, (short) 2, 3, 4l, 5.0f, 6.0, date, "object");
        elementNoDefaultsNullDateA = new Element(true, 'c', (byte) 1, (short) 2, 3, 4l, 5.0f, 6.0, null, "object");
        elementNoDefaultsNullDateB = new Element(true, 'c', (byte) 1, (short) 2, 3, 4l, 5.0f, 6.0, null, "object");
        elementNoDefaultsDifferentDate = new Element(true, 'c', (byte) 1, (short) 2, 3, 4l, 5.0f, 6.0, new Date(), "object");
        elementAllDefaults = new Element(false, (char) 0, (byte) 0, (short) 0, 0, 0l, 0.0f, 0.0, null, null);
    }


    /**
     * Test for two equal objects without java defaults.
     */
    public void testCheckEquals_equals() {

        ReflectionEquals reflectionEquals = ReflectionEquals.checkEquals(elementNoDefaultsA, elementNoDefaultsB, false, false);

        assertTrue(reflectionEquals.isEquals());
        assertNull(reflectionEquals.getDifferenceFieldStack());
        assertNull(reflectionEquals.getDifferenceLeftValue());
        assertNull(reflectionEquals.getDifferenceRightValue());
    }


    /**
     * Test with right object containing only java defaults.
     */
    public void testCheckEquals_equalsIgnoreDefaults() {

        ReflectionEquals reflectionEquals = ReflectionEquals.checkEquals(elementNoDefaultsA, elementAllDefaults, true, false);

        assertTrue(reflectionEquals.isEquals());
        assertNull(reflectionEquals.getDifferenceFieldStack());
        assertNull(reflectionEquals.getDifferenceLeftValue());
        assertNull(reflectionEquals.getDifferenceRightValue());
    }


    /**
     * Test for lenient dates with 2 null dates.
     */
    public void testCheckEquals_equalsLenientDatesBothNull() {

        ReflectionEquals reflectionEquals = ReflectionEquals.checkEquals(elementNoDefaultsNullDateA, elementNoDefaultsNullDateB, false, true);

        assertTrue(reflectionEquals.isEquals());
        assertNull(reflectionEquals.getDifferenceFieldStack());
        assertNull(reflectionEquals.getDifferenceLeftValue());
        assertNull(reflectionEquals.getDifferenceRightValue());
    }


    /**
     * Test for lenient dates with 2 not null dates.
     */
    public void testCheckEquals_equalsLenientDatesBothNotNull() {

        ReflectionEquals reflectionEquals = ReflectionEquals.checkEquals(elementNoDefaultsA, elementNoDefaultsDifferentDate, false, true);

        assertTrue(reflectionEquals.isEquals());
        assertNull(reflectionEquals.getDifferenceFieldStack());
        assertNull(reflectionEquals.getDifferenceLeftValue());
        assertNull(reflectionEquals.getDifferenceRightValue());
    }


    /**
     * Test with right object containing only java defaults but no ignore defaults.
     */
    public void testCheckEquals_notEqualsNoIgnoreDefaults() {

        ReflectionEquals reflectionEquals = ReflectionEquals.checkEquals(elementNoDefaultsB, elementAllDefaults, false, false);

        assertFalse(reflectionEquals.isEquals());
        assertEquals("booleanValue", reflectionEquals.getDifferenceFieldStack().get(0));
        assertEquals(Boolean.TRUE, reflectionEquals.getDifferenceLeftValue());
        assertEquals(Boolean.FALSE, reflectionEquals.getDifferenceRightValue());
    }


    /**
     * Test with left instead of right object containing only java defaults.
     */
    public void testCheckEquals_notEqualsIgnoreDefaultsButDefaultsLeft() {

        ReflectionEquals reflectionEquals = ReflectionEquals.checkEquals(elementAllDefaults, elementNoDefaultsB, true, true);

        assertFalse(reflectionEquals.isEquals());
        assertEquals("booleanValue", reflectionEquals.getDifferenceFieldStack().get(0));
        assertEquals(Boolean.FALSE, reflectionEquals.getDifferenceLeftValue());
        assertEquals(Boolean.TRUE, reflectionEquals.getDifferenceRightValue());
    }


    /**
     * Test for lenient dates but with only left date null.
     */
    public void testCheckEquals_notEqualsLenientDatesLeftDateNull() {

        ReflectionEquals reflectionEquals = ReflectionEquals.checkEquals(elementNoDefaultsNullDateA, elementNoDefaultsDifferentDate, false, true);

        assertFalse(reflectionEquals.isEquals());
        assertEquals("dateValue", reflectionEquals.getDifferenceFieldStack().get(0));
        assertNull(reflectionEquals.getDifferenceLeftValue());
        assertEquals(elementNoDefaultsDifferentDate.getDateValue(), reflectionEquals.getDifferenceRightValue());
    }


    /**
     * Test for lenient dates but with only right date null.
     */
    public void testCheckEquals_notEqualsLenientDatesRightDateNull() {

        ReflectionEquals reflectionEquals = ReflectionEquals.checkEquals(elementNoDefaultsDifferentDate, elementNoDefaultsNullDateA, false, true);

        assertFalse(reflectionEquals.isEquals());
        assertEquals("dateValue", reflectionEquals.getDifferenceFieldStack().get(0));
        assertEquals(elementNoDefaultsDifferentDate.getDateValue(), reflectionEquals.getDifferenceLeftValue());
        assertNull(reflectionEquals.getDifferenceRightValue());
    }


    /**
     * Test for lenient dates while ignore defaults but with only right date null (= not treated as default).
     */
    public void testCheckEquals_notEqualsLenientDatesAndIgnoreDefaultsWithRightDateNull() {

        ReflectionEquals reflectionEquals = ReflectionEquals.checkEquals(elementNoDefaultsDifferentDate, elementNoDefaultsNullDateA, true, true);

        assertFalse(reflectionEquals.isEquals());
        assertEquals("dateValue", reflectionEquals.getDifferenceFieldStack().get(0));
        assertEquals(elementNoDefaultsDifferentDate.getDateValue(), reflectionEquals.getDifferenceLeftValue());
        assertNull(reflectionEquals.getDifferenceRightValue());
    }


    /**
     * Test class with failing equals.
     */
    private class Element {

        /* A boolean value */
        private boolean booleanValue;

        /* A char value */
        private char charValue;

        /* A byte value */
        private byte byteValue;

        /* A short value */
        private short shortValue;

        /* An int value */
        private int intValue;

        /* A long value */
        private long longValue;

        /* A float value */
        private float floatValue;

        /* A double value */
        private double doubleValue;

        /* A date value */
        private Date dateValue;

        /* An object value */
        private Object objectValue;

        /**
         * Creates and initializes the element.
         *
         * @param booleanValue a boolean value
         * @param charValue    a char value
         * @param byteValue    a byte value
         * @param shortValue   a short value
         * @param intValue     an int value
         * @param longValue    a long value
         * @param floatValue   a float value
         * @param doubleValue  a double value
         * @param dateValue    a date value
         * @param objectValue  an object value
         */
        public Element(boolean booleanValue, char charValue, byte byteValue, short shortValue, int intValue, long longValue, float floatValue, double doubleValue, Date dateValue, Object objectValue) {
            this.booleanValue = booleanValue;
            this.charValue = charValue;
            this.byteValue = byteValue;
            this.shortValue = shortValue;
            this.intValue = intValue;
            this.longValue = longValue;
            this.floatValue = floatValue;
            this.doubleValue = doubleValue;
            this.dateValue = dateValue;
            this.objectValue = objectValue;
        }

        /**
         * Gets the boolean value.
         *
         * @return the boolean value
         */
        public boolean isBooleanValue() {
            return booleanValue;
        }

        /**
         * Gets the char value.
         *
         * @return the char value
         */
        public char getCharValue() {
            return charValue;
        }

        /**
         * Gets the byte value.
         *
         * @return the byte value
         */
        public byte getByteValue() {
            return byteValue;
        }

        /**
         * Gets the short value.
         *
         * @return the short value
         */
        public short getShortValue() {
            return shortValue;
        }

        /**
         * Gets the int value.
         *
         * @return the int value
         */
        public int getIntValue() {
            return intValue;
        }

        /**
         * Gets the long value.
         *
         * @return the long value
         */
        public long getLongValue() {
            return longValue;
        }

        /**
         * Gets the float value.
         *
         * @return the float value
         */
        public float getFloatValue() {
            return floatValue;
        }

        /**
         * Gets the double value.
         *
         * @return the double value
         */
        public double getDoubleValue() {
            return doubleValue;
        }

        /**
         * Gets the date value.
         *
         * @return the date value
         */
        public Date getDateValue() {
            return dateValue;
        }

        /**
         * Gets the object value.
         *
         * @return the object value
         */
        public Object getObjectValue() {
            return objectValue;
        }

        /**
         * Always returns false
         *
         * @param o the object to compare to
         */
        public boolean equals(Object o) {
            return false;
        }
    }


}