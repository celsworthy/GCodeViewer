/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.gcodeviewer.comms;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 *
 * @author Tony
 */
public class CommandQueue extends Thread
{
    private final List<String> commandList;

    public CommandQueue()
    {
        this.setDaemon(true);
        this.setName("CommandQueue");
        this.setPriority(Thread.MAX_PRIORITY);
        
        commandList = new ArrayList<>();
    }

    public synchronized boolean commandAvailable()
    {
        return !commandList.isEmpty();
    }

    public synchronized String getNextCommandFromQueue()
    {
        String command = "";
        if (! commandList.isEmpty()) {
            command = commandList.get(0);
            commandList.remove(0);
        }
        
        return command;
    }

    public synchronized void addCommandToQueue(String command)
    {
        //System.out.println("Add command " + command);
        commandList.add(command);
    }
    
    @Override
    public void run()
    {
        System.out.println("Reading commands from StdIn ...");
        Scanner scanner = new Scanner(System.in);
        boolean running = true;
        while (running) {
            String inputString = scanner.nextLine();
            if (inputString.equalsIgnoreCase("q")) {
                running = false;
            }
            addCommandToQueue(inputString);
        }
    }
}
