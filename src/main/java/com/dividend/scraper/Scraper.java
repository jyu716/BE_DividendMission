package com.dividend.scraper;

import com.dividend.model.Company;
import com.dividend.model.ScrapedResult;

public interface Scraper {

    ScrapedResult scrap (Company company);
    Company scrapCompanyByTicker(String ticker);
}
