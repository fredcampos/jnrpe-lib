/*
 * Copyright (c) 2013 Massimiliano Ziccardi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.jnrpe.utils.thresholds;

/**
 * Parses a negative infinity (-inf). The '-' is optional: if the infinity is at
 * the left side of the range, than it is assumed to be negative infinity. See
 * {@link http://nagiosplugins.org/rfc/new_threshold_syntax}
 *
 * Example Input : -inf..100
 *
 * Produced Output : ..100 and calls the
 * {@link RangeConfig#setNegativeInfinity(boolean)} passing
 * <code>true</code>
 *
 * @author Massimiliano Ziccardi
 */
class NegativeInfinityStage extends Stage {

    /**
     * The infinity sign.
     */
    private static final  String INFINITY = "inf";

    /**
     * The negative infinity sign.
     */
    private static final String NEG_INFINITY = "-inf";
    /**
     *
     */
    protected NegativeInfinityStage() {
        super("negativeinfinity");
    }

    /**
     * Parses the threshold to remove the matched '-inf' or 'inf' string.
     *
     * @param threshold
     *            The threshold chunk to be parsed
     * @param tc
     *            The threshold config object. This object will be populated
     *            according to the passed in threshold.
     * @see RangeConfig#setNegativeInfinity(boolean)
     * @return the remaining part of the threshold
     */
    @Override
    public String parse(final String threshold, final RangeConfig tc) {

        if (canParse(threshold)) {
            tc.setNegativeInfinity(true);

            if (threshold.startsWith(INFINITY)) {
                return threshold.substring(INFINITY.length());
            }

            if (threshold.startsWith(NEG_INFINITY)) {
                return threshold.substring(NEG_INFINITY.length());
            }
        }

        return threshold;
    }

    /**
     * Tells the parser if this stage is able to parse the current remaining
     * threshold part.
     *
     * @param threshold
     *            The threshold part to be parsed.
     * @return <code>true</code> if this object can consume a part of the
     *         threshold
     */
    @Override
    public boolean canParse(final String threshold) {
        return threshold.startsWith("inf") || threshold.startsWith("-inf");
    }

    /**
     * This method is used to generate the exception message.
     *
     * @return the token that this stage is waiting for.
     */
    @Override
    public String expects() {
        return "[-]inf";
    }
}
