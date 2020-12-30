import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.util.Logger;
import net.sourceforge.jFuzzyLogic.FIS;

public class FzzyAgent extends Agent {
    private Logger logger = Logger.getMyLogger(getClass().getName());
    private FIS fis = null;

    private class FuzzyAgentBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            Object[] args = getArguments();
            String name = args[0].toString();
            System.out.println("-> "+getName()+": Has been created with configuration "+name);
        }


    }
    protected void setup() {
        // Registration with the DF
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("FuzzyAgent");
        sd.setName(getName());
        // sd.setOwnership("TEAM05");
        dfd.setName(getAID());
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
            addBehaviour(new FuzzyAgentBehaviour());
        } catch (FIPAException e) {
            logger.log(Logger.SEVERE, "Agent "+getLocalName()+" - Cannot register with DF", e);
            doDelete();
        }
    }


    private double evaluateTipper(double[] variables) {
        fis.setVariable("service", variables[0]);
        fis.setVariable("food", variables[1]);
        double tip = fis.getVariable("tip").getLatestDefuzzifiedValue();
        return tip;
    }


    private double evaluateQoS(double[] variables) {
        fis.setVariable("commitment", variables[0]);
        fis.setVariable("clarity", variables[1]);
        fis.setVariable("influence", variables[2]);
        double serviceQuality = fis.getVariable("service_quality").getLatestDefuzzifiedValue();
        return serviceQuality;
    }
}