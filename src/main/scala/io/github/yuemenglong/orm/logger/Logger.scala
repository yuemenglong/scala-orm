package io.github.yuemenglong.orm.logger

import org.slf4j.{Logger => SLFLogger, LoggerFactory}

/**
  * Created by Administrator on 2017/7/3.
  */
object Logger {
  val logger: SLFLogger = LoggerFactory.getLogger("[ORM]")
  var enable: Boolean = true

  def trace(msg: String, params: Object*): Unit = {
    if (enable) {
      logger.trace(msg, params)
    }
  }

  def debug(msg: String, params: Object*): Unit = {
    if (enable) {
      logger.debug(msg, params)
    }
  }

  def info(msg: String, params: Object*): Unit = {
    if (enable) {
      logger.info(msg, params)
    }
  }

  def warn(msg: String, params: Object*): Unit = {
    if (enable) {
      logger.warn(msg, params)
    }
  }

  def error(msg: String, params: Object*): Unit = {
    if (enable) {
      logger.error(msg, params)
    }
  }

  def error(msg: String, e: Throwable): Unit = {
    if (enable) {
      logger.error(msg, e)
    }
  }

  def setEnable(b: Boolean): Unit = {
    enable = b
  }
}

