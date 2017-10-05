/**
 * 
 */
package com.bolenum.constant;

/**
 * @author chandan kumar singh
 * @date 26-Sep-2017
 */
public class BTCUrlConstant {
	private BTCUrlConstant(){
		
	}
	public static final String BASE_URI_V1 = "http://127.0.0.1:8081/api/v1/";
	public static final String HOT_WALLET = BASE_URI_V1+"hotwallet/create";
	public static final String WALLET_ADDR = BASE_URI_V1+"hotwallet/primaryaddress";
	public static final String WALLET_BAL = BASE_URI_V1+"hotwallet/balance";
}
