/*
 * Copyright © 2020-2021 Jamal Rothfuchs
 * Copyright © 2020-2021 Stéphane Lenclud
 * Copyright © 2015 Anthony Restaino
 */

package com.jamal2367.styx.view.find


/**
 * Defines interactions with the result of a find in page action.
 */
interface FindResults {

    /**
     * Select the next result of the find in page results. If [clearResults] has been called then
     * has no effect. If the last result is selected, then there will be no effect.
     */
    fun nextResult()

    /**
     * Select the previous result in the find in page results. If [clearResults] has been called
     * then has no effect. If the first result is selected, then there will be no effect.
     */
    fun previousResult()

    /**
     * Clear the find in page results.
     */
    fun clearResults()

}
