/*
 * Copyright © 2020-2021 Jamal Rothfuchs
 * Copyright © 2020-2021 Stéphane Lenclud
 * Copyright © 2015 Anthony Restaino
 */

package com.jamal2367.styx.database.adblock

import com.jamal2367.styx.adblock.UnifiedFilterResponse
import io.reactivex.Completable

/**
 * A repository that stores [Host].
 */
interface UserRulesRepository {

    fun addRules(rules: List<UnifiedFilterResponse>)

    /**
     * Remove all hosts in the repository.
     *
     * @return A [Completable] that completes when the removal finishes.
     */
    fun removeAllRules(): Completable

    // better use sequence or completable, but currently whatever
    //  sequence is not faster (tested), so no use

    fun removeRule(rule: UnifiedFilterResponse)

    // actually not needed, better done using only userFilterContainer
    //fun getRulesForPage(page: String): List<UnifiedFilterResponse>

    fun getAllRules(): List<UnifiedFilterResponse>

    companion object {
        const val RESPONSE_BLOCK = 1
        const val RESPONSE_NOOP = 0
        const val RESPONSE_EXCLUSION = -1
    }
}
