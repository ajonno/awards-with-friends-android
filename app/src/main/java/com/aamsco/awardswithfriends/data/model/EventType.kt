package com.aamsco.awardswithfriends.data.model

enum class EventType(val value: String, val displayName: String) {
    OSCARS("oscars", "Academy Awards"),
    EMMYS("emmys", "Emmy Awards"),
    GOLDEN_GLOBES("goldenglobes", "Golden Globe Awards"),
    GRAMMYS("grammys", "Grammy Awards"),
    TONYS("tonys", "Tony Awards"),
    SAG_AWARDS("sagawards", "SAG Awards"),
    BAFTAS("baftas", "BAFTA Awards"),
    OTHER("other", "Other");

    companion object {
        fun fromString(value: String): EventType {
            return entries.find { it.value == value } ?: OTHER
        }
    }
}
