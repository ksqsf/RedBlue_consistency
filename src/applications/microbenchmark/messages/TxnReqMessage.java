package applications.microbenchmark.messages;
import util.Debug;

import txstore.util.ProxyTxnId;

import util.UnsignedTypes;

/**
 * 
 * @author chengli sent from users to appServer to issue an user request
 */

public class TxnReqMessage extends MessageBase {

	protected ProxyTxnId proxyTxnId;
	int read;

	public TxnReqMessage(ProxyTxnId txid, int r) {
		super(MessageTags.TXNREQ, computeByteSize(txid, r));
		proxyTxnId = txid;
		int offset = getOffset();
		proxyTxnId.getBytes(getBytes(), offset);
		offset += proxyTxnId.getByteSize();
		read = r;
		UnsignedTypes.intToBytes(read, getBytes(), offset);
		offset += UnsignedTypes.uint16Size;
		if (offset != getBytes().length)
			throw new RuntimeException("did not fill up the byte array!");
	}

	public TxnReqMessage(byte[] b) {
		super(b);
		if (getTag() != MessageTags.TXNREQ)
			throw new RuntimeException("Invalid message tag.  looking for "
					+ MessageTags.TXNREQ + " found " + getTag());
		int offset = getOffset();
		proxyTxnId = new ProxyTxnId(b, offset);
		offset += proxyTxnId.getByteSize();
		read = UnsignedTypes.bytesToInt(b, offset);
		offset += UnsignedTypes.uint16Size;
		if (offset != b.length)
			throw new RuntimeException("did not consume the entire byte array!");
	}

	public ProxyTxnId getTxnId() {
		return proxyTxnId;
	}

	static int computeByteSize(ProxyTxnId proxyTxnId, int r) {
		return proxyTxnId.getByteSize() + UnsignedTypes.uint16Size;
	}
	
	public boolean isRead(){
		if(read == 0)
			return true;
		else
			return false;
	}

	public String toString() {
		return "<" + getTagString() + ", " + proxyTxnId + ">";
	}

}
