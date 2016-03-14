package txstore.scratchpad.rdbms;

interface DBScratchpadFactory extends txstore.scratchpad.ScratchpadFactory{

    // releases the scratchpad
    public void releaseScratchpad();
}