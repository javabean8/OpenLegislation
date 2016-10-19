package gov.nysenate.openleg.dao.spotcheck.bills;

import gov.nysenate.openleg.model.spotcheck.SpotCheckRefType;
import org.springframework.stereotype.Repository;

/**
 * Created by PKS on 10/13/16.
 */
@Repository
public class DaybreakBillIdSpotCheckReportDao extends BaseBillIdSpotCheckReportDao {
    public DaybreakBillIdSpotCheckReportDao(){
        super(SpotCheckRefType.LBDC_DAYBREAK);
    }
}
