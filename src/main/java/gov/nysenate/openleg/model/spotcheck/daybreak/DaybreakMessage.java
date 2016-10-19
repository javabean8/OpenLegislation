package gov.nysenate.openleg.model.spotcheck.daybreak;

import gov.nysenate.openleg.util.DateUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.mail.Message;
import javax.mail.MessagingException;
import java.time.LocalDateTime;

public class DaybreakMessage implements DaybreakDocument {

    protected Message message;
    protected DaybreakDocType daybreakDocType;
    protected LocalDateTime reportDateTime;

    public DaybreakMessage(Message message) throws MessagingException {
        this.message = message;
        this.daybreakDocType = DaybreakDocType.getMessageDocType(message.getSubject());
        this.reportDateTime = DateUtils.getLocalDateTime(message.getSentDate());
    }

    @Override
    public DaybreakDocType getDaybreakDocType() {
        return daybreakDocType;
    }

    @Override
    public LocalDateTime getReportDateTime() {
        return reportDateTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("message", message)
                .append("daybreakDocType", daybreakDocType)
                .append("reportDateTime", reportDateTime)
                .toString();
    }

    public Message getMessage() {
        return message;
    }
}
