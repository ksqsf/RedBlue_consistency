/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package txstore.baseserver;
import txstore.util.Result;
import txstore.util.Operation;

/**
 *
 * @author aclement
 */
public interface ServerApplication {
    
    public Result execute(Operation op);
    
}
