package org.adbcj.postgresql.codec.backend;

import org.testng.annotations.Test;
import org.testng.Assert;
import org.adbcj.postgresql.codec.DefaultConnectionState;
import org.adbcj.postgresql.codec.ConfigurationVariable;
import org.adbcj.postgresql.codec.PgField;
import org.adbcj.support.DecoderInputStream;
import org.adbcj.Type;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;

/**
 * @author Mike Heath
 */
public class BackendMessageDecoderTest {
	protected static int toDigit(char ch, int index) {
		int digit = Character.digit(ch, 16);
		if (digit == -1) {
			throw new IllegalStateException("Illegal hexadecimal charcter " + ch + " at index " + index);
		}
		return digit;
	}

	public static byte[] decodeHex(String data) {
		int len = data.length();
		if ((len & 0x01) != 0) {
			throw new IllegalStateException("Odd number of characters.");
		}
		byte[] out = new byte[len >> 1];
		// two characters form the hex value.
		for (int i = 0, j = 0; j < len; i++) {
			int f = toDigit(data.charAt(j), j) << 4;
			j++;
			f = f | toDigit(data.charAt(j), j);
			j++;
			out[i] = (byte) (f & 0xFF);
		}
		return out;
	}

	public static DecoderInputStream stream(String data) {
		return new DecoderInputStream(new ByteArrayInputStream(decodeHex(data)));
	}

	// Type: Authentication Request
	// Length: 12
	// Authentication Type: MD5 Password
	// Salt: 81368E2A
	private static final String AUTHENTICATION_REQUEST = "520000000c0000000581368e2a";

	@Test
	public void testDecodeAuthenticationRequest() throws Exception {
		AuthenticationMessage message = decode(AUTHENTICATION_REQUEST);
		Assert.assertEquals(message.getType(), BackendMessageType.AUTHENTICATION);
		Assert.assertEquals(message.getAuthenticaitonType(), AuthenticationType.MD5_PASSWORD);
		Assert.assertEquals(message.getData(), decodeHex("81368E2A"));
	}

	// Type: Authentication Request
	// Length: 8
	// Authentication Type: Success
	private static final String AUTHENTICATION_SUCCESS = "520000000800000000";

	@Test
	public void testDecodeAthenticationSuccess() throws Exception {
		AuthenticationMessage message = decode(AUTHENTICATION_SUCCESS);
		Assert.assertEquals(message.getType(), BackendMessageType.AUTHENTICATION);
		Assert.assertEquals(message.getAuthenticaitonType(), AuthenticationType.OK);
	}

	// Type: Parameter Status
	// Length: 28
	// Key: client_encoding
	// Value: UNICODE
	private static final String PARAMETER_CLIENT_ENCODING = "530000001c636c69656e745f656e636f64696e6700554e49434f444500";

	@Test
	public void testParameterStatus() throws Exception {
		ParameterMessage message = decode(PARAMETER_CLIENT_ENCODING);
		Assert.assertEquals(message.getType(), BackendMessageType.PARAMETER_STATUS);
		Assert.assertEquals(message.getVariable(), ConfigurationVariable.CLIENT_ENCODING);
		Assert.assertEquals(message.getValue(), "UNICODE");
	}

	// Type: Key
	// Length: 12
	// PID: 22008
	// Key: 1679401028
	private static final String KEY = "4b0000000c000055f86419a044";

	@Test
	public void testBackendKey() throws Exception {
		KeyMessage message = decode(KEY);
		Assert.assertEquals(message.getType(), BackendMessageType.KEY);
		Assert.assertEquals(message.getPid(), 22008);
		Assert.assertEquals(message.getKey(), 1679401028);
	}

	// Type: Ready for Query
	// Length: 5
	// Status: Idle
	private static final String READY_FOR_QUERY = "5a0000000549";

	@Test
	public void testReadyForQuery() throws Exception {
		ReadyMessage message = decode(READY_FOR_QUERY);
		Assert.assertEquals(message.getType(), BackendMessageType.READY_FOR_QUERY);
		Assert.assertEquals(message.getStatus(), Status.IDLE);
	}

	// Type: Parse Completion
	// Length: 4
	private static final String PARSE_COMPLETION = "3100000004";

	// Type: Bind Completion
	// Length: 4
	private static final String BIND_COMPLETION = "3200000004";
	
	@Test
	public void simpleBackendMessages() throws Exception {
		testSimpleBackendMessage(PARSE_COMPLETION, BackendMessageType.PARSE_COMPLETE);
		testSimpleBackendMessage(BIND_COMPLETION, BackendMessageType.BIND_COMPLETE);
	}

	private void testSimpleBackendMessage(String stream, BackendMessageType type) throws Exception {
		SimpleBackendMessage message = decode(stream);
		Assert.assertEquals(message.getType(), type);
	}

	// Type: Row Description
	// Length: 27
	// Columns: 1
	// Column name: id
	// Table OID: 0
	// Column index: 0
	// Type OID: 23
	// Column length: 4
	// Type modifier: -1
	// Format: Text (0)
	private static final String ROW_DESCRIPTION = "540000001b0001696400000000000000000000170004ffffffff0000";

	@Test
	public void rowDescription() throws Exception {
		RowDescriptionMessage message = decode(ROW_DESCRIPTION);
		Assert.assertEquals(message.getType(), BackendMessageType.ROW_DESCRIPTION);
		Assert.assertEquals(message.getFields().length, 1);
		PgField field = message.getFields()[0];
		Assert.assertEquals(field.getColumnLabel(), "id");
		Assert.assertEquals(field.getColumnType(), Type.INTEGER);
	}

	@SuppressWarnings({"unchecked"})
	private <T> T decode(String stream) throws Exception {
		return (T)createDecoder().decode(stream(stream), true);
	}

	private BackendMessageDecoder createDecoder() {
		DefaultConnectionState state = new DefaultConnectionState("test");
		state.setBackendCharset(Charset.defaultCharset());
		return new BackendMessageDecoder(state);
	}

}
