package com.jamal2367.styx.adblock

import com.jamal2367.styx.preference.IntEnum

/**
 * An enum representing the browser's available rendering modes.
 */
enum class AbpUpdateMode(override val value: Int) : IntEnum {
    ALWAYS(0),
    NONE(1),
    WIFI_ONLY(2)
}
