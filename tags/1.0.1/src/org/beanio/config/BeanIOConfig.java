/*
 * Copyright 2010-2011 Kevin Seim
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.beanio.config;

import java.util.*;

/**
 * Stores BeanIO stream mapping configuration settings.
 * 
 * @author Kevin Seim
 * @since 1.0
 */
public class BeanIOConfig {

    private List<StreamConfig> streamList = new ArrayList<StreamConfig>();
    private List<TypeHandlerConfig> handlerList = new ArrayList<TypeHandlerConfig>();

    /**
     * Constructs a new <tt>BeanIOConfig</tt>.
     */
    public BeanIOConfig() { }

    /**
     * Adds a stream mapping configuration to this configuration.
     * @param stream the stream mapping configuration
     */
    public void addStream(StreamConfig stream) {
        streamList.add(stream);
    }

    /**
     * Returns the list of stream mappings for this configuration.
     * @return list of stream mapping configurations
     */
    public List<StreamConfig> getStreamList() {
        return streamList;
    }

    /**
     * Adds a custom type handler to this configuration.
     * @param handler the type handler configuration
     */
    public void addTypeHandler(TypeHandlerConfig handler) {
        handlerList.add(handler);
    }

    /**
     * Returns the list of custom type handlers for this configuration.
     * @return list of custom type handlers
     */
    public List<TypeHandlerConfig> getTypeHandlerList() {
        return handlerList;
    }
}