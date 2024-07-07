package org.cryptolosers.transaq.xml.command;

import org.cryptolosers.transaq.xml.command.internal.Security;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Команда "subscribe" , подписаться на получение котировок, сделок и глубины рынка (стакана) по
 * одному или нескольким инструментам. */
@XmlRootElement(name = "command")
public class Subscribe {
    private String id = "subscribe";
    /** подписка на сделки рынка */
    private Alltrades alltrades;
    /** подписка на изменения показателей торгов */
    private Quotations quotations;
    /** подписка на изменения «стакана» */
    private Quotes quotes;

    public String getId() {
        return id;
    }

    @XmlAttribute
    public void setId(String id) {
        this.id = id;
    }

    public Alltrades getAlltrades() {
        return alltrades;
    }

    public void setAlltrades(Alltrades alltrades) {
        this.alltrades = alltrades;
    }

    public Quotations getQuotations() {
        return quotations;
    }

    public void setQuotations(Quotations quotations) {
        this.quotations = quotations;
    }

    public Quotes getQuotes() {
        return quotes;
    }

    public void setQuotes(Quotes quotes) {
        this.quotes = quotes;
    }

    public static class Alltrades {
        private List<Security> security = new ArrayList<>();

        public List<Security> getSecurity() {
            return security;
        }

        public void setSecurity(List<Security> security) {
            this.security = security;
        }
    }

    public static class Quotations {
        private List<Security> security = new ArrayList<>();

        public List<Security> getSecurity() {
            return security;
        }

        public void setSecurity(List<Security> security) {
            this.security = security;
        }
    }

    public static class Quotes {
        private List<Security> security = new ArrayList<>();

        public List<Security> getSecurity() {
            return security;
        }

        public void setSecurity(List<Security> security) {
            this.security = security;
        }
    }

}
