import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BufferPool {
    private Frame[] buffers;
    private int clockHand; //for the eviction policy -- where we are currently looking

    public BufferPool(int numOfFrames) {
        initialize(numOfFrames);
    }

    /**
     * Initializes a BufferPool object
     * @param numOfFrames   The number of frames the user wants in this pool
     */
    public void initialize(int numOfFrames) {
        this.buffers = new Frame[numOfFrames]; //1. build the array given the input arg
        this.clockHand = 0; // points at the first frame, in index 0 of the buffer

        for (int i = 0; i < numOfFrames; i++) { //2. go over each frame and initialize it
            this.buffers[i] = new Frame();
        }

        System.out.println("The program is ready for the next command.");
    }

    private void increaseClockhand() {
        // the modulo wraps the hand back to 0 once it goes past the last index
        clockHand = (clockHand + 1) % buffers.length;
    }

    // **************************************************************************
    // CORE GET/SET/PIN/UNPIN FUNCTIONS
    // **************************************************************************

    /**
     * Calculates which block (file) contains the record.
     *  Prints out:
     *      1. the record content
     *      2. whether or not an I/O was done
     *      3. the frame num that contains the block
     * @param recNum    The desired record number
     */
    public void GET(int recNum) {
        int blockId = (recNum - 1) / 100 + 1; //what file has the record
        int recInBlock = (recNum - 1) % 100 + 1; //the record num inside the file
        boolean isIO = false; //did we do IO

        int frameNum = searchBlock(blockId); //the frame number in the buffer array

        if (frameNum == -1) { //not in memory, so we have to bring it in
            try {
                addFrameToPool(blockId); //trying to add this frame to the pool
                isIO = true; //we had to write the block into memory
            } catch (Exception e) { //every frame is pinned
                System.out.println(e.getMessage());
                return;
            }
            frameNum = searchBlock(blockId); //since we had to add it to memory, where is the ACTUAL frameNum for it
        }

        //frame should be in memory now
        System.out.println("Record: " + buffers[frameNum].getRecord(recInBlock));
        System.out.println("I/O done: " + isIO);
        System.out.println("Frame #: " + (frameNum + 1));
    }

    /**
     * User sets the content of a specific record to a given string
     * @param recNum        The record number
     * @param newContent    The new content string
     */
    public void SET(int recNum, String newContent) {
        int blockId = (recNum - 1) / 100 + 1; //what file has the record
        int recInBlock = (recNum - 1) % 100 + 1; //the record num inside the file
        boolean isIO = false; //did we do IO

        int frameNum = searchBlock(blockId); //the frame number in the buffer array

        if (frameNum == -1) { //not in memory, so we have to bring it in
            try {
                addFrameToPool(blockId); //trying to add this frame to the pool
                isIO = true; //we had to write the block into memory
            } catch (Exception e) { //every frame is pinned
                System.out.println(e.getMessage());
                return;
            }
            frameNum = searchBlock(blockId); //since we had to add it to memory, where is the ACTUAL frameNum for it
        }

        buffers[frameNum].updateRecord(recInBlock, newContent);

        //frame should be in memory now
        System.out.println("Write was successful");
        System.out.println("I/O done: " + isIO);
        System.out.println("Frame #: " + (frameNum + 1));
    }

    /**
     * Pins a specific block (BID) in memory
     * @param blockId   The target block to pin
     */
    public void PIN(int blockId) {
        int frameNum = searchBlock(blockId); //the frame number in the buffer array

        if (frameNum == -1) { //not in memory, so we have to bring it in
            try {
                addFrameToPool(blockId); //trying to add this frame to the pool
            } catch (Exception e) { //every frame is pinned
                System.out.println("The corresponding block " + blockId + " cannot be pinned because the memory buffers are full.");
                return;
            }
            frameNum = searchBlock(blockId); //since we had to add it to memory, where is the ACTUAL frameNum for it
        }

        boolean alreadyPinned = buffers[frameNum].isPinned(); 
        buffers[frameNum].setPinned(true);

        //frame should be in memory now
        System.out.println("Frame #: " + (frameNum + 1));
        System.out.println("Pinned flag was " + ((alreadyPinned) ? "already " + alreadyPinned : "previously " + alreadyPinned));
    }

    public void UNPIN(int blockId) {
        int frameNum = searchBlock(blockId); //the frame number in the buffer array

        if (frameNum == -1) { //not in memory, so we have to bring it in
            System.out.println("The corresponding block " + blockId + " cannot be unpinned because it is not in memory.");
            return;
        } else {
            boolean alreadyPinned = buffers[frameNum].isPinned(); 
            buffers[frameNum].setPinned(false);

            System.out.println("Frame #: " + (frameNum + 1));
            System.out.println("Pinned flag was " + ((!alreadyPinned) ? "already " + !alreadyPinned : "previously " + alreadyPinned));
        }
    }


    // **************************************************************************
    // HELPER FUNCTIONS
    // **************************************************************************

    /**
     * Searches if a certain block (file) is available in the buffer pool
     * @param blockId   Block/file ID
     * @return          The buffer number (slot number in the array) holding this block
     */
    public int searchBlock(int blockId) { //each frame has a blockId
        int FrameNum = -1; //array index of the block
        
        for (int i = 0; i < buffers.length; i++){
            Frame currFrame = buffers[i];
            if (currFrame.getBlockId() == blockId) {
                FrameNum = i;
                break;
            }
        }
        return FrameNum; //-1 if not available, otherwise it's the array position
    }

    /**
     * Returns the content of a given block ID
     * @param blockId   Block ID
     * @return  A String array of records in that block
     */
    public String getBlockContents(int blockId) {
        System.out.println("in get block contents");

        // 1. get the block
        int blockPos = searchBlock(blockId);

        if (blockPos == -1) {
            return "Block not found"; //block not found
        }

        // 2. read and put the contents in a string array
        Frame currFrame = buffers[blockPos];
        String contentStr = new String(currFrame.getContent(), StandardCharsets.UTF_8);
        
        // 3. split contentStr based on records (deliminator is '.')
        //System.out.println(contentStr);

        return contentStr;
    }

    /**
     * Searches for an empty frame. If none, then run eviction policy to get an empty frame
     * @return  A number in the buffers array for an empty frame
     */
    public int getEmptyFrame(int blockId) {
        for (int i = 0; i < buffers.length; i++){
            if (buffers[i].getBlockId() == -1) {
                // if this frame's block ID is -1, then it's empty
                // so return i, which is the array position, of the empty frame
                return i;
            }
        }

        // if there are no empty frames, then run the eviction policy
        return evictFrame(blockId); // if -1, then all frames are pinned
    }

    /**
     * Evicts a frame with a policy similar to clock replacement but without the second chance bit
     * @return  The index of the evicted frame, which is now empty
     */
    public int evictFrame(int blockId) {
        //basically, if this frame isn't pinned then evict it lol
        int evictedFrame = -1;
        int originalHandPos = clockHand; //the original hand position so we don't keep looping forever
        
        // while we haven't found an evicted frame AND we're not back at the original position
        while (evictedFrame == -1) {
            if (buffers[clockHand].isPinned()) { //it is pinned
                // ignore it and move on
                
                increaseClockhand();
                if (originalHandPos == clockHand) {
                    break; //we looped around and there are no frames we can evict
                }
            } else { //it's not pinned
                evictedFrame = clockHand;
                increaseClockhand();
            }
        }

        if (evictedFrame != -1) {
            if (buffers[evictedFrame].isDirty()) { //if it's true, need to write back to disk
                // write to the file
                Path path = Paths.get("Project1/F" + evictedFrame + ".txt");

                try {
                    Files.write(path, buffers[evictedFrame].getContent());
                    //System.out.println("Contents written back to disk successfully");
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }

        return evictedFrame;
    }

    /**
     * If the needed blockID is not in the pool, then we add it
     * @param blockId    The block we want to add
     */
    public void addFrameToPool(int blockId) throws Exception {
        int emptyFrame = getEmptyFrame(blockId); //gets an empty frame first

        // no empty frames available
        if (emptyFrame == -1) {
            throw new Exception("The corresponding block #" + blockId + " cannot be accessed from disk because the memory buffers are full.");
        } else { //yes empty frame available
            Path path = Paths.get("/Project1/F" + blockId + ".txt");

            try {
                String newContent = Files.readString(path); //write that block in the memory 
                buffers[emptyFrame] = new Frame(false, false, blockId, newContent);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //System.out.println("I think we did it??? let's see");
    }
}
