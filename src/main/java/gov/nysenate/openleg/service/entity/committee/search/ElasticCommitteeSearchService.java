package gov.nysenate.openleg.service.entity.committee.search;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import gov.nysenate.openleg.dao.base.LimitOffset;
import gov.nysenate.openleg.dao.base.SearchIndex;
import gov.nysenate.openleg.dao.entity.committee.search.ElasticCommitteeSearchDao;
import gov.nysenate.openleg.model.base.SessionYear;
import gov.nysenate.openleg.model.entity.CommitteeSessionId;
import gov.nysenate.openleg.model.entity.CommitteeVersionId;
import gov.nysenate.openleg.model.search.*;
import gov.nysenate.openleg.service.base.search.ElasticSearchServiceUtils;
import gov.nysenate.openleg.service.entity.committee.data.CommitteeDataService;
import gov.nysenate.openleg.service.entity.committee.event.CommitteeUpdateEvent;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Collection;

@Service
public class ElasticCommitteeSearchService implements CommitteeSearchService
{
    @Autowired
    ElasticCommitteeSearchDao committeeSearchDao;

    @Autowired
    CommitteeDataService committeeDataService;

    @Autowired
    EventBus eventBus;

    @PostConstruct
    private void init() {
        eventBus.register(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SearchResults<CommitteeVersionId> searchAllCommittees(String query, String sort, LimitOffset limitOffset)
            throws SearchException {
        return searchCommittees(QueryBuilders.queryStringQuery(query), null, sort, limitOffset);
    }

    @Override
    public SearchResults<CommitteeVersionId> searchAllCurrentCommittees(String query, String sort, LimitOffset limitOffset)
            throws SearchException {
        return searchCommittees(QueryBuilders.queryStringQuery(query), getCurrentFilter(), sort, limitOffset);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SearchResults<CommitteeVersionId> searchCommitteesForSession(SessionYear sessionYear, String query,
                                                                        String sort, LimitOffset limitOffset) throws SearchException {
        return searchCommittees(QueryBuilders.queryStringQuery(query), getSessionFilter(sessionYear), sort, limitOffset);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SearchResults<CommitteeVersionId> searchCurrentCommitteesForSession(SessionYear sessionYear, String query,
                                                                               String sort, LimitOffset limitOffset) throws SearchException {
        QueryBuilder currentSessionFilter = QueryBuilders.boolQuery()
                .must(getSessionFilter(sessionYear))
                .must(getCurrentFilter());
        return searchCommittees(QueryBuilders.queryStringQuery(query), currentSessionFilter, sort, limitOffset);
    }

    /**
     * {@inheritDoc}
     */
    @Subscribe
    @Override
    public void handleCommitteeUpdateEvent(CommitteeUpdateEvent committeeUpdateEvent) {
        updateIndex(committeeUpdateEvent.getCommittee().getSessionId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateIndex(CommitteeSessionId content) {
        committeeSearchDao.updateCommitteeIndex(content);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateIndex(Collection<CommitteeSessionId> content) {
        committeeSearchDao.updateCommitteeIndexBulk(content);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearIndex() {
        committeeSearchDao.purgeIndices();
        committeeSearchDao.createIndices();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void rebuildIndex() {
        clearIndex();

        committeeSearchDao.updateCommitteeIndexBulk(committeeDataService.getAllCommitteeSessionIds());
    }

    /**
     * {@inheritDoc}
     */
    @Subscribe
    @Override
    public void handleRebuildEvent(RebuildIndexEvent event) {
        if (event.affects(SearchIndex.COMMITTEE)) {
            rebuildIndex();
        }
    }

    /** {@inheritDoc} */
    @Override
    @Subscribe
    public void handleClearEvent(ClearIndexEvent event) {
        if (event.affects(SearchIndex.COMMITTEE)) {
            clearIndex();
        }
    }

    /** --- Internal Methods --- */

    QueryBuilder getCurrentFilter() {
        return QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("reformed"));
    }

    QueryBuilder getSessionFilter(SessionYear sessionYear) {
        return QueryBuilders.termQuery("sessionYear", sessionYear.getYear());
    }

    private SearchResults<CommitteeVersionId> searchCommittees(QueryBuilder query, QueryBuilder postFilter,
                                                                     String sort, LimitOffset limitOffset) throws SearchException {
        if (limitOffset == null) {
            limitOffset = LimitOffset.ALL;
        }
        try {
            return committeeSearchDao.searchCommittees(query, postFilter,
                    ElasticSearchServiceUtils.extractSortBuilders(sort), limitOffset);
        } catch (SearchParseException ex) {
            throw new SearchException("There was a problem parsing the supplied query string.", ex);
        } catch (ElasticsearchException ex) {
            throw new UnexpectedSearchException(ex);
        }
    }
}