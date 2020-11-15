import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class UsrAgent extends Agent {
    @Override
    protected void setup() {
        super.setup();
        addBehaviour(new CyclicBehaviour() {
            boolean usrInput = true; // indicates if user input is needed
            AID manager = new AID("manager", AID.ISLOCALNAME);
            @Override
            public void action() {
                if (usrInput){
                    // read input through terminal

                    // pass instruction to Manager
                    //System.out.println("USR send instruction");
                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                    msg.addReceiver(manager);
                    msg.setContent("usr instruction");
                    send(msg);
                    //msg = null;
                    usrInput = false;
                }else{
                    //wait for manager response
                    //System.out.println("USR receive response");
                    ACLMessage msg = blockingReceive(MessageTemplate.MatchSender(manager));
                    if(msg!=null){
                        System.out.println("USR received: "+msg.getContent());
                    }
                    //msg = null;
                    usrInput = true;
                }
            }
        });
    }
}
