/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.oak.benchmark;

import com.google.common.base.Joiner;
import org.apache.commons.lang.ArrayUtils;

import java.io.PrintStream;
import java.util.Formatter;

public class MachineReadableConsoleStrategy extends AbstractOutputStrategy implements BenchmarkOutputStrategy {
    private final static String COMMENT_PATTERN = "#%s";
    private final PrintStream out;

    public MachineReadableConsoleStrategy(PrintStream out) {
        this.out = out;
    }

    @Override
    public void printHeader(AbstractTest test) {}

    private String getStatsFormatsJoined(AbstractTest test) {
        String comment = test.comment();
        String[] formatPattern =test.statsFormats();
        if (comment != null){
            formatPattern = (String[])ArrayUtils.add(formatPattern, COMMENT_PATTERN);
        }
        Joiner joiner = Joiner.on(',');
        return formatPattern.length > 0 ? joiner.join(formatPattern) : null;
    }

    @Override
    public void printStats(AbstractTest test) {
        String concatenatedFormat = Joiner.on(",").skipNulls().join("%s,%s,%d,%.0f,%.0f,%.0f,%.0f,%.0f,%d", getStatsFormatsJoined(test));
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);
        Object[] nameAndStats = ArrayUtils.addAll(new Object[]{test.toString()}, getAllStatsJoined(test));
        formatter.format(concatenatedFormat, nameAndStats);
        out.println(sb.toString().replaceAll(", *", ","));
    }
}
