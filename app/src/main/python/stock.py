import datetime
import json
import os
import time
import pykrx
from numpy import double
from pykrx import stock
from pykrx import bond
import yfinance as yf
import pandas as pd
import exchange_calendars as ecals


def update_tickers(date):
    tickers = stock.get_market_ticker_list(market='KOSPI')
    ticker_dict = dict()
    ticker_dict['lib_name'] = 'pykrx'
    ticker_dict['version'] = getVersion()
    ticker_dict['update_time'] = date
    ticker_dict['count'] = len(tickers)
    ticker_dict['data'] = tickers
    with open('ticker_data.json', 'w+') as f:
        json.dump(ticker_dict, f, indent=3)
        print('ticker_data.json 파일이 생성 되었습니다.')


def getTickers(date):
    if not os.path.exists('ticker_data.json'):
        update_tickers(date)
    with open('ticker_data.json', 'r+') as f:
        ticker_json = json.load(f)
        return ticker_json['data']


def update_tickerInfo(ticker, updateTime, previous_open):
    info = stock.get_market_ohlcv(updateTime, updateTime, ticker, 'd')
    stock_info = dict()
    if not info.empty:
        stock_info['id'] = ticker
        stock_info['name'] = stock.get_market_ticker_name(ticker)
        stock_info['rate'] = round(info['등락률'].values[0], 2)
        stock_info['price'] = int(info['종가'].values[0])
        stock_info['volume'] = int(info['거래량'].values[0])
        print(stock_info)
    else:
        if previous_open != '0':  # date를 -1해준다
            return update_tickerInfo(ticker, previous_open, '0')
    return stock_info


def update_marketInfo(market, date):
    now = datetime.datetime.now()
    market_list = list()
    xcals_kr = ecals.get_calendar('XKRX')  # 한국코드
    previous_open = xcals_kr.previous_open(now).strftime('%Y-%m-%d')  # 이전 개장일
    isRunMarket = xcals_kr.is_session(now.strftime('%Y-%m-%d'))  # 지금 휴장인지 확인.
    for market_name in market:
        stock_list = list()
        market_dict = dict()
        market_dict['market_name'] = market_name
        market_dict['isRunMarket'] = isRunMarket

        for i, ticker in getTickers(date):
            updateTime = now.strftime('%Y%m%d')
            stock_list.append(update_tickerInfo(ticker, updateTime, previous_open))
            if i % 500 == 0:
                time.sleep(0.1)
        market_dict['stock_data'] = stock_list
        market_list.append(market_dict)
    getMarket(previous_open)
    return market_list


def update_allMarketInfo(date):
    all_market_dict = dict()
    all_market_dict['lib_name'] = 'pykrx'
    all_market_dict['version'] = getVersion()
    all_market_dict['update_time'] = date
    all_market_dict['item_count'] = len(getTickers(date))
    all_market_dict['data'] = update_marketInfo(['KOSPI'], date)
    with open('stock_data.json', 'w+') as f:
        json.dump(all_market_dict, f, ensure_ascii=False, indent=3)
        print('stock_data.json 파일이 생성 되었습니다.')


def getMarketInfo(date):
    if not os.path.exists('stock_data.json'):
        update_allMarketInfo(date)
    with open('stock_data.json', 'r+') as f:
        stock_json = json.load(f)
        return stock_json


def update_market(market, date):
    market_info_dict = dict()
    market_info_dict['lib_name'] = 'pykrx'
    market_info_dict['version'] = getVersion()
    market_info_dict['update_time'] = date
    market_info_dict['item_count'] = len(market)
    market_list = list()
    for market_name in market:
        item = stock.get_index_price_change(date, date, market_name).iloc[0]

        market_dict = dict()
        market_dict['market_name'] = market_name
        market_dict['rate'] = round(item['등락률'], 2)
        market_dict['price'] = round(item['종가'], 2)
        market_list.append(market_dict)
    market_info_dict['data'] = market_list
    with open('market_data.json', 'w+') as f:
        json.dump(market_info_dict, f, ensure_ascii=False, indent=3)
        print('market_data.json 파일이 생성 되었습니다.')


def getMarket(date):
    if not os.path.exists('market_data.json'):
        update_market(['KOSPI', 'KOSDAQ'], date)
    with open('market_data.json', 'r+') as f:
        stock_json = json.load(f)
        return stock_json


def getVersion():
    return pykrx.__version__


if __name__ == "__main__":
    now = datetime.datetime.now()
    data = now.strftime("%Y-%m-%d %H:%M:%S")
    print(getVersion())

    # ticker = yf.Ticker('005930.KS')
    # df_minute = ticker.history(interval='1d', start="2024-05-03", end=now.strftime("%Y-%m-%d"))
    # temp_df = pd.DataFrame(df_minute['Close'])

    # for i in range(10):
    # df = stock.get_market_ohlcv('20240503', now.strftime('%Y%m%d'), '005930', 'd')
    # temp_df = pd.DataFrame(df['종가'])
    # print(temp_df.to_json())
    getMarketInfo(data)
    getMarket(now.strftime('%Y%m%d'))
