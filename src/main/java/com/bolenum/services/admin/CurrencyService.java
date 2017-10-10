package com.bolenum.services.admin;

import org.springframework.data.domain.Page;

import com.bolenum.dto.common.CurrencyForm;
import com.bolenum.model.Currency;
/**
 * 
 * @Author Himanshu
 * @Date 09-Oct-2017
 */
public interface CurrencyService {

	public Currency findByCurrencyName(String currencyName);

	public Currency saveCurrency(Currency currency);

	public Currency updateCurrency(CurrencyForm currencyForm, Currency isExistingCurrency);

	public Page<Currency> getCurrencyList(int pageNumber, int pageSize, String sortBy, String sortOrder,
			String searchData);

	public Currency findCurrencyById(Long currencyId);
}
