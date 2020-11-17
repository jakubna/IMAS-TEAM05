import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FzzyAgent extends Agent {
    @Override
    protected void setup() {
        super.setup();
        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                Object[] args = getArguments();
                String name = args[0].toString();
                System.out.println("I EXIST, I'M "+getName()+" WITH CONFIGURATION "+name);
            }
        });
    }
}
