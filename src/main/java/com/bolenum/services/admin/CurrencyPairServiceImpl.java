package com.bolenum.services.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.bolenum.model.Currency;
import com.bolenum.model.CurrencyPair;
import com.bolenum.repo.common.CurrencyPairRepo;
import com.bolenum.repo.common.CurrencyRepo;

/**
 * 
 * @Author Himanshu
 * @Date 09-Oct-2017
 */
@Service
public class CurrencyPairServiceImpl implements CurrencyPairService {

	@Autowired
	private CurrencyPairRepo currencyPairRepo;
	
	@Autowired
	private CurrencyRepo currencyRepo;
	
	@Override
	public CurrencyPair findByCurrencyPairName(String currencyPairName) {
		return currencyPairRepo.findByPairName(currencyPairName);
	}

	@Override
	public CurrencyPair saveCurrencyPair(CurrencyPair currencyPair) {
		currencyPair.setPairedCurrency(currencyPair.getToCurrency());
		return currencyPairRepo.saveAndFlush(currencyPair);
	}

	@Override
	public String createCurrencyPairName(Currency toCurrency, Currency pairedCurrency) {
		return toCurrency.getCurrencyAbbreviation() + "/" + pairedCurrency.getCurrencyAbbreviation();
	}

	@Override
	public CurrencyPair findByCurrencyPairNameByReverse(String currencyPairName) {
		String[] pairNameArray = currencyPairName.split("/");
		String pairNameByReverse = pairNameArray[1] + "/" + pairNameArray[0];
		return currencyPairRepo.findByPairName(pairNameByReverse);
	}

	@Override
	public Page<Currency> getCurrencyList(int pageNumber, int pageSize, String sortBy, String sortOrder,
			String searchData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean validCurrencyPair(CurrencyPair currencyPair) {
        Currency toCurrency=currencyRepo.findByCurrencyName(currencyPair.getToCurrency().get(0).getCurrencyName());
        Currency pairedCurrency=currencyRepo.findByCurrencyName(currencyPair.getPairedCurrency().get(0).getCurrencyName());
        Currency toCurrencyByAbbreviation=currencyRepo.findByCurrencyAbbreviation(currencyPair.getToCurrency().get(0).getCurrencyAbbreviation());
        Currency pairedCurrencyByAbbreviation=currencyRepo.findByCurrencyAbbreviation(currencyPair.getPairedCurrency().get(0).getCurrencyAbbreviation());
        
        if(toCurrency!=null && pairedCurrency!=null && toCurrencyByAbbreviation!=null && pairedCurrencyByAbbreviation!=null)
        {
        	return true;
        }
        return false;
	}

	// @Override
	// public Page<CurrencyP> getCurrencyPairList(int pageNumber, int pageSize,
	// String sortBy, String sortOrder,
	// String searchData) {
	// Direction sort;
	// if (sortOrder.equals("desc")) {
	// sort = Direction.DESC;
	// } else {
	// sort = Direction.ASC;
	// }
	// Pageable pageRequest = new PageRequest(pageNumber, pageSize, sort, sortBy);
	// return
	// currencyPairRepo.findByCurrencyNameOrCurrencyAbbreviationLike(searchData,
	// pageRequest);
	// }

}
