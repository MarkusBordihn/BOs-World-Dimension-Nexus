/*
 * Copyright 2025 Markus Bordihn
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.markusbordihn.worlddimensionnexus.utils;

import de.markusbordihn.worlddimensionnexus.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ModLogger {

  private ModLogger() {}

  public static PrefixLogger getPrefixLogger(final String prefix) {
    return new PrefixLogger(LogManager.getLogger(Constants.LOG_NAME), "[" + prefix + "]");
  }

  public static final class PrefixLogger {
    private final Logger log;
    private final String logPrefix;

    private PrefixLogger(final Logger log, final String logPrefix) {
      this.log = log;
      this.logPrefix = logPrefix;
    }

    public void info(final String message, final Object... params) {
      if (log.isInfoEnabled()) {
        log.info("{} " + message, withPrefix(params));
      }
    }

    public void debug(final String message, final Object... params) {
      if (log.isDebugEnabled()) {
        log.debug("{} " + message, withPrefix(params));
      }
    }

    public void error(final String message, final Object... params) {
      if (log.isErrorEnabled()) {
        log.error("{} " + message, withPrefix(params));
      }
    }

    public void warn(final String message, final Object... params) {
      if (log.isWarnEnabled()) {
        log.warn("{} " + message, withPrefix(params));
      }
    }

    private Object[] withPrefix(final Object... params) {
      Object[] all = new Object[params.length + 1];
      all[0] = this.logPrefix;
      System.arraycopy(params, 0, all, 1, params.length);
      return all;
    }
  }
}
