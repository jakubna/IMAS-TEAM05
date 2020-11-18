import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MngrAgent extends Agent {
    private Logger logger = Logger.getMyLogger(getClass().getName());

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
                System.out.println("-> MANAGER AGENT: Received '" + msg.getContent()+"'");

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
                            } catch (StaleProxyException e) {
                                e.printStackTrace();
                            }
                        }else{
                            System.out.println("-> MANAGER AGENT: "+name+" already exists");
                        }
                    }
                    // sleep to wait FA until ContractNet used
                    try {
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else if (type.equals("D_")) {
                    // request instruction
                    // - contractnet
                    System.out.println("-> MANAGER AGENT: not able to process requests");
                }

                // response
                msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(user);
                msg.setContent("manager default response");
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