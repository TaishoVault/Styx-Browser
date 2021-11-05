/*
 * Copyright © 2020-2021 Jamal Rothfuchs
 * Copyright © 2020-2021 Stéphane Lenclud
 * Copyright © 2015 Anthony Restaino
 */

package com.jamal2367.styx.browser.sessions

import android.os.Parcel
import android.os.Parcelable

/**
 * You can easily regenerate that parcelable implementation.
 * See: https://stackoverflow.com/a/49426012/3969362
 */
data class Session (
    var name: String = "",
    var tabCount: Int = -1,
    var isCurrent: Boolean = false
) : Parcelable {
    constructor(parcel: Parcel) : this() {
        name = parcel.readString() ?: ""
        tabCount = parcel.readInt()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeInt(tabCount)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Session> {
        override fun createFromParcel(parcel: Parcel): Session {
            return Session(parcel)
        }

        override fun newArray(size: Int): Array<Session?> {
            return arrayOfNulls(size)
        }
    }
}