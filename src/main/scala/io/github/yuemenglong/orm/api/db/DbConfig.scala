package io.github.yuemenglong.orm.api.db

import com.jolbox.bonecp.BoneCP

/**
 * Created by Administrator on 2017/5/16.
 */
trait DbConfig {
  def initPool(): BoneCP

  def setPoolArgs(min: Int, max: Int, partition: Int): DbConfig

  def setIsolation(isolation: Int): DbConfig

  def username: String

  def password: String

  def db: String

  def url: String
}

