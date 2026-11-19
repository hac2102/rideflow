package com.rideflow.server.dao;

import com.rideflow.core.model.FareQuote;
import java.util.Optional;

public interface FareQuoteDao {
    long save(FareQuote quote);
    Optional<FareQuote> findById(long quoteId);
}
