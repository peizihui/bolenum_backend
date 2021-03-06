/**
 * 
 */
package com.bolenum.services.user.trade;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bolenum.model.User;
import com.bolenum.model.coin.UserCoin;
import com.bolenum.repo.common.coin.UserCoinRepository;
import com.bolenum.services.common.coin.Erc20TokenService;
import com.bolenum.util.GenericUtils;
import com.bolenum.util.ResourceUtils;
import com.neemre.btcdcli4j.core.BitcoindException;
import com.neemre.btcdcli4j.core.CommunicationException;
import com.neemre.btcdcli4j.core.client.BtcdClient;

/**
 * @author chandan kumar singh
 * @date 19-Dec-2017
 */
@Service
public class TradeTransactionServiceImpl implements TradeTransactionService {
	private static Logger logger = LoggerFactory.getLogger(TradeTransactionServiceImpl.class);


	@Autowired
	private UserCoinRepository userCoinRepository;
	@Autowired
	private Erc20TokenService erc20TokenService;


	/**
	 * this will perform trade transaction for buyer to seller
	 */
	@Override

	public Boolean performTradeTransaction(double tfee, String currencyAbr, String currencyType, double qtyTraded,
			User buyer, User seller, Long tradeId) {
		Boolean txStatus;

		switch (currencyType) {
		case "CRYPTO":
			if ("BTC".equalsIgnoreCase(currencyAbr)) {
				logger.debug("BTC trade started");
				txStatus = performBtcTrade(seller, buyer, qtyTraded, tradeId);
				logger.debug("is BTC trade successed: {}", txStatus);
				/**
				 * if trade for users, then return result with mail notification to users
				 */
				if (txStatus) {
					return true;
				}
			} else if ("ETH".equalsIgnoreCase(currencyAbr)) {
				logger.debug("ETH transaction started");
				txStatus = performEthTrade(seller, currencyAbr, buyer, qtyTraded, tradeId);
				logger.debug("is ETH transaction successed: {}", txStatus);
				/**
				 * if transaction for users, then return result with mail notification to users
				 */
				if (txStatus) {
					return true;
				}
			}
			break;
		case "ERC20TOKEN":
			logger.debug("ERC20TOKEN trade started");
			txStatus = performErc20Trade(seller, currencyAbr, buyer, qtyTraded, tradeId);
			logger.debug("is ERC20TOKEN trade successed: {}", txStatus);
			/**
			 * if transaction for users, then return result with mail notification to users
			 */
			if (txStatus) {
				return txStatus;
			}
			break;
		default:
			break;
		}
		return false;
	}


	/**
	 * @description perform Erc20token Trade
	 * @param seller
	 * @param currencyAbr
	 * @param buyer
	 * @param qtyTraded
	 * @param tradeId
	 * @return Boolean
	 * 
	 */
	@Override
	public Boolean performErc20Trade(User seller, String currencyAbr, User buyer, double qtyTraded, Long tradeId) {
		UserCoin userErc20TokenSeller = userCoinRepository.findByTokenNameAndUser(currencyAbr, seller);
		UserCoin userErc20TokenBuyer = userCoinRepository.findByTokenNameAndUser(currencyAbr, buyer);
		if (userErc20TokenSeller == null || userErc20TokenBuyer == null) {
			erc20TokenService.createErc20Wallet(buyer, currencyAbr);
			erc20TokenService.createErc20Wallet(seller, currencyAbr);
			return false;
		}
		if (userErc20TokenSeller.getBalance() > 0) {
			Double newBalanceSeller = userErc20TokenSeller.getBalance() - qtyTraded;
			Double newBalanceBuyer = userErc20TokenBuyer.getBalance() + qtyTraded;

			logger.debug("seller: {}, existing balance: {} {}", seller.getEmailId(), userErc20TokenSeller.getBalance(),
					currencyAbr);
			logger.debug("After trade seller:{}, new balance: {} {}", seller.getEmailId(),
					GenericUtils.getDecimalFormatString(newBalanceSeller), currencyAbr);
			userErc20TokenSeller.setBalance(newBalanceSeller);

			logger.debug("buyer: {}, existing balance: {} {}", buyer.getEmailId(), userErc20TokenBuyer.getBalance(),
					currencyAbr);
			logger.debug("After trade buyer:{}, new balance: {} {}", buyer.getEmailId(),
					GenericUtils.getDecimalFormatString(newBalanceBuyer), currencyAbr);

			userErc20TokenBuyer.setBalance(newBalanceBuyer);
			logger.debug("saving trade transaction started for: {} tradeId: {}", currencyAbr, tradeId);
			userCoinRepository.save(userErc20TokenBuyer);
			userCoinRepository.save(userErc20TokenSeller);
			logger.debug("saving trade transaction completed for: {} tradeId: {}", currencyAbr, tradeId);
			return true;
		}
		return false;
	}

	/**
	 * @description perform eth Trade
	 * @param seller
	 * @param currencyAbr
	 * @param buyer
	 * @param qtyTraded
	 * @param tradeId
	 * @return Boolean
	 * 
	 */
	@Override
	public Boolean performEthTrade(User seller, String currencyAbr, User buyer, double qtyTraded, Long tradeId) {
		UserCoin userEthSeller = userCoinRepository.findByTokenNameAndUser(currencyAbr, seller);
		UserCoin userEthBuyer = userCoinRepository.findByTokenNameAndUser(currencyAbr, buyer);
		if (userEthSeller.getBalance() > 0) {
			Double newBalanceSeller = userEthSeller.getBalance() - qtyTraded;
			Double newBalanceBuyer = userEthBuyer.getBalance() + qtyTraded;

			logger.debug("seller: {}, existing balance: {} {}", seller.getEmailId(), userEthSeller.getBalance(),
					currencyAbr);
			logger.debug("After trade seller:{}, new balance: {} {}", seller.getEmailId(),
					GenericUtils.getDecimalFormatString(newBalanceSeller), currencyAbr);
			userEthSeller.setBalance(newBalanceSeller);

			logger.debug("buyer: {}, existing balance: {} {}", buyer.getEmailId(), userEthBuyer.getBalance(),
					currencyAbr);
			logger.debug("After trade buyer:{}, new balance: {} {}", buyer.getEmailId(),
					GenericUtils.getDecimalFormatString(newBalanceBuyer), currencyAbr);

			userEthBuyer.setBalance(newBalanceBuyer);
			logger.debug("saving trade transaction started for: {} tradeId: {}", currencyAbr, tradeId);
			userCoinRepository.save(userEthBuyer);
			userCoinRepository.save(userEthSeller);
			logger.debug("saving trade transaction completed for: {} tradeId: {}", currencyAbr, tradeId);
			return true;
		}
		return false;
	}

	/**
	 * @description perform btc Trade
	 * @param seller
	 * @param currencyAbr
	 * @param buyer
	 * @param qtyTraded
	 * @param tradeId
	 * @return Boolean
	 * 
	 */
	@Override
	public Boolean performBtcTrade(User fromUser, User toUser, double qtyTraded, Long tradeId) {
		try {
			BtcdClient client = ResourceUtils.getBtcdProvider();
			logger.debug("tx fee: {}", client.estimateFee(2));
			String fromAccount = fromUser.getUserId().toString();
			String toAccount = toUser.getUserId().toString();
			if (fromUser.getBtcWalletUuid().isEmpty()) {
				fromAccount = fromUser.getBtcWalletUuid();
			}
			if (toUser.getBtcWalletUuid().isEmpty()) {
				toAccount = toUser.getBtcWalletUuid();
			}
			logger.debug("from btc account: {} to btc account: {}", fromAccount, toAccount);
			BigDecimal currentBal = client.getBalance(fromAccount);
			BigDecimal balance = BigDecimal.valueOf(qtyTraded);
			logger.debug("user: {} has current account balance:{} and trade amount: {}", fromUser.getEmailId(),
					GenericUtils.getDecimalFormatString(currentBal.doubleValue()),
					GenericUtils.getDecimalFormatString(balance.doubleValue()));
			logger.debug("Comparaison result of BTC balance move: {}", currentBal.compareTo(balance) < 0);
			if (currentBal.compareTo(balance) < 0) {
				logger.debug("user: {} dont have sufficient BTC balance to move of tradeId:{}", fromUser.getEmailId(),
						tradeId);
				return false;
			}
			boolean res = client.move(fromAccount, toAccount, balance);
			logger.debug("btc trade res: {}", res);
			if (res) {
				logger.info("from user: {} to user: {} amount: {} for tradeId: {} success", fromUser.getEmailId(),
						toUser.getEmailId(), GenericUtils.getDecimalFormatString(balance.doubleValue()), tradeId);
				return res;
			}
			logger.error("from user: {} to user: {} amount: {} for tradeId: {} failed", fromUser.getEmailId(),
					toUser.getEmailId(), GenericUtils.getDecimalFormatString(balance.doubleValue()), tradeId);
		} catch (BitcoindException | CommunicationException e) {
			logger.error("BTC trade error: {}", e);
		}
		return false;
	}

	@Override
	public Boolean performTradeTransactionFee(String currencyAbr, String currencyType, double tradeFee, User buyer,
			User seller, Long tradeId) {
		Boolean txStatus;
		switch (currencyType) {
		case "CRYPTO":
			if ("BTC".equalsIgnoreCase(currencyAbr)) {
				logger.debug("BTC trade fee started");
				txStatus = performBtcTrade(seller, buyer, tradeFee, tradeId);
				logger.debug("is BTC trade fee successed: {}", txStatus);
				if (txStatus) {
					return true;
				}
			} else if ("ETH".equalsIgnoreCase(currencyAbr)) {
				logger.debug("ETH trade fee started");
				txStatus = performEthTrade(seller, currencyAbr, buyer, tradeFee, tradeId);
				logger.debug("is ETH trade fee successed: {}", txStatus);
				if (txStatus) {
					return true;
				}
			}
			break;
		case "ERC20TOKEN":
			logger.debug("ERC20TOKEN trade fee started");
			txStatus = performErc20Trade(seller, currencyAbr, buyer, tradeFee, tradeId);
			logger.debug("is ERC20TOKEN trade fee successed: {}", txStatus);
			if (txStatus) {
				return txStatus;
			}
			break;
		default:
			break;
		}
		return false;
	}
}
