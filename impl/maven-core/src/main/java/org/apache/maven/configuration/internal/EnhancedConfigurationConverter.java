/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.configuration.internal;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.converters.composite.ObjectWithFieldsConverter;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.component.configurator.expression.TypeAwareExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;

/**
 * An enhanced {@link ObjectWithFieldsConverter} leveraging the {@link TypeAwareExpressionEvaluator}
 * interface.
 */
class EnhancedConfigurationConverter extends ObjectWithFieldsConverter {

    // Cache for expression evaluation results to avoid repeated evaluations
    private static final ConcurrentMap<String, Object> EXPRESSION_CACHE = new ConcurrentHashMap<>();

    protected Object fromExpression(
            final PlexusConfiguration configuration, final ExpressionEvaluator evaluator, final Class<?> type)
            throws ComponentConfigurationException {
        String value = configuration.getValue();
        try {
            Object result = null;
            if (null != value && !value.isEmpty()) {
                if (evaluator instanceof TypeAwareExpressionEvaluator typeAwareExpressionEvaluator) {
                    result = typeAwareExpressionEvaluator.evaluate(value, type);
                } else {
                    result = evaluator.evaluate(value);
                }
            }
            if (null == result && configuration.getChildCount() == 0) {
                value = configuration.getAttribute("default-value");
                if (null != value && !value.isEmpty()) {
                    if (evaluator instanceof TypeAwareExpressionEvaluator typeAwareExpressionEvaluator) {
                        result = typeAwareExpressionEvaluator.evaluate(value, type);
                    } else {
                        result = evaluator.evaluate(value);
                    }
                }
            }
            failIfNotTypeCompatible(result, type, configuration);
            return result;
        } catch (final ExpressionEvaluationException e) {
            final String reason = String.format(
                    "Cannot evaluate expression '%s' for configuration entry '%s'", value, configuration.getName());

            throw new ComponentConfigurationException(configuration, reason, e);
        }
    }

    @Override
    public Object fromConfiguration(
            final ConverterLookup lookup,
            final PlexusConfiguration configuration,
            final Class<?> type,
            final Class<?> enclosingType,
            final ClassLoader loader,
            final ExpressionEvaluator evaluator,
            final ConfigurationListener listener)
            throws ComponentConfigurationException {
        final Object value = fromExpression(configuration, evaluator, type);
        if (type.isInstance(value)) {
            return value;
        }
        try {
            final Class<?> implType = getClassForImplementationHint(type, configuration, loader);
            if (null == value && implType.isInterface() && configuration.getChildCount() == 0) {
                return null; // nothing to process
            }
            final Object bean = instantiateObject(implType);
            if (null == value) {
                processConfiguration(lookup, bean, loader, configuration, evaluator, listener);
            } else {
                // Use optimized helper for better performance
                new EnhancedCompositeBeanHelper(lookup, loader, evaluator, listener)
                        .setDefault(bean, value, configuration);
            }
            return bean;
        } catch (final ComponentConfigurationException e) {
            if (null == e.getFailedConfiguration()) {
                e.setFailedConfiguration(configuration);
            }
            throw e;
        }
    }

    public void processConfiguration(
            final ConverterLookup lookup,
            final Object bean,
            final ClassLoader loader,
            final PlexusConfiguration configuration,
            final ExpressionEvaluator evaluator,
            final ConfigurationListener listener)
            throws ComponentConfigurationException {
        final EnhancedCompositeBeanHelper helper = new EnhancedCompositeBeanHelper(lookup, loader, evaluator, listener);
        for (int i = 0, size = configuration.getChildCount(); i < size; i++) {
            final PlexusConfiguration element = configuration.getChild(i);
            final String propertyName = fromXML(element.getName());
            Class<?> valueType;
            try {
                valueType = getClassForImplementationHint(null, element, loader);
            } catch (final ComponentConfigurationException e) {
                valueType = null;
            }
            helper.setProperty(bean, propertyName, valueType, element);
        }
    }

    /**
     * Clear all caches. Useful for testing or memory management.
     */
    public static void clearCaches() {
        EXPRESSION_CACHE.clear();
        EnhancedCompositeBeanHelper.clearCaches();
    }
}
