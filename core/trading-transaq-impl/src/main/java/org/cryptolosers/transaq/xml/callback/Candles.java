package org.cryptolosers.transaq.xml.callback;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.math.BigDecimal;
import java.util.List;

@XmlRootElement(name = "candles")
public class Candles {

    private Long secid;
    private Long period;
    private Long status;
    private String board;
    private String seccode;
    private List<Candle> candle;

    public Long getSecid() {
        return secid;
    }

    @XmlAttribute
    public void setSecid(Long secid) {
        this.secid = secid;
    }

    public Long getPeriod() {
        return period;
    }

    @XmlAttribute
    public void setPeriod(Long period) {
        this.period = period;
    }

    public Long getStatus() {
        return status;
    }

    @XmlAttribute
    public void setStatus(Long status) {
        this.status = status;
    }

    public String getBoard() {
        return board;
    }

    @XmlAttribute
    public void setBoard(String board) {
        this.board = board;
    }

    public String getSeccode() {
        return seccode;
    }

    @XmlAttribute
    public void setSeccode(String seccode) {
        this.seccode = seccode;
    }

    public List<Candle> getCandle() {
        return candle;
    }

    public void setCandle(List<Candle> candle) {
        this.candle = candle;
    }

    @XmlType(name="Candles.Candle")
    public static class Candle {
        private String date;
        private BigDecimal open;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal close;
        private Long volume;
        private Long oi;

        public String getDate() {
            return date;
        }

        @XmlAttribute
        public void setDate(String date) {
            this.date = date;
        }

        public BigDecimal getOpen() {
            return open;
        }

        @XmlAttribute
        public void setOpen(BigDecimal open) {
            this.open = open;
        }

        public BigDecimal getHigh() {
            return high;
        }

        @XmlAttribute
        public void setHigh(BigDecimal high) {
            this.high = high;
        }

        public BigDecimal getLow() {
            return low;
        }

        @XmlAttribute
        public void setLow(BigDecimal low) {
            this.low = low;
        }

        public BigDecimal getClose() {
            return close;
        }

        @XmlAttribute
        public void setClose(BigDecimal close) {
            this.close = close;
        }

        public Long getVolume() {
            return volume;
        }

        @XmlAttribute
        public void setVolume(Long volume) {
            this.volume = volume;
        }

        public Long getOi() {
            return oi;
        }

        @XmlAttribute
        public void setOi(Long oi) {
            this.oi = oi;
        }
    }
}

