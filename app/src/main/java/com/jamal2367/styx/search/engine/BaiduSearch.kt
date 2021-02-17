package com.jamal2367.styx.search.engine

import com.jamal2367.styx.R

/**
 * The Baidu search engine.
 */
class BaiduSearch : BaseSearchEngine(
    "file:///android_asset/baidu.png",
    "https://www.baidu.com/s?wd=",
    R.string.search_engine_baidu
)
