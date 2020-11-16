import java.io.File;
import javax.xml.bind.JAXBContext;
import  javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;


public class app {
    public static void readxml(String filepath) {
        File xmlFile = new File(filepath);
        JAXBContext jaxbContext;
        try
        {
            jaxbContext = JAXBContext.newInstance(config.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            config SimulationSettings = (config)jaxbUnmarshaller.unmarshal(xmlFile);
            System.out.println(SimulationSettings);
        }
        catch (JAXBException e)
        {
            e.printStackTrace();
        }
    }
}

