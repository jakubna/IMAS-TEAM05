import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import java.util.ArrayList;
import java.util.List;

public class MngrAgent extends Agent {
    @Override
    protected void setup() {
        super.setup();
        addBehaviour(new CyclicBehaviour() {
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
                                } catch (StaleProxyException e) {
                                    e.printStackTrace();
                                }
                            }else{
                                System.out.println(name+" already exists");
                            }
                        }
                    } else if (type.equals("D_")) {
                        // request instruction
                        // - contractnet
                        System.out.println();
                    }

                    // response
                    msg = new ACLMessage(ACLMessage.INFORM);
                    msg.addReceiver(user);
                    msg.setContent("manager response");
                    send(msg);
                }
            }
        });
    }
}
