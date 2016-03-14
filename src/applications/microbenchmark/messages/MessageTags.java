package applications.microbenchmark.messages;
import util.Debug;

public class MessageTags {

	public final static int TXNREQ = 14; // sent from
	// user to appServer when
	// user wants to issue a request

	public final static int TXNREP = 15; // sent from

	// appServer to user when
	// a txn is committed

	public final static String getString(int i) {
		switch (i) {
		case MessageTags.TXNREQ:
			return " TxnReq";
		case MessageTags.TXNREP:
			return " TxnRep";
		default:

			throw new RuntimeException("Invalid message tag:  " + i);
		}
	}

}