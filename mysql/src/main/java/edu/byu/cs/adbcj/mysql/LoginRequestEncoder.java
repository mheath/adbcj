package edu.byu.cs.adbcj.mysql;

import java.util.Collections;
import java.util.Set;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;

public class LoginRequestEncoder extends RequestEncoder<LoginRequest> {

	@Override
	protected void encode(IoSession session, LoginRequest request, ByteBuffer buffer) throws Exception {
		// Encode inital part of authentication request
		buffer.putEnumSetShort(request.getCapabilities());
		buffer.putEnumSetShort(request.getExtendedCapabilities());
		buffer.putInt(request.getMaxPacketSize());
		buffer.put(request.getCharSet().getId());
		
		// Encode filler
		buffer.fill((byte)0, LoginRequest.FILLER_LENGTH);
		
		// Encode username
		buffer.putString(request.getCredentials().getUserName(), request.getCharSet().getCharset().newEncoder());
		buffer.put((byte)0); // null-terminate username
		
		// Encode password
		final String password = request.getCredentials().getPassword();
		if (password != null && password.length() > 0) {
			ServerGreeting serverGreeting = (ServerGreeting)IoSessionUtil.getSessionAttribute(session, SessionId.SERVER_GREETING);
			byte[] encryptedPassword = PasswordEncryption.encryptPassword(password, serverGreeting.getSalt());
			buffer.put((byte)encryptedPassword.length);
			buffer.put(encryptedPassword);
		} else {
			buffer.put((byte)0); // null-terminate password
		}
		
		// Encode desired database/schema
		final String database = request.getCredentials().getDatabase();
		if (database != null) {
			buffer.putString(database, request.getCharSet().getCharset().newEncoder());
		}
		buffer.put((byte)0);
	}

	public Set<Class<LoginRequest>> getMessageTypes() {
		return Collections.singleton(LoginRequest.class); 
	}

} 
