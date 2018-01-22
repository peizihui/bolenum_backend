package com.bolenum.controller.common;

import com.bolenum.constant.UrlConstant;
import com.bolenum.model.Currency;
import com.bolenum.services.admin.CurrencyService;
import com.bolenum.services.common.chart.ChartService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(value = UrlConstant.BASE_USER_URI_V1)
@Api(value = "Chart Controller")
public class ChartController {

    private static final Logger logger = LoggerFactory.getLogger(ChartController.class);
    @Autowired
    private ChartService chartService;

    @Autowired
    private CurrencyService currencyService;

    /**
     * to configure the chart
     */
    @RequestMapping(value = UrlConstant.CONFIG, method = RequestMethod.GET)
    public ResponseEntity<Object> configChart() {
        Map<String, Object> map = chartService.getChartConfig();
        return new ResponseEntity<>(map, HttpStatus.OK);
    }

    @RequestMapping(value = UrlConstant.SYMBOLE, method = RequestMethod.GET)
    public ResponseEntity<Object> chartSymboleInfo(@RequestParam("symbol") String symbol) {
        logger.debug("symbole: {}", symbol);
        Map<String, Object> map;
        if (symbol.contains("BE")) {
            String[] ids = symbol.split("BE");
            long marketId = Long.parseLong(ids[0]);
            long pairId = Long.parseLong(ids[1]);
            Currency marketCurrency = currencyService.findCurrencyById(marketId);
            Currency pairCurrency = currencyService.findCurrencyById(pairId);
            map = chartService.getSymbolInfo(marketCurrency, pairCurrency);
            return new ResponseEntity<>(map, HttpStatus.OK);
        }
        return new ResponseEntity<>(Optional.empty(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @RequestMapping(value = UrlConstant.HISTORY, method = RequestMethod.GET)
    public ResponseEntity<Object> chartHistoryInfo(@RequestParam("symbol") String symbol
            , @RequestParam("from") String fromDate, @RequestParam("to") String toDate, @RequestParam(name = "resolution", required = false, defaultValue = "60") String resolution) {
        Map<String, Object> map;
        logger.debug("history req, symbol: {}, from: {}, to: {}, resolution: {}", symbol, fromDate, toDate, resolution);
        if (symbol.contains("BE")) {
            String[] ids = symbol.split("BE");
            long marketId = Long.parseLong(ids[0]);
            long pairId = Long.parseLong(ids[1]);
            Currency marketCurrency = currencyService.findCurrencyById(marketId);
            Currency pairCurrency = currencyService.findCurrencyById(pairId);
            if (marketCurrency != null && pairCurrency != null) {
                map = chartService.getHistroyInfo(marketId, pairId, fromDate, toDate, resolution);
                return new ResponseEntity<>(map, HttpStatus.OK);
            } else {
                logger.error("market currency: {} , pair currency: {}", marketCurrency, pairCurrency);
            }
        }
        return new ResponseEntity<>(Optional.empty(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    //@RequestMapping(value = UrlConstant.HISTORY, method = RequestMethod.GET)
    ResponseEntity<Object> tradeChartHistory(@RequestParam String symbol, @RequestParam String from, @RequestParam String to, @RequestParam String resolution) {
        ///Map<String, Object> result = new HashMap<String,Object>();
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<Object> mapType = new TypeReference<Object>() {
        };
        InputStream is = TypeReference.class.getResourceAsStream("/json/tradeHistory.json");
        Object tradeHistoryList = new Object();
        try {
            tradeHistoryList = mapper.readValue(is, mapType);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //result.put("history", tradeHistoryList);
        return new ResponseEntity<>(tradeHistoryList, HttpStatus.OK);
    }

}