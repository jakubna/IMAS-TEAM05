import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

import jade.util.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jade.proto.ContractNetInitiator;

public class MngrAgent extends Agent {
    private Logger logger = Logger.getMyLogger(getClass().getName());

    private class MnrAgentContractNet extends ContractNetInitiator{
        private int nResponders;
        private LinkedList<List<Double>> results = new LinkedList<>();
        private String file;
        long startTime;

        public MnrAgentContractNet(Agent a, ACLMessage cfp, int nResponders, String req_file) {
            super(a, cfp);
            this.nResponders = nResponders; // expected responders
            this.file = req_file;
            startTime = System.currentTimeMillis();
        }

        protected void handleFailure(ACLMessage failure) {
            nResponders--;
        }

        protected void handleAllResponses(Vector responses, Vector acceptances) {
            // Evaluate proposals.
            Enumeration e = responses.elements();
            while (e.hasMoreElements()) {
                ACLMessage msg = (ACLMessage) e.nextElement();
                if (msg.getPerformative() == ACLMessage.PROPOSE) {
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    acceptances.addElement(reply);
                }
            }
        }

        protected void handleInform(ACLMessage inform) {
            // accumulate results
            try {
                ArrayList<Double> result = (ArrayList<Double>) inform.getContentObject();
                results.add(result);
            } catch (UnreadableException e) {
                e.printStackTrace();
            }
            // when all results have been received
            if (results.size() == nResponders) {
                // aggregate
                ArrayList<Double> agg_results = aggregate(results);
                long elapsedTime = System.currentTimeMillis() - startTime;

                // save result to file
                String res_file = "result_" + this.file.split(File.separator)[1];
                Path outfile = Paths.get("files").resolve(res_file);
                try {
                    FileWriter writer = new FileWriter(outfile.toString());
                    writer.write("Time elapsed in milliseconds: "+elapsedTime+"\n");
                    writer.write("Aggregated results: \n");
                    for (Double val:agg_results) {
                        writer.write(val + "\n");
                    }
                    writer.write("\nRaw values returned by Fuzzy Agents:\n");
                    for (List<Double> res:results){
                        writer.write(res + "\n");
                    }

                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // send final result to user agent
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(new AID("user", AID.ISLOCALNAME));
                msg.setContent("Results saved in '"+res_file+"'.");
                msg.setEncoding("String");
                send(msg);
            }
        }

        @Override
        public int onEnd() {
            addBehaviour(new MngrAgentServeLoop());
            return super.onEnd();
        }
    }

    private class MngrAgentServeLoop extends SimpleBehaviour { // CyclicBehaviour
        AID user = new AID("user", AID.ISLOCALNAME);
        List<String> fuzzyAgents = new ArrayList<String>();
        boolean finished = false;
        @Override
        public void action() {
            ACLMessage msg = blockingReceive(MessageTemplate.MatchSender(user));

            if(msg!=null) {
                String cnt = msg.getContent();
                String type = cnt.substring(0, 2);
                String file = cnt.substring(2);

                if (type.equals("I_")) {
                    boolean found = false;
                    // Initialize instruction
                    // Read settings
                    SimSettings conf = SimSettings.fromXML(file);
                    if (conf == null) {
                        msg = new ACLMessage(ACLMessage.INFORM);
                        msg.addReceiver(user);
                        msg.setContent("Could not process configuration file. Format does not seem to correspond.");
                        msg.setEncoding("String");
                        send(msg);
                        finished = false;
                    } else {
                        // Fuzzy agents creation
                        String[] fuzzyS = conf.getFuzzySettings();
                        String domain = conf.getApplication();
                        ContainerController container = getContainerController();
                        for (int i = 0; i < conf.getFuzzyagents(); i++) {
                            String name = "FS" + i + domain + fuzzyS[i];
                            if (!fuzzyAgents.contains(name)) {
                                Object[] args = new Object[2];
                                args[0] = domain;
                                args[1] = fuzzyS[i];
                                try {
                                    AgentController a = container.createNewAgent(name, "FzzyAgent", args);
                                    a.start();
                                    fuzzyAgents.add(name);
                                } catch (StaleProxyException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                found = true;
                            }
                        }
                        msg = new ACLMessage(ACLMessage.INFORM);
                        msg.addReceiver(user);
                        if (found) {
                            msg.setContent("Found '" + domain + "' already initialised.");
                        } else {
                            msg.setContent("Configuration '" + domain + "' initialised.");
                        }
                        msg.setEncoding("String");
                        send(msg);
                        finished = false;
                    }
                    // sleep to wait FA until ContractNet used
                    try {
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else if (type.equals("D_")) {
                    byte[] encoded = null;
                    try {
                        encoded = Files.readAllBytes(Paths.get(file));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    String request = new String(encoded, StandardCharsets.UTF_8);

                    if (!request.matches("\\w+\n(([0-9]+(\\.[0-9]+)?,?)+\n?)+")) {
                        msg = new ACLMessage(ACLMessage.INFORM);
                        msg.addReceiver(user);
                        msg.setContent("Input file not following request format.");
                        msg.setEncoding("String");
                        send(msg);
                        finished = false;
                        return;
                    }

                    String [] req_spl = request.split("\n");
                    String domain = req_spl[0];
                    String[] data = Arrays.copyOfRange(req_spl, 1, req_spl.length);

                    ArrayList<Double[]> matrix = new ArrayList<>();
                    for (String d: data) {
                        Double[] row = Arrays.stream(d.split(",")).map(Double::valueOf).toArray(Double[]::new);
                        matrix.add(row);
                    }

                    ServiceDescription sd = new ServiceDescription();
                    DFAgentDescription dfd = new DFAgentDescription();
                    sd.setType(domain);
                    dfd.addServices(sd);
                    DFAgentDescription[] result = null;
                    try {
                        result = DFService.search(myAgent, dfd);
                    } catch (FIPAException e) {
                        e.printStackTrace();
                    }
                    if (result.length > 0) {
                        ACLMessage msg2 = new ACLMessage(ACLMessage.CFP);
                        for (int i = 0; i < result.length; i++) {
                            msg2.addReceiver(result[i].getName());
                        }
                        msg2.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);

                        // We want to receive a reply in 10 secs
                        msg2.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
                        try {
                            msg2.setContentObject(matrix);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        myAgent.addBehaviour(new MnrAgentContractNet(myAgent, msg2, result.length, file));
                        finished = true;
                    } else {
                        msg = new ACLMessage(ACLMessage.INFORM);
                        msg.addReceiver(user);
                        msg.setContent("Configuration '"+domain+"' not previously initialised.");
                        msg.setEncoding("String");
                        send(msg);
                        finished = false;
                    }
                }
            }
        }

        public boolean done() {
            return finished;
        }
    }

    protected void setup() {
        // Registration with the DF
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("ManagerAgent");
        sd.setName(getName());
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

    protected ArrayList<Double> aggregate(List<List<Double>> results) {
        //  aggregation = average
        ArrayList<Double> sum_results = new ArrayList<>(Collections.nCopies(results.get(0).size(), 0.));

        for (List<Double> res:results) {
            for (int j=0; j<res.size(); j++) {
                sum_results.set(j, sum_results.get(j) + res.get(j));
            }
        }

        List<Double> avg_results = sum_results.stream().map(d -> d / results.size()).collect(Collectors.toList());
        return new ArrayList<>(avg_results);
    }
}
