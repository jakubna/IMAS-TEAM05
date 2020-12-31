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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ContractNetResponder;
import net.sourceforge.jFuzzyLogic.FIS;

public class FzzyAgent extends Agent {
    private Logger logger = Logger.getMyLogger(getClass().getName());
    private FIS fis = null;

    private class FuzzyAgentBehaviour extends ContractNetResponder{
        public FuzzyAgentBehaviour(Agent a, MessageTemplate mt) {
            super(a, mt);
            Object[] args = getArguments();
            String name = args[0].toString();
            //System.out.println("-> "+myAgent.getName()+": Has been created with configuration "+name);
        }

        @Override
        protected ACLMessage handleCfp(ACLMessage cfp) throws RefuseException, FailureException, NotUnderstoodException {
            //System.out.println("Agent "+getLocalName()+": CFP received from "+cfp.getSender().getName()+". Action is "+cfp.getContent());
            ArrayList<Float[]> data = null;
            try {
                data = (ArrayList<Float[]>) cfp.getContentObject();
            } catch (UnreadableException e) {
                e.printStackTrace();
            }
            int proposal = (int) (Math.random() * 10);
            if (proposal > 2) {
                // We provide a proposal
                //System.out.println("Agent "+getLocalName()+": Proposing "+proposal);
                ACLMessage propose = cfp.createReply();
                propose.setPerformative(ACLMessage.PROPOSE);
                propose.setContent(String.valueOf(proposal));
                return propose;
            }
            else {
                // We refuse to provide a proposal
                //System.out.println("Agent "+getLocalName()+": Refuse");
                throw new RefuseException("evaluation-failed");
            }
        }

        @Override
        protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) throws FailureException {
            //System.out.println("Agent "+getLocalName()+": Proposal accepted");
            //codicio temporal
            if (cfp != null) {
                //System.out.println("Agent "+getLocalName()+": Action successfully performed");
                ACLMessage inform = accept.createReply();
                inform.setPerformative(ACLMessage.INFORM);
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
        String type = args[0].toString();

        // Registration with the DF
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("FuzzyAgent");
        sd.setType(type);
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