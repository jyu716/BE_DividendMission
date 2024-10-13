package com.dividend.scraper;

import com.dividend.model.Company;
import com.dividend.model.Dividend;
import com.dividend.model.ScrapedResult;
import com.dividend.model.constants.Month;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class YahooFinanceScraper implements Scraper {

    private static final String STATISTICS_URL = "https://finance.yahoo.com/quote/%s/history/?filter=div&period1=%d&period2=%d&frequency=1d";
    private static final String SUMMARY_URL = "https://finance.yahoo.com/quote/%s?p=%s";
    private static final long START_TIME = 86400;

    @Override
    public ScrapedResult scrap(Company company) {
        var scrapResult = new ScrapedResult();
        scrapResult.setCompany(company);

        try {
            long now = System.currentTimeMillis() / 1000;
            String url = String.format(STATISTICS_URL, company.getTicker(), START_TIME, now);
            Connection connection = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");
            Document doc = connection.get();

            Elements parsingDivs = doc.getElementsByAttributeValue("data-testid", "history-table");
            Element tableEle = parsingDivs.get(0);

            Element tbody = tableEle.children().get(2); // table 내 tbody 가져오기.
            Elements tds = tbody.children().get(0).children().get(1).children();

            List<Dividend> dividends = new ArrayList<>();
            for (Element td : tds) {

                String text = td.text();

                String[] splits = text.split(" ");

                int month = Month.strToNumber(splits[0]);
                int day = Integer.parseInt(splits[1].replace(",",""));
                int year = Integer.parseInt(splits[2]);
                String dividend = splits[3];

                if(month < 0){
                    throw new RuntimeException("Unexpected Month enum value :: " + month);
                }

                dividends.add(new Dividend(LocalDateTime.of(year, month, day, 0, 0), dividend));

            }
            scrapResult.setDividends(dividends);



        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        return scrapResult;
    }

    @Override
    public Company scrapCompanyByTicker(String ticker){
        String url = String.format(SUMMARY_URL, ticker, ticker);

        try {
            Document document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36")
                            .get();

            Element titleEle =  document.select("h1.yf-xxbei9").first();
            String[] titleSplite = titleEle.text().split(" ");
            StringBuffer title = new StringBuffer();
            for(int i = 0; i < titleSplite.length-1; i++){
                title.append(titleSplite[i]);
                if(i != titleSplite.length-2){
                    title.append(" ");
                }
            }

            return new Company(ticker, title.toString());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
