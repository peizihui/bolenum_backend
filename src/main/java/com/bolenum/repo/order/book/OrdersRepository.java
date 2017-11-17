package com.bolenum.repo.order.book;

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bolenum.enums.OrderStatus;
import com.bolenum.enums.OrderType;
import com.bolenum.model.Currency;
import com.bolenum.model.CurrencyPair;
import com.bolenum.model.User;
import com.bolenum.model.orders.book.Orders;

/**
 * 
 * @author Vishal kumar
 * @date 06-Oct-2017
 *
 */
public interface OrdersRepository extends JpaRepository<Orders, Long> {
	// Double getSumVolumeByPairId(Long pairId);

	List<Orders> findByOrderTypeAndOrderStatusAndPairAndPriceLessThanEqualOrderByPriceAsc(OrderType ordertype,
			OrderStatus orderStatus, CurrencyPair pair, Double price);

	List<Orders> findByOrderTypeAndOrderStatusAndPairAndPriceGreaterThanEqualOrderByPriceDesc(OrderType ordertype,
			OrderStatus orderStatus, CurrencyPair pair, Double price);

	List<Orders> findByOrderTypeAndOrderStatusAndPairOrderByPriceAsc(OrderType ordertype, OrderStatus orderStatus,
			CurrencyPair pairId);

	List<Orders> findByOrderTypeAndOrderStatusAndPairOrderByPriceDesc(OrderType ordertype, OrderStatus orderStatus,
			CurrencyPair pairId);

	@Query("select min(o.price) from Orders o where o.orderType = 'SELL' and o.orderStatus = 'SUBMITTED' and o.pair= :pair")
	Double getBestBuy(@Param("pair") CurrencyPair pair);

	@Query("select max(o.price) from Orders o where o.orderType = 'SELL' and o.orderStatus = 'SUBMITTED' and o.pair= :pair")
	Double getWrostBuy(@Param("pair") CurrencyPair pair);

	@Query("select max(o.price) from Orders o where o.orderType = 'BUY' and o.orderStatus = 'SUBMITTED' and o.pair= :pair")
	Double getBestSell(@Param("pair") CurrencyPair pair);

	@Query("select min(o.price) from Orders o where o.orderType = 'BUY' and o.orderStatus = 'SUBMITTED' and o.pair= :pair")
	Double getWrostSell(@Param("pair") CurrencyPair pair);

	@Query("select count(o) from Orders o where o.orderType = :orderType and o.orderStatus = 'SUBMITTED' and o.pair= :pair and o.price >= :price")
	Long countOrderByOrderTypeAndPriceGreaterThan(@Param("orderType") OrderType orderType, @Param("pair") CurrencyPair pair,
			@Param("price") Double price);

	@Query("select count(o) from Orders o where o.orderType = :orderType and o.orderStatus = 'SUBMITTED' and o.pair= :pair and o.price <= :price")
	Long countOrderByOrderTypeAndPriceLessThan(@Param("orderType") OrderType orderType, @Param("pair") CurrencyPair pair, @Param("price") Double price);

	@Query("select count(o) from Orders o where o.orderType = :orderType and o.orderStatus = 'SUBMITTED'")
	Long countOrderByOrderType(@Param("orderType") OrderType orderType);
	
	@Query("select o from Orders o where o.pair = :pair and o.orderType = :orderType and o.orderStatus = :orderStatus order by o.price asc")
	Page<Orders> findBuyOrderList(@Param("pair")CurrencyPair pair, @Param("orderType") OrderType orderType, @Param("orderStatus") OrderStatus orderStatus, Pageable pageable);
	
	@Query("select o from Orders o where o.pair = :pair and o.orderType = :orderType and o.orderStatus = :orderStatus order by o.price desc")
	Page<Orders> findSellOrderList(@Param("pair")CurrencyPair pair, @Param("orderType") OrderType orderType, @Param("orderStatus") OrderStatus orderStatus, Pageable pageable);

	List<Orders> findByUserAndOrderStatus(User user, OrderStatus orderStatus);
	
	List<Orders> findByUserAndOrderStatusAndOrderTypeAndPairToCurrency(User user, OrderStatus orderStatus,OrderType orderType, Currency currency);
	
	@Query("select SUM(o.price) from Orders o where o.orderType = 'SELL' and o.user = :user and (o.pair.toCurrency = :toCurrencyList or o.pair.pairedCurrency = :pairedCurrencyList)")
	Double totalUserBalanceInBook(@Param("user") User user, @Param("toCurrency") Currency toCurrency, @Param("pairedCurrency") Currency pairedCurrency);

	//@Query("SELECT e FROM Events e WHERE e.eventsDate BETWEEN :startDate AND :endDate")
	Long countOrdersByCreatedOnBetween(Date startDate,Date endDate);
	
	Long countOrderByOrderTypeAndCreatedOnBetween(OrderType orderType,Date startDate,Date endDate);
	
	Long countOrderByUserAndOrderType(User user,OrderType orderType);
	
	Orders findByMatchedOrder(Orders orders);

	//Page<Orders> findByCreatedOnIsBetween(Date startDate, Date endDate, PageRequest pageRequest);

	Page<Orders> findByCreatedOnBetween(Date startDate, Date endDate, Pageable page);
}
