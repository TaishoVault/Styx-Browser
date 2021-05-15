package com.jamal2367.styx.search.engine

import com.jamal2367.styx.R
import com.jamal2367.styx.preference.UserPreferences

/**
 * A custom search engine.
 */
class CustomSearch(queryUrl: String, userPreferences: UserPreferences) : BaseSearchEngine(
    userPreferences.imageUrlString,
    queryUrl,
    R.string.search_engine_custom
)
