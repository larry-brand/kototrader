package org.cryptolosers.transaq.xml.callback;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

@XmlRootElement(name = "candlekinds")
public class Candlekinds {

    private List<Kind> kind;

    public List<Kind> getKind() {
        return kind;
    }

    public void setKind(List<Kind> kind) {
        this.kind = kind;
    }

    @XmlType(name="Candlekinds.Kind")
    public static class Kind {
        private Long id;
        private String period;
        private String name;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getPeriod() {
            return period;
        }

        public void setPeriod(String period) {
            this.period = period;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

    }
}

