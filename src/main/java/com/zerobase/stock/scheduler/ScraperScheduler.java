package com.zerobase.stock.scheduler;

import com.zerobase.stock.model.Company;
import com.zerobase.stock.model.ScrapedResult;
import com.zerobase.stock.model.constants.CacheKey;
import com.zerobase.stock.persist.CompanyRepository;
import com.zerobase.stock.persist.DividendRepository;
import com.zerobase.stock.persist.entity.CompanyEntity;
import com.zerobase.stock.persist.entity.DividendEntity;
import com.zerobase.stock.scraper.Scraper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@AllArgsConstructor
@Component
@EnableCaching
public class ScraperScheduler {

    private final CompanyRepository companyRepository;
    private final DividendRepository dividendRepository;
    private final Scraper yahooFinanceScraper;

    @CacheEvict(value = CacheKey.KEY_FIANCE, allEntries = true)
    @Scheduled(cron = "${scheduler.scrap.yahoo}")
    public void yahooFinanceScheduling(){

        //저장된 회사 목록을 조회

        List<CompanyEntity> companies = this.companyRepository.findAll();

        //회사마다 배당금 정보를 새로 스크래핑

        for(var company : companies){
            log.info("scraping scheduler is started -> " + company.getName());
            ScrapedResult scrapedResult = this.yahooFinanceScraper
                    .scrap(new Company(company.getTicker(), company.getName()));

            //스크래핑한 배당금 정보 중 디비에 없는 값은 저장
            scrapedResult.getDividends().stream()
                    .map(e -> new DividendEntity(company.getId(), e))
                    .forEach(e -> {
                        boolean exists = this.dividendRepository.existsByCompanyIdAndDate(e.getCompanyId(), e.getDate());
                        if(!exists){
                            this.dividendRepository.save(e);
                            log.info("insert new dividend -> " + e.toString());
                        }
                    });

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }


    }
}
