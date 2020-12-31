import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPANames;
import jade.lang.acl.*;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.util.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ContractNetResponder;
import net.sourceforge.jFuzzyLogic.FIS;

public class FzzyAgent extends Agent {
    private Logger logger = Logger.getMyLogger(getClass().getName());
    private String domain = null;
    private FIS fis = null;

    private class FuzzyAgentBehaviour extends ContractNetResponder{
        private ArrayList<Double[]> input = null;

        public FuzzyAgentBehaviour(Agent a, MessageTemplate mt) {
            super(a, mt);
            Object[] args = getArguments();
            String name = args[0].toString();
            //System.out.println("-> "+myAgent.getName()+": Has been created with configuration "+name);
        }

        @Override
        protected ACLMessage handleCfp(ACLMessage cfp) {
            //System.out.println("Agent "+getLocalName()+": CFP received from "+cfp.getSender().getName()+". Action is "+cfp.getContent());

            try {
                input = (ArrayList<Double[]>) cfp.getContentObject();
            } catch (UnreadableException e) {
                e.printStackTrace();
            }
            // We provide a proposal
            //System.out.println("Agent "+getLocalName()+": Proposing "+proposal);
            ACLMessage propose = cfp.createReply();
            propose.setPerformative(ACLMessage.PROPOSE);
            propose.setContent(domain);
            return propose;
        }

        @Override
        protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) throws FailureException {
            //System.out.println("Agent "+getLocalName()+": Proposal accepted");
            //codicio temporal
            if (cfp != null) {
                //System.out.println("Agent "+getLocalName()+": Action successfully performed");
                ACLMessage inform = accept.createReply();
                inform.setPerformative(ACLMessage.INFORM);

                ArrayList<Double> results = processRequest(input);
                try {
                    inform.setContentObject(results);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return inform;
            }
            else {
                //System.out.println("Agent "+getLocalName()+": Action execution failed");
                throw new FailureException("unexpected-error");
            }
        }

        @Override
        protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
            //System.out.println("Agent "+getLocalName()+": Proposal rejected");
        }

        @Override
        public int onEnd() {
            //System.out.println("Agent "+getLocalName()+": RESETING");
            reset();
            return super.onEnd();
        }
    }


    protected void setup() {
        Object[] args = getArguments();
        domain = args[0].toString();
        fis = FIS.load("./files/" + args[1].toString() + ".fcl", true);

        // Registration with the DF
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("FuzzyAgent");
        sd.setType(domain);
        sd.setName(getName());
        // sd.setOwnership("TEAM05");
        dfd.setName(getAID());
        dfd.addServices(sd);

        MessageTemplate template = MessageTemplate.and(
                MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),
                MessageTemplate.MatchPerformative(ACLMessage.CFP) );

        try {
            DFService.register(this, dfd);
            addBehaviour(new FuzzyAgentBehaviour(this, template));
        } catch (FIPAException e) {
            logger.log(Logger.SEVERE, "Agent "+getLocalName()+" - Cannot register with DF", e);
            doDelete();
        }
    }


    private ArrayList<Double> processRequest(ArrayList<Double[]> input) {
        ArrayList<Double> result = new ArrayList<>(input.size());

        if (domain.equals("tipper")) {
            for (Double[] variables:input) {
                result.add(evaluateTipper(variables));
            }
        }
        else if (domain.equals("qualityservice")) {
            for (Double[] variables:input) {
                result.add(evaluateQoS(variables));
            }
        }
        return result;
    }
    
    
    private Double evaluateTipper(Double[] variables) {
        fis.setVariable("service", variables[0]);
        fis.setVariable("food", variables[1]);
        fis.evaluate();
        Double tip = fis.getVariable("tip").getLatestDefuzzifiedValue();
        return tip;
    }


    private Double evaluateQoS(Double[] variables) {
        fis.setVariable("commitment", variables[0]);
        fis.setVariable("clarity", variables[1]);
        fis.setVariable("influence", variables[2]);
        fis.evaluate();
        Double serviceQuality = fis.getVariable("service_quality").getLatestDefuzzifiedValue();
        return serviceQuality;
    }
}