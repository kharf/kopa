package io.kharf.kopa.core.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.pattern.color.ANSIConstants.BOLD
import ch.qos.logback.core.pattern.color.ANSIConstants.DEFAULT_FG
import ch.qos.logback.core.pattern.color.ANSIConstants.GREEN_FG
import ch.qos.logback.core.pattern.color.ForegroundCompositeConverterBase

class KopaLogMessageColorConverter : ForegroundCompositeConverterBase<ILoggingEvent>() {
    override fun getForegroundColorCode(event: ILoggingEvent?): String {
        return if (event!!.message.contains("SUCCESSFULLY")) BOLD + GREEN_FG else DEFAULT_FG
    }
}
