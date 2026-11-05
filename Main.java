import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        // must scan for user inputs
        Scanner scanner = new Scanner(System.in);

        int numOfFrames;
        if (args.length > 0) {
            numOfFrames = Integer.parseInt(args[0]);
        } else {
            System.out.print("Enter the number of frames you want in the buffer pool: ");
            numOfFrames = Integer.parseInt(scanner.nextLine());
        }
        
        // initialize the buffer
        BufferPool buffPool = new BufferPool(numOfFrames);
        System.out.println("Type 'exit' to quit");

        while (true) {
            System.out.println("\n ----");
            //buffPool.showBuffer();

            String userInput = scanner.nextLine();

            if (userInput.equalsIgnoreCase("EXIT")) {
                break;
            }
            
            // 4 cmds
            // 1. GET k, where k is the record num
            // 2. SET k <string of 40 bytes>, where k is the record num
            // 3. PIN BID, where BID is the block ID (file number), NOT record num
            // 4. UNPIN BID

            // [0]: command like GET, SET, PIN, UNPIN
            // [1]: k or BID
            // [2]: for SET -- the new content
            String userCmd = userInput.stripLeading();
            String[] cmd = userCmd.split(" ", 3);

            switch (cmd[0].toUpperCase()){
                case "GET": //GET k
                    buffPool.GET(Integer.parseInt(cmd[1]));
                    break;
                case "SET":
                    buffPool.SET(Integer.parseInt(cmd[1]), cmd[2]);
                    break;
                case "PIN":
                    buffPool.PIN(Integer.parseInt(cmd[1]));
                    break;
                case "UNPIN":
                    buffPool.UNPIN(Integer.parseInt(cmd[1]));
                    break;   
            }
        }

        // close scanner
        scanner.close();
    }
    
}
