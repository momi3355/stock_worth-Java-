import datetime
import json
import os.path
import platform
import random
import time

import pykrx
from pykrx import stock
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

        for i, ticker in enumerate(ticker_list):
            stock_list.append(update_tickerInfo(ticker, updateTime))
            if i % 300 == 0:
                time.sleep(random.uniform(2, 4))  # 2 ~ 4s
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
    updateTime = getPreviousOpen('XKRX')  # TODO 이른 아침이면 전날로 표기요함.
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
    if now.time().hour < 9:  # 장시간 전
        now -= datetime.timedelta(days=1)
    # elif now.time().hour > 16:  # 장시간 후
    #     return False
    return cals.is_session(now.strftime('%Y-%m-%d'))


def getPreviousOpen(countryCode):
    now = datetime.datetime.now()
    cals = ecals.get_calendar(countryCode)  # 한국코드('XKRX')
    if now.time().hour < 9:  # 장시간 전
        now -= datetime.timedelta(days=1)
    return cals.previous_open(now).strftime('%Y%m%d')  # 이전 개장일


# def getPreviousOpen_count(countryCode, count):
#     open_list = []
#     now = datetime.datetime.now()
#     cals = ecals.get_calendar(countryCode)  # 한국코드('XKRX')
#     if now.time().hour < 9:  # 장시간 전
#         now -= datetime.timedelta(days=1)
#     open_list.append(cals.previous_open(now).strftime('%Y%m%d'))  # 이전 개장일
#     for i in range(count):
#         now = cals.previous_open(now)
#         open_list.append(cals.previous_open(now).strftime('%Y%m%d'))  # 이전 개장일
#     return open_list


def getTickerInfo(date1, date2, ticker_id):
    return stock.get_market_ohlcv(date1, date2, ticker_id, adjusted=False)


def getVersion():
    return pykrx.__version__


if __name__ == "__main__":
    if not isRunMarket('XKRX'):
        exit(300)
    now = datetime.datetime.now()
    date = now.strftime('%Y%m%d')
    data = dict()
    # ticker_data.json 이 데이터는 한달에 한 번 갱신
    data['ticker_data.json'] = getTickers(date)
    data['market_data.json'] = getMarket(date)
    time.sleep(random.uniform(5, 10))  # 5 ~ 10s
    data['stock_data.json'] = getMarketInfo(date)

    if platform.system() == 'Windows':  # 윈도우
        for item in data:
            print(item)  # 데이터 출력
    else:  # 리눅스 및 서버
        dire = '/var/www/html'
        if os.path.isdir(dire):
            for item in data.keys():
                file_name = dire + '/' + item
                if not os.path.isfile(file_name):
                    os.system("sudo touch {file_url}".format(file_url=file_name))  # 파일 생성
                os.system("sudo chmod 777 {file_url}".format(file_url=file_name))  # 권한 변경
                fp = open(file_name, 'w')
                fp.write(data[item])
                fp.close()
                os.system("sudo chmod 744 {file_url}".format(file_url=file_name))
    print('[' + date + '] 모든 주식정보가 저장되었습니다.')
