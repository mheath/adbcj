/*
 *   Copyright (c) 2007 Mike Heath.  All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.safehaus.adbcj.mysql;

import java.util.HashSet;
import java.util.Set;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;

public class CommandRequestEncoder extends RequestEncoder<CommandRequest> {

	@Override
	protected void encode(IoSession session, CommandRequest request, ByteBuffer buffer)	throws Exception {
		MysqlConnection connection = IoSessionUtil.getMysqlConnection(session);
		
		buffer.put(request.getCommand().getCommandCode());
		if (request.getPayload() != null && request.getPayload().length() > 0) {
			buffer.putString(request.getPayload(), connection.getCharacterSet().getCharset().newEncoder());
		}
	}

	public Set<Class<? extends CommandRequest>> getMessageTypes() {
		Set<Class<? extends CommandRequest>> type = new HashSet<Class<? extends CommandRequest>>();
		type.add(CommandRequest.class);
		return type;
	}
}
