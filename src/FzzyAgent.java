import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
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

public class FzzyAgent extends Agent {
    private Logger logger = Logger.getMyLogger(getClass().getName());
    private class FuzzyAgentBehaviour extends ContractNetResponder{
        public FuzzyAgentBehaviour(Agent a, MessageTemplate mt) {
            super(a, mt);
            Object[] args = getArguments();
            String name = args[0].toString();
            System.out.println("-> "+myAgent.getName()+": Has been created with configuration "+name);
        }

        @Override
        protected ACLMessage handleCfp(ACLMessage cfp) throws RefuseException, FailureException, NotUnderstoodException {
            System.out.println("Agent "+getLocalName()+": CFP received from "+cfp.getSender().getName()+". Action is "+cfp.getContent());
            int proposal = (int) (Math.random() * 10);
            if (proposal > 2) {
                // We provide a proposal
                System.out.println("Agent "+getLocalName()+": Proposing "+proposal);
                ACLMessage propose = cfp.createReply();
                propose.setPerformative(ACLMessage.PROPOSE);
                propose.setContent(String.valueOf(proposal));
                return propose;
            }
            else {
                // We refuse to provide a proposal
                System.out.println("Agent "+getLocalName()+": Refuse");
                throw new RefuseException("evaluation-failed");
            }
        }

        @Override
        protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) throws FailureException {
            System.out.println("Agent "+getLocalName()+": Proposal accepted");
            //codicio temporal
            if (cfp != null) {
                System.out.println("Agent "+getLocalName()+": Action successfully performed");
                ACLMessage inform = accept.createReply();
                inform.setPerformative(ACLMessage.INFORM);
                return inform;
            }
            else {
                System.out.println("Agent "+getLocalName()+": Action execution failed");
                throw new FailureException("unexpected-error");
            }
        }

        @Override
        protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
            System.out.println("Agent "+getLocalName()+": Proposal rejected");
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
}