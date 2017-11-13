/**
 * 
 */
package com.bolenum.services.user.transactions;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.springframework.data.domain.Page;

import com.bolenum.enums.TransactionStatus;
import com.bolenum.model.Transaction;
import com.bolenum.model.User;

/**
 * @author chandan kumar singh
 * @date 29-Sep-2017
 */
public interface TransactionService {
	
	public Future<Boolean> performEthTransaction(User fromUser, String toAddress, Double amount,TransactionStatus transactionStatus);
	public Future<Boolean> performBtcTransaction(User fromUser, String toAddress, Double amount,TransactionStatus transactionStatus);
	
	public Future<Boolean> performTransaction(String currencyAbr, double qtyTraded, User buyer, User seller) throws InterruptedException, ExecutionException;
	public Page<Transaction> getListOfUserTransaction(User user, TransactionStatus withdraw, int pageNumber, int pageSize,
			String sortOrder, String sortBy);
	Future<Boolean> performErc20Transaction(User fromUser, String tokenName, String toAddress, Double amount,
			TransactionStatus transactionStatus) throws InterruptedException, ExecutionException;
}
