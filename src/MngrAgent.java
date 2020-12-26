import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ParallelBehaviour;
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

import java.util.*;

import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;

public class  MngrAgent extends Agent {
    private Logger logger = Logger.getMyLogger(getClass().getName());
    private int nResponders = 0;

    private class MnrAgentContractNet extends ContractNetInitiator{
        public MnrAgentContractNet(Agent a, ACLMessage cfp) {
            super(a, cfp);
            System.out.println("Content: "+cfp.getAllReceiver());
        }

        protected void handlePropose(ACLMessage propose, Vector v) {
            System.out.println("Agent "+propose.getSender().getName()+" proposed "+propose.getContent());
        }

        protected void handleRefuse(ACLMessage refuse) {
            System.out.println("Agent "+refuse.getSender().getName()+" refused");
        }

        protected void handleFailure(ACLMessage failure) {
            if (failure.getSender().equals(myAgent.getAMS())) {
                // FAILURE notification from the JADE runtime: the receiver does not exist
                System.out.println("Responder does not exist");
            }
            else {
                System.out.println("Agent "+failure.getSender().getName()+" failed");
            }
            // Immediate failure --> we will not receive a response from this agent
            nResponders--;
        }

        protected void handleAllResponses(Vector responses, Vector acceptances) {
            if (responses.size() < nResponders) {
                // Some responder didn't reply within the specified timeout
                System.out.println("Timeout expired: missing "+(nResponders - responses.size())+" responses");
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
                    System.out.println("Accepting proposal "+proposal+" from responder "+msg.getSender());
                }
            }
        }

        protected void handleInform(ACLMessage inform) {
            System.out.println("Agent "+inform.getSender().getName()+" successfully performed the requested action");
        }
    }

    private class MngrAgentServeLoop extends CyclicBehaviour {
        AID user = new AID("user", AID.ISLOCALNAME);
        List<String> fuzzyAgents = new ArrayList<String>();
        @Override
        public void action() {
            ACLMessage msg = blockingReceive(MessageTemplate.MatchSender(user));

            if(msg!=null) {
                String cnt = msg.getContent();
                String type = cnt.substring(0, 2);
                String file = cnt.substring(2);
                System.out.println("manager received: " + msg.getContent());

                if (type.equals("I_")) {
                    // Initialize instruction
                    // Read settings
                    SimSettings conf = SimSettings.fromXML(file);
                    // Fuzzy agents creation
                    String[] fuzzyS = conf.getFuzzySettings();
                    ContainerController container = getContainerController();
                    for (int i=0;i<conf.getFuzzyagents();i++)
                    {
                        String name = "FS"+i+fuzzyS[i];
                        if (!fuzzyAgents.contains(name)) {
                            Object[] args = new Object[1];
                            args[0] = fuzzyS[i];
                            try {
                                AgentController a = container.createNewAgent(name, "FzzyAgent", args);
                                a.start();
                                //a.activate();
                                fuzzyAgents.add(name);
                                nResponders++;
                            } catch (StaleProxyException e) {
                                e.printStackTrace();
                            }
                        }else{
                            System.out.println(name+" already exists");
                        }
                    }
                } else if (type.equals("D_")) {
                    ACLMessage msg2 = new ACLMessage(ACLMessage.CFP);
                    ServiceDescription sd = new ServiceDescription();
                    DFAgentDescription dfd = new DFAgentDescription();
                    sd.setType("FuzzyAgent");
                    dfd.addServices(sd);
                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, dfd);
                        for (int i = 0; i < result.length; i++){
                            System.out.println(result[i].getName());
                            msg2.addReceiver(result[i].getName());
                        }
                    } catch (FIPAException e) {
                        e.printStackTrace();
                    }

                    System.out.println(msg2.getAllReceiver());
                    // request instruction
                    msg2.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                    // We want to receive a reply in 10 secs
                    msg2.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
                    msg2.setContent("try-action");

                    MnrAgentContractNet ax = new MnrAgentContractNet (myAgent, msg2);
                    //ParallelBehaviour par = new ParallelBehaviour(ParallelBehaviour.WHEN_ALL);
                    //par.addSubBehaviour(new MnrAgentContractNet (myAgent, msg2));
                    //ACLMessage msg3 = blockingReceive(MessageTemplate.MatchSender(ax));
                    System.out.println("Behavior created");
                }

                // response
                msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(user);
                msg.setContent("manager response");
                send(msg);
            }
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