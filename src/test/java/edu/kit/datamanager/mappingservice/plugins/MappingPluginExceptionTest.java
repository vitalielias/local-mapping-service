/*
 * Copyright 2022 Karlsruhe Institute of Technology.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.kit.datamanager.mappingservice.plugins;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MappingPluginExceptionTest {

    @Test
    void testConstructor() {
        MappingPluginException ex = new MappingPluginException(MappingPluginState.UNKNOWN_ERROR);
        assertEquals(MappingPluginState.UNKNOWN_ERROR, ex.getState());
        assertEquals(MappingPluginState.UNKNOWN_ERROR.toString(), ex.getMessage());
        assertNull(ex.getCause());

        ex = new MappingPluginException(MappingPluginState.UNKNOWN_ERROR, "test");
        assertEquals(MappingPluginState.UNKNOWN_ERROR, ex.getState());
        assertEquals("test", ex.getMessage());
        assertNull(ex.getCause());

        ex = new MappingPluginException(MappingPluginState.UNKNOWN_ERROR, "test", new Exception());
        assertEquals(MappingPluginState.UNKNOWN_ERROR, ex.getState());
        assertEquals("test", ex.getMessage());
        assertNotNull(ex.getCause());
    }

}