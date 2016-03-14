package applications.microbenchmark.messages;
import util.Debug;

import util.UnsignedTypes;

public class MessageFactory {
	public MessageFactory() {

	}

	public MessageBase fromBytes(byte[] bytes) {
		int offset = 0;
		int tag = UnsignedTypes.bytesToInt(bytes, offset);
		offset += UnsignedTypes.uint16Size;

		switch (tag) {
		case MessageTags.TXNREQ:
			return new TxnReqMessage(bytes);
		case MessageTags.TXNREP:
			return new TxnRepMessage(bytes);
		default:
			throw new RuntimeException("Invalid message tag:  " + tag);
		}

	}

}
