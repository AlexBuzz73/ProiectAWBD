package com.example.demo.services;

import com.example.demo.domain.ExchangeRate;
import com.example.demo.repositories.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

/**
 * Job zilnic care preia cursurile USD->RON si EUR->RON de la BNR
 * (https://www.bnr.ro/nbrfxrates.xml) si le salveaza in EXCHANGE_RATES.
 * Conform doc/flows/9-exchanges.md (sectiunea 13): se pastreaza un singur curs
 * per zi per pereche valutara, doar pentru USD->RON si EUR->RON.
 *
 * NOTA: nu am putut testa fetch-ul efectiv catre bnr.ro din acest mediu (acces retea
 * restrictionat la sandbox). Structura XML de mai jos e cea documentata public de BNR
 * (DataSet/Body/Cube[@date]/Rate[@currency][@multiplier]) - verifica un raspuns real
 * inainte sa consideri jobul gata de demo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateUpdateJob {

    private static final String BNR_URL = "https://www.bnr.ro/nbrfxrates.xml";
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("USD", "EUR");

    private final ExchangeRateRepository exchangeRateRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    // BNR publica de regula in jurul orei 13:00; rulam putin mai tarziu, sa fim siguri ca exista deja datele zilei.
    @Scheduled(cron = "0 0 14 * * *")
    public void updateExchangeRates() {
        try {
            String xml = restTemplate.getForObject(BNR_URL, String.class);
            if (xml == null) {
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
                double rawRate = Double.parseDouble(rateElement.getTextContent().trim());
                double rate = rawRate / multiplier;

                if (saveRateIfMissing(currency, "RON", rate, rateDate)) {
                    saved++;
                }
            }

            log.info("ExchangeRateUpdateJob: cursuri valutare BNR procesate pentru {} ({} curs/uri noi salvate).", rateDate, saved);
        } catch (Exception e) {
            log.error("ExchangeRateUpdateJob: eroare la actualizare - {}", e.getMessage(), e);
        }
    }

    private boolean saveRateIfMissing(String currencyFrom, String currencyTo, double rate, Date rateDate) {
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
        // dezactivam rezolvarea entitatilor externe (best practice de securitate la parsare XML)
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
