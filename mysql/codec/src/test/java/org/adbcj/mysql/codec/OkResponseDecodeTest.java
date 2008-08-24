package org.adbcj.mysql.codec;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.EnumSet;

import org.adbcj.mysql.codec.MySqlClientDecoder.State;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class OkResponseDecodeTest {

	// Packet length: 48
	// Packet number: 1
	// Affected rows: 0
	// Server Status: AUTO_COMMIT, NO_INDEX_USED
	// Warnings: 0
	// Message: "(Rows matched: 0  Changed: 0  Warnings: 0"
	private final byte[] OK_RESPONSE_WITH_MESSAGE = {
		(byte)0x30, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x22, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x28, (byte)0x52, (byte)0x6f, (byte)0x77, (byte)0x73,
		(byte)0x20, (byte)0x6d, (byte)0x61, (byte)0x74, (byte)0x63, (byte)0x68, (byte)0x65, (byte)0x64, (byte)0x3a, (byte)0x20, (byte)0x30, (byte)0x20, (byte)0x20, (byte)0x43, (byte)0x68, (byte)0x61,
		(byte)0x6e, (byte)0x67, (byte)0x65, (byte)0x64, (byte)0x3a, (byte)0x20, (byte)0x30, (byte)0x20, (byte)0x20, (byte)0x57, (byte)0x61, (byte)0x72, (byte)0x6e, (byte)0x69, (byte)0x6e, (byte)0x67,
		(byte)0x73, (byte)0x3a, (byte)0x20, (byte)0x30
	};

	@Test
	public void okRepsonseWithMessage() throws Exception {
		InputStream in = new ByteArrayInputStream(OK_RESPONSE_WITH_MESSAGE);
		MySqlClientDecoder decoder = new MySqlClientDecoder();
		decoder.setState(State.RESPONSE);
		OkResponse response = (OkResponse) decoder.decode(in, true);

		assertEquals(response.getPacketLength(), 48);
		assertEquals(response.getPacketNumber(), 1);
		assertEquals(response.getAffectedRows(), 0);
		assertEquals(response.getServerStatus(), EnumSet.of(ServerStatus.AUTO_COMMIT, ServerStatus.NO_INDEX));
		assertEquals(response.getMessage(), "(Rows matched: 0  Changed: 0  Warnings: 0");
	}

	// Packet length: 7
	// Packet number: 1
	// Affected rows: 1
	// Server Status: AUTO_COMMIT
	// Warnings: 0
	// Message: ""
	private byte[] OK_RESPONSE_ONE_AFFECTED_ROW = {
		(byte)0x07, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x02, (byte)0x00, (byte)0x00, (byte)0x00
	};

	@Test
	public void okResponseOneAffectedRow() throws Exception {
		InputStream in = new ByteArrayInputStream(OK_RESPONSE_ONE_AFFECTED_ROW);
		MySqlClientDecoder decoder = new MySqlClientDecoder();
		decoder.setState(State.RESPONSE);
		OkResponse response = (OkResponse) decoder.decode(in, true);

		assertEquals(response.getPacketLength(), 7);
		assertEquals(response.getPacketNumber(), 1);
		assertEquals(response.getAffectedRows(), 1);
		assertEquals(response.getServerStatus(), EnumSet.of(ServerStatus.AUTO_COMMIT));
		assertEquals(response.getMessage(), "");
	}

}
