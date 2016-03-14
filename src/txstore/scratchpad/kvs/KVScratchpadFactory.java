package txstore.scratchpad.kvs;

interface KVScratchpadFactory extends txstore.scratchpad.ScratchpadFactory{

    // releases the scratchpad
    public void releaseScratchpad();
}