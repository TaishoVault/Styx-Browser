package com.jamal2367.styx.settings

import com.jamal2367.styx.preference.IntEnum

/**
 * An enum representing what detail level should be displayed in the search box.
 */
enum class NewTabPosition(override val value: Int) : IntEnum {
    AFTER_CURRENT_TAB(0),
    START_OF_TAB_LIST(1),
    END_OF_TAB_LIST(2)
}
