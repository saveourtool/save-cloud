package com.saveourtool.save.frontend.utils

import com.saveourtool.save.frontend.App
import com.saveourtool.save.frontend.externals.findByTextAndCast
import com.saveourtool.save.frontend.externals.render
import com.saveourtool.save.frontend.externals.screen
import react.create
import react.react
import web.html.HTMLHeadingElement
import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ParsePositionStringTest {
    @Test
    @Suppress("DestructuringDeclarationWithTooManyEntries")
    fun shouldParseValidString(): Promise<Unit> {
        return Promise.resolve("Test File Name (1:2-3:4)")
            .then { positionString ->
                val positionList = positionString.parsePositionString()!!
                val (startRow, startCol, endRow, endCol) = positionList
                assertEquals(1, startRow)
                assertEquals(2, startCol)
                assertEquals(3, endRow)
                assertEquals(4, endCol)
            }
    }

    @Test
    fun shouldNotParseInvalidString(): Promise<Unit> {
        return Promise.resolve(")(")
            .then { positionString -> assertNull(positionString.parsePositionString()) }
    }

    @Test
    fun shouldNotParseAnotherInvalidString(): Promise<Unit> {
        return Promise.resolve("(1-1:2)")
            .then { positionString -> assertNull(positionString.parsePositionString()) }
    }
}
