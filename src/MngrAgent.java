import com.sun.tools.internal.xjc.model.CNonElement;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

import jade.util.Logger;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;

public class MngrAgent extends Agent {
    private Logger logger = Logger.getMyLogger(getClass().getName());
    private int nResponders = 0;

    private class MnrAgentContractNet extends ContractNetInitiator{
        public MnrAgentContractNet(Agent a, ACLMessage cfp) {
            super(a, cfp);
            //System.out.println("Content: "+cfp.getAllReceiver());
        }

        protected void handlePropose(ACLMessage propose, Vector v) {
            //System.out.println("Agent "+propose.getSender().getName()+" proposed "+propose.getContent());
        }

        protected void handleRefuse(ACLMessage refuse) {
            //System.out.println("Agent "+refuse.getSender().getName()+" refused");
        }

        protected void handleFailure(ACLMessage failure) {
            if (failure.getSender().equals(myAgent.getAMS())) {
                // FAILURE notification from the JADE runtime: the receiver does not exist
                //System.out.println("Responder does not exist");
            }
            else {
                //System.out.println("Agent "+failure.getSender().getName()+" failed");
            }
            // Immediate failure --> we will not receive a response from this agent
            nResponders--;
        }

        protected void handleAllResponses(Vector responses, Vector acceptances) {
            if (responses.size() < nResponders) {
                // Some responder didn't reply within the specified timeout
                //System.out.println("Timeout expired: missing "+(nResponders - responses.size())+" responses");
            }
            // Evaluate proposals.
            Enumeration e = responses.elements();
            while (e.hasMoreElements()) {
                ACLMessage msg = (ACLMessage) e.nextElement();
                if (msg.getPerformative() == ACLMessage.PROPOSE) {
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    acceptances.addElement(reply);
                    int proposal = Integer.parseInt(msg.getContent());
                    //System.out.println("Accepting proposal "+proposal+" from responder "+msg.getSender());
                }
            }
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(new AID("user", AID.ISLOCALNAME));
            msg.setContent("request result");
            send(msg);
        }

        protected void handleInform(ACLMessage inform) {
            //System.out.println("Agent "+inform.getSender().getName()+" successfully performed the requested action");
        }

        @Override
        public int onEnd() {
            //System.out.println("Agent "+getLocalName()+": RESETING");
            addBehaviour(new MngrAgentServeLoop());
            return super.onEnd();
        }
    }

    private class MngrAgentServeLoop extends SimpleBehaviour { //CyclicBehaviour
        AID user = new AID("user", AID.ISLOCALNAME);
        List<String> fuzzyAgents = new ArrayList<String>();
        boolean finished = false;
        @Override
        public void action() {
            ACLMessage msg = blockingReceive(MessageTemplate.MatchSender(user));

            if(msg!=null) {
                String cnt = msg.getContent();
                String type = cnt.substring(0, 2);
                String file = cnt.substring(2);
                //System.out.println("manager received: " + msg.getContent());

                if (type.equals("I_")) {
                    boolean found = false;
                    // Initialize instruction
                    // Read settings
                    SimSettings conf = SimSettings.fromXML(file);
                    if (conf == null) {
                        msg = new ACLMessage(ACLMessage.INFORM);
                        msg.addReceiver(user);
                        msg.setContent("Could not process configuration file. Format does not seem to correspond.");
                        send(msg);
                        finished = false;
                    } else {
                        // Fuzzy agents creation
                        String[] fuzzyS = conf.getFuzzySettings();
                        String scenario = conf.getApplication();
                        ContainerController container = getContainerController();
                        for (int i = 0; i < conf.getFuzzyagents(); i++) {
                            String name = "FS" + i + scenario + fuzzyS[i];
                            if (!fuzzyAgents.contains(name)) {
                                Object[] args = new Object[2];
                                args[0] = scenario;
                                args[1] = fuzzyS[i];
                                try {
                                    AgentController a = container.createNewAgent(name, "FzzyAgent", args);
                                    a.start();
                                    fuzzyAgents.add(name);
                                    nResponders++;
                                } catch (StaleProxyException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                found = true;
                            }
                        }
                        msg = new ACLMessage(ACLMessage.INFORM);
                        msg.addReceiver(user);
                        if (found) {
                            msg.setContent("Found '" + scenario + "' already initialised.");
                        } else {
                            msg.setContent("Configuration '" + scenario + "' initialised.");
                        }
                        send(msg);
                        finished = false;
                    }
                    // sleep to wait FA until ContractNet used
                    try {
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else if (type.equals("D_")) {
                    byte[] encoded = null;
                    try {
                        encoded = Files.readAllBytes(Paths.get(file));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    String request = new String(encoded, StandardCharsets.UTF_8);

                    if (!request.matches("\\w+\n(([0-9]+(\\.[0-9]+)?,?)+\n?)+")) {
                        msg = new ACLMessage(ACLMessage.INFORM);
                        msg.addReceiver(user);
                        msg.setContent("Input file not following request format.");
                        send(msg);
                        finished = false;
                        return;
                    }

                    String [] req_spl = request.split("\n");
                    String scenario = req_spl[0];
                    String[] data = Arrays.copyOfRange(req_spl, 1, req_spl.length);

                    ArrayList<Float[]> matrix = new ArrayList<Float[]>();
                    for (String d: data) {
                        Float [] row = Arrays.stream(d.split(",")).map(Float::valueOf).toArray(Float[]::new);
                        matrix.add(row);
                    }

                    ServiceDescription sd = new ServiceDescription();
                    DFAgentDescription dfd = new DFAgentDescription();
                    sd.setType(scenario);
                    dfd.addServices(sd);
                    DFAgentDescription[] result = null;
                    try {
                        result = DFService.search(myAgent, dfd);
                    } catch (FIPAException e) {
                        e.printStackTrace();
                    }
                    if (result.length > 0) {
                        ACLMessage msg2 = new ACLMessage(ACLMessage.CFP);
                        for (int i = 0; i < result.length; i++) {
                            //System.out.println(result[i].getName());
                            msg2.addReceiver(result[i].getName());
                        }
                        msg2.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                        // We want to receive a reply in 10 secs
                        msg2.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
                        try {
                            msg2.setContentObject(matrix);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        myAgent.addBehaviour(new MnrAgentContractNet(myAgent, msg2));
                        finished = true;
                    } else {
                        msg = new ACLMessage(ACLMessage.INFORM);
                        msg.addReceiver(user);
                        msg.setContent("Configuration '"+scenario+"' not previously initialised.");
                        send(msg);
                        finished = false;
                    }
                }
            }
        }

        public boolean done() {
            return finished;
        }
    }

    protected void setup() {
        // Registration with the DF
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("ManagerAgent");
        sd.setName(getName());
        // sd.setOwnership("TEAM05");
        dfd.setName(getAID());
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
            addBehaviour(new MngrAgentServeLoop());
        } catch (FIPAException e) {
            logger.log(Logger.SEVERE, "Agent "+getLocalName()+" - Cannot register with DF", e);
            doDelete();
        }
    }
}