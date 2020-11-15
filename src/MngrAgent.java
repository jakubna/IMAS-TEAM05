import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.concurrent.TimeUnit;

public class MngrAgent extends Agent {
    @Override
    protected void setup() {
        super.setup();
        addBehaviour(new CyclicBehaviour() {
            AID user = new AID("user", AID.ISLOCALNAME);
            @Override
            public void action() {
                //System.out.println("MNGR START");
                ACLMessage msg = blockingReceive(MessageTemplate.MatchSender(user));

                //System.out.println("MNGR RECEIVE");
                if(msg!=null) {
                    System.out.println("manager received: " + msg.getContent());

                    //System.out.println("MNGR WAIT (working on response)");
                    // - fuzzy agent creation
                    // - contractnet
                    try {
                        TimeUnit.SECONDS.sleep(5);
                    } catch (Exception e) {

                    }

                    // response
                    //System.out.println("MNGR SEND");
                    msg = new ACLMessage(ACLMessage.INFORM);
                    msg.addReceiver(user);
                    msg.setContent("manager response");
                    send(msg);

                    //System.out.println("MNGR END");
                }
            }
        });
    }
}
