package io.github.rygel.outerstellar.i18n

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ParameterInjectorTest {

    @Test
    fun testInjectWithSingleStringParam() {
        val result = ParameterInjector.inject("Hello {0}", "World")
        assertEquals("Hello World", result)
    }

    @Test
    fun testInjectWithMultipleParams() {
        val result = ParameterInjector.inject("{0} is {1} years old", "John", 30)
        assertEquals("John is 30 years old", result)
    }

    @Test
    fun testInjectWithIntegerParams() {
        val result = ParameterInjector.inject("Count: {0}, {1}, {2}", 1, 2, 3)
        assertEquals("Count: 1, 2, 3", result)
    }

    @Test
    fun testInjectWithNoParams() {
        val result = ParameterInjector.inject("Hello World", "unused")
        assertEquals("Hello World", result)
    }

    @Test
    fun testInjectSkipsMissingIndices() {
        val result = ParameterInjector.inject("{0} and {2}", "X", "Y", "Z")
        assertEquals("X and Z", result)
    }
}
