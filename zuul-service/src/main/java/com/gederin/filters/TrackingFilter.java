package com.gederin.filters;

import com.gederin.util.FilterUtils;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
/**
 * All Zuul filters must extend the ZuulFilter class and override four methods: filterType(), filterOrder(), shouldFilter(), and run()
 */
public class TrackingFilter extends ZuulFilter {

    private static final int FILTER_ORDER = 1;
    private static final boolean SHOULD_FILTER = true;

    private static final Logger logger = LoggerFactory.getLogger(TrackingFilter.class);

    private final FilterUtils filterUtils;

    /**
     * The filterType() method is used to tell Zuul whether the
     * filter is a pre-, route, or post filter.
     */
    @Override
    public String filterType() {
        return FilterUtils.PRE_FILTER_TYPE;
    }

    /**
     * The filterOrder() method returns an integer value indicating what order
     * Zuul should send requests through the different filter types.
     */
    @Override
    public int filterOrder() {
        return FILTER_ORDER;
    }

    /**
     * The shouldFilter() method returns a Boolean indicating
     * whether or not the filter should be active.
     */
    @Override
    public boolean shouldFilter() {
        return SHOULD_FILTER;
    }

    /***
     * The run() method is the code that is executed every time a service passes through the filter.
     * In your run() function, you check to see if the correlation-id is present
     * and if it isn’t, you generate a correlation value and set the correlation-id HTTP
     */
    private boolean isCorrelationIdPresent() {
        return filterUtils.getCorrelationId() != null;

    }

    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public Object run() {

        if (isCorrelationIdPresent()) {
            logger.debug("correlation-id found in tracking filter: {}. ", filterUtils.getCorrelationId());
        } else {
            filterUtils.setCorrelationId(generateCorrelationId());
            logger.debug("correlation-id generated in tracking filter: {}.", filterUtils.getCorrelationId());
        }

        RequestContext ctx = RequestContext.getCurrentContext();
        logger.debug("Processing incoming request for {}.", ctx.getRequest().getRequestURI());

        return null;
    }
}