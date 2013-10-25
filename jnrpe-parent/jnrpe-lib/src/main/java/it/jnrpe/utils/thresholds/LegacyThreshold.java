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

import it.jnrpe.Status;

import java.math.BigDecimal;

/**
 * This class represent a parser/evaluator for the old threshold syntax.
 *
 * @author Massimiliano Ziccardi
 */
public class LegacyThreshold implements IThreshold {
    /**
     * The range for an OK status.
     */
    private final LegacyRange okRange;
    /**
     * The range for a Warning status.
     */
    private final LegacyRange warnRange;
    /**
     * The range for a critical status.
     */
    private final LegacyRange critRange;
    /**
     * The metric associated with this threshold specification.
     */
    private final String metricName;

    /**
     * Builds and initializes the threshold object.
     *
     * @param metric
     *            The metric associated with this threshold.
     * @param ok
     *            The ok range.
     * @param warn
     *            The warning range.
     * @param crit
     *            The critical range.
     */
    public LegacyThreshold(final String metric, final LegacyRange ok,
            final LegacyRange warn, final LegacyRange crit) {
        okRange = ok;
        warnRange = warn;
        critRange = crit;
        metricName = metric;
    }

    /**
     * Evaluates the value agains the specified ranges.
     * The followed flow is:
     * <ol>
     * <li>If a critical range is defined and the value falls inside the
     * specified range, a {@link Status#CRITICAL} is returned.
     * <li>If a warning range is defined and the value falls inside the
     * specified range, a {@link Status#WARNING} is returned.
     * <li>If a OK range is defined and the value falls inside the
     * specified range, a {@link Status#OK} is returned.
     * <li>If the OK range is not specified, a {@link Status#CRITICAL} is
     * returned
     * <li>{@link Status#OK} is returned
     * </ol>
     * @param value The value to be evaluated.
     * @return the evaluated status.
     */
    public final Status evaluate(final BigDecimal value) {
        if (critRange != null && critRange.isValueInside(value)) {
            return Status.CRITICAL;
        }
        if (warnRange != null && warnRange.isValueInside(value)) {
            return Status.WARNING;
        }
        if (okRange != null) {
            if (okRange.isValueInside(value)) {
                return Status.OK;
            } else {
                return Status.CRITICAL;
            }
        }

        return Status.OK;
    }

    /**
     * @param metric The metric we want to evaluate.
     * @return wether this threshold is about the passed in metric.
     */
    public final boolean isAboutMetric(final String metric) {
        return metricName.equalsIgnoreCase(metric);
    }

    /**
     * The metric referred by this threshold.
     * @return the metric name.
     */
    public final String getMetric() {
        return metricName;
    }

    /**
     * @param status the range we are interested in.
     * @return the requested unparsed range string.
     */
    public final String getRangesAsString(final Status status) {
        switch (status) {
        case OK:
            if (okRange != null) {
                return okRange.getThresholdString();
            }
            break;
        case WARNING:
            if (warnRange != null) {
                return warnRange.getThresholdString();
            }
            break;
        case CRITICAL:
            if (critRange != null) {
                return critRange.getThresholdString();
            }
            break;
        default:
        }

        return null;
    }

    /**
     * @return the unit of measure as string.
     */
    public final String getUnitString() {
        return null;
    }
}
