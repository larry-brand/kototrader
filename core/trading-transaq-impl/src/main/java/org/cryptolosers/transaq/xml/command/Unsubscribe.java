package org.cryptolosers.transaq.xml.command;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Команда "unsubscribe" , прекратить получение котировок, сделок и глубины рынка (стакана) по
 * одному или нескольким инструментам. */
@XmlRootElement(name = "command")
public class Unsubscribe {
    private String id = "unsubscribe";
    /** подписка на сделки рынка */
    private Subscribe.Alltrades alltrades;
    /** подписка на изменения показателей торгов */
    private Subscribe.Quotations quotations;
    /** подписка на изменения «стакана» */
    private Subscribe.Quotes quotes;

    public String getId() {
        return id;
    }

    @XmlAttribute
    public void setId(String id) {
        this.id = id;
    }

    public Subscribe.Alltrades getAlltrades() {
        return alltrades;
    }

    public void setAlltrades(Subscribe.Alltrades alltrades) {
        this.alltrades = alltrades;
    }

    public Subscribe.Quotations getQuotations() {
        return quotations;
    }

    public void setQuotations(Subscribe.Quotations quotations) {
        this.quotations = quotations;
    }

    public Subscribe.Quotes getQuotes() {
        return quotes;
    }

    public void setQuotes(Subscribe.Quotes quotes) {
        this.quotes = quotes;
    }
}
