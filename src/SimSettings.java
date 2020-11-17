import java.io.File;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "SimulationSettings")
@XmlAccessorType(XmlAccessType.FIELD)
public class SimSettings {
    private String title;
    private String application;
    private Integer fuzzyagents;
    private String fuzzySettings;
    private String aggregation;

    public  SimSettings(){
        super();
    }

    public String getTitle() {
        return title;
    }

    public String getApplication() {
        return application;
    }

    public Integer getFuzzyagents() {
        return fuzzyagents;
    }

    public String[] getFuzzySettings() {
        return fuzzySettings.split(",");
    }

    public String getAggregation() {
        return aggregation;
    }

    public static SimSettings fromXML(String filepath) {
        File xmlFile = new File(filepath);
        JAXBContext jaxbContext;
        SimSettings simSettings = null;
        try
        {
            jaxbContext = JAXBContext.newInstance(SimSettings.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            simSettings = (SimSettings)jaxbUnmarshaller.unmarshal(xmlFile);
            System.out.println(simSettings);
        }
        catch (JAXBException e)
        {
            e.printStackTrace();
        }
        return simSettings;
    }
    @Override
    public String toString() {
        return "\n SimulationSettings: \n - Title = " + title
                + "\n - Application = " + application + "\n - N fuzzy agents = "
                + fuzzyagents + "\n - Fuzzy settings = "+ fuzzySettings
                + "\n - Aggregation = "+ aggregation ;
    }
}
