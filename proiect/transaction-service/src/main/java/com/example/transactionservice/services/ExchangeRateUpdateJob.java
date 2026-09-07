package com.example.transactionservice.services;

import com.example.transactionservice.domain.ExchangeRate;
import com.example.transactionservice.repositories.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateUpdateJob {

    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("USD", "EUR");

    @Value("${bnr.url:https://www.bnr.ro/nbrfxrates.xml}")
    private String bnrUrl;

    private final ExchangeRateRepository exchangeRateRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void runOnStartup() {
        log.info("ExchangeRateUpdateJob: rulare la pornirea aplicatiei.");
        seedDefaultRatesIfMissing();
        updateExchangeRates();
    }

    @Scheduled(cron = "0 0 14 * * *")
    public void updateExchangeRates() {
        try {
            RestClient restClient = RestClient.builder().build();
            String xml = restClient.get().uri(bnrUrl).retrieve().body(String.class);
            if (xml == null || xml.isBlank()) {
                log.warn("ExchangeRateUpdateJob: raspuns gol de la BNR.");
                return;
            }

            Document document = parseXml(xml);
            NodeList cubeList = document.getElementsByTagName("Cube");
            if (cubeList.getLength() == 0) {
                log.warn("ExchangeRateUpdateJob: format XML neasteptat (lipseste elementul Cube).");
                return;
            }
            Element cube = (Element) cubeList.item(0);
            Date rateDate = parseRateDate(cube.getAttribute("date"));

            NodeList rateNodes = cube.getElementsByTagName("Rate");
            int saved = 0;
            for (int i = 0; i < rateNodes.getLength(); i++) {
                Element rateElement = (Element) rateNodes.item(i);
                String currency = rateElement.getAttribute("currency");

                if (!SUPPORTED_CURRENCIES.contains(currency)) {
                    continue;
                }

                int multiplier = rateElement.hasAttribute("multiplier")
                        ? Integer.parseInt(rateElement.getAttribute("multiplier"))
                        : 1;
                BigDecimal rawRate = new BigDecimal(rateElement.getTextContent().trim());
                BigDecimal rate = rawRate.divide(BigDecimal.valueOf(multiplier), 6, RoundingMode.HALF_UP);

                if (saveRateIfMissing(currency, "RON", rate, rateDate)) {
                    saved++;
                }
            }

            log.info("ExchangeRateUpdateJob: cursuri valutare BNR procesate pentru {} ({} curs/uri noi salvate).", rateDate, saved);
        } catch (Exception e) {
            log.warn("ExchangeRateUpdateJob: nu s-a putut actualiza cursul de la BNR ({}). Se utilizează cursurile existente/implicite.", e.getMessage());
        }
    }

    private void seedDefaultRatesIfMissing() {
        Date today = startOfToday();
        saveRateIfMissing("EUR", "RON", new BigDecimal("4.975000"), today);
        saveRateIfMissing("USD", "RON", new BigDecimal("4.550000"), today);
    }

    private boolean saveRateIfMissing(String currencyFrom, String currencyTo, BigDecimal rate, Date rateDate) {
        if (exchangeRateRepository.existsByCurrencyFromAndCurrencyToAndRateDate(currencyFrom, currencyTo, rateDate)) {
            return false;
        }

        ExchangeRate exchangeRate = new ExchangeRate();
        exchangeRate.setCurrencyFrom(currencyFrom);
        exchangeRate.setCurrencyTo(currencyTo);
        exchangeRate.setRate(rate);
        exchangeRate.setRateDate(rateDate);
        exchangeRate.setSource("BNR");
        exchangeRate.setCreatedAt(new Date());

        exchangeRateRepository.save(exchangeRate);
        return true;
    }

    private Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private Date parseRateDate(String dateAttribute) throws Exception {
        if (dateAttribute == null || dateAttribute.isBlank()) {
            return startOfToday();
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        return format.parse(dateAttribute);
    }

    private Date startOfToday() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }
}
