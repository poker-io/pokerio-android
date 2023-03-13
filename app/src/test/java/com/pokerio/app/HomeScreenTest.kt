package com.pokerio.app

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class HomeScreenTest {
    @Test
    fun testGetAnimatedString() {
        assertEquals("Jetpack Compose", getAnimatedString())
    }
}
