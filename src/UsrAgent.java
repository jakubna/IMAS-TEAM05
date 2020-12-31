import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;

import java.io.File;
import java.util.Scanner;

public class UsrAgent extends Agent {

    private Logger logger = Logger.getMyLogger(getClass().getName());

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
                //System.out.println("USR send instruction");
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(manager);
                msg.setContent(input);
                send(msg);
                //msg = null;
                hasToReadInput = false;

            } else {
                //wait for manager response
                //System.out.println("USR receive response");
                ACLMessage msg = blockingReceive(MessageTemplate.MatchSender(manager));
                if (msg != null) {
                    System.out.println("-> USER AGENT: Received '"+msg.getContent()+"'");
                }
                //msg = null;
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
        // sd.setOwnership("TILAB");
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
        System.out.println("For example: I_config.txt, D_config.txt");

        // loop until a valid input is obtained
        do {
            System.out.print("Enter input file:\n> ");
            input = keyboard.nextLine();

            String type = input.substring(0, 2);
            String file = input.substring(2);
            input = type + "./files/" + file;
        } while (!validateInput(input));

        return input;
    }

    // some checks
    private boolean validateInput(String input) {
        if (!input.startsWith("D_") && !input.startsWith("I_")) {
            System.out.println("ERROR: Invalid input. Expected format is <action>_<file> " +
                    "with action being D | I. For example: I_config.txt, D_config.txt");
            return false;
        }

        String path = input.substring(2);
        if (!(new File(path)).exists()) {
            System.out.println("ERROR: File \"" + path + "\" could not be found. " +
                    "Please enter a valid path.");
            return false;
        }
        return true;
    }
}
