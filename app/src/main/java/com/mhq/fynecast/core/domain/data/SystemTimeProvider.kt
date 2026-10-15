package com.mhq.fynecast.core.domain.data

import com.mhq.fynecast.core.domain.time.TimeProvider

class SystemTimeProvider : TimeProvider {
    override fun nowMillis() = System.currentTimeMillis()
}