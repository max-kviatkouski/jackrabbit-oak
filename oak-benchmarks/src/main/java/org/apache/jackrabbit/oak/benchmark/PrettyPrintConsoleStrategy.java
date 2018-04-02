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

public class PrettyPrintConsoleStrategy implements BenchmarkOutputStrategy {
    private AbstractTest test;

    public PrettyPrintConsoleStrategy(AbstractTest test) {
        this.test = test;
    }

    @Override
    public void printHeader() {
        System.out.format(
                "# %-26.26s       C     min     10%%     50%%     90%%     max       N%s%n",
                test.toString(), getStatsNamesJoined());
    }

    private String getStatsNamesJoined() {
        return Joiner.on("  ").join(test.statsNames());
    }

    private String getStatsFormatsJoined() {
        String comment = test.comment();
        String[] formatPattern = test.statsFormats();
        if (comment != null){
            String commentPattern = "    #%s";
            formatPattern = (String[]) ArrayUtils.add(formatPattern, commentPattern);
        }
        return Joiner.on("  ").join(formatPattern);
    }

    @Override
    public void printStats(Object[] stats) {
        System.out.format(
                "%-28.28s  %6d  %6.0f  %6.0f  %6.0f  %6.0f  %6.0f  %6d"+getStatsFormatsJoined()+"%n",
                stats);
    }
}
