package txstore.scratchpad.rdbms;

import txstore.util.Result;

public interface IPrimaryExec
{
	public void addResult( Result r); 
	public Result getResult( int pos);
}
