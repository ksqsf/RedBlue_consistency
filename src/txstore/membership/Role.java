/*
import util.Debug;
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package txstore.membership;

/**
 *
 * @author chengli
 */
public enum Role {
    PROXY, COORDINATOR, REMOTECOORDINATOR, STORAGE;
	// the remaining are more "application" side
	// they should not be included
        //USER, APPPROXY;
}
