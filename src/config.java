import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement(name = "SimulationSettings")
@XmlAccessorType(XmlAccessType.FIELD)
public class config implements Serializable {
    private static final long serialVersionUID = 1L;
    private String title;
    private String application;
    private Integer fuzzyagents;
    private String fuzzySettings;
    private String aggregation;

    public  config(){
        super();
    }

    public config(String title_, String app, int nfuzzyagents, String fuzzy_settings, String aggregation_){
       super();
       this.title = title_;
       this.application=app;
       this.fuzzyagents= nfuzzyagents;
       this.fuzzySettings=fuzzy_settings;
       this.aggregation=aggregation_;
    }
    @Override
    public String toString() {
        return "\n SimulationSettings: \n - Title = " + title + "\n - Application = " + application + "\n - N fuzzy agents = " + fuzzyagents + "\n - Fuzzy settings = "+ fuzzySettings + "\n - Aggregation = "+ aggregation ;
    }
}
