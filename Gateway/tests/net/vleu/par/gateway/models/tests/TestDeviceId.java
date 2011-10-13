/*
 * Copyright ©2011 Brice Arnould
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.vleu.par.gateway.models.tests;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import net.vleu.par.gateway.models.DeviceId;

import org.junit.Test;

import com.google.appengine.repackaged.com.google.common.util.Base64DecoderException;

/**
 *
 */
public class TestDeviceId {

	private static final long[] SAMPLE_IDS = new long[] { 0, -1, 1,
			Long.MIN_VALUE, Long.MAX_VALUE, Long.MIN_VALUE / 2,
			Long.MAX_VALUE / 2 };

	/**
	 * Test method for
	 * {@link net.vleu.par.gateway.models.DeviceId#DeviceId(long)}.
	 */
	@Test
	public void testDeviceId() {
		for (final long l : SAMPLE_IDS) {
			final DeviceId id = new DeviceId(l);
			assertEquals(l, id.asLong);
		}
	}

	/**
	 * Test method for
	 * {@link net.vleu.par.gateway.models.DeviceId#equals(Object)}.
	 */
	@Test
	public void testEquals() {
		for (final long l : SAMPLE_IDS) {
			final DeviceId id1 = new DeviceId(l);
			final DeviceId id2 = new DeviceId(l);
			final DeviceId id3 = new DeviceId(l + 1);
			assertEquals(id1, id2);
			assertThat(id1, is(not(equalTo(id3))));
		}
	}

	/**
	 * Test method for
	 * {@link net.vleu.par.gateway.models.DeviceId#fromBase64(java.lang.String)}
	 * and {@link net.vleu.par.gateway.models.DeviceId#toBase64()}
	 */
	@Test
	public void testFromBase64() throws Base64DecoderException {
		for (final long l : SAMPLE_IDS) {
			final DeviceId origId = new DeviceId(l);
			final DeviceId decodedId = DeviceId.fromBase64(origId.toBase64());
			assertEquals(origId, decodedId);
		}
	}
}
