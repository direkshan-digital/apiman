/*
 * Copyright 2018 Pete Cornish
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.apiman.gateway.engine.hazelcast;

import com.hazelcast.config.Config;
import io.apiman.gateway.engine.hazelcast.common.HazelcastInstanceManager;
import io.apiman.gateway.engine.hazelcast.model.Example;
import io.apiman.gateway.engine.hazelcast.support.HazelcastConfigUtil;
import io.apiman.gateway.engine.impl.ByteBufferFactoryComponent;
import io.apiman.gateway.engine.io.ByteBuffer;
import io.apiman.gateway.engine.io.ISignalReadStream;
import io.apiman.gateway.engine.io.ISignalWriteStream;
import org.junit.Before;
import org.junit.Test;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Tests for {@link HazelcastCacheStoreComponent}.
 *
 * @author Pete Cornish
 */
public class HazelcastCacheStoreComponentTest {
    private HazelcastCacheStoreComponent component;

    @Before
    public void setUp() {
        final Config config = HazelcastConfigUtil.buildConfigWithDisabledNetwork();
        HazelcastInstanceManager.DEFAULT_MANAGER.setOverrideConfig(config);

        component = new HazelcastCacheStoreComponent(emptyMap());
        component.setBufferFactory(new ByteBufferFactoryComponent());
    }

    @Test
    public void putAndGetFromCluster() throws Exception {
        final String cacheKey = "cacheKeySimple";
        final Example cacheValue = new Example("cacheValue");

        component.put(cacheKey, cacheValue, 120);
        component.get(cacheKey, Example.class, result -> {
            assertFalse("Result should be retrieved successfully from initial member", result.isError());
            assertEquals(cacheValue, result.getResult());
        });
    }

    @Test
    public void putAndGetBinaryFromCluster() throws Exception {
        final String cacheKey = "cacheKeyBinary";
        final String cacheHead = "cacheHead";
        final String cacheBody = "cacheBody";

        final ISignalWriteStream writeStream = component.putBinary(cacheKey, cacheHead, 120);
        writeStream.write(new ByteBuffer(cacheBody));
        writeStream.end();

        component.getBinary(cacheKey, String.class, result -> {
            assertFalse("Result should be retrieved successfully from initial member", result.isError());

            final ISignalReadStream<String> readStream = result.getResult();
            readStream.bodyHandler(bodyResult -> assertEquals(cacheBody, new String(bodyResult.getBytes())));
            readStream.endHandler(endResult -> { /* no op */});
            readStream.transmit();

            assertEquals(cacheHead, readStream.getHead());
        });
    }
}
