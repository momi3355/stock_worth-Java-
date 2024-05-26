import datetime
import json
import os
import time
import pykrx
from numpy import double
from pykrx import stock
from pykrx import bond
# import yfinance as yf
import pandas as pd
import exchange_calendars as ecals


def getTickers(date):
    tickers = stock.get_market_ticker_list(market='KOSPI')
    ticker_dict = dict()
    ticker_dict['lib_name'] = 'pykrx'
    ticker_dict['version'] = getVersion()
    ticker_dict['update_time'] = date  # 시:분:초 를 적어도 된다.
    ticker_dict['count'] = len(tickers)
    ticker_dict['data'] = tickers
    return json.dumps(ticker_dict, indent=3)


def update_tickerInfo(ticker, updateTime):
    info = stock.get_market_ohlcv(updateTime, updateTime, ticker, 'd')
    stock_info = dict()
    if not info.empty:
        stock_info['id'] = ticker
        stock_info['name'] = stock.get_market_ticker_name(ticker)
        stock_info['rate'] = round(info['등락률'].values[0], 2)
        stock_info['price'] = int(info['종가'].values[0])
        stock_info['volume'] = int(info['거래량'].values[0])
        # print(stock_info)
    return stock_info


def update_marketInfo(market, ticker_list):
    market_list = list()
    updateTime = getPreviousOpen('XKRX')
    for market_name in market:
        stock_list = list()
        market_dict = dict()
        market_dict['market_name'] = market_name
        market_dict['isRunMarket'] = isRunMarket('XKRX')

        for i, ticker in enumerate(ticker_list):
            stock_list.append(update_tickerInfo(ticker, updateTime))
            if i % 500 == 0:
                time.sleep(0.5)
        market_dict['stock_data'] = stock_list
        market_list.append(market_dict)
    return market_list


def getMarketInfo(date):
    tickers = stock.get_market_ticker_list(market='KOSPI')
    all_market_dict = dict()
    all_market_dict['lib_name'] = 'pykrx'
    all_market_dict['version'] = getVersion()
    all_market_dict['update_time'] = date  # 시:분:초 를 적어도 된다.
    all_market_dict['item_count'] = len(tickers)
    all_market_dict['data'] = update_marketInfo(['KOSPI'], tickers)
    return json.dumps(all_market_dict, ensure_ascii=False, indent=3)


def update_market(market, date):
    market_info_dict = dict()
    market_info_dict['lib_name'] = 'pykrx'
    market_info_dict['version'] = getVersion()
    market_info_dict['update_time'] = date  # 시:분:초 를 적어도 된다.
    market_info_dict['item_count'] = len(market)
    updateTime = getPreviousOpen('XKRX')
    market_list = list()
    for market_name in market:
        item = stock.get_index_price_change(updateTime, updateTime, market_name).iloc[0]

        market_dict = dict()
        market_dict['market_name'] = market_name
        market_dict['rate'] = round(item['등락률'], 2)
        market_dict['price'] = round(item['종가'], 2)
        market_list.append(market_dict)
    market_info_dict['data'] = market_list
    return json.dumps(market_info_dict, ensure_ascii=False, indent=3)


def getMarket(date):
    return update_market(['KOSPI', 'KOSDAQ'], date)


def isRunMarket(countryCode):
    now = datetime.datetime.now()
    cals = ecals.get_calendar(countryCode)  # 한국코드('XKRX')
    return cals.is_session(now.strftime('%Y-%m-%d'))


def getPreviousOpen(countryCode):
    now = datetime.datetime.now()
    cals = ecals.get_calendar(countryCode)  # 한국코드('XKRX')
    return cals.previous_open(now).strftime('%Y%m%d')  # 이전 개장일


def getVersion():
    return pykrx.__version__


def temp():
    return str(stock.get_index_price_change('20240508', '20240508', 'KOSPI').iloc[0]['종가'])


if __name__ == "__main__":
    now = datetime.datetime.now()
    data = now.strftime("%Y%m%d")
    print(getVersion())

    # ticker = yf.Ticker('005930.KS')
    # df_minute = ticker.history(interval='1d', start="2024-05-03", end=now.strftime("%Y-%m-%d"))
    # temp_df = pd.DataFrame(df_minute['Close'])

    # for i in range(10):
    # df = stock.get_market_ohlcv('20240503', now.strftime('%Y%m%d'), '005930', 'd')
    # temp_df = pd.DataFrame(df['종가'])
    # print(temp_df.to_json())
    # getMarketInfo(data)

    print(getMarket(data))
    if not isRunMarket('XKRX'):
        print(getPreviousOpen('XKRX'))
    # print(getMarket(now.strftime('%Y%m') + '08'))
    # print(temp())
