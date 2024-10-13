package com.dividend.schaduler;

import com.dividend.model.Company;
import com.dividend.model.ScrapedResult;
import com.dividend.model.constants.CacheKey;
import com.dividend.persist.CompanyRepository;
import com.dividend.persist.DividendRepository;
import com.dividend.persist.entity.CompanyEntity;
import com.dividend.persist.entity.DividendEntity;
import com.dividend.scraper.Scraper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@EnableCaching
@AllArgsConstructor
public class ScraperScheduler {

    private final CompanyRepository companyRepository;
    private final DividendRepository dividendRepository;
    private final Scraper yahooFinanceScraper;

    @CacheEvict(value = CacheKey.KEY_FINANCE, allEntries = true) // redis 캐시 지우기
    @Scheduled(cron = "${scheduler.scrap.yahoo}")
    public void
    yahooFinanceScheduling(){
        log.info("Scraper scheduling started");

        // 저장된 회사 목록 조회
        List<CompanyEntity> companies = companyRepository.findAll();

        // 회사마다 배당금 정보 새로 스크래핑
        for(var company : companies){
            ScrapedResult scrapedResult = this.yahooFinanceScraper.scrap(new Company(company.getName(), company.getTicker()));

            //스크래핑한 배당금 정보 중 DB없는 값은 저장
            scrapedResult.getDividends().stream()
                    // Dividend 모델을 Dividend 엔티티로 맵핑
                    .map(e -> new DividendEntity(company.getId(), e))
                    // Elements를 하나씩 Repository에 삽입
                    .forEach( e -> {
                        boolean exists = this.dividendRepository.existsByCompanyIdAndDate(e.getCompanyId(), e.getDate());
                            if(!exists){
                                this.dividendRepository.save(e);
                            }
                        });

            // 연속적 스크래핑 대상 사이트 서버에 요청을 날리지 않도록 일시정지
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }


    }

}
