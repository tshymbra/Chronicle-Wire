/*
 * Copyright 2016 higherfrequencytrading.com
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
package net.openhft.chronicle.wire;

import java.io.File;
import java.nio.file.Files;

import net.openhft.chronicle.bytes.MappedBytes;
import net.openhft.chronicle.bytes.MappedFile;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author lburgazzoli
 */
public class WireResourcesTest {

    @Test
    public void testMappedBytesClose() throws Exception {
        File tmp = Files.createTempFile("chronicle-", ".wire").toFile();
        tmp.deleteOnExit();

        MappedBytes mb0;
        try (MappedBytes mb = MappedBytes.mappedBytes(tmp, 64 * 1024)) {
            assertEquals(1, mb.mappedFile().refCount());
            assertEquals(1, mb.refCount());

            Wire wire = WireType.TEXT.apply(mb);

            assert wire.startUse();
            wire.headerNumber(0);

            assertEquals(1, mb.mappedFile().refCount());
            wire.writeFirstHeader(); // first touches the file.
            assertEquals(2, mb.mappedFile().refCount());

            wire.updateFirstHeader();
            assert wire.endUse();

            assertEquals(2, mb.mappedFile().refCount());
            assertEquals(1, mb.refCount());

            mb0 = mb;
        }
        assertEquals(0, mb0.mappedFile().refCount());
        assertEquals(0, mb0.refCount());
    }

    @Test
    public void testMappedBytesWireRelease() throws Exception {
        File tmp = Files.createTempFile("chronicle-", ".wire").toFile();
        tmp.deleteOnExit();

        Wire wire;
        try (MappedBytes mb = MappedBytes.mappedBytes(tmp, 64 * 1024)) {
            assertEquals(1, mb.mappedFile().refCount());
            assertEquals(1, mb.refCount());

            wire = WireType.TEXT.apply(mb);
            wire.bytes().reserve();

            assertEquals(1, mb.mappedFile().refCount());
            assertEquals(2, mb.refCount());
        } // not really closed as we have one reverse left.

        assertEquals(1, wire.bytes().refCount());

        assert wire.startUse();
        wire.headerNumber(1);
        wire.writeFirstHeader();
        wire.updateFirstHeader();
        assert wire.endUse();

        wire.bytes().release();
        assertEquals(0, wire.bytes().refCount());
    }

    @Ignore("mh!!")
    @Test
    public void testMappedBytesWireRelease2() throws Exception {
        File tmp = Files.createTempFile("chronicle-", ".wire").toFile();
        tmp.deleteOnExit();

        Wire wire = WireType.TEXT.apply(MappedBytes.mappedBytes(tmp, 64 * 1024));

        assert wire.startUse();
        wire.headerNumber(1);
        wire.writeFirstHeader();
        wire.updateFirstHeader();

        assertEquals(1, wire.bytes().refCount());
        assertEquals(2, mappedFile(wire).refCount()); // Shouldn't be 1 ?

        assert wire.endUse();

        wire.bytes().release();
        assertEquals(0, wire.bytes().refCount());
        assertEquals(0, mappedFile(wire).refCount()); // Shouldn't be 1 ?
    }

    protected MappedFile mappedFile(Wire wire) {
        return ((MappedBytes)wire.bytes()).mappedFile();
    }
}
