package com.daaass142.smsstrongalert

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsMatcherTest {
    @Test
    fun matchesParkingKeywordAndSavedPlate() {
        val result = SmsMatcher.match(
            "【交警】您的小型汽车粤E12345违法停车，请立即驶离。",
            listOf("粤E12345"),
        )
        assertTrue(result.matched)
        assertEquals("粤E12345", result.matchedPlate)
    }

    @Test
    fun rejectsOtherVehicleWhenPlateListExists() {
        val result = SmsMatcher.match(
            "您的车辆粤A99999违法停车，请立即驶离。",
            listOf("粤E12345"),
        )
        assertFalse(result.matched)
    }

    @Test
    fun keywordOnlyModeWorksWithoutSavedPlate() {
        val result = SmsMatcher.match(
            "检测到违法停放，请及时移车。",
            emptyList(),
        )
        assertTrue(result.matched)
    }

    @Test
    fun ignoresOrdinarySms() {
        val result = SmsMatcher.match(
            "您的验证码是 123456，请勿泄露。",
            emptyList(),
        )
        assertFalse(result.matched)
    }
}
