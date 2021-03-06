/**
 * 
 */
package com.bolenum.services.user.wallet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.bolenum.enums.OrderStandard;
import com.bolenum.model.Currency;
import com.bolenum.model.User;
import com.bolenum.model.coin.Erc20Token;
import com.bolenum.model.coin.UserCoin;
import com.bolenum.model.orders.book.Orders;
import com.bolenum.services.common.coin.Erc20TokenService;
import com.bolenum.util.GenericUtils;

/**
 * @author chandan kumar singh
 * @date 17-Oct-2017
 */
@Service
public class WalletServiceImpl implements WalletService {

	private Logger logger = LoggerFactory.getLogger(WalletServiceImpl.class);

	@Autowired
	@Lazy
	private BTCWalletService btcWalletService;

	@Autowired
	private EtherumWalletService etherumWalletService;

	@Autowired
	private Erc20TokenService erc20TokenService;

	/**
	 * to get the balance of user wallet
	 * 
	 * @param currency
	 *            Abbrivation (ETH, BTC, BLN)
	 * @param currencyType,
	 *            CRYPTO, ERC20TOKEN
	 * @param user
	 * @return balance of user wallet
	 */
	@Override
	public String getBalance(String ticker, String currencyType, User user) {
		logger.debug("get wallet balance ticker: {}", ticker);
		String balance = "0";
		switch (currencyType) {
		case "CRYPTO":
			switch (ticker) {
			case "BTC":
				balance = btcWalletService.getBtcAccountBalance(user.getBtcWalletUuid());
				break;
			case "ETH":
				UserCoin userCoin = etherumWalletService.ethWalletBalance(user, ticker);
				balance = String.valueOf(userCoin.getBalance());
				break;
			default:
			}
			break;
		case "ERC20TOKEN":
			Erc20Token erc20Token = erc20TokenService.getByCoin(ticker);
			balance = String.valueOf(erc20TokenService.erc20WalletBalance(user, erc20Token).getBalance());
			break;
		case "FIAT":
			break;
		default:

		}
		logger.debug("get wallet balance: {} of User: {}", balance, user.getEmailId());
		return balance;
	}

	@Override
	public String getPairedBalance(Orders orders, Currency marketCurrency, Currency pairedCurrency, double qtyTraded) {
		String minBalance = "0.0";
		/**
		 * if order type is BUY then for Market order, user should have total market
		 * price, for Limit order user should have volume (volume * price), price limit
		 * given by user
		 */
		if (OrderStandard.LIMIT.equals(orders.getOrderStandard())) {
			logger.debug("limit order buy on price: {} {} and quantity trading: {} {} ",
					GenericUtils.getDecimalFormatString(orders.getPrice()), marketCurrency.getCurrencyAbbreviation(),
					GenericUtils.getDecimalFormatString(qtyTraded), pairedCurrency.getCurrencyAbbreviation());
			minBalance = String.valueOf(qtyTraded * orders.getPrice());
		} else {
			/**
			 * fetching the market BTC price of buying currency
			 */

			/**
			 * 1 UNIT buying currency price in BTC Example 1 ETH = 0.0578560 BTC, this will
			 * update according to order selling book
			 */
			double buyingCurrencyValue = pairedCurrency.getPriceBTC();
			logger.debug("order value : {}, buyingCurrencyValue: {}", GenericUtils.getDecimalFormatString(qtyTraded),
					GenericUtils.getDecimalFormatString(buyingCurrencyValue));
			if (buyingCurrencyValue > 0) {
				/**
				 * user must have this balance to give market order, Example user want to BUY 3
				 * BTC on market price, at this moment 1 ETH = 0.0578560 BTC then for 3 BTC
				 * (3/0.0578560) BTC, then user must have 51.852876106 ETH to buy 3 BTC
				 */
				minBalance = String.valueOf(qtyTraded / buyingCurrencyValue);
			}
		}
		return minBalance;
	}

}
