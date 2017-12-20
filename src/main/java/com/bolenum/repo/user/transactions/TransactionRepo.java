/**
 * 
 */
package com.bolenum.repo.user.transactions;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bolenum.enums.TransactionStatus;
import com.bolenum.enums.TransferStatus;
import com.bolenum.model.Transaction;
import com.bolenum.model.User;

/**
 * @author chandan kumar singh
 * @date 29-Sep-2017
 */
public interface TransactionRepo extends JpaRepository<Transaction, Serializable> {

	/**
	 * @description findByTxHash
	 * 
	 */
	Transaction findByTxHash(String txHash);

	Page<Transaction> findByFromUserAndTransactionStatus(User fromUser, TransactionStatus transactionStatus,
			Pageable pageable);

	@Query("select t from Transaction t where t.toUser=:toUser and (t.transactionStatus='WITHDRAW' or t.transactionStatus='DEPOSIT')")
	Page<Transaction> findByToUserAndTransactionStatusOrTransactionStatus(@Param("toUser") User toUser,
			Pageable pageable);
//	@Query("select t from Transaction t where t.transactionStatus=:transactionStatus and t.transferStatus=:transferStatus order by t.createdOn limit :size")
//	Transaction findByTransactionStatusAndTransferStatus(@Param("transactionStatus")TransactionStatus transactionStatus, @Param("transferStatus") TransferStatus transferStatus, @Param("size") int size);
	
	Transaction findFirstByTransactionStatusAndTransferStatusOrderByCreatedOnAsc(TransactionStatus transactionStatus, TransferStatus transferStatus);
	
	List<Transaction> findByToUserAndCurrencyNameAndTransactionStatusAndTransferStatus(User toUser, String currencyName, TransactionStatus transactionStatus, TransferStatus transferStatus);
	
//	@Query("select t from Transaction t where t.txStatus=:txStatus order by createdOn asc limit 1")
//	Transaction findByTxStatus(@Param("txStatus") String txStatus);

}
