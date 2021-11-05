/*
 * Copyright © 2020-2021 Jamal Rothfuchs
 * Copyright © 2020-2021 Stéphane Lenclud
 * Copyright © 2015 Anthony Restaino
 */

package com.jamal2367.styx.js

import com.anthonycr.mezzanine.FileStream

/**
 * Set HTML meta viewport thus enabling desktop mode or other zoom trick.
 */
@FileStream("app/src/main/js/SetMetaViewport.js")
interface SetMetaViewport {

    fun provideJs(): String

}