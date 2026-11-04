import java.nio.charset.StandardCharsets;

public class Frame {
    // Frame ID is defined by its position index in the buffers array
    //1 frame = 1 block = 1 file = 100 records

    private static final int BLOCK_SIZE = 4 * 1024;
    private static final int RECORD_SIZE = 40;

    private boolean dirty;
    private boolean pinned;
    private int blockId;        //one block in a frame
    private byte[] content;     //that block holds some contents


    public Frame(boolean isDirty, boolean isPinned, int blockId, String content) {
        this.dirty = isDirty;
        this.pinned = isPinned;
        this.blockId = blockId;
        this.content = content.getBytes(StandardCharsets.UTF_8);
    }

    public Frame() {
        this.dirty = false;
        this.pinned = false;
        this.blockId = -1; //empty frame
        this.content = new byte[BLOCK_SIZE];
    }

    //getters and setters
    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(Boolean flag) {
        this.dirty = flag;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(Boolean flag) {
        this.pinned = flag;
    }

    public int getBlockId() {
        return blockId;
    }

    public void setBlockId(int id) {
        this.blockId = id;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(String newContent) {
        this.content = newContent.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Returns a specific record in this frame
     * @param recNum    The record number
     * @return          The content of this record as a String
     */
    public String getRecord(int recNum) {
        int offset = (recNum - 1) * 40; //where the record starts at
        String record = new String(content, offset, RECORD_SIZE, StandardCharsets.UTF_8);
        return record;
    }

    /**
     * Updates a specific rcord in this frame
     * @param recNum        The record number
     * @param newContent    A string containing the new content
     */
    public void updateRecord(int recNum, String newContent) {
        int offset = (recNum - 1) * 40; //where the record starts at
        byte[] newByteContent = newContent.getBytes(StandardCharsets.UTF_8);

        System.arraycopy(newByteContent, 0, content, offset, RECORD_SIZE);
        dirty = true; //set dirty=true bc it was modified
    }
}
