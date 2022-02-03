@file:Suppress("unused")

package com.mrx

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.core.config.ConfigurationFactory
import org.apache.logging.log4j.core.config.Configurator
import kotlin.reflect.KClass

/**
 * 使用代码配置 Log4j2
 */
object XLog {

    init {
        val builder = ConfigurationFactory.newConfigurationBuilder()
        val console = builder.newAppender("stdout", "Console")
        val std = builder.newLayout("PatternLayout").apply {
            addAttribute(
                "pattern",
                "%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %logger{36} %highlight{%-5level}: %msg%n%throwable"
            )
        }
        console.add(std)
        builder.add(console)
        builder.add(builder.newRootLogger(Level.WARN).apply { add(builder.newAppenderRef("stdout")) })
        Configurator.initialize(builder.build())
    }

    fun getLogger(name: String): Logger = LogManager.getLogger(name)

    fun getLogger(kClass: KClass<*>): Logger = LogManager.getLogger(kClass.qualifiedName)

}