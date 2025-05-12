package org.wspcgir.rave_giraffe

import org.junit.Test

import org.junit.Assert.*
import org.wspcgir.rave_giraffe.lib.dtFormat
import java.time.LocalDateTime

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun date_parsing() {
        LocalDateTime.parse("2025-05-06 19:00", dtFormat)
    }
}