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


def isRunMarket(countryCode):
    now = datetime.datetime.now()
    cals = ecals.get_calendar(countryCode)  # 한국코드('XKRX')
    return cals.is_session(now.strftime('%Y-%m-%d'))


def getPreviousOpen(countryCode):
    now = datetime.datetime.now()
    cals = ecals.get_calendar(countryCode)  # 한국코드('XKRX')
    if now.time().hour < 9:  # 장시간 전
        now -= datetime.timedelta(days=1)
    return cals.previous_open(now).strftime('%Y%m%d')  # 이전 개장일


def getPreviousOpen_count(countryCode, count):
    open_list = []
    now = datetime.datetime.now()
    cals = ecals.get_calendar(countryCode)  # 한국코드('XKRX')
    if now.time().hour < 9:  # 장시간 전
        now -= datetime.timedelta(days=1)
    open_list.append(cals.previous_open(now).strftime('%Y%m%d'))  # 이전 개장일
    for i in range(count):
        now = cals.previous_open(now)
        open_list.append(cals.previous_open(now).strftime('%Y%m%d'))  # 이전 개장일
    return open_list

def getMarketInfo(date, market_name):
    return stock.get_index_price_change(date, date, market_name).iloc[0]

def getTickerInfo(date1, date2, date_format, ticker_id):
    return stock.get_market_ohlcv(date1, date2, ticker_id, date_format, adjusted=False)


def getVersion():
    return pykrx.__version__


# pykrx를 작동하는지 확인하는 메소드
def temp():
    return str(stock.get_index_price_change('20240508', '20240508', 'KOSPI').iloc[0]['종가'])


if __name__ == "__main__":
    now = datetime.datetime.now()
    date = now.strftime("%Y%m%d")
    print(getVersion())

    # ticker = yf.Ticker('005930.KS')
    # df_minute = ticker.history(interval='1d', start="2024-05-03", end=now.strftime("%Y-%m-%d"))
    # temp_df = pd.DataFrame(df_minute['Close'])

    # for i in range(10):
    # df = stock.get_market_ohlcv('20240503', now.strftime('%Y%m%d'), '005930', 'd')
    # temp_df = pd.DataFrame(df['종가'])
    # print(temp_df.to_json())
    # getMarketInfo(data)

    # print(getMarket(date))
    # if not isRunMarket('XKRX'):
    #     print(getPreviousOpen('XKRX'))
    # print(update_market(['KOSPI', 'KOSDAQ'], date))
    # print(getMarket(now.strftime('%Y%m') + '08'))
    print(temp())

    print(getTickerInfo("20240731", "20240801", "d", "001040"))
