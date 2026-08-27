package com.daaass142.smsstrongalert

object SmsMatcher {
    private val parkingKeywords = listOf(
        "违停",
        "违法停车",
        "违法停放",
        "违规停车",
        "请立即驶离",
        "立即驶离",
        "尽快驶离",
        "请移车",
        "及时移车",
        "临时停车",
        "占道停车",
    )

    data class Result(
        val matched: Boolean,
        val matchedPlate: String? = null,
        val matchedKeyword: String? = null,
    )

    fun match(body: String, plates: List<String>): Result {
        if (body.isBlank()) return Result(false)

        val matchedKeyword = parkingKeywords.firstOrNull(body::contains)
            ?: return Result(false)

        if (plates.isEmpty()) {
            return Result(true, matchedKeyword = matchedKeyword)
        }

        val normalizedBody = normalizeForPlateSearch(body)
        val matchedPlate = plates.firstOrNull { plate ->
            val normalizedPlate = normalizePlate(plate)
            normalizedPlate.isNotEmpty() && normalizedBody.contains(normalizedPlate)
        } ?: return Result(false)

        return Result(
            matched = true,
            matchedPlate = matchedPlate,
            matchedKeyword = matchedKeyword,
        )
    }

    internal fun normalizePlate(value: String): String =
        value
            .uppercase()
            .replace(Regex("[\\s·•-]"), "")
            .trim()

    private fun normalizeForPlateSearch(value: String): String = normalizePlate(value)
}
