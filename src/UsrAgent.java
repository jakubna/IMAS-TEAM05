import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.util.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;

public class UsrAgent extends Agent {

    private Logger logger = Logger.getMyLogger(getClass().getName());
    private String currentFile = null;

    private class UserAgentServeLoop extends CyclicBehaviour {
        boolean hasToReadInput = true; // indicates if user input is needed
        AID manager = new AID("manager", AID.ISLOCALNAME);

        @Override
        public void action() {
            String input;
            if (hasToReadInput) {
                // read input through terminal
                input = readUserInput();

                // pass instruction to Manager
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(manager);
                msg.setContent(input);
                send(msg);
                hasToReadInput = false;

            } else {
                //wait for manager response
                ACLMessage msg = blockingReceive(MessageTemplate.MatchSender(manager));

                if (msg.getEncoding().equals("String")) {
                    System.out.println("-> USER AGENT: Received '"+msg.getContent()+"'");
                }
                else {
                    ArrayList<Double> res = null;
                    try {
                        res = (ArrayList<Double>) msg.getContentObject();
                    } catch (UnreadableException e) {
                        e.printStackTrace();
                    }

                    // save result to file
                    Path outfile = Paths.get("files").resolve("result_" + currentFile);
                    try {
                        FileWriter writer = new FileWriter(outfile.toString());
                        for (Double val:res) {
                            writer.write(val + "\n");
                        }
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println("-> USER AGENT: Received response. Saving results to '" + outfile.toString() + "'");
                }

                hasToReadInput = true;
            }
        }
    }

    protected void setup() {
        logger.log(Logger.INFO, "Starting user agent");
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("UserAgent");
        sd.setName(getName());
        dfd.setName(getAID());
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
            addBehaviour(new UserAgentServeLoop());
        } catch (FIPAException e) {
            logger.log(Logger.SEVERE, "Agent "+getLocalName()+" - Cannot register with DF", e);
            doDelete();
        }
    }

    // basic UI
    private String readUserInput() {
        Scanner keyboard = new Scanner(System.in);
        String input;

        System.out.println("\n--------------------------------------------");
        System.out.println("FA-DSS Menu\n");
        System.out.println("Action format: <action>_<file> with action being D | I.");
        System.out.println("Place input file in folder 'files', then enter name such as: I_config.txt, D_requests.txt");

        // loop until a valid input is obtained
        do {
            System.out.print("Enter input file:\n> ");
            input = keyboard.nextLine();

            String type = input.substring(0, 2);
            currentFile = input.substring(2);
            Path file = Paths.get("files").resolve(currentFile);
            input = type + file.toString();
        } while (!validateInput(input));

        return input;
    }

    // some checks
    private boolean validateInput(String input) {
        if (!input.startsWith("D_") && !input.startsWith("I_")) {
            System.out.println("ERROR: Invalid input. Expected format is <action>_<file> " +
                    "with action being D | I. For example: I_config.txt, D_requests.txt");
            return false;
        }

        String path = input.substring(2);
        if (!(new File(path)).exists()) {
            System.out.println("ERROR: File \"" + path + "\" could not be found. " +
                    "Please enter a valid path. Files must be placed in 'files' folder.");
            return false;
        }
        return true;
    }
}
