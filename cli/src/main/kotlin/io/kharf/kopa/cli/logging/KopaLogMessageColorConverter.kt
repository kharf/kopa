package io.kharf.kopa.cli.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.pattern.color.ANSIConstants.BOLD
import ch.qos.logback.core.pattern.color.ANSIConstants.DEFAULT_FG
import ch.qos.logback.core.pattern.color.ANSIConstants.GREEN_FG
import ch.qos.logback.core.pattern.color.ANSIConstants.RED_FG
import ch.qos.logback.core.pattern.color.ForegroundCompositeConverterBase

class KopaLogMessageColorConverter : ForegroundCompositeConverterBase<ILoggingEvent>() {
    override fun getForegroundColorCode(event: ILoggingEvent?): String {
        return when {
            event!!.message.contains("BUILD SUCCESSFUL") -> BOLD + GREEN_FG
            event!!.message.contains("INIT SUCCESSFUL") -> BOLD + GREEN_FG
            event.message.contains("BUILD ERROR") -> BOLD + RED_FG
            else -> DEFAULT_FG
        }
    }
}
