import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class MngrAgent extends Agent {
    @Override
    protected void setup() {
        super.setup();
        addBehaviour(new CyclicBehaviour() {
            AID user = new AID("user", AID.ISLOCALNAME);
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
                        System.out.println(conf);
                        for (String s : conf.getFuzzySettings()) {
                            System.out.println("manager creates fs with: " + s);
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
