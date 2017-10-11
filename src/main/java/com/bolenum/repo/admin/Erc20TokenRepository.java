package com.bolenum.repo.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.bolenum.model.Erc20Token;

/**
 * 
 * @author Vishal Kumar
 * @date 04-Oct-2017
 *
 */
public interface Erc20TokenRepository extends JpaRepository<Erc20Token, Long> {

	/**
	 * 
	 * @param binarykey
	 * @return Erc20Token
	 */
	Erc20Token findByBinaryKey(String binarykey);
	
	/**
	 * 
	 * @param searchDate
	 * @param pageable
	 * @return Page<Erc20Token>
	 */
	Page<Erc20Token> findByContractAddressOrWalletAddressLike(String searchDate, Pageable pageable);
	
	/**
	 * 
	 * @param contractaddress
	 * @return Erc20Token
	 */
	Erc20Token findByContractAddress(String contractaddress);
}
