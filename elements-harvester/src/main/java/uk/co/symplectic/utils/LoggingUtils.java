/*
 * ******************************************************************************
 *   Copyright (c) 2019 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 *   Version :  ${git.branch}:${git.commit.id}
 * ******************************************************************************
 */

package uk.co.symplectic.utils;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

/**
 * Simple utility class designed to ease instantiation of the logback based logging framework.
 * It offers one method (initialise) that will attempt to initialise the loggers using the named file,
 * falling back to look for files named logback-test.xml and logback.xml in the application's classpath.
 */
public class LoggingUtils {

    public static class LoggingInitialisationException extends Exception{
        LoggingInitialisationException(Throwable error) {
            super("Failed to initialise logging framework", error);
        }
    }

    public static void initialise(String loggerFileName) throws LoggingInitialisationException {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        JoranConfigurator jc = new JoranConfigurator();
        jc.setContext(context);

        InputStream inputStream = null;
        try {
            try {

                if (loggerFileName != null) {
                    inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(loggerFileName);
                }
                if (inputStream == null) {
                    inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("logback-test.xml");
                }
                if (inputStream == null) {
                    inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("logback.xml");
                }

                if (inputStream != null) {
                    context.reset();
                    context.stop();
                    jc.doConfigure(inputStream);
                    context.start();
                }
            } finally {
                if (inputStream != null) inputStream.close();
            }
        } catch (Exception e) {
            throw new LoggingInitialisationException(e);
        }
    }
}
