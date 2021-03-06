package com.bolenum.repo.order.book;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bolenum.model.Currency;
import com.bolenum.model.User;
import com.bolenum.model.orders.book.Trade;

public interface TradeRepository extends JpaRepository<Trade, Long> {

	@Query("select t from Trade t where (t.buyer =:buyer or t.seller =:seller) and t.createdOn > :startDate and t.createdOn < :endDate")
	Page<Trade> getByBuyerOrSellerWithDate(@Param("buyer") User buyer, @Param("seller") User seller,
			@Param("startDate") Date startDate, @Param("endDate") Date endDate, Pageable pageable);

	Page<Trade> findByBuyerOrSeller(User buyer, User seller, Pageable pageable);

	@Query("select t from Trade t where t.buyer =:buyer and t.createdOn > :startDate and t.createdOn < :endDate")
	Page<Trade> getByBuyerWithDate(@Param("buyer") User buyer, @Param("startDate") Date startDate,
			@Param("endDate") Date endDate, Pageable pageable);

	Page<Trade> findByBuyer(User buyer, Pageable pageable);

	@Query("select t from Trade t where t.seller =:seller and t.createdOn > :startDate and t.createdOn < :endDate")
	Page<Trade> getBySellerWithDate(@Param("seller") User seller, @Param("startDate") Date startDate,
			@Param("endDate") Date endDate, Pageable pageable);

	Page<Trade> findBySeller(User seller, Pageable pageable);

	@Query("select count(t) from Trade t where t.marketCurrency.currencyId=:marketCurrencyId and t.pairedCurrency.currencyId=:pairedCurrencyId and t.createdOn > :endDate")
	long count24hTrade(@Param("marketCurrencyId") long marketCurrencyId,
			@Param("pairedCurrencyId") long pairedCurrencyId, @Param("endDate") Date endDate);

	List<Trade> findByMarketCurrency(Currency currency);

	//@Query(value="select floor(min(UNIX_TIMESTAMP(t.createdOn))/:timeInterval)*:timeInterval as timestamp, sum(t.volume) as volume, SUBSTRING(MIN(CONCAT(t.createdOn,t.price)),LENGTH(t.createdOn)) as open, max(t.price) as high, min(t.price) as low, substring(max(concat(t.createdOn,t.price)),LENGTH(t.createdOn)) as close from Trade t where marketCurrency= :marketCurrency and pairedCurrency= :pairedCurrency and (t.createdOn between :startDate and :endDate) GROUP BY floor(unix_timestamp(t.createdOn)/:timeInterval)")
    @Query(value="select floor(min(UNIX_TIMESTAMP(created_on))/:timeInterval)*:timeInterval as timestamp, sum(volume) as volume, substring_index(min(concat(created_on,'_',price)),'_',-1) as open, max(price) as high, min(price) as low, substring_index(max(concat(created_on,'_',price)),'_',-1) as 'close' from orders where market_currency_currency_id= :marketCurrency and paired_currency_currency_id= :pairedCurrency and (created_on between :startDate and :endDate) group by floor(unix_timestamp(created_on)/:timeInterval)",nativeQuery = true)
    List<Object[]> getTradeHistory(@Param("timeInterval") BigDecimal timeInterval, @Param("marketCurrency") Long marketCurrency, @Param("pairedCurrency") Long pairedCurrency, @Param("startDate") String startDate, @Param("endDate") String endDate);

	@Query(value="select UNIX_TIMESTAMP(created_on) as timestamp, sum(volume) as volume,(SELECT t2.price FROM orders t2 WHERE t2.id = MIN(t.id)) AS open, MAX(t.price) AS high, MIN(t.price) AS low,(SELECT t2.price FROM orders t2 WHERE t2.id = MAX(t.id)) AS close FROM orders t where t.market_currency_currency_id = :marketCurrency and t.paired_currency_currency_id = :pairedCurrency and (t.created_on between :startDate and :endDate) GROUP BY YEAR(t.created_on), MONTH(t.created_on)", nativeQuery = true)
	List<Object[]> getTradeHistoryMonth(@Param("marketCurrency") Long marketCurrency, @Param("pairedCurrency") Long pairCurrency, @Param("startDate") String startDate, @Param("endDate") String endDate);

//	@Query(value="select MAX(createdOn) from Trade where marketCurrency=:marketCurrency and pairCurrency = :pairCurrency where ORDER BY createdOn DESC limit 1,1")
//	Trade getSecondLastPrice(@Param("marketCurrency") Currency marketCurrency, @Param("pairCurrency") Currency pairCurrency);

}
