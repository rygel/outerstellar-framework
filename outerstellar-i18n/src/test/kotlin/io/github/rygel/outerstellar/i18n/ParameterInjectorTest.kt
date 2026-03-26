package io.github.rygel.outerstellar.i18n

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

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

    @Test
    fun `malformed placeholder with non-numeric index is preserved`() {
        val result = ParameterInjector.inject("Hello {abc}", "World")
        assertEquals("Hello {abc}", result)
    }

    @Test
    fun `negative index placeholder is preserved`() {
        val result = ParameterInjector.inject("Hello {-1}", "World")
        assertEquals("Hello {-1}", result)
    }

    @Test
    fun `out-of-bounds index is preserved`() {
        val result = ParameterInjector.inject("Hello {100}", "World")
        assertEquals("Hello {100}", result)
    }

    @Test
    fun `unclosed brace is preserved`() {
        val result = ParameterInjector.inject("Hello {0", "World")
        assertEquals("Hello {0", result)
    }

    @Test
    fun `empty template returns empty string`() {
        val result = ParameterInjector.inject("", "unused")
        assertEquals("", result)
    }

    @Test
    fun `empty params returns template unchanged`() {
        val result = ParameterInjector.inject("Hello {0}")
        assertEquals("Hello {0}", result)
    }

    @Test
    fun `unicode characters in parameters`() {
        val result = ParameterInjector.inject("Grüße {0}", "Wörld")
        assertEquals("Grüße Wörld", result)
    }

    @Test
    fun `repeated placeholder uses same parameter`() {
        val result = ParameterInjector.inject("{0} and {0}", "X")
        assertEquals("X and X", result)
    }

    @Test
    fun `list overload works same as vararg`() {
        val result = ParameterInjector.inject("{0} + {1}", listOf("A", "B"))
        assertEquals("A + B", result)
    }
}
