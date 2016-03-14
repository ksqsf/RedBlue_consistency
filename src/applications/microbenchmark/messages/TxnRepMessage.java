package applications.microbenchmark.messages;
import util.Debug;

import txstore.util.ProxyTxnId;

import util.UnsignedTypes;

/**
 * 
 * @author chengli
 * sent from appServer to user about a response to user's txn request message
 */

public class TxnRepMessage extends MessageBase{
	
	protected ProxyTxnId proxyTxnId;
	int success;// 0 : committed, 1: aborted
	int color; // 0: red, 1: blue

	public TxnRepMessage(ProxyTxnId txid, int s, int c) {
		super(MessageTags.TXNREP, computeByteSize(txid));
		proxyTxnId = txid;
		success = s;
		color = c;
		int offset = getOffset();
		proxyTxnId.getBytes(getBytes(), offset);
		offset += proxyTxnId.getByteSize();
		UnsignedTypes.intToBytes(s, getBytes(), offset);
		offset += UnsignedTypes.uint16Size;
		UnsignedTypes.intToBytes(c, getBytes(), offset);
		offset += UnsignedTypes.uint16Size;
		if (offset != getBytes().length)
			throw new RuntimeException("did not fill up the byte array!");
	}

	public TxnRepMessage(byte[] b) {
		super(b);
		if (getTag() != MessageTags.TXNREP)
			throw new RuntimeException("Invalid message tag.  looking for "
					+ MessageTags.TXNREP + " found " + getTag());
		int offset = getOffset();
		proxyTxnId = new ProxyTxnId(b, offset);
		offset += proxyTxnId.getByteSize();
		success = UnsignedTypes.bytesToInt(b, offset);
		offset += UnsignedTypes.uint16Size;
		color = UnsignedTypes.bytesToInt(b, offset);
		offset += UnsignedTypes.uint16Size;
		if (offset != b.length)
			throw new RuntimeException("did not consume the entire byte array!");
	}

	public ProxyTxnId getTxnId() {
		return proxyTxnId;
	}

	public int getResult() {
		return success;
	}
	
	public int getColor() {
		return color;
	}

	static int computeByteSize(ProxyTxnId proxyTxnId) {
		return proxyTxnId.getByteSize() + UnsignedTypes.uint16Size + UnsignedTypes.uint16Size;
	}

	public String toString() {
		return "<" + getTagString() + ", " + proxyTxnId + ">";
	}


}
